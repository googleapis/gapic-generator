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
  private static final String PROJECT_ID_TOKEN = "$PROJECT_ID";

  /** Update {@code root} with configuration in {@code initFieldConfigString}. */
  public static void parse(InitCodeNode root, String initFieldConfigString) {
    parse(root, initFieldConfigString, ImmutableMap.<String, InitValueConfig>of());
  }

  /** Update {@code root} with configuration in {@code initFieldConfigString}. */
  public static void parse(
      InitCodeNode root,
      String initFieldConfigString,
      Map<String, InitValueConfig> initValueConfigMap) {
    parseConfig(root, initFieldConfigString, initValueConfigMap);
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

  // Parses `config` to construct the `InitCodeNode` it specifies. `config` must be a valid
  // config satisfying the eBNF grammar below:
  //
  // config = path ['%' ident] ['=' value];
  // path = ident pathElem*
  // pathElem = ('.' ident) | ('[' int ']') | ('{' value '}');
  // value = int | string | ident;
  //
  // For compatibility with the previous parser, when ident is used as a value, the value is
  // the name of the ident. Eg, if the ident is "x", the value is simply "x", not the content
  // of the variable named "x".
  //
  private static InitCodeNode parseConfig(
      InitCodeNode root, String config, Map<String, InitValueConfig> initValueConfigMap) {
    Scanner scanner = new Scanner(config);

    InitCodeNode parent = parsePath(root, scanner);

    int fieldNamePos = config.length();
    int token = scanner.lastToken();

    String entityName = null;
    if (token == '%') {
      fieldNamePos = scanner.pos() - 1;
      Preconditions.checkArgument(
          scanner.scan() == Scanner.IDENT, "expected ident after '%': %s", config);
      entityName = scanner.tokenStr();
      token = scanner.scan();
    }

    InitValue initValue = null;
    if (token == '=') {
      String valueString = parseValue(scanner);
      if (valueString.contains(InitFieldConfig.RANDOM_TOKEN)) {
        initValue = InitValue.createRandom(valueString);
      } else if (valueString.contains(PROJECT_ID_TOKEN)) {
        Preconditions.checkArgument(
            valueString.equals(PROJECT_ID_TOKEN),
            "%s cannot be used as substring: %s",
            PROJECT_ID_TOKEN,
            config);
        initValue = InitValue.createVariable(InitFieldConfig.PROJECT_ID_VARIABLE_NAME);
      } else {
        initValue = InitValue.createLiteral(valueString);
      }
    }

    InitValueConfig valueConfig =
        createInitValueConfig(
            InitFieldConfig.newBuilder()
                .fieldPath(config.substring(0, fieldNamePos))
                .entityName(entityName)
                .value(initValue)
                .build(),
            initValueConfigMap);
    if (valueConfig != null) {
      parent.updateInitValueConfig(
          InitCodeNode.mergeInitValueConfig(parent.getInitValueConfig(), valueConfig));
      parent.setLineType(InitCodeLineType.SimpleInitLine);
    }
    return root;
  }

  /**
   * Parses the path found in {@code scanner} and descend the tree rooted at {@code root}. If
   * children specified by the path do not exist, they are created.
   */
  public static InitCodeNode parsePath(InitCodeNode root, Scanner scanner) {
    Preconditions.checkArgument(
        scanner.scan() == Scanner.IDENT, "expected root identifier: %s", scanner.input());
    InitCodeNode parent = root.mergeChild(InitCodeNode.create(scanner.tokenStr()));
    int token;

    while (true) {
      token = scanner.scan();
      switch (token) {
        case '%':
        case '=':
        case Scanner.EOF:
          return parent;
        case '.':
          Preconditions.checkArgument(
              scanner.scan() == Scanner.IDENT,
              "expected identifier after '.': %s",
              scanner.input());
          parent.setLineType(InitCodeLineType.StructureInitLine);
          parent = parent.mergeChild(InitCodeNode.create(scanner.tokenStr()));
          break;

        case '[':
          Preconditions.checkArgument(
              scanner.scan() == Scanner.INT, "expected number after '[': %s", scanner.input());
          parent.setLineType(InitCodeLineType.ListInitLine);
          parent = parent.mergeChild(InitCodeNode.create(scanner.tokenStr()));

          Preconditions.checkArgument(
              scanner.scan() == ']', "expected closing ']': %s", scanner.input());
          break;

        case '{':
          parent.setLineType(InitCodeLineType.MapInitLine);
          parent = parent.mergeChild(InitCodeNode.create(parseKey(scanner)));

          Preconditions.checkArgument(
              scanner.scan() == '}', "expected closing '}': %s", scanner.input());
          break;

        default:
          throw new IllegalArgumentException(
              String.format("unexpected character '%c': %s", token, scanner.input()));
      }
    }
  }

  /** Returns the entity name specified by `path` or null if `path` does not contain `%`. */
  public static String parseEntityName(String path) {
    Scanner scanner = new Scanner(path);
    Preconditions.checkArgument(
        scanner.scan() == Scanner.IDENT, "expected root identifier: %s", scanner.input());
    int token;
    String entityName = null;
    while (true) {
      token = scanner.scan();
      switch (token) {
        case '%':
          Preconditions.checkArgument(
              entityName == null, "expected only one \"%%\" in path: %s", path);
          Preconditions.checkArgument(
              scanner.scan() == Scanner.IDENT,
              "expected identifier after '%%': %s",
              scanner.input());
          entityName = scanner.tokenStr();
          break;
        case '=':
        case Scanner.EOF:
          return entityName;
        case '.':
          Preconditions.checkArgument(
              scanner.scan() == Scanner.IDENT,
              "expected identifier after '.': %s",
              scanner.input());
          break;
        case '[':
          Preconditions.checkArgument(
              scanner.scan() == Scanner.INT, "expected number after '[': %s", scanner.input());
          Preconditions.checkArgument(
              scanner.scan() == ']', "expected closing ']': %s", scanner.input());
          break;
        case '{':
          parseKey(scanner);
          Preconditions.checkArgument(
              scanner.scan() == '}', "expected closing '}': %s", scanner.input());
          break;
        default:
          throw new IllegalArgumentException(
              String.format("unexpected character '%c': %s", token, scanner.input()));
      }
    }
  }

  private static String parseKey(Scanner scanner) {
    int token = scanner.scan();
    Preconditions.checkArgument(
        token == Scanner.INT || token == Scanner.IDENT || token == Scanner.STRING,
        "invalid value: %s",
        scanner.input());
    return scanner.tokenStr();
  }

  /**
   * Parses the value of configs (i.e. the RHS of the '='). If the value is a double-quoted string
   * literal we return it unquoted. Otherwise we strip leading spaces and return the rest of value
   * as a string for backward compatibility.
   */
  private static String parseValue(Scanner scanner) {
    int token = scanner.scan();

    // Scanner.STRING means a double-quoted string literal
    if (token == Scanner.STRING) {
      String tokenStr = scanner.tokenStr();
      if (scanner.scan() == Scanner.EOF) {
        return tokenStr;
      }
    }
    return scanner.tokenStr() + scanner.input().substring(scanner.pos());
  }
}
