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
package com.google.api.codegen.util.go;

import com.google.api.codegen.util.ImportType;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GoTypeTable implements TypeTable {

  private final TreeMap<String, TypeAlias> imports = new TreeMap<>();

  @Override
  public TypeTable cloneEmpty() {
    return new GoTypeTable();
  }

  @Override
  public TypeName getTypeName(String fullName) {
    String[] parts = fullName.split(";", -1);
    if (parts.length != 4) {
      return new TypeName(fullName);
    }
    return new TypeName(fullName, parts[3] + parts[1] + "." + parts[2]);
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    throw new UnsupportedOperationException("getTypeNameInImplicitPackage not supported by Go");
  }

  @Override
  public NamePath getNamePath(String fullName) {
    return NamePath.dotted(fullName);
  }

  @Override
  public TypeName getContainerTypeName(String containerFullName, String... elementFullNames) {
    return getTypeName(containerFullName);
  }

  @Override
  public String getAndSaveNicknameFor(String fullName) {
    return getAndSaveNicknameFor(getTypeName(fullName));
  }

  @Override
  public String getAndSaveNicknameFor(TypeName typeName) {
    return typeName.getAndSaveNicknameIn(this);
  }

  @Override
  public String getAndSaveNicknameFor(TypeAlias alias) {
    String[] parts = alias.getFullName().split(";", -1);
    if (parts.length == 4) {
      imports.put(parts[0], TypeAlias.create(parts[0], parts[1]));
    }
    return alias.getNickname();
  }

  @Override
  public Map<String, TypeAlias> getImports() {
    return imports;
  }

  /**
   * Similar to ImportTypeTransformer.generateImports, but specific to Go since Go imports
   * differently than other languages.
   *
   * <p>Specifically, {@code fullName} and {@code nickname} of each {@code ImportTypeView} is not
   * the names of a type, but the names of an imported package.
   */
  public static List<ImportTypeView> generateImports(Map<String, TypeAlias> imports) {
    List<ImportTypeView> standard = new ArrayList<>(imports.size());
    List<ImportTypeView> thirdParty = new ArrayList<>(imports.size());

    for (Map.Entry<String, TypeAlias> imp : imports.entrySet()) {
      String importPath = imp.getKey();
      String packageRename = imp.getValue().getNickname();
      List<ImportTypeView> target = isStandardImport(importPath) ? standard : thirdParty;
      target.add(
          ImportTypeView.newBuilder()
              .fullName('"' + importPath + '"')
              .nickname(packageRename)
              .type(ImportType.SimpleImport)
              .build());
    }

    List<ImportTypeView> merge = new ArrayList<>(standard);
    if (!standard.isEmpty() && !thirdParty.isEmpty()) {
      merge.add(
          ImportTypeView.newBuilder()
              .fullName("")
              .nickname("")
              .type(ImportType.SimpleImport)
              .build());
    }
    merge.addAll(thirdParty);
    return merge;
  }

  private static boolean isStandardImport(String importPath) {
    // TODO(pongad): Some packages in standard library have slashes,
    // we might have to special case them.
    if (importPath.equals("net/http")) {
      return true;
    }
    return !importPath.contains("/");
  }

  @Override
  public String getAndSaveNicknameForInnerType(
      String containerFullName, String innerTypeShortName) {
    throw new UnsupportedOperationException("getAndSaveNicknameForInnerType not supported by Go");
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
