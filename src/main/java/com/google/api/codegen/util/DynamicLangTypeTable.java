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
package com.google.api.codegen.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** A generic TypeTable that can be used by dynamic language implementations. */
public class DynamicLangTypeTable implements TypeTable {
  /** A bi-map from full names to short names indicating the import map. */
  private final BiMap<String, TypeAlias> imports = HashBiMap.create();

  private final Set<String> usedNicknames = new HashSet<>();

  private final String implicitPackageName;

  private final String separator;

  public DynamicLangTypeTable(String implicitPackageName, String separator) {
    this.implicitPackageName = implicitPackageName;
    this.separator = separator;
  }

  public String getSeparator() {
    return separator;
  }

  public BiMap<String, TypeAlias> getImportsBimap() {
    return imports;
  }

  public String getImplicitPackageName() {
    return implicitPackageName;
  }

  @Override
  public TypeTable cloneEmpty() {
    return new DynamicLangTypeTable(implicitPackageName, separator);
  }

  @Override
  public TypeTable cloneEmpty(String packageName) {
    return new DynamicLangTypeTable(packageName, separator);
  }

  @Override
  public TypeName getTypeName(String fullName) {
    int lastSeparatorIndex = fullName.lastIndexOf(getSeparator());
    if (lastSeparatorIndex < 0) {
      throw new IllegalArgumentException("expected fully qualified name");
    }
    String nickname = fullName.substring(lastSeparatorIndex + getSeparator().length());
    return new TypeName(fullName, nickname);
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    String fullName = implicitPackageName + getSeparator() + shortName;
    return new TypeName(fullName, shortName);
  }

  @Override
  public NamePath getNamePath(String fullName) {
    throw new UnsupportedOperationException("getNamePath not supported");
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
    if (!alias.needsImport()) {
      return alias.getNickname();
    }
    // Derive a short name if possible
    if (imports.containsKey(alias.getFullName())) {
      // Short name already there.
      return imports.get(alias.getFullName()).getNickname();
    }
    if (usedNicknames.contains(alias.getNickname())) {
      // Short name clashes, use long name.
      return alias.getFullName();
    }
    imports.put(alias.getFullName(), alias);
    usedNicknames.add(alias.getNickname());
    return alias.getNickname();
  }

  @Override
  public Map<String, TypeAlias> getImports() {
    return new TreeMap<>(imports);
  }

  @Override
  public Map<String, TypeAlias> getAllImports() {
    return new TreeMap<>(imports);
  }

  @Override
  public String getAndSaveNicknameForInnerType(
      String containerFullName, String innerTypeShortName) {
    throw new UnsupportedOperationException("getAndSaveNicknameForInnerType not supported");
  }
}
