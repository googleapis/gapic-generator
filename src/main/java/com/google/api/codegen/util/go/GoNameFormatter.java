/* Copyright 2016 Google LLC
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
package com.google.api.codegen.util.go;

import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NameFormatter;
import com.google.api.codegen.util.NamePath;
import com.google.common.collect.ImmutableSet;

/** The NameFormatter for Java. */
public class GoNameFormatter implements NameFormatter {

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
    return name.toLowerCamel();
  }

  @Override
  public String privateFieldName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerCamel());
  }

  @Override
  public String publicFieldName(Name name) {
    return name.toUpperCamel();
  }

  @Override
  public String localVarName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerCamel());
  }

  @Override
  public String localVarReference(Name name) {
    return localVarName(name);
  }

  @Override
  public String publicMethodName(Name name) {
    return name.toUpperCamel();
  }

  @Override
  public String privateMethodName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerCamel());
  }

  @Override
  public String staticFunctionName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerCamel());
  }

  @Override
  public String inittedConstantName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerCamel());
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
    return name.toLowerUnderscore() + ".go";
  }

  /**
   * A set of Go reserved identifiers. See https://golang.org/ref/spec#Keywords
   * https://golang.org/ref/spec#Predeclared_identifiers
   */
  public static final ImmutableSet<String> RESERVED_IDENTIFIER_SET =
      ImmutableSet.<String>builder()
          .add(
              // Keywords
              "break",
              "case",
              "chan",
              "const",
              "continue",
              "default",
              "defer",
              "else",
              "fallthrough",
              "for",
              "func",
              "go",
              "goto",
              "if",
              "import",
              "interface",
              "map",
              "package",
              "range",
              "return",
              "select",
              "struct",
              "switch",
              "type",
              "var",
              // Predeclared identifiers
              "bool",
              "byte",
              "complex64",
              "complex128",
              "error",
              "float32",
              "float64",
              "int",
              "int8",
              "int16",
              "int32",
              "int64",
              "rune",
              "string",
              "uint",
              "uint8",
              "uint16",
              "uint32",
              "uint64",
              "uintptr",
              "true",
              "false",
              "iota",
              "nil",
              "append",
              "cap",
              "close",
              "complex",
              "copy",
              "delete",
              "imag",
              "len",
              "make",
              "new",
              "panic",
              "print",
              "println",
              "real",
              "recover")
          .build();
}
