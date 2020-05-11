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
import com.google.api.codegen.DeprecatedCollectionConfigProto;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that represents a google.api.ResourceDescriptor annotation, and is used to construct
 * ResourceNameConfig objects.
 */
@AutoValue
public abstract class ResourceDescriptorConfig {

  /** Whether this resource is defined at message level. */
  public abstract boolean isDefinedAtMessageLevel();

  /** The unified resource type, taken from the annotation. */
  public abstract String getUnifiedResourceType();

  /** List of resource patterns, taken from the annotation. */
  public abstract ImmutableList<String> getPatterns();

  /** The name field taken from the annotation. */
  public abstract String getNameField();

  /** The history field taken from the annotation. */
  public abstract ResourceDescriptor.History getHistory();

  /**
   * Boolean for whether this resource should be represented in client libraries by a Oneof object.
   */
  public abstract boolean getRequiresOneofConfig();

  /**
   * Pattern for a single resource that will be treated differently for the purposes of entity
   * naming. This pattern will also exist in getPatterns. If there is no single resource, will be
   * "".
   */
  public abstract String getSinglePattern();

  /**
   * Returns the proto file to which the resource name config has been assigned. This is required to
   * ensure that a consistent namespace can be calculated for the resource name.
   */
  public abstract ProtoFile getAssignedProtoFile();

  /** The entity name for the resource config. */
  public abstract String getDerivedEntityName();

  public static ResourceDescriptorConfig getWildcardResource(ProtoFile protoFile) {
    return new AutoValue_ResourceDescriptorConfig(
        /* isDefinedAtMessageLevel= */ false,
        /* unifiedResourceType= */ "",
        /*patterns=*/ ImmutableList.of("*"),
        /*nameField=*/ "",
        /* history= */ ResourceDescriptor.History.HISTORY_UNSPECIFIED,
        /* requiresOneofConfig= */ false,
        /* singlePattern= */ "*",
        protoFile,
        /* derivedEntityName= */ "");
  }

