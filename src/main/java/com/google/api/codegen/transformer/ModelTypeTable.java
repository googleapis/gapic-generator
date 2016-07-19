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
package com.google.api.codegen.transformer;

import com.google.api.tools.framework.model.TypeRef;

import java.util.List;

/**
 * A ModelTypeTable manages the imports for a set of fully-qualified type names, and
 * provides helper methods for importing instances of TypeRef.
 */
public interface ModelTypeTable {

  /**
   * Creates a new ModelTypeTable of the same concrete type, but with an empty import set.
   */
  ModelTypeTable cloneEmpty();

  /**
   * Compute the nickname for the given fullName and save it in the import set.
   */
  void saveNicknameFor(String fullName);

  /**
   * Get the full name for the given type.
   */
  String getFullNameFor(TypeRef type);

  /**
   * If the given type is repeated, then returns the contained type; if the type is
   * not a repeated type, then returns the boxed form of the type.
   */
  String getFullNameForElementType(TypeRef type);

  /**
   * Returns the nickname for the given type (without adding the full name to the import set).
   */
  String getNicknameFor(TypeRef type);

  /**
   * Computes the nickname for the given full name, adds the full name to the import set,
   * and returns the nickname.
   */
  String getAndSaveNicknameFor(String fullName);

  /**
   * Computes the nickname for the given type, adds the full name to the import set,
   * and returns the nickname.
   */
  String getAndSaveNicknameFor(TypeRef type);

  /**
   * This function will compute the nickname for the element type, add the full name to the
   * import set, and then return the nickname. If the given type is repeated, then the
   * element type is the contained type; if the type is not a repeated type, then the element
   * type is the boxed form of the type.
   */
  String getAndSaveNicknameForElementType(TypeRef type);

  /**
   * Computes the nickname for the given container type name and element type name,
   * saves the full names of those types in the import set, and then returns the
   * formatted nickname for the type.
   */
  String getAndSaveNicknameForContainer(String containerFullName, String elementFullName);

  /**
   * Renders the primitive value of the given type.
   */
  String renderPrimitiveValue(TypeRef type, String key);

  /**
   * If the given type is not implicitly imported, the add it to the import set, then return
   * the zero value for that type.
   */
  String getZeroValueAndSaveNicknameFor(TypeRef type);

  /**
   * Returns the imports accumulated so far.
   */
  List<String> getImports();
}
