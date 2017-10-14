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
package com.google.api.codegen.util.php;

import com.google.api.codegen.util.DynamicLangTypeTable;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.TreeMap;

/** The TypeTable for PHP. */
public class PhpTypeTable implements TypeTable {

  private final DynamicLangTypeTable dynamicTypeTable;

  public PhpTypeTable(String implicitPackageName) {
    dynamicTypeTable = new DynamicLangTypeTable(implicitPackageName, "\\");
  }

  @Override
  public TypeTable cloneEmpty() {
    return new PhpTypeTable(dynamicTypeTable.getImplicitPackageName());
  }

  @Override
  public TypeTable cloneEmpty(String packageName) {
    return new PhpTypeTable(packageName);
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
    return NamePath.backslashed(fullName);
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
    Map<String, TypeAlias> imports = dynamicTypeTable.getImports();
    // Clean up the imports.
    Map<String, TypeAlias> cleanedImports = new TreeMap<>();
    // Imported type is in package, can be ignored.
    for (String imported : imports.keySet()) {
      if (!dynamicTypeTable.getImplicitPackageName().isEmpty()
          && imported.startsWith(dynamicTypeTable.getImplicitPackageName())) {
        if (!imported
            .substring(dynamicTypeTable.getImplicitPackageName().length() + 1)
            .contains("\\")) {
          continue;
        }
      }
      cleanedImports.put(imported, imports.get(imported));
    }
    return cleanedImports;
  }

  @Override
  public Map<String, TypeAlias> getAllImports() {
    return dynamicTypeTable.getAllImports();
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
   * : A set of PHP keywords and built-ins. Keywords:
   * http://docs.ruby-lang.org/en/2.3.0/keywords_rdoc.html
   */
  public static final ImmutableSet<String> RESERVED_IDENTIFIER_SET =
      ImmutableSet.<String>builder()
          .add(
              "abstract",
              "and",
              "array",
              "as",
              "break",
              "callable",
              "case",
              "catch",
              "class",
              "clone",
              "const",
              "continue",
              "declare",
              "default",
              "die",
              "do",
              "echo",
              "else",
              "elseif",
              "empty",
              "enddeclare",
              "endfor",
              "endforeach",
              "endif",
              "endswitch",
              "endwhile",
              "eval",
              "exit",
              "extends",
              "final",
              "finally",
              "for",
              "foreach",
              "function",
              "global",
              "goto",
              "if",
              "implements",
              "include",
              "include_once",
              "instanceof",
              "insteadof",
              "interface",
              "isset",
              "list",
              "namespace",
              "new",
              "or",
              "print",
              "private",
              "protected",
              "public",
              "require",
              "require_once",
              "return",
              "static",
              "switch",
              "throw",
              "trait",
              "try",
              "unset",
              "use",
              "var",
              "while",
              "xor",
              "yield")
          .build();
}
