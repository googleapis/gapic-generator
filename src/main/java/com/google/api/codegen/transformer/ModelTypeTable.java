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

import com.google.api.codegen.util.TypeTable;
import com.google.api.tools.framework.model.TypeRef;

import java.util.List;

/**
 * A ModelTypeTable manages the imports for a set of fully-qualified type names, and
 * provides helper methods for importing instances of TypeRef.
 */
public class ModelTypeTable implements ModelTypeFormatter {
  private ModelTypeFormatterImpl typeFormatter;
  private TypeTable typeTable;
  private ModelTypeNameConverter typeNameConverter;

  /**
   * Standard constructor.
   */
  public ModelTypeTable(TypeTable typeTable, ModelTypeNameConverter typeNameConverter) {
    this.typeFormatter = new ModelTypeFormatterImpl(typeNameConverter);
    this.typeTable = typeTable;
    this.typeNameConverter = typeNameConverter;
  }

  @Override
  public String getFullNameFor(TypeRef type) {
    return typeFormatter.getFullNameFor(type);
  }

  @Override
  public String getNicknameFor(TypeRef type) {
    return typeFormatter.getNicknameFor(type);
  }

  @Override
  public String renderPrimitiveValue(TypeRef type, String value) {
    return typeFormatter.renderPrimitiveValue(type, value);
  }

  /**
   * Creates a new ModelTypeTable of the same concrete type, but with an empty import set.
   */
  public ModelTypeTable cloneEmpty() {
    return new ModelTypeTable(typeTable.cloneEmpty(), typeNameConverter);
  }

  /**
   * Compute the nickname for the given fullName and save it in the import set.
   */
  public void saveNicknameFor(String fullName) {
    getAndSaveNicknameFor(fullName);
  }

  /**
   * Computes the nickname for the given full name, adds the full name to the import set,
   * and returns the nickname.
   */
  public String getAndSaveNicknameFor(String fullName) {
    return typeTable.getAndSaveNicknameFor(fullName);
  }

  /**
   * Computes the nickname for the given type, adds the full name to the import set,
   * and returns the nickname.
   */
  public String getAndSaveNicknameFor(TypeRef type) {
    return typeTable.getAndSaveNicknameFor(typeNameConverter.getTypeName(type));
  }

  /**
   * This function will compute the nickname for the element type, add the full name to the
   * import set, and then return the nickname. If the given type is repeated, then the
   * element type is the contained type; if the type is not a repeated type, then the element
   * type is the boxed form of the type.
   */
  public String getAndSaveNicknameForElementType(TypeRef type) {
    return typeTable.getAndSaveNicknameFor(typeNameConverter.getTypeNameForElementType(type));
  }

  /**
   * If the given type is not implicitly imported, the add it to the import set, then return
   * the zero value for that type.
   */
  public String getZeroValueAndSaveNicknameFor(TypeRef type) {
    return typeNameConverter.getZeroValue(type).getValueAndSaveTypeNicknameIn(typeTable);
  }

  /**
   * Returns the imports accumulated so far.
   */
  public List<String> getImports() {
    return typeTable.getImports();
  }
}
