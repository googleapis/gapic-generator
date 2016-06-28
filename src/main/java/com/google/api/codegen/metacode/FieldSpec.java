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
public abstract class FieldSpec implements Spec {

  public static FieldSpec create(String name, Object subStructure) {
    return new AutoValue_FieldSpec(name, subStructure);
  }

  public abstract String getName();

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
          "Inconsistent structure: " + mergedTypeName + " encountered first, then field");
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> mergedMap = (Map<String, Object>) mergedStructure;

    if (mergedMap.containsKey(getName())) {
      Object mergedSubStructure = mergedMap.get(getName());
      Object newSubStructure = FieldStructureParser.merge(mergedSubStructure, getSubStructure());
      mergedMap.put(getName(), newSubStructure);
    } else {
      mergedMap.put(getName(), FieldStructureParser.populate(getSubStructure()));
    }
    return mergedStructure;
  }

  @Override
  public Object populate() {
    Map<String, Object> mergedMap = new HashMap<>();
    mergedMap.put(getName(), FieldStructureParser.populate(getSubStructure()));
    return mergedMap;
  }
}
