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
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.common.collect.ImmutableSet;
import java.util.Map;

/** The TypeTable for NodeJS. */
public class NodeJSTypeTable implements TypeTable {

  private final DynamicLangTypeTable dynamicTypeTable;

  public NodeJSTypeTable(String implicitPackageName) {
    dynamicTypeTable = new DynamicLangTypeTable(implicitPackageName, ".");
  }

  @Override
  public TypeTable cloneEmpty() {
    return new NodeJSTypeTable(dynamicTypeTable.getImplicitPackageName());
  }

  @Override
  public TypeName getTypeName(String fullName) {
    return dynamicTypeTable.getTypeName(fullName);
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    return dynamicTypeTable.getTypeNameInImplicitPackage(shortName);
  }

  @Override
  public NamePath getNamePath(String fullName) {
    return NamePath.dotted(fullName);
  }

  @Override
  public TypeName getContainerTypeName(String containerFullName, String... elementFullNames) {
    return dynamicTypeTable.getContainerTypeName(containerFullName, elementFullNames);
  }

  @Override
  public String getAndSaveNicknameFor(String fullName) {
    return dynamicTypeTable.getAndSaveNicknameFor(fullName);
  }

  @Override
  public String getAndSaveNicknameFor(TypeName typeName) {
    return dynamicTypeTable.getAndSaveNicknameFor(typeName);
  }

  @Override
  public String getAndSaveNicknameFor(TypeAlias alias) {
    return dynamicTypeTable.getAndSaveNicknameFor(alias);
  }

  @Override
  public Map<String, TypeAlias> getImports() {
    return dynamicTypeTable.getImports();
  }

  public boolean hasImports() {
    return !getImports().isEmpty();
  }

  @Override
  public String getAndSaveNicknameForInnerType(
      String containerFullName, String innerTypeShortName) {
    return dynamicTypeTable.getAndSaveNicknameForInnerType(containerFullName, innerTypeShortName);
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
