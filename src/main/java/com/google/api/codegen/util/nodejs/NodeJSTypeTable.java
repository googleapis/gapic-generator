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
package com.google.api.codegen.util.nodejs;

import com.google.api.codegen.util.DynamicLangTypeTable;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeTable;
import com.google.common.collect.ImmutableSet;

/** The TypeTable for NodeJS. */
public class NodeJSTypeTable extends DynamicLangTypeTable {

  public NodeJSTypeTable(String implicitPackageName) {
    super(implicitPackageName);
  }

  @Override
  protected String getSeparator() {
    return ".";
  }

  @Override
  public TypeTable cloneEmpty() {
    return new NodeJSTypeTable(getImplicitPackageName());
  }

  @Override
  public NamePath getNamePath(String fullName) {
    return NamePath.dotted(fullName);
  }

  /**
   * A set of ECMAScript 2016 reserved words. See
   * https://tc39.github.io/ecma262/2016/#sec-reserved-words
   */
  public static final ImmutableSet<String> RESERVED_IDENTIFIER_SET =
      ImmutableSet.<String>builder()
          .add(
              "break",
              "do",
              "in",
              "typeof",
              "case",
              "else",
              "instanceof",
              "var",
              "catch",
              "export",
              "new",
              "void",
              "class",
              "extends",
              "return",
              "while",
              "const",
              "finally",
              "super",
              "with",
              "continue",
              "for",
              "switch",
              "yield",
              "debugger",
              "function",
              "this",
              "default",
              "if",
              "throw",
              "delete",
              "import",
              "try",
              "let",
              "static",
              "enum",
              "await",
              "implements",
              "package",
              "protected",
              "interface",
              "private",
              "public",
              "null",
              "true",
              "false",
              // common parameters passed to methods.
              "otherArgs",
              "options",
              "callback")
          .build();
}
