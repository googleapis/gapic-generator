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
package com.google.api.codegen.util.nodejs;

import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.TreeMap;

/**
 * The TypeTable for NodeJS.
 */
public class NodeJSTypeTable implements TypeTable {
  /**
   * A bi-map from full names to short names indicating the import map.
   */
  private final BiMap<String, String> imports = HashBiMap.create();

  private final String implicitPackageName;

  public NodeJSTypeTable(String implicitPackageName) {
    this.implicitPackageName = implicitPackageName;
  }

  @Override
  public TypeTable cloneEmpty() {
    return new NodeJSTypeTable(implicitPackageName);
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
  public NamePath getNamePath(String fullName) {
    return NamePath.dotted(fullName);
  }

  @Override
  public TypeName getContainerTypeName(String containerFullName, String elementFullName) {
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
      return imports.get(alias.getFullName());
    }
    if (imports.containsValue(alias.getNickname())) {
      // Short name clashes, use long name.
      return alias.getFullName();
    }
    imports.put(alias.getFullName(), alias.getNickname());
    return alias.getNickname();
  }

  @Override
  public Map<String, String> getImports() {
    return new TreeMap<>(imports);
  }

  public boolean hasImports() {
    return !getImports().isEmpty();
  }

  /**
   * A set of ECMAScript 2016 reserved words. See
   * https://tc39.github.io/ecma262/2016/#sec-reserved-words
   */
  private static final ImmutableSet<String> KEYWORD_BUILT_IN_SET =
      ImmutableSet.<String>builder()
          .add(
              "break",
              "do",
              "in",
              "typeof",
              "case",
              "else",
              "instanceof",
              "var",
              "catch",
              "export",
              "new",
              "void",
              "class",
              "extends",
              "return",
              "while",
              "const",
              "finally",
              "super",
              "with",
              "continue",
              "for",
              "switch",
              "yield",
              "debugger",
              "function",
              "this",
              "default",
              "if",
              "throw",
              "delete",
              "import",
              "try",
              "let",
              "static",
              "enum",
              "await",
              "implements",
              "package",
              "protected",
              "interface",
              "private",
              "public",
              "null",
              "true",
              "false",
              // common parameters passed to methods.
              "otherArgs",
              "options",
              "callback")
          .build();
}
