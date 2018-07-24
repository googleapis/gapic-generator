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
import com.google.api.codegen.util.Scanner;
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
    return parsePartialDottedPathToInitCodeNode(fieldConfig.fieldPath(), valueConfig);
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

  // Grammar in (almost) yacc syntax.
  // config:
  //   ident
  //   config '.' ident
  //   config '[' number ']'
  //   config '{' number '}'
  //   config '{' string '}'
  //   config '{' ident '}' (for compatibility, the identifier is just treated as a string)
  private static InitCodeNode parsePartialDottedPathToInitCodeNode(
      String config, InitValueConfig initValueConfig) {
    Scanner scanner = new Scanner(config);

    Preconditions.checkArgument(
        scanner.scan() == Scanner.IDENT, "expected root identifier: %s", config);
    InitCodeNode root = InitCodeNode.create(scanner.token());

    InitCodeNode parent = root;
    while (true) {
      int token = scanner.scan();
      switch (token) {
        case Scanner.EOF:
          if (initValueConfig != null) {
            parent.updateInitValueConfig(initValueConfig);
            parent.setLineType(InitCodeLineType.SimpleInitLine);
          }
          return root;

        case '.':
          Preconditions.checkArgument(
              scanner.scan() == Scanner.IDENT, "expected identifier after '.': %s", config);
          parent.setLineType(InitCodeLineType.StructureInitLine);
          InitCodeNode child = InitCodeNode.create(scanner.token());
          parent.mergeChild(child);
          parent = child;
          break;

        case '[':
          Preconditions.checkArgument(
              scanner.scan() == Scanner.INT, "expected number after '[': %s", config);
          parent.setLineType(InitCodeLineType.ListInitLine);
          child = InitCodeNode.create(scanner.token());
          parent.mergeChild(child);
          parent = child;

          Preconditions.checkArgument(scanner.scan() == ']', "expected closing ']': %s", config);
          break;

        case '{':
          token = scanner.scan();
          Preconditions.checkArgument(
              token == Scanner.INT || token == Scanner.IDENT || token == Scanner.STRING,
              "invalid key after '{': %s",
              config);
          parent.setLineType(InitCodeLineType.MapInitLine);
          child = InitCodeNode.create(scanner.token());
          parent.mergeChild(child);
          parent = child;

          Preconditions.checkArgument(scanner.scan() == '}', "expected closing '}': %s", config);
          break;

        default:
          throw new IllegalArgumentException(
              String.format("unexpected character '%c': %s", token, config));
      }
    }
  }
}
