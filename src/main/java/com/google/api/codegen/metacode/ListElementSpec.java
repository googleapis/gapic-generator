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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class ListElementSpec implements PathSpec {

  public static ListElementSpec create(String index, Object subStructure) {
    return new AutoValue_ListElementSpec(index, subStructure);
  }

  public abstract String getIndex();

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
      // list
      mergedStructure = new ArrayList<>();
    } else if (!(mergedStructure instanceof List)) {
      String mergedTypeName = mergedStructure.getClass().getName();
      if (mergedStructure instanceof Map) {
        mergedTypeName = "field";
      }
      throw new IllegalArgumentException(
          "Inconsistent structure: " + mergedTypeName + " encountered first, then list");
    }

    @SuppressWarnings("unchecked")
    List<Object> mergedList = (List<Object>) mergedStructure;

    int index = Integer.valueOf(getIndex());
    if (index < mergedList.size()) {
      Object mergedSubStructure = mergedList.get(index);
      Object newSubStructure = FieldStructureParser.merge(mergedSubStructure, getSubStructure());
      mergedList.set(index, newSubStructure);
    } else if (index == mergedList.size()) {
      mergedList.add(FieldStructureParser.populate(getSubStructure()));
    } else {
      throw new IllegalArgumentException(
          "Index leaves gap: last index = " + (mergedList.size() - 1) + ", this index = " + index);
    }
    return mergedStructure;
  }

  @Override
  public Object populate() {
    int index = Integer.valueOf(getIndex());
    if (index != 0) {
      throw new IllegalArgumentException("First element in list must have index 0");
    }
    List<Object> list = new ArrayList<>();
    list.add(FieldStructureParser.populate(getSubStructure()));
    return list;
  }
}
