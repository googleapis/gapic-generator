/* Copyright 2017 Google LLC
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
package com.google.api.codegen.util.js;

import com.google.api.codegen.util.DynamicLangTypeTable;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Map;

/** The TypeTable for JS. */
public class JSTypeTable implements TypeTable {

  private final DynamicLangTypeTable dynamicTypeTable;

  public JSTypeTable(String implicitPackageName) {
    dynamicTypeTable = new DynamicLangTypeTable(implicitPackageName, ".");
  }

  @Override
  public TypeTable cloneEmpty() {
    return new JSTypeTable(dynamicTypeTable.getImplicitPackageName());
  }

  @Override
  public TypeTable cloneEmpty(String packageName) {
    return new JSTypeTable(packageName);
  }

  @Override
  public TypeName getTypeName(String fullName) {
    // Assumes the namespace part starts with lowercase while others start
    // with uppercase.
    ArrayList<String> shortNameParts = new ArrayList<>();
    boolean namespacesFinished = false;
    for (String name : Splitter.on(".").split(fullName)) {
      if (Character.isLowerCase(name.charAt(0))) {
        continue;
      } else {
        namespacesFinished = true;
      }
      if (namespacesFinished) {
        shortNameParts.add(name);
      }
    }
    String shortName = Joiner.on(".").join(shortNameParts);
    return new TypeName(
        fullName, dynamicTypeTable.getImplicitPackageName() + ".types." + shortName);
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

  @Override
  public Map<String, TypeAlias> getAllImports() {
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
}
