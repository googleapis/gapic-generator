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

import java.util.Map;

/** A type table manages the imports for a set of fully-qualified type names. */
public interface TypeTable extends TypeNameConverter {

  @Override
  TypeName getTypeName(String fullName);

  @Override
  NamePath getNamePath(String fullName);

  @Override
  TypeName getContainerTypeName(String containerFullName, String... elementFullNames);

  /** Return a new TypeTable with the same concrete type as this one. */
  TypeTable cloneEmpty();

  TypeTable cloneEmpty(String packageName);

  /**
   * Computes the nickname for the given full name, adds the full name to the import set, and
   * returns the nickname.
   */
  String getAndSaveNicknameFor(String fullName);

  /**
   * Computes the nickname for the given container full name and inner type short name, adds the
   * full inner type name to the static import set, and returns the nickname.
   */
  String getAndSaveNicknameForInnerType(String containerFullName, String innerTypeShortName);

  /**
   * Computes the nickname for the given type, adds the full name to the import set, and returns the
   * nickname.
   */
  String getAndSaveNicknameFor(TypeName typeName);

  /**
   * Determines if the nickname of the given alias can be used, and if so, then saves it in the
   * import table and returns it; otherwise (e.g. if there would be a clash), returns the full name.
   */
  String getAndSaveNicknameFor(TypeAlias alias);

  /** Returns the imports accumulated so far. */
  Map<String, TypeAlias> getImports();
}
