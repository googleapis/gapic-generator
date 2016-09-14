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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  public static InitCodeNode parse(String fieldSpec) {
    return parse(fieldSpec, ImmutableMap.<String, InitValueConfig>of());
  }

  public static InitCodeNode parse(
      String fieldSpec, Map<String, InitValueConfig> initValueConfigMap) {
    String[] equalsParts = fieldSpec.split("[=]");
    if (equalsParts.length > 2) {
      throw new IllegalArgumentException("Inconsistent: found multiple '=' characters");
    }

    InitValueConfig valueConfig = null;
    if (equalsParts.length == 2) {
      valueConfig = InitValueConfig.createWithValue(equalsParts[1]);
    } else if (initValueConfigMap.containsKey(fieldSpec)) {
      valueConfig = initValueConfigMap.get(fieldSpec);
    }

    return parsePartialFieldToInitCodeLineNode(
        equalsParts[0], InitCodeLineType.Unknown, valueConfig, null);
  }

  private static InitCodeNode parsePartialFieldToInitCodeLineNode(
      String toMatch,
      InitCodeLineType prevType,
      InitValueConfig initValueConfig,
      InitCodeNode prevItem) {

    InitCodeLineType nextType;
    String key;
    Matcher structureMatcher = fieldStructurePattern.matcher(toMatch);
    Matcher listMatcher = fieldListPattern.matcher(toMatch);
    Matcher mapMatcher = fieldMapPattern.matcher(toMatch);
    if (structureMatcher.matches()) {
      key = structureMatcher.group(2);
      nextType = InitCodeLineType.StructureInitLine;
      toMatch = structureMatcher.group(1);
    } else if (listMatcher.matches()) {
      key = listMatcher.group(2);
      nextType = InitCodeLineType.ListInitLine;
      toMatch = listMatcher.group(1);
    } else if (mapMatcher.matches()) {
      key = mapMatcher.group(2);
      nextType = InitCodeLineType.MapInitLine;
      toMatch = mapMatcher.group(1);
    } else {
      // No pattern match implies toMatch contains simple field (with no "." separators)
      key = toMatch;
      nextType = InitCodeLineType.Unknown;
      toMatch = null;
    }

    InitCodeNode item;
    if (prevItem != null) {
      item = InitCodeNode.createWithChildren(key, prevType, prevItem);
    } else if (initValueConfig != null) {
      item = InitCodeNode.createWithValue(key, initValueConfig);
    } else {
      item = InitCodeNode.create(key);
    }

    if (toMatch == null) {
      return item;
    }
    return parsePartialFieldToInitCodeLineNode(toMatch, nextType, null, item);
  }
}
