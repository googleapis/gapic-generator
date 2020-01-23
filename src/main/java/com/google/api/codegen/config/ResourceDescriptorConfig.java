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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

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
            getAssignedProtoFile());
    resourceNameConfigs.put(oneOfId, oneofConfig);
    return resourceNameConfigs.build();
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
    for (Map.Entry<String, ResourceDescriptorConfig> entry : descriptorConfigMap.entrySet()) {
      List<ResourceDescriptorConfig> parentResource =
          getParentResourceDescriptor(entry.getValue(), patternResourceDescriptorMap);
      if (!parentResource.isEmpty()) {
        builder.put(entry.getKey(), parentResource);
      }
    }
    return builder.build();
  }

  @Nullable
  private static List<ResourceDescriptorConfig> getParentResourceDescriptor(
      ResourceDescriptorConfig childResource,
      Map<String, List<ResourceDescriptorConfig>> patternResourceDescriptorMap) {
    List<ResourceDescriptorConfig> resources =
        patternResourceDescriptorMap
            .values()
            .stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());

    ImmutableList.Builder<ResourceDescriptorConfig> parentResources = ImmutableList.builder();

    Map<String, Boolean> matchedParentPatterns =
        childResource
            .getPatterns()
            .stream()
            .map(ResourceDescriptorConfig::getParentPattern)
            .distinct()
            .collect(Collectors.toMap(p -> p, p -> false));
    int unmatchedPatternsCount = matchedParentPatterns.size();

    for (ResourceDescriptorConfig resource : resources) {
      for (String pattern : resource.getPatterns()) {
        Boolean matched = matchedParentPatterns.get(pattern);
        if (matched == null) {
          continue;
        }
        if (matched == false) {
          unmatchedPatternsCount -= 1;
        }
        matchedParentPatterns.put(pattern, true);
      }
      parentResources.add(resource);
    }

    if (unmatchedPatternsCount == 0) {
      return parentResources.build();
    }
    return Collections.emptyList();
  }

  @VisibleForTesting
  static String getParentPattern(String pattern) {
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
}
