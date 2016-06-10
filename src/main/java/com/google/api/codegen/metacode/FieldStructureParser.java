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

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FieldStructureParser parses a dotted path specification into a map of String to Map, List, or
 * InitValueConfig.
 */
public class FieldStructureParser {
  // example: "messages[0]"
  private static Pattern fieldListPattern = Pattern.compile("([^\\[]+)\\[([^\\]]+)\\]");

  /**
   * Parses a dotted path specification into a map of String to Map, List, or InitValueConfig.
   */
  public static Map<String, Object> parseFields(Collection<String> fieldSpecs) {
    return parseFields(fieldSpecs, ImmutableMap.<String, InitValueConfig>of());
  }

  /**
   * Parses a dotted path specification into a map of String to Map, List, or InitValueConfig, and
   * also sets InitValueConfig on fields that match the paths in initValueConfigMap.
   */
  public static Map<String, Object> parseFields(
      Collection<String> fieldSpecs, ImmutableMap<String, InitValueConfig> initValueConfigMap) {
    List<Object> unmergedFields = parseFieldList(fieldSpecs, initValueConfigMap);
    return mergeFieldList(unmergedFields);
  }

  private static List<Object> parseFieldList(
      Collection<String> fieldSpecs, ImmutableMap<String, InitValueConfig> initValueConfigMap) {
    List<Object> unmergedFields = new ArrayList<>();
    for (String fieldSpec : fieldSpecs) {
      Object topLevel = InitValueConfig.create();
      if (initValueConfigMap.containsKey(fieldSpec)) {
        topLevel = initValueConfigMap.get(fieldSpec);
      }
      String[] stringParts = fieldSpec.split("[.]");
      for (int i = stringParts.length - 1; i >= 0; i--) {
        Matcher listMatcher = fieldListPattern.matcher(stringParts[i]);
        if (listMatcher.matches()) {
          String name = listMatcher.group(1);
          String index = listMatcher.group(2);
          ListElementSpec listStructure = new ListElementSpec(index, topLevel);
          topLevel = new FieldSpec(name, listStructure);
          // TODO also support maps - use {key} notation
        } else {
          String name = stringParts[i];
          topLevel = new FieldSpec(name, topLevel);
        }
      }
      unmergedFields.add(topLevel);
    }
    return unmergedFields;
  }

  private static Map<String, Object> mergeFieldList(List<Object> unmergedFields) {
    Map<String, Object> mergedFields = new HashMap<>();
    for (Object field : unmergedFields) {
      merge(mergedFields, field);
    }
    return mergedFields;
  }

  private static Object merge(Object mergedStructure, Object unmergedStructure) {
    if (unmergedStructure instanceof FieldSpec) {
      FieldSpec fieldSpec = (FieldSpec) unmergedStructure;
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

      if (mergedMap.containsKey(fieldSpec.name)) {
        Object mergedSubStructure = mergedMap.get(fieldSpec.name);
        Object newSubStructure = merge(mergedSubStructure, fieldSpec.subStructure);
        mergedMap.put(fieldSpec.name, newSubStructure);
      } else {
        mergedMap.put(fieldSpec.name, populate(fieldSpec.subStructure));
      }
    } else if (unmergedStructure instanceof ListElementSpec) {
      ListElementSpec listElementSpec = (ListElementSpec) unmergedStructure;
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

      int index = Integer.valueOf(listElementSpec.index);
      if (index < mergedList.size()) {
        Object mergedSubStructure = mergedList.get(index);
        Object newSubStructure = merge(mergedSubStructure, listElementSpec.subStructure);
        mergedList.set(index, newSubStructure);
      } else if (index == mergedList.size()) {
        mergedList.add(populate(listElementSpec.subStructure));
      } else {
        throw new IllegalArgumentException(
            "Index leaves gap: last index = "
                + (mergedList.size() - 1)
                + ", this index = "
                + index);
      }
    } else {
      throw new IllegalArgumentException(
          "merge: didn't expect " + unmergedStructure.getClass().getName());
    }
    return mergedStructure;
  }

  private static Object populate(Object unmergedStructure) {
    if (unmergedStructure instanceof FieldSpec) {
      FieldSpec fieldSpec = (FieldSpec) unmergedStructure;
      Map<String, Object> mergedMap = new HashMap<>();
      mergedMap.put(fieldSpec.name, populate(fieldSpec.subStructure));
      return mergedMap;
    } else if (unmergedStructure instanceof ListElementSpec) {
      ListElementSpec listElementSpec = (ListElementSpec) unmergedStructure;
      int index = Integer.valueOf(listElementSpec.index);
      if (index != 0) {
        throw new IllegalArgumentException("First element in list must have index 0");
      }
      List<Object> list = new ArrayList<>();
      list.add(populate(listElementSpec.subStructure));
      return list;
    } else {
      return unmergedStructure;
    }
  }

  private static class FieldSpec {
    public String name;
    public Object subStructure;

    public FieldSpec(String name, Object subStructure) {
      this.name = name;
      this.subStructure = subStructure;
    }
  }

  private static class ListElementSpec {
    public String index;
    public Object subStructure;

    public ListElementSpec(String index, Object subStructure) {
      this.index = index;
      this.subStructure = subStructure;
    }
  }
}
