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
package com.google.api.codegen.util;

import com.google.auto.value.AutoValue;
import java.util.Comparator;
import javax.annotation.Nullable;

/**
 * TypeAlias represents an alias between a fully-qualified version of a name (the "fullName") and a
 * short version of a name (the "nickname").
 */
@AutoValue
public abstract class TypeAlias {

  /** Creates a TypeAlias where the fullName and nickname are the same. */
  public static TypeAlias create(String name) {
    return create(name, name);
  }

  /** Creates a type alias with the given fullName and nickname. */
  public static TypeAlias create(String fullName, String nickname) {
    return new AutoValue_TypeAlias(fullName, nickname, null, ImportType.SimpleImport);
  }

  /** Creates a type alias with the given fullName, nickname and importName. */
  public static TypeAlias createOuterImport(String fullName, String nickname, String importName) {
    return new AutoValue_TypeAlias(fullName, nickname, importName, ImportType.OuterImport);
  }

  /** Creates a type alias with the given fullName, nickname and parentName. */
  public static TypeAlias create(String fullName, String nickname, String parentName) {
    return new AutoValue_TypeAlias(fullName, nickname, parentName, ImportType.StaticImport);
  }

  /** Creates a type alias with the given fullName and nickname that represents an aliased import */
  public static TypeAlias createAliasedImport(String fullName, String nickname) {
    return new AutoValue_TypeAlias(fullName, nickname, null, ImportType.AliasedImport);
  }

  /** The full name of the alias. */
  public abstract String getFullName();

  /** The nickname of the alias. */
  public abstract String getNickname();

  /** The full name of the parent class of the type. If this is not an inner class, will be null. */
  @Nullable
  public abstract String getParentFullName();

  /** The import type of the TypeAlias. */
  public abstract ImportType getImportType();

  /**
   * Returns true if the alias needs to be imported to refer to it only through the nickname. This
   * will be false if the full name and nickname are the same.
   */
  public boolean needsImport() {
    return !getFullName().equals(getNickname());
  }

  public static Comparator<TypeAlias> getNicknameComparator() {
    return new Comparator<TypeAlias>() {
      @Override
      public int compare(TypeAlias o1, TypeAlias o2) {
        return o1.getNickname().compareTo(o2.getNickname());
      }
    };
  }
}
