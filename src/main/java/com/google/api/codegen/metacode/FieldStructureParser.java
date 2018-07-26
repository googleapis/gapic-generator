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

  public static InitCodeNode parse(String initFieldConfigString) {
    return parse(initFieldConfigString, ImmutableMap.<String, InitValueConfig>of());
  }

  public static InitCodeNode parse(
      String initFieldConfigString, Map<String, InitValueConfig> initValueConfigMap) {
    return parseConfig(initFieldConfigString, initValueConfigMap);
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

  // Parses `path` to construct the `InitCodeNode` it specifies. `path` must be a valid config
  // satisfying the eBNF grammar below:
  //
  // config = path ['=' value];
  // path = ident pathElem* ['%' ident]
  // pathElem = ('.' ident) | ('[' int ']') | ('{' value '}');
  // value = int | string | ident;
  //
  // For compatibility with the previous parser, when ident is used as a value, the value is
  // the name of the ident.
  private static InitCodeNode parseConfig(
      String config, Map<String, InitValueConfig> initValueConfigMap) {
    Scanner scanner = new Scanner(config);

    Preconditions.checkArgument(
        scanner.scan() == Scanner.IDENT, "expected root identifier: %s", config);
    InitCodeNode root = InitCodeNode.create(scanner.token());

    InitCodeNode parent = root;
    int token;

    pathElem:
    while (true) {
      token = scanner.scan();
      switch (token) {
        case '%':
        case '=':
        case Scanner.EOF:
          break pathElem;

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
          child = InitCodeNode.create(parseValue(scanner));
          parent.setLineType(InitCodeLineType.MapInitLine);
          parent.mergeChild(child);
          parent = child;

          Preconditions.checkArgument(scanner.scan() == '}', "expected closing '}': %s", config);
          break;

        default:
          throw new IllegalArgumentException(
              String.format("unexpected character '%c': %s", token, config));
      }
    }

    int fieldNamePos = config.length();

    String entityName = null;
    if (token == '%') {
      fieldNamePos = scanner.pos() - 1;
      Preconditions.checkArgument(
          scanner.scan() == Scanner.IDENT, "expected ident after '%': %s", config);
      entityName = scanner.token();
      token = scanner.scan();
    }

    InitValue initValue = null;
    if (token == '=') {
      fieldNamePos = Math.min(fieldNamePos, scanner.pos() - 1);

      // TODO(pongad): Currently the RHS of equal sign is just arbitrary run of text treated as string, eg
      //   a.b=https://foo.bar.com/zip/zap
      // We'll quote the RHS of existing configs, then we can use parseValue here.
      // For now, just preserve old behavior.
      // String valueString = parseValue(scanner);
      String valueString = config.substring(scanner.pos());

      if (valueString.contains(InitFieldConfig.RANDOM_TOKEN)) {
        System.err.println(valueString);
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
      token = scanner.scan();
    }

    // TODO(pongad): When we can actually parse the RHS, we should expect EOF.
    // Preconditions.checkArgument(scanner.scan() == Scanner.EOF, "expected EOF: %s", config);

    InitValueConfig valueConfig =
        createInitValueConfig(
            InitFieldConfig.newBuilder()
                .fieldPath(config.substring(0, fieldNamePos))
                .entityName(entityName)
                .value(initValue)
                .build(),
            initValueConfigMap);
    if (valueConfig != null) {
      parent.updateInitValueConfig(valueConfig);
      parent.setLineType(InitCodeLineType.SimpleInitLine);
    }
    return root;
  }

  private static String parseValue(Scanner scanner) {
    int token = scanner.scan();
    Preconditions.checkArgument(
        token == Scanner.INT || token == Scanner.IDENT || token == Scanner.STRING,
        "invalid value: %s",
        scanner.input());
    return scanner.token();
  }
}
