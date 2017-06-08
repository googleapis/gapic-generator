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

import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeTable;
import java.util.Map;

/**
 * A DiscoTypeTable manages the imports for a set of fully-qualified type names, and provides helper
 * methods for importing instances of TypeRef.
 */
public class DiscoTypeTable {
  private SchemaTypeFormatterImpl typeFormatter;
  private TypeTable typeTable;
  private SchemaTypeNameConverter typeNameConverter;

  public DiscoTypeTable(TypeTable typeTable, SchemaTypeNameConverter typeNameConverter) {
    this.typeFormatter = new SchemaTypeFormatterImpl(typeNameConverter);
    this.typeTable = typeTable;
    this.typeNameConverter = typeNameConverter;
  }

  public String getImplicitPackageFullNameFor(String shortName) {
    return typeFormatter.getImplicitPackageFullNameFor(shortName);
  }

  /** Creates a new DiscoTypeTable of the same concrete type, but with an empty import set. */
  public DiscoTypeTable cloneEmpty() {
    return new DiscoTypeTable(typeTable.cloneEmpty(), typeNameConverter);
  }

  /** Compute the nickname for the given fullName and save it in the import set. */
  public void saveNicknameFor(String fullName) {
    getAndSaveNicknameFor(fullName);
  }

  /**
   * Computes the nickname for the given full name, adds the full name to the import set, and
   * returns the nickname.
   */
  public String getAndSaveNicknameFor(String fullName) {
    return typeTable.getAndSaveNicknameFor(fullName);
  }

  /** Adds the given type alias to the import set, and returns the nickname. */
  public String getAndSaveNicknameFor(TypeAlias typeAlias) {
    return typeTable.getAndSaveNicknameFor(typeAlias);
  }

  /**
   * Computes the nickname for the given container full name and inner type short name, adds the
   * full inner type name to the static import set, and returns the nickname.
   */
  public String getAndSaveNicknameForInnerType(
      String containerFullName, String innerTypeShortName) {
    return typeTable.getAndSaveNicknameForInnerType(containerFullName, innerTypeShortName);
  }

  /**
   * Computes the nickname for the given type, adds the full name to the import set, and returns the
   * nickname.
   */
  public String getAndSaveNicknameFor(String key, Schema schema) {
    return typeTable.getAndSaveNicknameFor(typeNameConverter.getTypeName(key, schema));
  }

  /**
   * For a given schema, add the full name to the import set, and then return the nickname.
   *
   * @param key The String that maps to the given schema. If schema.id() is empty, then the nickname
   *     will be based off this key.
   * @param schema The schema to save and get the nickname for.
   * @return nickname for the schema.
   *     <p>If the given schema type is an array, then the element type is the contained type;
   *     otherwise the element type is the boxed form of the type.
   */
  public String getAndSaveNicknameForElementType(String key, Schema schema, String parentName) {
    return typeTable.getAndSaveNicknameFor(
        typeNameConverter.getTypeNameForElementType(key, schema, parentName));
  }

  /** Returns the imports accumulated so far. */
  public Map<String, TypeAlias> getImports() {
    return typeTable.getImports();
  }

  public TypeTable getTypeTable() {
    return typeTable;
  }
}
