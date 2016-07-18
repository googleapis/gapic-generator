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

/**
 * TypeAlias represents an alias between a fully-qualified version of a name
 * (the "fullName") and a short version of a name (the "nickname").
 */
public class TypeAlias {
  private final String fullName;
  private final String nickname;

  /**
   * Creates a TypeAlias where the fullName and nickname are the same.
   */
  public TypeAlias(String name) {
    this(name, name);
  }

  /**
   * Standard constructor.
   */
  public TypeAlias(String fullName, String nickname) {
    this.fullName = fullName;
    this.nickname = nickname;
  }

  /**
   * The full name of the alias.
   */
  public String getFullName() {
    return fullName;
  }

  /**
   * The nickname of the alias.
   */
  public String getNickname() {
    return nickname;
  }

  /**
   * Returns true if the alias needs to be imported to refer to it only
   * through the nickname. This will be false if the full name and nickname
   * are the same.
   */
  public boolean needsImport() {
    return !fullName.equals(nickname);
  }
}
