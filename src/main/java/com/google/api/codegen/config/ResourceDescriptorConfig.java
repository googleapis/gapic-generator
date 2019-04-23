/* Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.config;

import com.google.api.ResourceDescriptor;
import com.google.api.codegen.util.Name;
import com.google.api.pathtemplate.PathTemplate;
import com.google.api.pathtemplate.ValidationException;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AutoValue
public abstract class ResourceDescriptorConfig {

  public abstract String getUnifiedResourceType();

  public abstract ImmutableList<String> getPatterns();

  public abstract String getNameField();

  public abstract ResourceDescriptor.History getHistory();

  public abstract boolean getRequiresOneofConfig();

  public abstract String getSinglePattern();

  /**
   * Returns the proto file to which the resource name config has been assigned. This is required to
   * ensure that a consistent namespace can be calculated for the resource name.
   */
  public abstract ProtoFile getAssignedProtoFile();

  public abstract String getDerivedEntityName();

  public static ResourceDescriptorConfig from(
      ResourceDescriptor descriptor, ProtoFile assignedProtoFile) {
    // The logic for requiresOneofConfig and requiresSinglePattern is finicky, so let's lay out
    // the desired result for all possible combinations of History and number of patterns:
    // (history, patterns) -> (requiresOneofConfig, requiresSinglePattern)
    //
    // (HISTORY_UNSPECIFIED,       1)  -> (false, true)
    // (HISTORY_UNSPECIFIED,       2+) -> (true, false)
    // (ORIGINALLY_SINGLE_PATTERN, 1)  -> (false, true) !!! WARNING, very odd
    // (ORIGINALLY_SINGLE_PATTERN, 2+) -> (true, true)
    // (FUTURE_MULTI_PATTERN,      1)  -> (true, false)
    // (FUTURE_MULTI_PATTERN,      2+) -> (true, false) !!! WARNING, very odd

    boolean requiresOneofConfig =
        descriptor.getHistory() == ResourceDescriptor.History.FUTURE_MULTI_PATTERN
            || descriptor.getPatternList().size() > 1;
    boolean requiresSinglePattern =
        descriptor.getHistory() == ResourceDescriptor.History.ORIGINALLY_SINGLE_PATTERN
            || (descriptor.getHistory() == ResourceDescriptor.History.HISTORY_UNSPECIFIED
                && descriptor.getPatternList().size() == 1);

    String unqualifiedTypeName = getUnqualifiedTypeName(descriptor.getType());
    return new AutoValue_ResourceDescriptorConfig(
        descriptor.getType(),
        ImmutableList.copyOf(descriptor.getPatternList()),
        descriptor.getNameField(),
        descriptor.getHistory(),
        requiresOneofConfig,
        requiresSinglePattern ? descriptor.getPattern(0) : "",
        assignedProtoFile,
        requiresOneofConfig ? (unqualifiedTypeName + "Oneof") : unqualifiedTypeName);
  }

  public static String getUnqualifiedTypeName(String typeName) {
    return typeName.substring(typeName.lastIndexOf("/") + 1);
  }

  private String getUnqualifiedTypeName() {
    return getUnqualifiedTypeName(getUnifiedResourceType());
  }

  private static ArrayList<ResourceNameConfig> buildSingleResourceNameConfigs(
      List<String> patterns,
      Map<String, Name> nameMap,
      ProtoFile protoFile,
      DiagCollector diagCollector) {
    try {
      return patterns
          .stream()
          .map(
              (String p) ->
                  SingleResourceNameConfig.newBuilder()
                      .setNamePattern(p)
                      .setNameTemplate(PathTemplate.create(p))
                      .setAssignedProtoFile(protoFile)
                      .setEntityId(nameMap.get(p).toUpperCamel())
                      .setEntityName(nameMap.get(p))
                      .build())
          .collect(Collectors.toCollection(ArrayList::new));
    } catch (ValidationException e) {
      // Catch exception that may be thrown by PathTemplate.create
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL, e.getMessage()));
      return new ArrayList<>();
    }
  }

  public List<ResourceNameConfig> buildResourceNameConfigs(DiagCollector diagCollector) {
    HashMap<String, Name> entityNameMap = buildEntityNameMap(getPatterns());
    Name unqualifiedTypeName = Name.anyCamel(getUnqualifiedTypeName());
    for (String key : entityNameMap.keySet()) {
      if (key.equals(getSinglePattern())) {
        entityNameMap.put(key, unqualifiedTypeName);
      } else {
        entityNameMap.put(key, entityNameMap.get(key).join(unqualifiedTypeName));
      }
    }

    ArrayList<ResourceNameConfig> resourceNameConfigs =
        buildSingleResourceNameConfigs(
            getPatterns(), entityNameMap, getAssignedProtoFile(), diagCollector);

    if (getRequiresOneofConfig()) {
      String oneofId = getUnqualifiedTypeName() + "Oneof";
      resourceNameConfigs.add(
          new AutoValue_ResourceNameOneofConfig(
              oneofId,
              Name.anyCamel(oneofId),
              ImmutableList.copyOf(resourceNameConfigs),
              getAssignedProtoFile()));
    }
    return resourceNameConfigs;
  }

  public List<ResourceNameConfig> buildParentResourceNameConfigs(DiagCollector diagCollector) {
    List<String> parentPatterns = getParentPatterns();
    HashMap<String, Name> entityNameMap = buildEntityNameMap(parentPatterns);
    ArrayList<ResourceNameConfig> resourceNameConfigs =
        buildSingleResourceNameConfigs(
            parentPatterns, entityNameMap, getAssignedProtoFile(), diagCollector);

    if (parentPatterns.size() > 1) {
      String oneofId = "ParentOneof";
      resourceNameConfigs.add(
          new AutoValue_ResourceNameOneofConfig(
              oneofId,
              Name.anyCamel(oneofId),
              ImmutableList.copyOf(resourceNameConfigs),
              getAssignedProtoFile()));
    }
    return resourceNameConfigs;
  }

  public String getDerivedParentEntityName() {
    List<String> parentPatterns = getParentPatterns();
    if (parentPatterns.size() == 0) {
      throw new IllegalArgumentException(
          String.format(
              "Unexpected error - size of getParentPatterns is zero. patterns: [%s]",
              String.join(", ", getPatterns())));
    }
    if (parentPatterns.size() > 1) {
      return "ParentOneof";
    } else {
      List<String> segments = getSegments(parentPatterns.get(0));
      if (segments.size() == 0) {
        throw new IllegalArgumentException(
            String.format(
                "Unexpected error - size of segments is zero. pattern: %s", parentPatterns.get(0)));
      }
      String lastSegment = segments.get(segments.size() - 1);
      if (isVariableBinding(lastSegment)) {
        return Name.from(unwrapVariableSegment(lastSegment)).toUpperCamel();
      } else {
        return Name.anyCamel(lastSegment).toUpperCamel();
      }
    }
  }

  public List<String> getParentPatterns() {
    return getPatterns()
        .stream()
        .map(ResourceDescriptorConfig::getParentPattern)
        .distinct()
        .collect(Collectors.toList());
  }

  public static String getParentPattern(String pattern) {
    List<String> segments = getSegments(pattern);
    int index = segments.size() - 2;
    while (index >= 0 && !isVariableBinding(segments.get(index))) {
      index--;
    }
    index++;
    if (index <= 0) {
      return "";
    }
    return String.join("/", segments.subList(0, index));
  }

  private static List<String> getSegments(String pattern) {
    return ImmutableList.copyOf(pattern.split("/"));
  }

  private static boolean isVariableBinding(String segment) {
    return segment.startsWith("{") && segment.endsWith("}");
  }

  private static String unwrapVariableSegment(String segment) {
    return segment.substring(1, segment.length() - 1);
  }

  private static HashMap<String, Name> buildEntityNameMap(List<String> patterns) {
    TrieNode trie = new TrieNode();
    Map<String, List<String>> patternsToSegmentsMap =
        patterns
            .stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    (String p) ->
                        Lists.reverse(
                            getSegments(p)
                                .stream()
                                .filter(ResourceDescriptorConfig::isVariableBinding)
                                .map(ResourceDescriptorConfig::unwrapVariableSegment)
                                .collect(Collectors.toList()))));
    for (List<String> segments : patternsToSegmentsMap.values()) {
      insertSegmentsIntoTrie(segments, trie);
    }

    HashMap<String, Name> nameMap = new HashMap<>();
    for (String pattern : patternsToSegmentsMap.keySet()) {
      List<String> identifyingNamePieces = new ArrayList<>();
      TrieNode node = trie;
      for (String segment : patternsToSegmentsMap.get(pattern)) {
        if (node.size() > 1) {
          identifyingNamePieces.add(segment);
        }
        node = node.get(segment);
      }
      Name entityName = Name.from(Lists.reverse(identifyingNamePieces).toArray(new String[0]));
      nameMap.put(pattern, entityName);
    }
    return nameMap;
  }

  private static class TrieNode extends HashMap<String, TrieNode> {}

  private static void insertSegmentsIntoTrie(List<String> segments, TrieNode trieNode) {
    for (String segment : segments) {
      if (!trieNode.containsKey(segment)) {
        trieNode.put(segment, new TrieNode());
      }
      trieNode = trieNode.get(segment);
    }
  }
}
