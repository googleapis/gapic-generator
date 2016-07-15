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
package com.google.api.codegen.util;

import java.util.ArrayList;
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

  public TypeName(String name) {
    this(name, name);
  }

  public TypeName(String fullName, String nickname) {
    this.topLevelAlias = new TypeAlias(fullName, nickname);
    this.pattern = null;
    this.innerTypeNames = Arrays.asList();
  }

  /**
   *
   * @param fullName
   * @param nickname
   * @param pattern Use the pattern %s for self and %i for inner types, one for each inner type in the order that they are provided.
   * @param innerTypeNames
   */
  public TypeName(String fullName, String nickname, String pattern, TypeName... innerTypeNames) {
    this.topLevelAlias = new TypeAlias(fullName, nickname);
    this.pattern = pattern;
    this.innerTypeNames = Arrays.asList(innerTypeNames);
  }

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

  public List<TypeAlias> getAliases() {
    List<TypeAlias> aliases = new ArrayList<>();

    aliases.add(topLevelAlias);
    for (TypeName innerTypeName : innerTypeNames) {
      aliases.addAll(innerTypeName.getAliases());
    }

    return aliases;
  }
}
