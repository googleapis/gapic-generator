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

import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** The TypeTable for Python. */
public class PythonTypeTable implements TypeTable {

  /** A bi-map from short names to file names. Should be kept 1:1 with moduleImports keys. */
  private BiMap<String, String> usedShortNames = HashBiMap.create();

  /**
   * A multimap from module names to type aliases in the module. Keys should be kept 1:1 with
   * usedShortNames.
   */
  private Multimap<String, TypeAlias> moduleImports = HashMultimap.create();

  private final String implicitPackageName;

  public PythonTypeTable(String implicitPackageName) {
    this.implicitPackageName = implicitPackageName;
  }

  @Override
  public TypeTable cloneEmpty() {
    return new PythonTypeTable(implicitPackageName);
  }

  @Override
  public TypeTable cloneEmpty(String packageName) {
    return new PythonTypeTable(packageName);
  }

  @Override
  public TypeName getTypeName(String fullName) {
    List<String> namespaces = new ArrayList<>();
    List<String> shortNameParts = new ArrayList<>();
    for (String name : Splitter.on(".").split(fullName)) {
      if (shortNameParts.isEmpty() && Character.isLowerCase(name.charAt(0))) {
        namespaces.add(name);
      } else {
        shortNameParts.add(name);
      }
    }
    if (namespaces.isEmpty() || shortNameParts.isEmpty()) {
      throw new IllegalArgumentException("expected fully qualified name");
    }
    String filename = namespaces.get(namespaces.size() - 1);
    String nickname = Joiner.on(".").join(shortNameParts);
    return new TypeName(fullName, filename + "." + nickname);
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    return new TypeName(implicitPackageName + "." + shortName, shortName);
  }

  @Override
  public NamePath getNamePath(String fullName) {
    return NamePath.doubleColoned(fullName);
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

    String shortName = alias.getNickname().substring(0, alias.getNickname().indexOf("."));
    String className = alias.getNickname().substring(alias.getNickname().indexOf(".") + 1);
    String moduleName =
        alias.getFullName().substring(0, alias.getFullName().length() - className.length() - 1);
    if (usedShortNames.containsKey(shortName)) {
      // Short name already there.
      String oldModuleName = usedShortNames.get(shortName);
      if (moduleName.equals(oldModuleName)) {
        // New alias for existing module import, no clash
        // Name already in multimap, add alias
        moduleImports.put(moduleName, alias);
        return alias.getNickname();
      }

      // Short name clashes, disambiguate.
      String disambiguatedOldShortName = disambiguate(oldModuleName, shortName);
      String disambiguatedNewShortName = disambiguate(moduleName, shortName);

      usedShortNames.remove(shortName);
      updateOldImports(disambiguatedOldShortName, moduleImports.removeAll(oldModuleName));

      return getAndSaveNicknameFor(
          TypeAlias.createAliasedImport(
              alias.getFullName(), disambiguatedNewShortName + "." + className));
    }

    if (moduleImports.containsKey(moduleName)) {
      // Use existing local name for already used module
      String newShortName = usedShortNames.inverse().get(moduleName);
      return getAndSaveNicknameFor(
          TypeAlias.createAliasedImport(alias.getFullName(), newShortName + "." + className));
    }

    usedShortNames.put(shortName, moduleName);
    moduleImports.put(moduleName, alias);
    return alias.getNickname();
  }

  /* Attempts to disambiguate an import by moving the highest-level single package name not already
   * present in the alias into the alias:
   *   "from foo import bar as baz" ====> "from foo import bar as foo_baz"
   *   "from foo.bar import baz as bar_baz" ====> "from foo.bar import baz as foo_bar_baz"
   */
  private String disambiguate(String moduleName, String localName) {
    List<String> nameParts = Splitter.on(".").splitToList(moduleName);
    String localNamePackagePrefix = "";

    for (int i = nameParts.size() - 2; i >= 0; --i) {
      localNamePackagePrefix = nameParts.get(i) + "_" + localNamePackagePrefix;
      if (!localName.contains(localNamePackagePrefix)) {
        return nameParts.get(i) + "_" + localName;
      }
    }

    throw new IllegalStateException("Can't disambiguate a module (" + moduleName + ") with itself");
  }

  private void updateOldImports(String shortName, Collection<TypeAlias> aliases) {
    for (TypeAlias alias : aliases) {
      String className = alias.getNickname().substring(alias.getNickname().indexOf(".") + 1);
      getAndSaveNicknameFor(
          TypeAlias.createAliasedImport(alias.getFullName(), shortName + "." + className));
    }
  }

  @Override
  public Map<String, TypeAlias> getImports() {
    ImmutableMap.Builder<String, TypeAlias> imports = ImmutableMap.builder();
    for (Collection<TypeAlias> aliases : moduleImports.asMap().values()) {
      TypeAlias alias = aliases.iterator().next();
      imports.put(alias.getFullName(), alias);
    }
    return imports.build();
  }

  public boolean hasImports() {
    return !getImports().isEmpty();
  }

  @Override
  public String getAndSaveNicknameForInnerType(
      String containerFullName, String innerTypeShortName) {
    throw new UnsupportedOperationException("getAndSaveNicknameForInnerType not supported");
  }
}
