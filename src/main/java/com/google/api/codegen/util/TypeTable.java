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
 * A type table manages the imports for a set of fully-qualified type names.
 */
public interface TypeTable {
  /**
   * Determines if the nickname of the given alias can be used, and if so, then
   * saves it in the import table and returns it; otherwise (e.g. if there would
   * be a clash), returns the full name.
   */
  String getAndSaveNicknameFor(TypeAlias alias);
}
