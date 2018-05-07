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
          // Keywords
          .add("break")
          .add("case")
          .add("chan")
          .add("const")
          .add("continue")
          .add("default")
          .add("defer")
          .add("else")
          .add("fallthrough")
          .add("for")
          .add("func")
          .add("go")
          .add("goto")
          .add("if")
          .add("import")
          .add("interface")
          .add("map")
          .add("package")
          .add("range")
          .add("return")
          .add("select")
          .add("struct")
          .add("switch")
          .add("type")
          .add("var")
          // Predeclared identifiers
          .add("bool")
          .add("byte")
          .add("complex64")
          .add("complex128")
          .add("error")
          .add("float32")
          .add("float64")
          .add("int")
          .add("int8")
          .add("int16")
          .add("int32")
          .add("int64")
          .add("rune")
          .add("string")
          .add("uint")
          .add("uint8")
          .add("uint16")
          .add("uint32")
          .add("uint64")
          .add("uintptr")
          .add("true")
          .add("false")
          .add("iota")
          .add("nil")
          .add("append")
          .add("cap")
          .add("close")
          .add("complex")
          .add("copy")
          .add("delete")
          .add("imag")
          .add("len")
          .add("make")
          .add("new")
          .add("panic")
          .add("print")
          .add("println")
          .add("real")
          .add("recover")
          // so common they might as well be keywords
          .add("context")
          .build();
}