  public static ResourceDescriptorConfig from(
      ResourceDescriptor descriptor, ProtoFile assignedProtoFile, boolean isDefinedAtMessageLevel) {
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
        isDefinedAtMessageLevel,
        descriptor.getType(),
        ImmutableList.copyOf(descriptor.getPatternList()),
        descriptor.getNameField(),
        descriptor.getHistory(),
        requiresOneofConfig,
        requiresSinglePattern ? descriptor.getPattern(0) : "",
        assignedProtoFile,
        requiresOneofConfig ? (unqualifiedTypeName + "Oneof") : unqualifiedTypeName);
  }

  static String getUnqualifiedTypeName(String typeName) {
    return typeName.substring(typeName.lastIndexOf("/") + 1);
  }

  private String getUnqualifiedTypeName() {
    return getUnqualifiedTypeName(getUnifiedResourceType());
  }

  /** Package-private for use in GapicProductConfig. */
  Map<String, ResourceNameConfig> buildResourceNameConfigs(
      DiagCollector diagCollector,
      Map<String, SingleResourceNameConfig> configOverrides,
      Map<String, DeprecatedCollectionConfigProto> deprecatedPatternResourceMap,
      TargetLanguage language) {

    Name unqualifiedTypeName = Name.anyCamel(getUnqualifiedTypeName());
    Preconditions.checkArgument(
        getPatterns().size() > 0, "Resource %s has no patterns.", getUnifiedResourceType());

    // Single-pattern resource.
    if (getPatterns().size() == 1) {
      String pattern = getPatterns().get(0);

      if ("*".equals(pattern)) {
        return Collections.singletonMap(getUnqualifiedTypeName(), AnyResourceNameConfig.instance());
      }

      return Collections.singletonMap(
          getUnqualifiedTypeName(),
          SingleResourceNameConfig.createSingleResourceNameWithOverride(
              diagCollector,
              getUnqualifiedTypeName(),
              getPatterns().get(0),
              configOverrides.get(
                  unqualifiedTypeName.toLowerUnderscore()))); // entity names in gapic config are in
      // lower_underscore case.
    }
    ImmutableMap.Builder<String, ResourceNameConfig> resourceNameConfigs = ImmutableMap.builder();

    // Multi-pattern resource.
    for (String pattern : getPatterns()) {
      DeprecatedCollectionConfigProto deprecatedResourceProto =
          deprecatedPatternResourceMap.get(pattern);
      if (deprecatedResourceProto == null) {
        continue;
      }
      SingleResourceNameConfig deprecatedSingleResource =
          SingleResourceNameConfig.createDeprecatedSingleResourceName(
              diagCollector, deprecatedResourceProto, getAssignedProtoFile(), language);
      resourceNameConfigs.put(deprecatedSingleResource.getEntityId(), deprecatedSingleResource);
    }
    String oneOfId = getUnqualifiedTypeName() + "Oneof";
    ResourceNameOneofConfig oneofConfig =
        new AutoValue_ResourceNameOneofConfig(
            oneOfId,
            Name.anyCamel(oneOfId),
            ImmutableList.copyOf(resourceNameConfigs.build().values()),
            getAssignedProtoFile(),
            getPatterns()
                .stream()
                .map(ResourceNamePatternConfig::new)
                .collect(ImmutableList.toImmutableList()));
    resourceNameConfigs.put(oneOfId, oneofConfig);
    return resourceNameConfigs.build();
  }

  /**
   * Returns a map from resource name patterns to resources.
   *
   * <p>Package private for use in GapicProductConfig.
   */
  static Map<String, List<ResourceDescriptorConfig>> getPatternResourceMap(
      Collection<ResourceDescriptorConfig> resourceDescriptors) {
    // Create a pattern-to-resource map to make looking up parent resources easier.
    Map<String, List<ResourceDescriptorConfig>> patternResourceDescriptorMap = new HashMap<>();
    for (ResourceDescriptorConfig resourceDescriptor : resourceDescriptors) {
      for (String pattern : resourceDescriptor.getPatterns()) {
        List<ResourceDescriptorConfig> resources = patternResourceDescriptorMap.get(pattern);
        if (resources == null) {
          resources = new ArrayList<>();
          patternResourceDescriptorMap.put(pattern, resources);
        }
        resources.add(resourceDescriptor);
      }
    }
    return patternResourceDescriptorMap
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                entry -> entry.getKey(), entry -> ImmutableList.copyOf(entry.getValue())));
  }

  /**
   * Returns a map from unified resource types to parent resources.
   *
   * <p>We consider the list of resources to be another resource Bar's parents if the union of all
   * patterns in the list have one-to-one parent-child mapping with Bar's patterns.
   *
   * <p>Package private for use in GapicProductConfig.
   */
  static Map<String, List<ResourceDescriptorConfig>> getChildParentResourceMap(
      Map<String, ResourceDescriptorConfig> descriptorConfigMap,
      Map<String, List<ResourceDescriptorConfig>> patternResourceDescriptorMap) {
    ImmutableMap.Builder<String, List<ResourceDescriptorConfig>> builder = ImmutableMap.builder();
    List<ResourceDescriptorConfig> allResources =
        ImmutableList.copyOf(descriptorConfigMap.values());
    for (Map.Entry<String, ResourceDescriptorConfig> entry : descriptorConfigMap.entrySet()) {
      ResourceDescriptorConfig childResource = entry.getValue();
      System.out.println(
          "DEL: Looking at entry "
              + entry.getKey()
              + " : "
              + entry.getValue()
              + ", getparent: "
              + getParentPatternsMap(childResource));
      for (int i = 0; i < allResources.size(); i++) {
        if (childResource.getPatterns().contains("*")) {
          System.out.println("DEL: Contains pattern wildcard");
          System.out.println(
              "DEL: Will use wildcard pattern "
                  + getWildcardResource(childResource.getAssignedProtoFile()));
          builder.put(
              entry.getKey(),
              Arrays.asList(getWildcardResource(childResource.getAssignedProtoFile())));
          break;
        }

        List<ResourceDescriptorConfig> parentResource =
            matchParentResourceDescriptor(
                getParentPatternsMap(childResource),
                allResources,
                new ArrayList<>(),
                childResource.getPatterns().size(),
                i);
        if (parentResource != null) {
          builder.put(entry.getKey(), parentResource);
          break;
        }
      }
    }

    Map<String, List<ResourceDescriptorConfig>> result = builder.build();
    return result;
  }

  private static List<ResourceDescriptorConfig> matchParentResourceDescriptor(
      Map<String, Boolean> parentPatterns,
      List<ResourceDescriptorConfig> allResources,
      List<ResourceDescriptorConfig> matchedParentResources,
      int unmatchedPatternsCount,
      int i) {
    // We make a copy to advance in the depth-first search. There won't be
    // too many patterns in a resource so performance-wise it is not a problem.
    Map<String, Boolean> parentPatternsCopy = new HashMap<>(parentPatterns);
    ResourceDescriptorConfig matchingResource = allResources.get(i);
    for (String pattern : matchingResource.getPatterns()) {
      if (!parentPatternsCopy.containsKey(pattern)) {
        return null;
      }
      if (parentPatternsCopy.get(pattern) == false) {
        unmatchedPatternsCount--;
        parentPatternsCopy.put(pattern, true);
      }
    }
    matchedParentResources.add(matchingResource);
    if (unmatchedPatternsCount == 0) {
      return ImmutableList.copyOf(matchedParentResources);
    }
    for (int j = i + 1; j < allResources.size(); j++) {
      List<ResourceDescriptorConfig> result =
          matchParentResourceDescriptor(
              parentPatternsCopy, allResources, matchedParentResources, unmatchedPatternsCount, j);

      // We stop when we find the first list of matched parent resources
      if (result != null) {
        return result;
      }
    }
    matchedParentResources.remove(matchedParentResources.size() - 1);
    return null;
  }

  @VisibleForTesting
  static String getParentPattern(String pattern) {
    Preconditions.checkArgument(!pattern.equals(""), "resource pattern can't be an empty string.");
    if (pattern.equals("*")) {
      return "*";
    }

    List<String> segments = getSegments(pattern);
    int index = segments.size() - 1;
    if (isVariableBinding(segments.get(index))) {
      index -= 2;
    } else {
      index--;
    }

    Preconditions.checkArgument(
        index >= -1, "malformatted pattern, can't calculate parent: %s", pattern);
    return String.join("/", segments.subList(0, index + 1));
  }

  private static Map<String, Boolean> getParentPatternsMap(ResourceDescriptorConfig resource) {
    return resource
        .getPatterns()
        .stream()
        .map(ResourceDescriptorConfig::getParentPattern)
        .filter(p -> !p.isEmpty())
        .collect(ImmutableMap.toImmutableMap(p -> p, p -> false));
  }

  private static List<String> getSegments(String pattern) {
    return ImmutableList.copyOf(pattern.split("/"));
  }

  private static boolean isVariableBinding(String segment) {
    return segment.startsWith("{") && segment.endsWith("}");
  }
}
