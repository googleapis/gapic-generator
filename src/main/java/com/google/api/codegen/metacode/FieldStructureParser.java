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
package com.google.api.codegen.metacode;

import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/*
 * FieldStructureParser parses a dotted path specification and into a "tree" of InitCodeNode
 * objects, and returns the root. Each InitCodeNode object will have at most 1 child, so the "tree"
 * will actually be a list.
 */
public class FieldStructureParser {

  public static InitCodeNode parse(String initFieldConfigString) {
    return parse(initFieldConfigString, ImmutableMap.<String, InitValueConfig>of());
  }

  public static InitCodeNode parse(
      String initFieldConfigString, Map<String, InitValueConfig> initValueConfigMap) {
    InitFieldConfig fieldConfig = InitFieldConfig.from(initFieldConfigString);
    InitValueConfig valueConfig = createInitValueConfig(fieldConfig, initValueConfigMap);
    return parsePartialDottedPathToInitCodeNode(
        fieldConfig.fieldPath(), InitCodeLineType.Unknown, valueConfig, null);
  }

  private static InitValueConfig createInitValueConfig(
      InitFieldConfig fieldConfig, Map<String, InitValueConfig> initValueConfigMap) {
    if (fieldConfig.isFormattedConfig()
        && !initValueConfigMap.containsKey(fieldConfig.fieldPath())) {
      // If the field is a formatted field, it must exist in the value config map.
      throw new IllegalArgumentException("The field name is not found in the collection map.");
    }

    InitValueConfig valueConfig = null;
    if (fieldConfig.hasFormattedInitValue()) {
      valueConfig =
          initValueConfigMap
              .get(fieldConfig.fieldPath())
              .withInitialCollectionValue(fieldConfig.entityName(), fieldConfig.value());
    } else if (fieldConfig.hasSimpleInitValue()) {
      InitValue initValue = fieldConfig.value();
      valueConfig =
          InitValueConfig.createWithValue(
              InitValue.create(
                  CommonRenderingUtil.stripQuotes(initValue.getValue()), initValue.getType()));
    } else if (initValueConfigMap.containsKey(fieldConfig.fieldPath())) {
      valueConfig = initValueConfigMap.get(fieldConfig.fieldPath());
    }
    return valueConfig;
  }

  private static InitCodeNode parsePartialDottedPathToInitCodeNode(
      String path,
      InitCodeLineType prevType,
      InitValueConfig initValueConfig,
      InitCodeNode prevNode) {

    InitCodeLineType nextType;
    String key;
    if (path.endsWith("]")) {
      nextType = InitCodeLineType.ListInitLine;
      int p = path.lastIndexOf("[");
      Preconditions.checkArgument(p >= 0, "invalid list expression: %s", path);
      key = path.substring(p + 1, path.length() - 1);
      path = path.substring(0, p);
    } else if (path.endsWith("}")) {
      nextType = InitCodeLineType.MapInitLine;
      int p = path.lastIndexOf("{");
      Preconditions.checkArgument(p >= 0, "invalid map expression: %s", path);
      key = CommonRenderingUtil.stripQuotes(path.substring(p + 1, path.length() - 1));
      path = path.substring(0, p);
    } else {
      int p = path.lastIndexOf('.');
      if (p >= 0) {
        nextType = InitCodeLineType.StructureInitLine;
        key = path.substring(p + 1);
        path = path.substring(0, p);
      } else {
        key = path;
        path = null;
        nextType = InitCodeLineType.Unknown;
      }
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

    if (path == null) {
      return item;
    }
    return parsePartialDottedPathToInitCodeNode(path, nextType, null, item);
  }
}
