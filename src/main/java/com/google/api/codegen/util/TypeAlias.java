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

import com.google.auto.value.AutoValue;

/**
 * TypeAlias represents an alias between a fully-qualified version of a name
 * (the "fullName") and a short version of a name (the "nickname").
 */
@AutoValue
public abstract class TypeAlias {

  /**
   * Creates a TypeAlias where the fullName and nickname are the same.
   */
  public static TypeAlias create(String name) {
    return create(name, name);
  }

  /**
   * Standard constructor.
   */
  public static TypeAlias create(String fullName, String nickname) {
    return new AutoValue_TypeAlias(fullName, nickname);
  }

  /**
   * The full name of the alias.
   */
  public abstract String getFullName();

  /**
   * The nickname of the alias.
   */
  public abstract String getNickname();

  /**
   * Returns true if the alias needs to be imported to refer to it only
   * through the nickname. This will be false if the full name and nickname
   * are the same.
   */
  public boolean needsImport() {
    return !getFullName().equals(getNickname());
  }
}
