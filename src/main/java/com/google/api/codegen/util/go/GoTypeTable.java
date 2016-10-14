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

import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
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

  public static List<String> formatImports(Map<String, TypeAlias> imports) {
    List<String> standard = new ArrayList<>(imports.size());
    List<String> thirdParty = new ArrayList<>(imports.size());

    for (Map.Entry<String, TypeAlias> imp : imports.entrySet()) {
      String importPath = imp.getKey();
      String packageRename = imp.getValue().getNickname();
      List<String> target = isStandardImport(importPath) ? standard : thirdParty;
      if (packageRename.equals("")) {
        target.add(String.format("\"%s\"", importPath));
      } else {
        target.add(String.format("%s \"%s\"", packageRename, importPath));
      }
    }

    List<String> merge = new ArrayList<>(standard);
    if (!standard.isEmpty() && !thirdParty.isEmpty()) {
      merge.add("");
    }
    merge.addAll(thirdParty);
    return merge;
  }

  private static boolean isStandardImport(String importPath) {
    // TODO(pongad): Some packages in standard library have slashes,
    // we might have to special case them.
    return !importPath.contains("/");
  }

  @Override
  public String getAndSaveNicknameForInnerType(
      String containerFullName, String innerTypeShortName) {
    throw new UnsupportedOperationException("getAndSaveNicknameForInnerType not supported by Go");
  }
}
