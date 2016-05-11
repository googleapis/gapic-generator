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
package com.google.api.codegen.go;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;

/**
 * Represents one Go import.
 */
@AutoValue
abstract class GoImport implements Comparable<GoImport> {

  public abstract String moduleName();

  public abstract String localName();

  /**
   * Creates a Go import with the given module name with the local name.
   */
  public static GoImport create(String moduleName, String localName) {
    return new AutoValue_GoImport(moduleName, localName);
  }

  /**
   * Creates a Go import with then given module name.
   */
  public static GoImport create(String moduleName) {
    return create(moduleName, "");
  }

  /**
   * Returns a line of import declaration used in the generated Go files.
   */
  public String importString() {
    if (Strings.isNullOrEmpty(localName())) {
      return "\"" + moduleName() + "\"";
    } else {
      return localName() + " \"" + moduleName() + "\"";
    }
  }

  @Override
  public int compareTo(GoImport other) {
    return moduleName().compareTo(other.moduleName());
  }
}
