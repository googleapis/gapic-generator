/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.config;

import com.google.api.codegen.CollectionOneofProto;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** ResourceNameOneofConfig represents the configuration for a oneof set of resource names. */
@AutoValue
public abstract class ResourceNameOneofConfig implements ResourceNameConfig {

  @Override
  public abstract String getEntityName();

  public abstract List<ResourceNameConfig> getResourceNameConfigs();

  @Override
  public abstract ProtoFile getAssignedProtoFile();

  public Iterable<SingleResourceNameConfig> getSingleResourceNameConfigs() {
    return Iterables.filter(getResourceNameConfigs(), SingleResourceNameConfig.class);
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

    return new AutoValue_ResourceNameOneofConfig(oneofName, configList, file);
  }

  @Override
  public ResourceNameType getResourceNameType() {
    return ResourceNameType.ONEOF;
  }
}
