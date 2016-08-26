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
package com.google.api.codegen.util.ruby;

import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.TreeMap;

/** The TypeTable for Ruby. */
public class RubyTypeTable implements TypeTable {
  /*
   * A bi-map from full names to short names. In other languages this would indicate imports, but
   * in ruby this only indicates types to their fully qualified types.
   */
  private final BiMap<String, String> nameMap = HashBiMap.create();

  @Override
  public TypeTable cloneEmpty() {
    return new RubyTypeTable();
  }

  @Override
  public TypeName getTypeName(String fullName) {
    int lastColonedIndex = fullName.lastIndexOf("::");
    if (lastColonedIndex < 0) {
      throw new IllegalArgumentException("expected fully qualified name");
    }
    String nickname = fullName.substring(lastColonedIndex + 2);
    return new TypeName(fullName, nickname);
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
    if (nameMap.containsKey(alias.getFullName())) {
      // Short name already there.
      return nameMap.get(alias.getFullName());
    }
    if (nameMap.containsValue(alias.getNickname())) {
      // Short name clashes, use long name.
      return alias.getFullName();
    }
    nameMap.put(alias.getFullName(), alias.getNickname());
    return alias.getNickname();
  }

  @Override
  public Map<String, String> getImports() {
    // Since the import map will be used for type aliasing in Ruby, use a TreeMap to sort by
    // the value.
    return HashBiMap.create(new TreeMap<>(nameMap.inverse())).inverse();
  }

  public boolean hasImports() {
    return !getImports().isEmpty();
  }

  /**:
   * A set of ruby keywords and built-ins. keywords:
   * http://docs.ruby-lang.org/en/2.3.0/keywords_rdoc.html
   */
  private static final ImmutableSet<String> KEYWORD_BUILT_IN_SET =
      ImmutableSet.<String>builder()
          .add(
              "__ENCODING__",
              "__LINE__",
              "__FILE__",
              "BEGIN",
              "END",
              "alias",
              "and",
              "begin",
              "break",
              "case",
              "class",
              "def",
              "defined?",
              "do",
              "else",
              "elsif",
              "end",
              "ensure",
              "false",
              "for",
              "if",
              "in",
              "module",
              "next",
              "nil",
              "not",
              "or",
              "redo",
              "rescue",
              "retry",
              "return",
              "self",
              "super",
              "then",
              "true",
              "undef",
              "unless",
              "until",
              "when",
              "while",
              "yield",
              // "options" is here because it's a common keyword argument to
              // specify a CallOptions instance.
              "options")
          .build();
}
