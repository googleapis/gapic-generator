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
package com.google.api.codegen.util.ruby;

import com.google.api.codegen.util.DynamicLangTypeTable;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.TreeMap;

/** The TypeTable for Ruby. */
public class RubyTypeTable implements TypeTable {

  private final DynamicLangTypeTable dynamicTypeTable;

  public RubyTypeTable(String implicitPackageName) {
    dynamicTypeTable = new DynamicLangTypeTable(implicitPackageName, "::");
  }

  @Override
  public TypeTable cloneEmpty() {
    return new RubyTypeTable(dynamicTypeTable.getImplicitPackageName());
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
    return NamePath.doubleColoned(fullName);
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
    TreeMap<TypeAlias, String> inverseMap = new TreeMap<>(TypeAlias.getNicknameComparator());
    inverseMap.putAll(dynamicTypeTable.getImportsBimap().inverse());
    return HashBiMap.create(inverseMap).inverse();
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
   * : A set of ruby keywords and built-ins. keywords:
   * http://docs.ruby-lang.org/en/2.3.0/keywords_rdoc.html
   */
  private static final ImmutableSet<String> KEYWORD_BUILT_IN_SET =
      ImmutableSet.<String>builder()
          .add(
              "__ENCODING__",
              "__LINE__",
              "__FILE__",
              "BEGIN",
              "END",
              "alias",
              "and",
              "begin",
              "break",
              "case",
              "class",
              "def",
              "defined?",
              "do",
              "else",
              "elsif",
              "end",
              "ensure",
              "false",
              "for",
              "if",
              "in",
              "module",
              "next",
              "nil",
              "not",
              "or",
              "redo",
              "rescue",
              "retry",
              "return",
              "self",
              "super",
              "then",
              "true",
              "undef",
              "unless",
              "until",
              "when",
              "while",
              "yield",
              // "options" is here because it's a common keyword argument to
              // specify a CallOptions instance.
              "options")
          .build();
}
