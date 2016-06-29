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
package com.google.api.codegen.metacode;

import com.google.auto.value.AutoValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class MapElementSpec implements PathSpec {

  public static MapElementSpec create(String key, Object subStructure) {
    return new AutoValue_MapElementSpec(key, subStructure);
  }

  public abstract String getKey();

  public abstract Object getSubStructure();

  @Override
  public Object merge(Object mergedStructure) {
    if (mergedStructure instanceof InitValueConfig) {
      InitValueConfig metadata = (InitValueConfig) mergedStructure;
      if (!metadata.isEmpty()) {
        throw new IllegalArgumentException(
            "Inconsistent: found both substructure and initialization metadata");
      }
      // we encountered a partially-specified structure, so replace it with a
      // map
      mergedStructure = new HashMap<>();
    } else if (!(mergedStructure instanceof Map)) {
      String mergedTypeName = mergedStructure.getClass().getName();
      if (mergedStructure instanceof List) {
        mergedTypeName = "list";
      }
      throw new IllegalArgumentException(
          "Inconsistent structure: " + mergedTypeName + " encountered first, then map");
    }

    @SuppressWarnings("unchecked")
    HashMap<String, Object> mergedMap = (HashMap<String, Object>) mergedStructure;

    if (mergedMap.containsKey(getKey())) {
      Object mergedSubStructure = mergedMap.get(getKey());
      Object newSubStructure = FieldStructureParser.merge(mergedSubStructure, getSubStructure());
      mergedMap.put(getKey(), newSubStructure);
    } else {
      mergedMap.put(getKey(), FieldStructureParser.populate(getSubStructure()));
    }
    return mergedStructure;
  }

  @Override
  public Object populate() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put(getKey(), FieldStructureParser.populate(getSubStructure()));
    return map;
  }
}
