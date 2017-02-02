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
package com.google.api.codegen.util.py;

import com.google.api.codegen.util.DynamicLangTypeTable;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.TreeMap;

/** The TypeTable for Python. */
public class PythonTypeTable implements TypeTable {

  private final DynamicLangTypeTable dynamicTypeTable;

  public PythonTypeTable(String implicitPackageName) {
    dynamicTypeTable = new DynamicLangTypeTable(implicitPackageName, ".");
  }

  @Override
  public TypeTable cloneEmpty() {
    return new PythonTypeTable(dynamicTypeTable.getImplicitPackageName());
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
   * A set of Python reserved keywords. See
   * https://docs.python.org/2/reference/lexical_analysis.html#keywords
   * https://docs.python.org/2/library/functions.html
   */
  public static final ImmutableSet<String> RESERVED_IDENTIFIER_SET =
      ImmutableSet.<String>builder()
          .add(
              "and",
              "del",
              "from",
              "not",
              "while",
              "as",
              "elif",
              "global",
              "or",
              "with",
              "assert",
              "else",
              "if",
              "pass",
              "yield",
              "break",
              "except",
              "import",
              "print",
              "class",
              "exec",
              "in",
              "raise",
              "continue",
              "finally",
              "is",
              "return",
              "def",
              "for",
              "lambda",
              "try",
              // Built-ins
              "abs",
              "all",
              "any",
              "basestring",
              "bin",
              "bool",
              "bytearray",
              "callable",
              "chr",
              "classmethod",
              "cmp",
              "compile",
              "complex",
              "delattr",
              "dict",
              "dir",
              "divmod",
              "enumerate",
              "eval",
              "execfile",
              "file",
              "filter",
              "float",
              "format",
              "frozenset",
              "getattr",
              "globals",
              "hasattr",
              "hash",
              "help",
              "hex",
              "id",
              "input",
              "int",
              "isinstance",
              "issubclass",
              "iter",
              "len",
              "list",
              "locals",
              "long",
              "map",
              "max",
              "memoryview",
              "min",
              "next",
              "object",
              "oct",
              "open",
              "ord",
              "pow",
              "print",
              "property",
              "range",
              "raw_input",
              "reduce",
              "reload",
              "repr",
              "reversed",
              "round",
              "set",
              "setattr",
              "slice",
              "sorted",
              "staticmethod",
              "str",
              "sum",
              "super",
              "tuple",
              "type",
              "unichr",
              "unicode",
              "vars",
              "xrange",
              "zip",
              "__import__")
          .build();
}
