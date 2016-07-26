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

import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The TypeTable for PHP.
 */
public class PhpTypeTable implements TypeTable {
  /**
   * A bi-map from full names to short names indicating the import map.
   */
  private final BiMap<String, String> imports = HashBiMap.create();

  @Override
  public TypeTable cloneEmpty() {
    return new PhpTypeTable();
  }

  @Override
  public TypeName getTypeName(String fullName) {
    int lastBackslashIndex = fullName.lastIndexOf('\\');
    if (lastBackslashIndex < 0) {
      throw new IllegalArgumentException("expected fully qualified name");
    }
    String nickname = fullName.substring(lastBackslashIndex + 1);
    return new TypeName(fullName, nickname);
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
  public List<String> getImports() {
    // Clean up the imports.
    List<String> cleanedImports = new ArrayList<>();
    for (String imported : imports.keySet()) {
      cleanedImports.add(imported);
    }
    Collections.sort(cleanedImports);
    return cleanedImports;
  }

  public boolean hasImports() {
    return !getImports().isEmpty();
  }

  /**
   * A set of PHP keywords and built-ins. keywords: http://php.net/manual/en/reserved.keywords.php
   */
  private static final ImmutableSet<String> KEYWORD_BUILT_IN_SET =
      ImmutableSet.<String>builder()
          .add(
              "__halt_compiler",
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
              "yield",
              "__CLASS__",
              "__DIR__",
              "__FILE__",
              "__FUNCTION__",
              "__LINE__",
              "__METHOD__",
              "__NAMESPACE__",
              "__TRAIT__")
          .build();
}
