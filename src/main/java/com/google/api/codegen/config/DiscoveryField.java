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
package com.google.api.codegen.config;

import com.google.api.codegen.discogapic.StringTypeModel;
import com.google.api.codegen.discogapic.transformer.DiscoGapicParser;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.discovery.Schema.Format;
import com.google.api.codegen.discovery.Schema.Type;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.TypeName;
import com.google.api.tools.framework.model.Oneof;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/** A field declaration wrapper around a Discovery Schema. */
public class DiscoveryField implements FieldModel, TypeModel {
  private final List<DiscoveryField> properties;
  private final DiscoApiModel apiModel;
  // Dereferenced schema to use for rendering type names and determining properties, type, and format.
  private final Schema schema;
  // Not dereferenced schema; used in rendering this FieldModel's parameter name.
  private final Schema originalSchema;

  private final String simpleName;

  private static Comparator<Schema> toplevelSchemaComparator =
      (Schema s1, Schema s2) -> s1.getIdentifier().compareTo(s2.getIdentifier());

  private static Map<Schema, DiscoveryField> globalObjects = new HashMap<>();
  private static Map<Schema, String> schemaNames = new TreeMap<>(toplevelSchemaComparator);
  private static Comparator<String> caseInsensitiveComparator =
      (String s1, String s2) -> s1.compareToIgnoreCase(s2);
  private static SymbolTable idSymbolTable = new SymbolTable(caseInsensitiveComparator);

  /**
   * Create a FieldModel object from a non-null Schema object, and internally dereference the input
   * schema.
   */
  private DiscoveryField(Schema refSchema, DiscoApiModel apiModel) {
    Preconditions.checkNotNull(refSchema);
    this.originalSchema = refSchema;
    this.schema = refSchema.dereference();
    this.apiModel = apiModel;
    if (schemaNames.containsKey(schema)) {
      this.simpleName = schemaNames.get(schema);
    } else {
      String simpleName = DiscoGapicParser.stringToName(refSchema.getIdentifier()).toLowerCamel();
      if (isTopLevelSchema(schema)) {
        if (schemaNames.containsKey(schema)) {
          simpleName = schemaNames.get(schema);
        } else {
          simpleName = idSymbolTable.getNewSymbol(simpleName);
          schemaNames.put(schema, simpleName);
        }
      }
      this.simpleName = simpleName;
    }
    ImmutableList.Builder<DiscoveryField> propertiesBuilder = ImmutableList.builder();
    for (Schema child : this.schema.properties().values()) {
      propertiesBuilder.add(DiscoveryField.create(child, apiModel));
    }
    this.properties = propertiesBuilder.build();
  }

  /** Create a FieldModel object from a non-null Schema object. */
  public static DiscoveryField create(Schema schema, DiscoApiModel rootApiModel) {
    Preconditions.checkNotNull(schema);
    Preconditions.checkNotNull(rootApiModel);
    if (globalObjects.containsKey(schema)) {
      return globalObjects.get(schema);
    }
    if (!Strings.isNullOrEmpty(schema.reference())) {
      // First create a DiscoveryField for the underlying referenced Schema.
      create(schema.dereference(), rootApiModel);
    }
    DiscoveryField field = new DiscoveryField(schema, rootApiModel);
    globalObjects.put(schema, field);
    return field;
  }

  /** @return the underlying Discovery Schema. */
  public Schema getDiscoveryField() {
    return schema;
  }

  @Override
  public String getSimpleName() {
    return simpleName;
  }

  @Override
  public String getFullName() {
    return DiscoGapicParser.getSchemaNameAsParameter(originalSchema).toUpperCamel();
  }

  @Override
  public String getNameAsParameter() {
    return getNameAsParameterName().toLowerCamel();
  }

  @Override
  public Name getNameAsParameterName() {
    return DiscoGapicParser.getSchemaNameAsParameter(originalSchema);
  }

  @Override
  public String getTypeFullName() {
    return originalSchema.getIdentifier();
  }

