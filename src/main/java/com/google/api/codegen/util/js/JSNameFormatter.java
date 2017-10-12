/* Copyright 2017 Google Inc
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
package com.google.api.codegen.util.js;

import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NameFormatter;
import com.google.api.codegen.util.NamePath;
import com.google.common.collect.ImmutableSet;

/** The NameFormatter for JS. */
public class JSNameFormatter implements NameFormatter {

  private String wrapIfKeywordOrBuiltIn(String name) {
    if (RESERVED_IDENTIFIER_SET.contains(name)) {
      return name + "_";
    } else {
      return name;
    }
  }

  @Override
  public String publicClassName(Name name) {
    return name.toUpperCamel();
  }

  @Override
  public String privateClassName(Name name) {
    return publicClassName(name);
  }

  @Override
  public String localVarName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerCamel());
  }

  @Override
  public String privateFieldName(Name name) {
    return name.toLowerCamel();
  }

  @Override
  public String publicFieldName(Name name) {
    return name.toLowerCamel();
  }

  @Override
  public String localVarReference(Name name) {
    return localVarName(name);
  }

  @Override
  public String publicMethodName(Name name) {
    return name.toLowerCamel();
  }

  @Override
  public String privateMethodName(Name name) {
    return name.toLowerCamel();
  }

  @Override
  public String staticFunctionName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerCamel());
  }

  @Override
  public String inittedConstantName(Name name) {
    return name.toUpperUnderscore();
  }

  @Override
  public String keyName(Name name) {
    return name.toLowerCamel();
  }

  @Override
  public String qualifiedName(NamePath namePath) {
    return namePath.toDotted();
  }

  @Override
  public String packageFilePathPiece(Name name) {
    return name.toOriginal();
  }

  @Override
  public String classFileNameBase(Name name) {
    return name.toOriginal();
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
              "false")
          .build();
}
