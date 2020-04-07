/* Copyright 2016 Google LLC
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

import com.google.api.codegen.CollectionOneofProto;
import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** ResourceNameOneofConfig represents the configuration for a oneof set of resource names. */
@AutoValue
public abstract class ResourceNameOneofConfig implements ResourceNameConfig {

  @Override
  public abstract Name getEntityName();

  public abstract List<ResourceNameConfig> getResourceNameConfigs();

  @Override
  public abstract ProtoFile getAssignedProtoFile();

  @Override
  @Nullable
  public String getCommonResourceName() {
    return null;
  }

  /**
   * Returns a list of ResourceNamePatternConfigs, each of which is created from a pattern of this
   * multi-pattern resources.
   */
  public abstract List<ResourceNamePatternConfig> getPatterns();

  /** Returns true of any of its patterns is "*". */
  public boolean hasAnyResourceNamePattern() {
    return getPatterns().stream().anyMatch(p -> "*".equals(p));
  }

  /**
   * Returns a list of SingleResourceNameConfigs for this oneof config created from either
   * collections field in gapic v1 or deprecated_collections field in gapic v2.
   */
  public List<SingleResourceNameConfig> getSingleResourceNameConfigs() {
    return getResourceNameConfigs()
        .stream()
        .filter(c -> c.getResourceNameType() == ResourceNameType.SINGLE)
        .map(c -> (SingleResourceNameConfig) c)
        .collect(Collectors.toList());
  }

  /**
   * Returns a list of SingleResourceNameConfigs, each of which is created from a non-fixed pattern
   * of this multi-pattern resource.
   */
  public List<SingleResourceNameConfig> getPatternsAsSingleResourceNameConfigs() {
    return getPatterns()
        .stream()
        .filter(pttn -> !pttn.isFixedPattern())
        .map(ResourceNamePatternConfig::asSingleResourceNameConfig)
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Returns an optional SingleResourceNameConfig for the first pattern of the resource name as if
   * it is a single-pattern resource name. The purpose of this method is to generate
   * backward-compatible code when a single-pattern resource name becomes a multi-pattern one. The
   * returned optional is empty if the ResourceNameOneofConfig is created from gapic config.
   */
  public Optional<SingleResourceNameConfig> getFirstPatternAsSingleResourceNameConfig() {
    if (getPatterns().isEmpty()) {
      return Optional.<SingleResourceNameConfig>empty();
    }
    ResourceNamePatternConfig firstPattern = getPatterns().get(0);
    String entityId = getEntityId().replace("Oneof", "");
    SingleResourceNameConfig singleResourceName =
        SingleResourceNameConfig.newBuilder()
            .setNamePattern(firstPattern.getPattern())
            .setNameTemplate(firstPattern.getNameTemplate())
            .setEntityId(entityId)
            .setEntityName(Name.upperCamel(entityId))
            .setAssignedProtoFile(getAssignedProtoFile())
            .build();
    return Optional.of(singleResourceName);
  }

  @Nullable
  public static ResourceNameOneofConfig createResourceNameOneof(
      DiagCollector diagCollector,
      CollectionOneofProto collectionOneofProto,
      ImmutableMap<String, SingleResourceNameConfig> singleResourceNameConfigs,
      ImmutableMap<String, FixedResourceNameConfig> fixedResourceNameConfigs,
      ProtoFile file) {
    String oneofName = collectionOneofProto.getOneofName();
    if (singleResourceNameConfigs.containsKey(oneofName)) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "oneof_name \"" + oneofName + "\" already exists in collection configs"));
      return null;
    }
    List<ResourceNameConfig> configList = new ArrayList<>();
    boolean gotSingleResourceName = false;
    for (String entityName : collectionOneofProto.getCollectionNamesList()) {
      ResourceNameConfig resourceNameConfig = singleResourceNameConfigs.get(entityName);
      if (resourceNameConfig == null) {
        resourceNameConfig = fixedResourceNameConfigs.get(entityName);
      } else {
        gotSingleResourceName = true;
      }
      if (resourceNameConfig == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "entity_name \""
                    + entityName
                    + "\" in collection oneof \""
                    + oneofName
                    + "\" not found in collection configs"));
        return null;
      }
      configList.add(resourceNameConfig);
    }
    if (!gotSingleResourceName) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "At least one ResourceNameConfig with type SINGLE is required in oneof "
                  + oneofName));
      return null;
    }

    return new AutoValue_ResourceNameOneofConfig(
        oneofName,
        ResourceNameMessageConfig.entityNameToName(oneofName),
        configList,
        file,
        ImmutableList.of());
  }

  @Override
  public ResourceNameType getResourceNameType() {
    return ResourceNameType.ONEOF;
  }
}
