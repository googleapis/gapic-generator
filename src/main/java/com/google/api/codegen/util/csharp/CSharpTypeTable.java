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
package com.google.api.codegen.util.csharp;

import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class CSharpTypeTable implements TypeTable {

  private final String implicitPackageName;
  // Full name to nickname map
  private final Map<String, TypeAlias> imports = new HashMap<>();

  public CSharpTypeTable(String implicitPackageName) {
    this.implicitPackageName = implicitPackageName;
  }

  @Override
  public TypeName getTypeName(String fullName) {
    int firstGenericOpenIndex = fullName.indexOf('<');
    if (firstGenericOpenIndex >= 0) {
      int lastGenericCloseIndex = fullName.lastIndexOf('>');
      String containerTypeName = fullName.substring(0, firstGenericOpenIndex);
      List<String> genericParamNames =
          Splitter.on(',')
              .trimResults()
              .splitToList(fullName.substring(firstGenericOpenIndex + 1, lastGenericCloseIndex));
      return getContainerTypeName(
          containerTypeName, genericParamNames.toArray(new String[genericParamNames.size()]));
    }
    int lastDotIndex = fullName.lastIndexOf('.');
    if (lastDotIndex < 0) {
      return new TypeName(fullName, fullName);
    }
    String shortTypeName = fullName.substring(lastDotIndex + 1);
    return new TypeName(fullName, shortTypeName);
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    String fullName = implicitPackageName + "." + shortName;
    return new TypeName(fullName, shortName);
  }

  @Override
  public NamePath getNamePath(String fullName) {
    return NamePath.dotted(fullName);
  }

  @Override
  public TypeName getContainerTypeName(String containerFullName, String... elementFullNames) {
    TypeName containerTypeName = getTypeName(containerFullName);
    TypeName[] elementTypeNames = new TypeName[elementFullNames.length];
    for (int i = 0; i < elementTypeNames.length; i++) {
      elementTypeNames[i] = getTypeName(elementFullNames[i]);
    }
    String argPattern = Joiner.on(", ").join(Collections.nCopies(elementTypeNames.length, "%i"));
    String pattern = "%s<" + argPattern + ">";
    return new TypeName(
        containerTypeName.getFullName(),
        containerTypeName.getNickname(),
        pattern,
        elementTypeNames);
  }

  @Override
  public TypeTable cloneEmpty() {
    return new CSharpTypeTable(implicitPackageName);
  }

  @Override
  public TypeTable cloneEmpty(String packageName) {
    return new CSharpTypeTable(packageName);
  }

  private String resolveInner(String name) {
    return name.replace('+', '.');
  }

  @Override
  public String getAndSaveNicknameFor(String fullName) {
    return resolveInner(getAndSaveNicknameFor(getTypeName(fullName)));
  }

  @Override
  public String getAndSaveNicknameFor(TypeName typeName) {
    return resolveInner(typeName.getAndSaveNicknameIn(this));
  }

  @Override
  public String getAndSaveNicknameFor(TypeAlias alias) {
    if (!alias.needsImport()) {
      return alias.getNickname();
    }
    // Derive a short name if possible
    if (imports.containsKey(alias.getFullName())) {
      // Short name already there.
      return imports.get(alias.getFullName()).getNickname();
    }
    // TODO: Handle name clashes
    imports.put(alias.getFullName(), alias);
    return alias.getNickname();
  }

  @Override
  public Map<String, TypeAlias> getImports() {
    SortedMap<String, TypeAlias> result = new TreeMap<>();
    for (String fullName : imports.keySet()) {
      int index = fullName.lastIndexOf('.');
      if (index >= 0) {
        String using = fullName.substring(0, index);
        if (!implicitPackageName.equals(using)) {
          result.put(using, TypeAlias.create(using)); // Value isn't used
        }
      }
    }
    return result;
  }

  @Override
  public Map<String, TypeAlias> getAllImports() {
    return new TreeMap<>(imports);
  }

  @Override
  public String getAndSaveNicknameForInnerType(
      String containerFullName, String innerTypeShortName) {
    throw new UnsupportedOperationException("getAndSaveNicknameForInnerType not supported by C#");
  }
}
