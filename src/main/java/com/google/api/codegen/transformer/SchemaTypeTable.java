/* Copyright 2017 Google LLC
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

import com.google.api.codegen.config.DiscoveryField;
import com.google.api.codegen.config.DiscoveryRequestType;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.discogapic.EmptyTypeModel;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.transformer.SchemaTypeNameConverter.BoxingBehavior;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.api.codegen.util.TypedValue;
import java.util.Map;

/**
 * A SchemaTypeTable manages the imports for a set of fully-qualified type names, and provides
 * helper methods for importing instances of Schema.
 */
public class SchemaTypeTable implements ImportTypeTable, SchemaTypeFormatter {
  private SchemaTypeFormatterImpl typeFormatter;
  private TypeTable typeTable;
  private SchemaTypeNameConverter typeNameConverter;
  private DiscoGapicNamer discoGapicNamer;

  public SchemaTypeTable(
      TypeTable typeTable,
      SchemaTypeNameConverter typeNameConverter,
      DiscoGapicNamer discoGapicNamer) {
    this.typeFormatter = new SchemaTypeFormatterImpl(typeNameConverter);
    this.typeTable = typeTable;
    this.typeNameConverter = typeNameConverter;
    this.discoGapicNamer = discoGapicNamer;
  }

  @Override
  public SchemaTypeNameConverter getTypeNameConverter() {
    return typeNameConverter;
  }

  @Override
  public String renderPrimitiveValue(Schema type, String value) {
    return typeFormatter.renderPrimitiveValue(type, value);
  }

  @Override
  public String getNicknameFor(Schema type) {
    return typeNameConverter.getTypeName(type).getNickname();
  }

  @Override
  public String getFullNameFor(Schema type) {
    return typeFormatter.getFullNameFor(type);
  }

  @Override
  public String getImplicitPackageFullNameFor(String shortName) {
    return typeFormatter.getImplicitPackageFullNameFor(shortName);
  }

  @Override
  public String getInnerTypeNameFor(Schema schema) {
    return typeFormatter.getInnerTypeNameFor(schema);
  }

  @Override
  public String getEnumValue(FieldModel type, String value) {
    return getNotImplementedString("SchemaTypeTable.getFullNameFor(FieldModel type, String value)");
  }

  /** Returns the enum value string */
  public String getEnumValue(Schema type, String value) {
    if (!type.isEnum()) {
      return value;
    }
    for (String enumValue : type.enumValues()) {
      if (enumValue.equals(value)) {
        return typeNameConverter
            .getEnumValue(type, enumValue)
            .getValueAndSaveTypeNicknameIn(typeTable);
      }
    }
    throw new IllegalArgumentException("Unrecognized enum value: " + value);
  }

  @Override
  public String getEnumValue(TypeModel type, String value) {
    return getEnumValue(((DiscoveryField) type).getDiscoveryField(), value);
  }

  @Override
  public String getAndSaveNicknameFor(TypeModel type) {
    return typeTable.getAndSaveNicknameFor(typeNameConverter.getTypeName(type));
  }

  /** Creates a new SchemaTypeTable of the same concrete type, but with an empty import set. */
  @Override
  public SchemaTypeTable cloneEmpty() {
    return new SchemaTypeTable(typeTable.cloneEmpty(), typeNameConverter, discoGapicNamer);
  }

  @Override
  public SchemaTypeTable cloneEmpty(String packageName) {
    return new SchemaTypeTable(
        typeTable.cloneEmpty(packageName), typeNameConverter, discoGapicNamer);
  }

  /** Compute the nickname for the given fullName and save it in the import set. */
  @Override
  public void saveNicknameFor(String fullName) {
    getAndSaveNicknameFor(fullName);
  }

  /**
   * Computes the nickname for the given full name, adds the full name to the import set, and
   * returns the nickname.
   */
  @Override
  public String getAndSaveNicknameFor(String fullName) {
    return typeTable.getAndSaveNicknameFor(fullName);
  }

  /** Adds the given type alias to the import set, and returns the nickname. */
  @Override
  public String getAndSaveNicknameFor(TypeAlias typeAlias) {
    return typeTable.getAndSaveNicknameFor(typeAlias);
  }

  /**
   * Computes the nickname for the given container full name and inner type short name, adds the
   * full inner type name to the static import set, and returns the nickname.
   */
  @Override
  public String getAndSaveNicknameForInnerType(
      String containerFullName, String innerTypeShortName) {
    return typeTable.getAndSaveNicknameForInnerType(containerFullName, innerTypeShortName);
  }

  /**
   * Computes the nickname for the given type, adds the full name to the import set, and returns the
   * nickname.
   */
  public String getAndSaveNicknameFor(Schema schema) {
    return typeTable.getAndSaveNicknameFor(
        typeNameConverter.getTypeName(schema, BoxingBehavior.BOX_PRIMITIVES));
  }

  public String getFullNameForElementType(Schema type) {
    return typeFormatter.getFullNameFor(type);
  }

