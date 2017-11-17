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

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a simple or complex type and keeps track of the aliases for the contributing types.
 */
public class TypeName {
  private final TypeAlias topLevelAlias;
  private final String pattern;
  private final List<TypeName> innerTypeNames;

  /** Constructs a TypeName where the full name and nickname are the same. */
  public TypeName(String name) {
    this(name, name);
  }

  /** Creates a type alias with the given fullName and nickname. */
  public TypeName(String fullName, String nickname) {
    this(TypeAlias.create(fullName, nickname));
  }

  /** Creates a type alias for a static inner type with the given fullName and nickname. */
  public TypeName(String fullName, String nickname, String parentTypeName) {
    this(TypeAlias.create(fullName, nickname, parentTypeName));
  }

  /** Creates a TypeName with a given TypeAlias. */
  public TypeName(TypeAlias typeAlias) {
    this.topLevelAlias = typeAlias;
    this.pattern = null;
    this.innerTypeNames = Arrays.asList();
  }

  /**
   * Constructs a composite TypeName with a special string pattern to render the type, and a list of
   * inner TypeName instances that parameterize the type.
   *
   * @param fullName full name
   * @param nickname nickname
   * @param pattern Use the pattern %s for self and %i for inner types, one for each inner type in
   *     the order that they are provided. If this is null, then the type name of the outer type is
   *     used, and inner type names are ignored.
   * @param innerTypeNames
   */
  public TypeName(String fullName, String nickname, String pattern, TypeName... innerTypeNames) {
    this.topLevelAlias = TypeAlias.create(fullName, nickname);
    this.pattern = pattern;
    this.innerTypeNames = Arrays.asList(innerTypeNames);
  }

  /** Renders the fully-qualified name of this type given its pattern. */
  public String getFullName() {
    if (pattern == null) {
      return topLevelAlias.getFullName();
    }
    String result = StringUtils.replaceOnce(pattern, "%s", topLevelAlias.getFullName());
    for (TypeName innerTypeName : innerTypeNames) {
      result = StringUtils.replaceOnce(result, "%i", innerTypeName.getFullName());
    }
    return result;
  }

  /** Renders the short name of this type given its pattern. */
  public String getNickname() {
    if (pattern == null) {
      return topLevelAlias.getNickname();
    }
    String result = StringUtils.replaceOnce(pattern, "%s", topLevelAlias.getNickname());
    for (TypeName innerTypeName : innerTypeNames) {
      result = StringUtils.replaceOnce(result, "%i", innerTypeName.getNickname());
    }
    return result;
  }

  /**
   * Renders the short name of this type given its pattern, and adds any necessary nicknames to the
   * given type table.
   */
  public String getAndSaveNicknameIn(TypeTable typeTable) {
    String topLevelNickname = typeTable.getAndSaveNicknameFor(topLevelAlias);
    if (pattern == null) {
      return topLevelNickname;
    }
    String result = StringUtils.replaceOnce(pattern, "%s", topLevelNickname);
    for (TypeName innerTypeName : innerTypeNames) {
      result = StringUtils.replaceOnce(result, "%i", innerTypeName.getAndSaveNicknameIn(typeTable));
    }
    return result;
  }

  /**
   * Renders the short name of this type given its pattern, and adds any necessary nicknames to the
   * given type table.
   */
  public List<TypeName> getInnerTypeNames() {
    return innerTypeNames;
  }
}
