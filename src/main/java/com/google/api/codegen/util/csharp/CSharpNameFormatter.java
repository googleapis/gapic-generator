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
package com.google.api.codegen.util.csharp;

import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NameFormatter;
import com.google.api.codegen.util.NamePath;
import com.google.common.collect.ImmutableSet;

public class CSharpNameFormatter implements NameFormatter {

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
    return name.toLowerCamel();
  }

  @Override
  public String privateFieldName(Name name) {
    return "_" + name.toLowerCamel();
  }

  @Override
  public String publicFieldName(Name name) {
    return name.toLowerCamel();
  }

  @Override
  public String localVarReference(Name name) {
    return name.toLowerCamel();
  }

  @Override
  public String privateMethodName(Name name) {
    return name.toUpperCamel();
  }

  @Override
  public String publicMethodName(Name name) {
    return name.toUpperCamel();
  }

  @Override
  public String staticFunctionName(Name name) {
    return name.toUpperCamel();
  }

  @Override
  public String inittedConstantName(Name name) {
    return name.toUpperCamel();
  }

  @Override
  public String keyName(Name name) {
    return name.toUpperCamel();
  }

  @Override
  public String qualifiedName(NamePath namePath) {
    return namePath.toDotted();
  }

  @Override
  public String packageFilePathPiece(Name name) {
    return name.toUpperCamel();
  }

  @Override
  public String classFileNameBase(Name name) {
    return publicClassName(name);
  }

  /**
   * A set of C# reserved identifiers. See https://msdn.microsoft.com/en-us/library/x53a06bb.aspx
   */
  public static final ImmutableSet<String> RESERVED_IDENTIFIER_SET =
      ImmutableSet.<String>builder()
          .add(
              "abstract",
              "as",
              "base",
              "bool",
              "break",
              "byte",
              "case",
              "catch",
              "char",
              "checked",
              "class",
              "const",
              "continue",
              "decimal",
              "default",
              "delegate",
              "do",
              "double",
              "else",
              "enum",
              "event",
              "explicit",
              "extern",
              "false",
              "finally",
              "fixed",
              "float",
              "for",
              "foreach",
              "goto",
              "if",
              "implicit",
              "in",
              "int",
              "interface",
              "internal",
              "is",
              "lock",
              "long",
              "namespace",
              "new",
              "null",
              "object",
              "operator",
              "out",
              "override",
              "params",
              "private",
              "protected",
              "public",
              "readonly",
              "ref",
              "return",
              "sbyte",
              "sealed",
              "short",
              "sizeof",
              "stackalloc",
              "static",
              "string",
              "struct",
              "switch",
              "this",
              "throw",
              "true",
              "try",
              "typeof",
              "uint",
              "ulong",
              "unchecked",
              "unsafe",
              "ushort",
              "using",
              "virtual",
              "void",
              "volatile",
              "while")
          .build();
}