  @Override
  public boolean isMap() {
    return originalSchema.additionalProperties() != null;
  }

  @Override
  public TypeModel getMapKeyType() {
    if (isMap()) {
      // Assume that the schema's additionalProperties map keys are Strings.
      return StringTypeModel.getInstance();
    }
    return null;
  }

  @Override
  public TypeModel getMapValueType() {
    if (isMap()) {
      return DiscoveryField.create(originalSchema.additionalProperties(), apiModel);
    }
    return null;
  }

  @Override
  public boolean isMessage() {
    return !isPrimitiveType();
  }

  @Override
  public boolean isRequired() {
    return schema.required();
  }

  @Override
  public boolean isRepeated() {
    return schema.type() == Type.ARRAY;
  }

  @Override
  public boolean mayBeInResourceName() {
    // A ResourceName will only contain path parameters.
    return schema.isPathParam();
  }

  @Override
  public String getParentFullName() {
    String parentName;
    if (schema.parent() instanceof Method) {
      parentName = DiscoGapicParser.getRequestName((Method) schema.parent()).toUpperCamel();
    } else if (schema.parent() instanceof Schema) {
      parentName = Name.anyCamel(((Schema) schema.parent()).getIdentifier()).toUpperCamel();
    } else if (schema.parent() instanceof Document) {
      parentName = ((Document) schema.parent()).name();
    } else {
      parentName = "";
    }

    return ResourceNameMessageConfig.getFullyQualifiedMessageName(
        apiModel.getDefaultPackageName(), parentName);
  }

  @Override
  public String getParentSimpleName() {
    return schema.parent().id();
  }

  @Override
  public TypeName getParentTypeName(ImportTypeTable typeTable) {
    if (schema.parent() instanceof Schema) {
      DiscoveryField parent = DiscoveryField.create((Schema) schema.parent(), apiModel);
      return typeTable.getTypeTable().getTypeName(typeTable.getFullNameFor((FieldModel) parent));
    }
    return typeTable.getTypeTable().getTypeName(typeTable.getFullNameFor((FieldModel) this));
  }

  @Override
  public Cardinality getCardinality() {
    throw new IllegalArgumentException("Discovery model types have no defined Cardinality.");
  }

  @Override
  public boolean isEnum() {
    // TODO(andrealin): implement.
    return false;
  }

  public static boolean isTopLevelSchema(Schema schema) {
    return !schema.properties().isEmpty()
        || (schema.items() != null && !schema.items().properties().isEmpty());
  }

  @Override
  public boolean isPrimitive() {
    return schema.items() == null && schema.type() != Type.OBJECT;
  }

  @Override
  /* @Get the description of the element scoped to the visibility as currently set in the model. */
  public String getScopedDocumentation() {
    return schema.description();
  }

  @Override
  public boolean isString() {
    return schema.type().equals(Type.STRING);
  }

  @Override
  public boolean isBytes() {
    return schema.type().equals(Type.ANY)
        || (schema.type().equals(Type.STRING) && schema.format().equals(Format.BYTE));
  }

  @Override
  public String getKind() {
    return schema.type().toString();
  }

  @Nullable
  @Override
  public Oneof getOneof() {
    return null;
  }

  @Override
  public void validateValue(String value) {
    switch (schema.type()) {
      case BOOLEAN:
        String lowerCaseValue = value.toLowerCase();
        if (lowerCaseValue.equals("true") || lowerCaseValue.equals("false")) {
          return;
        }
        break;
      case NUMBER:
        if (Pattern.matches("[+-]?([0-9]*[.])?[0-9]+", value)) {
          return;
        }
        break;
      case INTEGER:
        if (Pattern.matches("[+-]?[0-9]+", value)) {
          return;
        }
        break;
      case STRING:
        switch (schema.format()) {
          case INT64:
          case UINT64:
            if (Pattern.matches("[+-]?[0-9]+", value)) {
              return;
            }
            break;
          default:
            Matcher matcher = Pattern.compile("([^\\\"']*)").matcher(value);
            if (matcher.matches()) {
              return;
            }
            break;
        }
      default:
        // Throw an exception if a value is unsupported for the given type.
        throw new IllegalArgumentException(
            "Tried to assign value for unsupported Schema type "
                + schema.type()
                + ", format "
                + schema.format()
                + "; value "
                + value);
    }
    throw new IllegalArgumentException(
        "Could not assign value '"
            + value
            + "' to type "
            + schema.type()
            + ", format "
            + schema.format());
  }

