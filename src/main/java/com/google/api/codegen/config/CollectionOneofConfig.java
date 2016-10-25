/* Copyright 2016 Google Inc
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
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** CollectionOneofConfig represents the configuration for a oneof set of resource names. */
@AutoValue
public abstract class CollectionOneofConfig {

  public abstract String getOneofName();

  public abstract List<CollectionConfig> getCollectionConfigs();

  @Nullable
  public static CollectionOneofConfig createCollectionOneof(
      DiagCollector diagCollector,
      CollectionOneofProto collectionOneofProto,
      ImmutableMap<String, CollectionConfig> collectionConfigs) {
    String oneofName = collectionOneofProto.getOneofName();
    if (collectionConfigs.containsKey(oneofName)) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "oneof_name \"" + oneofName + "\" already exists in collection configs"));
      return null;
    }
    List<CollectionConfig> configList = new ArrayList<>();
    for (String entityName : collectionOneofProto.getCollectionNamesList()) {
      if (!collectionConfigs.containsKey(entityName)) {
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
      configList.add(collectionConfigs.get(entityName));
    }

    return new AutoValue_CollectionOneofConfig(oneofName, configList);
  }
}