  /** Get the full name for the given type. */
  @Override
  public String getFullNameFor(FieldModel type) {
    return getFullNameFor(((DiscoveryField) type).getDiscoveryField());
  }

  @Override
  public String getFullNameFor(InterfaceModel type) {
    return type.getFullName();
  }

  @Override
  public String getFullNameFor(TypeModel type) {
    if (type instanceof DiscoveryRequestType) {
      Method method = ((DiscoveryRequestType) type).parentMethod().getDiscoMethod();
      return discoGapicNamer.getRequestTypeName(method).getFullName();
    } else if (type instanceof EmptyTypeModel) {
      return "java.lang.Void";
    }
    return getFullNameFor(((DiscoveryField) type).getDiscoveryField());
  }

  @Override
  public String getFullNameForMessageType(TypeModel type) {
    return getFullNameFor(type);
  }

  /** Get the full name for the element type of the given type. */
  @Override
  public String getFullNameForElementType(FieldModel type) {
    return getFullNameForElementType(((DiscoveryField) type).getDiscoveryField());
  }

  @Override
  public String getAndSaveNicknameForElementType(TypeModel type) {
    return getAndSaveNicknameForElementType(((FieldModel) type));
  }

  /** Returns the nickname for the given type (without adding the full name to the import set). */
  @Override
  public String getNicknameFor(FieldModel type) {
    return typeFormatter.getNicknameFor(type);
  }

  @Override
  public String getNicknameFor(TypeModel type) {
    return typeFormatter.getNicknameFor(type);
  }

  /** Renders the primitive value of the given type. */
  @Override
  public String renderPrimitiveValue(FieldModel type, String key) {
    return renderPrimitiveValue(((DiscoveryField) type).getDiscoveryField(), key);
  }

  @Override
  public String renderPrimitiveValue(TypeModel type, String key) {
    return renderPrimitiveValue(((DiscoveryField) type).getDiscoveryField(), key);
  }

  @Override
  public String renderValueAsString(String key) {
    return typeNameConverter.renderValueAsString(key);
  }

  /**
   * Computes the nickname for the given type, adds the full name to the import set, and returns the
   * nickname.
   */
  @Override
  public String getAndSaveNicknameFor(FieldModel type) {
    return typeTable.getAndSaveNicknameFor(
        typeNameConverter.getTypeName(((DiscoveryField) type).getDiscoveryField()));
  }

  /*
   * Computes the nickname for the given FieldConfig, and ResourceName. Adds the full name to
   * the import set, and returns the nickname.
   */
  @Override
  public String getAndSaveNicknameForTypedResourceName(
      FieldConfig fieldConfig, String typedResourceShortName) {
    return typeTable.getAndSaveNicknameFor(
        typeNameConverter.getTypeNameForTypedResourceName(fieldConfig, typedResourceShortName));
  }

  /*
   * Computes the nickname for the element type given FieldConfig, and ResourceName. Adds the full
   * name to the import set, and returns the nickname.
   */
  @Override
  public String getAndSaveNicknameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName) {
    return typeTable.getAndSaveNicknameFor(
        typeNameConverter.getTypeNameForResourceNameElementType(
            fieldConfig, typedResourceShortName));
  }

  @Override
  public String getAndSaveNicknameForElementType(FieldModel type) {
    return typeTable.getAndSaveNicknameFor(
        typeNameConverter.getTypeNameForElementType(((DiscoveryField) type).getDiscoveryField()));
  }

  @Override
  public String getAndSaveNicknameForContainer(
      String containerFullName, String... elementFullNames) {
    TypeName completeTypeName = typeTable.getContainerTypeName(containerFullName, elementFullNames);
    return typeTable.getAndSaveNicknameFor(completeTypeName);
  }

  @Override
  public String getSnippetZeroValueAndSaveNicknameFor(FieldModel type) {
    return typeNameConverter
        .getSnippetZeroValue(((DiscoveryField) type).getDiscoveryField())
        .getValueAndSaveTypeNicknameIn(typeTable);
  }

  @Override
  public String getSnippetZeroValueAndSaveNicknameFor(TypeModel type) {
    TypedValue typedValue;
    if (type instanceof EmptyTypeModel) {
      typedValue = TypedValue.create(new TypeName("java.lang.Void"), "");
    } else {
      typedValue =
          typeNameConverter.getSnippetZeroValue(((DiscoveryField) type).getDiscoveryField());
    }
    return typedValue.getValueAndSaveTypeNicknameIn(typeTable);
  }

  @Override
  public String getImplZeroValueAndSaveNicknameFor(FieldModel type) {
    return typeNameConverter
        .getImplZeroValue(((DiscoveryField) type).getDiscoveryField())
        .getValueAndSaveTypeNicknameIn(typeTable);
  }

  /** Returns the imports accumulated so far. */
  @Override
  public Map<String, TypeAlias> getImports() {
    return typeTable.getImports();
  }

  @Override
  public TypeTable getTypeTable() {
    return typeTable;
  }

  public String getNotImplementedString(String feature) {
    return "$ NOT IMPLEMENTED: " + feature + " $";
  }
}
