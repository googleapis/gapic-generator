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

public class TypeAlias {
  private final String fullName;
  private final String nickname;

  public TypeAlias(String fullName, String nickname) {
    this.fullName = fullName;
    this.nickname = nickname;
  }

  public TypeAlias(String name) {
    this.fullName = name;
    this.nickname = name;
  }

  public String getFullName() {
    return fullName;
  }

  public String getNickname() {
    return nickname;
  }

  public boolean needsImport() {
    return !fullName.equals(nickname);
  }
}
