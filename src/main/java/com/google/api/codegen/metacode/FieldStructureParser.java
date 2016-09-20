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

/*
 * FieldStructureParser parses a dotted path specification and into a "tree" of InitCodeNode
 * objects, and returns the root. Each InitCodeNode object will have at most 1 child, so the "tree"
 * will actually be a list.
 */
public class FieldStructureParser {

  private static Pattern fieldStructurePattern = Pattern.compile("(.+)[.]([^.\\{\\[]+)");
  private static Pattern fieldListPattern = Pattern.compile("(.+)\\[([^\\]]+)\\]");
  private static Pattern fieldMapPattern = Pattern.compile("(.+)\\{([^\\}]+)\\}");

  private static Pattern singleQuoteStringPattern = Pattern.compile("'([^\\\']*)'");
  private static Pattern doubleQuoteStringPattern = Pattern.compile("\"([^\\\"]*)\"");

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

  public static InitCodeNode parse(String dottedPathString) {
    return parse(dottedPathString, ImmutableMap.<String, InitValueConfig>of());
  }

  public static InitCodeNode parse(
      String dottedPathString, Map<String, InitValueConfig> initValueConfigMap) {
    String[] equalsParts = dottedPathString.split("[=]");
    if (equalsParts.length > 2) {
      throw new IllegalArgumentException("Inconsistent: found multiple '=' characters");
    }

    InitValueConfig valueConfig = null;
    if (equalsParts.length == 2) {
      valueConfig = InitValueConfig.createWithValue(stripQuotes(equalsParts[1]));
    } else if (initValueConfigMap.containsKey(dottedPathString)) {
      valueConfig = initValueConfigMap.get(dottedPathString);
    }

    return parsePartialDottedPathToInitCodeNode(
        equalsParts[0], InitCodeLineType.Unknown, valueConfig, null);
  }

  private static InitCodeNode parsePartialDottedPathToInitCodeNode(
      String partialDottedPath,
      InitCodeLineType prevType,
      InitValueConfig initValueConfig,
      InitCodeNode prevNode) {

    InitCodeLineType nextType;
    String key;
    Matcher structureMatcher = fieldStructurePattern.matcher(partialDottedPath);
    Matcher listMatcher = fieldListPattern.matcher(partialDottedPath);
    Matcher mapMatcher = fieldMapPattern.matcher(partialDottedPath);
    if (structureMatcher.matches()) {
      key = structureMatcher.group(2);
      nextType = InitCodeLineType.StructureInitLine;
      partialDottedPath = structureMatcher.group(1);
    } else if (listMatcher.matches()) {
      key = listMatcher.group(2);
      nextType = InitCodeLineType.ListInitLine;
      partialDottedPath = listMatcher.group(1);
    } else if (mapMatcher.matches()) {
      key = stripQuotes(mapMatcher.group(2));
      nextType = InitCodeLineType.MapInitLine;
      partialDottedPath = mapMatcher.group(1);
    } else {
      // No pattern match implies toMatch contains simple field (with no "." separators)
      key = partialDottedPath;
      nextType = InitCodeLineType.Unknown;
      partialDottedPath = null;
    }

    // Create new InitCodeNode with prevItem as a child node. If prevItem is null, then this is the
    // first call to parsePartialFieldToInitCodeNode(), and we create a new InitCodeNode using
    // initValueConfig (if it is not also null)
    InitCodeNode item;
    if (prevNode != null) {
      item = InitCodeNode.createWithChildren(key, prevType, prevNode);
    } else if (initValueConfig != null) {
      item = InitCodeNode.createWithValue(key, initValueConfig);
    } else {
      item = InitCodeNode.create(key);
    }

    if (partialDottedPath == null) {
      return item;
    }
    return parsePartialDottedPathToInitCodeNode(partialDottedPath, nextType, null, item);
  }

  private static String stripQuotes(String value) {
    Matcher singleQuoteMatcher = singleQuoteStringPattern.matcher(value);
    Matcher doubleQuoteMatcher = doubleQuoteStringPattern.matcher(value);
    if (singleQuoteMatcher.matches()) {
      value = singleQuoteMatcher.group(1);
    } else if (doubleQuoteMatcher.matches()) {
      value = doubleQuoteMatcher.group(1);
    }
    return value;
  }
}
