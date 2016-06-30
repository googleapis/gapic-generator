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

import com.google.common.annotations.VisibleForTesting;
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

  private static Pattern fieldStructurePattern = Pattern.compile("(.+)[.]([^.\\{\\[]+)");
  private static Pattern fieldListPattern = Pattern.compile("(.+)\\[([^\\]]+)\\]");
  private static Pattern fieldMapPattern = Pattern.compile("(.+)\\{([^\\}]+)\\}");

  @VisibleForTesting
  static Pattern getFieldStructurePattern() {
    return fieldStructurePattern;
  }

  @VisibleForTesting
  static Pattern getFieldListPattern() {
    return fieldListPattern;
  }

  @VisibleForTesting
  static Pattern getFieldMapPattern() {
    return fieldMapPattern;
  }

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
      String[] equalsParts = fieldSpec.split("[=]");
      if (equalsParts.length > 2) {
        throw new IllegalArgumentException("Inconsistent: found multiple '=' characters");
      }
      if (equalsParts.length == 2) {
        topLevel = InitValueConfig.createWithValue(equalsParts[1]);
      } else if (initValueConfigMap.containsKey(fieldSpec)) {
        topLevel = initValueConfigMap.get(fieldSpec);
      }

      String toMatch = equalsParts[0];
      while (toMatch != null) {
        Matcher structureMatcher = fieldStructurePattern.matcher(toMatch);
        Matcher listMatcher = fieldListPattern.matcher(toMatch);
        Matcher mapMatcher = fieldMapPattern.matcher(toMatch);
        if (structureMatcher.matches()) {
          String key = structureMatcher.group(2);
          topLevel = FieldSpec.create(key, topLevel);
          toMatch = structureMatcher.group(1);
        } else if (listMatcher.matches()) {
          String index = listMatcher.group(2);
          topLevel = ListElementSpec.create(index, topLevel);
          toMatch = listMatcher.group(1);
        } else if (mapMatcher.matches()) {
          String key = mapMatcher.group(2);
          topLevel = MapElementSpec.create(key, topLevel);
          toMatch = mapMatcher.group(1);
        } else {
          // No pattern match implies toMatch contains simple field (with no "." separators)
          topLevel = FieldSpec.create(toMatch, topLevel);
          toMatch = null;
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

  public static Object merge(Object mergedStructure, Object unmergedStructure) {
    if (unmergedStructure instanceof PathSpec) {
      return ((PathSpec) unmergedStructure).merge(mergedStructure);
    } else if (unmergedStructure instanceof InitValueConfig) {
      // Only valid to merge an InitValueConfig if the existing merged structure is
      // an empty InitValueConfig
      if (mergedStructure instanceof InitValueConfig) {
        InitValueConfig metadata = (InitValueConfig) mergedStructure;
        if (metadata.isEmpty()) {
          // Replace empty structure with unmergedStructure
          return unmergedStructure;
        }
      }
      throw new IllegalArgumentException(
          "Inconsistent: found both substructure and initialization metadata");
    } else {
      throw new IllegalArgumentException(
          "merge: didn't expect "
              + unmergedStructure.getClass().getName()
              + "; mergedStructure: "
              + mergedStructure
              + "; unmergeredStructure: "
              + unmergedStructure);
    }
  }

  public static Object populate(Object unmergedStructure) {
    if (unmergedStructure instanceof PathSpec) {
      return ((PathSpec) unmergedStructure).populate();
    } else {
      return unmergedStructure;
    }
  }
}