  @Override
  public List<DiscoveryField> getFields() {
    return properties;
  }

  @Override
  public DiscoveryField getField(String key) {
    for (DiscoveryField field : getFields()) {
      if (field.getNameAsParameter().equals(key)) {
        return field;
      }
    }

    Schema parentTypeSchema = getDiscoveryField();
    List<Schema> pathToKeySchema = parentTypeSchema.findChild(key);
    if (pathToKeySchema.size() == 0) {
      return null; // key not found.
    }
    return DiscoveryField.create(pathToKeySchema.get(pathToKeySchema.size() - 1), apiModel);
  }

  @Override
  // Schemas are immutable, so this is just the identity function.
  public TypeModel makeOptional() {
    return this;
  }

  @Override
  public String getPrimitiveTypeName() {
    Preconditions.checkArgument(isPrimitiveType());
    switch (schema.type()) {
      case INTEGER:
        switch (schema.format()) {
          case UINT32:
            return "uint32";
          default:
            return "int32";
        }
      case NUMBER:
        switch (schema.format()) {
          case FLOAT:
            return "float";
          case DOUBLE:
          default:
            return "double";
        }
      case BOOLEAN:
        return "bool";
      case STRING:
        if (schema.format() == null) {
          return "string";
        }
        switch (schema.format()) {
          case BYTE:
            return "bytes";
          case INT64:
            return "sint64";
          case UINT64:
            return "uint64";
          default:
            return "string";
        }
      default:
        return null;
    }
  }

  private boolean isPrimitiveType() {
    return schema.type().equals(Type.BOOLEAN)
        || schema.type().equals(Type.INTEGER)
        || schema.type().equals(Type.NUMBER)
        || schema.type().equals(Type.STRING);
  }

  @Override
  public boolean isBooleanType() {
    return schema.type().equals(Type.BOOLEAN);
  }

  @Override
  public boolean isStringType() {
    return schema.type().equals(Type.STRING);
  }

  @Override
  public boolean isFloatType() {
    return schema.type().equals(Type.NUMBER) && schema.format().equals(Format.FLOAT);
  }

  @Override
  public boolean isBytesType() {
    return schema.type().equals(Type.STRING) && schema.format().equals(Format.BYTE);
  }

  @Override
  public boolean isDoubleType() {
    return schema.type().equals(Type.NUMBER) && schema.format().equals(Format.DOUBLE);
  }

  @Override
  public String getTypeName() {
    if (isPrimitiveType()) {
      return getPrimitiveTypeName();
    }
    switch (schema.type()) {
      case ARRAY:
        return "list";
      default:
        return "message";
    }
  }

  @Override
  public DiscoveryField getType() {
    return this;
  }

  @Override
  public boolean isEmptyType() {
    return false;
  }

  @Override
  public OneofConfig getOneOfConfig(String fieldName) {
    return null;
  }

  @Override
  public int hashCode() {
    return 5 + 31 * schema.hashCode() + 37 * getParentFullName().hashCode();
  }

  @Override
  public String toString() {
    return String.format("Discovery FieldModel: {%s}", schema.toString());
  }

  @Override
  public boolean equals(Object o) {
    return o != null
        && o instanceof DiscoveryField
        && ((DiscoveryField) o).schema.equals(this.schema)
        && getParentFullName().equals(((DiscoveryField) o).getParentFullName());
  }
}
