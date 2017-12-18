/* Copyright 2016 Google LLC
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
package com.google.api.codegen.util.java;

import static com.google.api.codegen.util.java.JavaTypeTable.JavaLangResolution.ESCAPE_JAVA_LANG_CLASH;

import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.common.base.Joiner;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** The TypeTable for Java. */
public class JavaTypeTable implements TypeTable {
  /** A bi-map from full names to type alias indicating the import map. */
  private final BiMap<String, TypeAlias> imports = HashBiMap.create();

  private final Set<String> usedNicknames = new HashSet<>();

  /**
   * A map from simple type name to a boolean, indicating whether its in java.lang or not. If a
   * simple type name is not in the map, this information is unknown.
   */
  private final Map<String, Boolean> implicitImports = Maps.newHashMap();

  private static final String JAVA_LANG_TYPE_PREFIX = "java.lang.";

  /** A map from unboxed Java primitive type name to boxed counterpart. */
  private static final ImmutableMap<String, String> BOXED_TYPE_MAP =
      ImmutableMap.<String, String>builder()
          .put("boolean", "Boolean")
          .put("int", "Integer")
          .put("long", "Long")
          .put("float", "Float")
          .put("double", "Double")
          .build();

  private final String implicitPackageName;
  private final JavaLangResolution javaLangResolution;

  public JavaTypeTable(String implicitPackageName) {
    this(implicitPackageName, ESCAPE_JAVA_LANG_CLASH);
  }

  public JavaTypeTable(String implicitPackageName, JavaLangResolution javaLangResolution) {
    this.implicitPackageName = implicitPackageName;
    this.javaLangResolution = javaLangResolution;
  }

  public enum JavaLangResolution {
    IGNORE_JAVA_LANG_CLASH,
    ESCAPE_JAVA_LANG_CLASH
  }

  @Override
  public TypeTable cloneEmpty() {
    return new JavaTypeTable(implicitPackageName, javaLangResolution);
  }

  @Override
  public TypeTable cloneEmpty(String packageName) {
    return new JavaTypeTable(packageName);
  }

  @Override
  public TypeName getTypeName(String fullName) {
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
    String argPattern = Joiner.on(",").join(Collections.nCopies(elementTypeNames.length, "%i"));
    String pattern = "%s<" + argPattern + ">";
    return new TypeName(
        containerTypeName.getFullName(),
        containerTypeName.getNickname(),
        pattern,
        elementTypeNames);
  }

  @Override
  public String getAndSaveNicknameFor(String fullName) {
    return getAndSaveNicknameFor(getTypeName(fullName));
  }

  @Override
  public String getAndSaveNicknameForInnerType(
      String containerFullName, String innerTypeShortName) {
    int lastDotIndex = innerTypeShortName.lastIndexOf('.');
    if (lastDotIndex != -1) {
      throw new IllegalArgumentException("Cannot have qualified innerTypeShortName.");
    }
    String fullName = containerFullName + "." + innerTypeShortName;

    return getAndSaveNicknameFor(new TypeName(fullName, innerTypeShortName, containerFullName));
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
    } else if (javaLangResolution.equals(ESCAPE_JAVA_LANG_CLASH)
        && !alias.getFullName().startsWith(JAVA_LANG_TYPE_PREFIX)
        && isImplicitImport(alias.getNickname())) {
      // Short name clashes with java.lang; use long name.
      return alias.getFullName();
    }
    imports.put(alias.getFullName(), alias);
    usedNicknames.add(alias.getNickname());
    return alias.getNickname();
  }

  /** Returns the Java representation of a basic type in boxed form. */
  public static String getBoxedTypeName(String primitiveTypeName) {
    return LanguageUtil.getRename(primitiveTypeName, BOXED_TYPE_MAP);
  }

  @Override
  public Map<String, TypeAlias> getImports() {
    // Clean up the imports.
    Map<String, TypeAlias> cleanedImports = new TreeMap<>();
    // Imported type is in java.lang or in package, can be ignored.
    for (String imported : imports.keySet()) {
      if (imported.startsWith(JAVA_LANG_TYPE_PREFIX)) {
        continue;
      } else if (!implicitPackageName.isEmpty() && imported.startsWith(implicitPackageName)) {
        // Imported type is in a subpackage must not be ignored.
        if (!imported.substring(implicitPackageName.length() + 1).contains(".")) {
          continue;
        }
      }
      cleanedImports.put(imported, imports.get(imported));
    }
    return cleanedImports;
  }

  @Override
  public Map<String, TypeAlias> getAllImports() {
    return new TreeMap<>(imports);
  }

  /**
   * Checks whether the simple type name is implicitly imported from java.lang and memoizes the
   * result.
   */
  private boolean isImplicitImport(String name) {
    Boolean yes = implicitImports.get(name);
    if (yes != null) {
      return yes;
    }
    yes = isJavaLangImport(name);
    implicitImports.put(name, yes);
    return yes;
  }

  /** Checks whether the simple type name is implicitly imported from java.lang. */
  public static boolean isJavaLangImport(String name) {
    // Use reflection to determine whether the name exists in java.lang.
    try {
      Class.forName("java.lang." + name);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
