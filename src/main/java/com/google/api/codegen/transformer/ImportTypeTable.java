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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeTable;
import java.util.Map;

/**
 * A ModelTypeTable manages the imports for a set of fully-qualified type names, and provides helper
 * methods for importing instances of FieldModel.
 */
public interface ImportTypeTable extends TypeFormatter {
  /** Returns the underlying TypeTable. */
  TypeTable getTypeTable();

  /** Returns the enum value string. */
  String getEnumValue(FieldModel type, String value);

  /** Creates a new ModelTypeTable of the same concrete type, but with an empty import set. */
  ImportTypeTable cloneEmpty();

  /** Creates a new ModelTypeTable of the same concrete type, but with an empty import set. */
  ImportTypeTable cloneEmpty(String packageName);

  /** Compute the nickname for the given fullName and save it in the import set. */
  void saveNicknameFor(String fullName);

  /**
   * Computes the nickname for the given full name, adds the full name to the import set, and
   * returns the nickname.
   */
  String getAndSaveNicknameFor(String fullName);

  /** Adds the given type alias to the import set, and returns the nickname. */
  String getAndSaveNicknameFor(TypeAlias typeAlias);

  /**
   * Computes the nickname for the given container full name and inner type short name, adds the
   * full inner type name to the static import set, and returns the nickname.
   */
  String getAndSaveNicknameForInnerType(String containerFullName, String innerTypeShortName);

  /**
   * Computes the nickname for the given type, adds the full name to the import set, and returns the
   * nickname.
   */
  String getAndSaveNicknameFor(FieldModel type);

  /**
   * Computes the nickname for the given type, adds the full name to the import set, and returns the
   * nickname.
   */
  String getAndSaveNicknameFor(TypeModel type);

  /** Returns the enum value string. */
  String getEnumValue(TypeModel type, String value);

  /*
   * Computes the nickname for the given FieldConfig, and ResourceName. Adds the full name to
   * the import set, and returns the nickname.
   */
  String getAndSaveNicknameForTypedResourceName(
      FieldConfig fieldConfig, String typedResourceShortName);

  /*
   * Computes the nickname for the element type given FieldConfig, and ResourceName. Adds the full
   * name to the import set, and returns the nickname.
   */
  String getAndSaveNicknameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName);

  /**
   * This function will compute the nickname for the element type, add the full name to the import
   * set, and then return the nickname. If the given type is repeated, then the element type is the
   * contained type; if the type is not a repeated type, then the element type is the boxed form of
   * the type.
   */
  String getAndSaveNicknameForElementType(FieldModel type);

  /**
   * This function will compute the nickname for the element type, add the full name to the import
   * set, and then return the nickname. If the given type is repeated, then the element type is the
   * contained type; if the type is not a repeated type, then the element type is the boxed form of
   * the type.
   */
  String getAndSaveNicknameForElementType(TypeModel type);

  String getAndSaveNicknameForContainer(String containerFullName, String... elementFullNames);

  /**
   * If the given type is not implicitly imported, the add it to the import set, then return the
   * zero value for that type.
   */
  String getSnippetZeroValueAndSaveNicknameFor(FieldModel type);

  /**
   * If the given type is not implicitly imported, the add it to the import set, then return the
   * zero value for that type.
   */
  String getSnippetZeroValueAndSaveNicknameFor(TypeModel type);

  String getImplZeroValueAndSaveNicknameFor(FieldModel type);

  /** Returns the imports accumulated so far. */
  Map<String, TypeAlias> getImports();

  /** Returns the TypeNameConverter. */
  TypeNameConverter getTypeNameConverter();
}
