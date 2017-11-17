/* Copyright 2017 Google LLC
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
package com.google.api.codegen.config;

import static com.google.api.codegen.config.ApiSource.DISCOVERY;

import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.discovery.Schema.Format;
import com.google.api.codegen.discovery.Schema.Type;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.tools.framework.model.Oneof;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/** A field declaration wrapper around a Discovery Schema. */
public class DiscoveryField implements FieldModel, TypeModel {
  private final List<DiscoveryField> properties;
  private final Schema schema;
  private final DiscoGapicNamer discoGapicNamer;

  /* Create a FieldModel object from a non-null Schema object. */
  private DiscoveryField(Schema schema, DiscoGapicNamer discoGapicNamer) {
    Preconditions.checkNotNull(schema);
    this.schema = schema;
    this.discoGapicNamer = discoGapicNamer;

    ImmutableList.Builder<DiscoveryField> propertiesBuilder = ImmutableList.builder();
    for (Schema child : schema.properties().values()) {
      propertiesBuilder.add(DiscoveryField.create(child, discoGapicNamer));
    }
    this.properties = propertiesBuilder.build();
  }

  /* Create a FieldModel object from a non-null Schema object. */
  public static DiscoveryField create(Schema schema, DiscoGapicNamer discoGapicNamer) {
    Preconditions.checkNotNull(schema);
    return new DiscoveryField(schema, discoGapicNamer);
  }

  @Override
  /* @return the type of the underlying model resource. */
  public ApiSource getApiSource() {
    return DISCOVERY;
  }

  /* @return the underlying Discovery Schema. */
  public Schema getDiscoveryField() {
    return schema;
  }

  @Override
  public String getSimpleName() {
    String name =
        Strings.isNullOrEmpty(schema.reference()) ? schema.getIdentifier() : schema.reference();
    String[] pieces = name.split("_");
    return Name.anyCamel(pieces).toLowerCamel();
  }

  @Override
  public String getFullName() {
    return discoGapicNamer
        .getLanguageNamer()
        .publicClassName(DiscoGapicNamer.getSchemaNameAsParameter(schema));
  }

  @Override
  public String getNameAsParameter() {
    return getNameAsParameterName().toLowerCamel();
  }

  @Override
  public Name getNameAsParameterName() {
    return DiscoGapicNamer.getSchemaNameAsParameter(schema);
  }

  @Override
  public Name asName() {
    return Name.anyCamel(getSimpleName());
  }

  @Override
  public String getTypeFullName() {
    return schema.getIdentifier();
  }

  @Override
  public boolean isMap() {
    return false;
  }

  @Override
  public FieldModel getMapKeyField() {
    throw new IllegalArgumentException("Discovery model types have no map keys.");
  }

  @Override
  public FieldModel getMapValueField() {
    throw new IllegalArgumentException("Discovery model types have no map values.");
  }

  @Override
  public boolean isMessage() {
    return false;
  }

  @Override
  public boolean isRequired() {
    return schema.required();
  }

  @Override
  public boolean isRepeated() {
    if (schema.type() == Type.ARRAY) {
      return true;
    }
    if (!Strings.isNullOrEmpty(schema.reference()) && schema.dereference() != null) {
      return schema.dereference().type() == Type.ARRAY;
    }
    return false;
  }

  @Override
  public boolean mayBeInResourceName() {
    // A ResourceName will only contain path parameters.
    return schema.isPathParam();
  }

  @Override
  public String getParentFullName() {
    SurfaceNamer surfaceNamer = discoGapicNamer.getLanguageNamer();
    TypeNameConverter typeNameConverter = surfaceNamer.getTypeNameConverter();
    if (schema.parent() instanceof Method) {
      return typeNameConverter
          .getTypeNameInImplicitPackage(
              surfaceNamer.publicClassName(
                  DiscoGapicNamer.getRequestName((Method) schema.parent())))
          .getFullName();
    } else if (schema.parent() instanceof Schema) {
      return typeNameConverter
          .getTypeNameInImplicitPackage(
              surfaceNamer.publicClassName(
                  Name.anyCamel(((Schema) schema.parent()).getIdentifier())))
          .getFullName();
    } else if (schema.parent() instanceof Document) {
      return ((Document) schema.parent()).name();
    }
    return "";
  }

  @Override
  public String getParentSimpleName() {
    return schema.parent().id();
  }

  @Override
  public TypeName getParentTypeName(ImportTypeTable typeTable) {
    if (schema.parent() instanceof Schema) {
      DiscoveryField parent = DiscoveryField.create((Schema) schema.parent(), discoGapicNamer);
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
    return schema.isEnum();
  }

  @Override
  public boolean isPrimitive() {
    return schema.reference().isEmpty() && schema.items() == null;
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

  public DiscoGapicNamer getDiscoGapicNamer() {
    return discoGapicNamer;
  }

  @Nullable
  @Override
  public Oneof getOneof() {
    return null;
  }

  @Override
  public List<String> getPagedResponseResourceMethods(
      FeatureConfig featureConfig, FieldConfig startingFieldConfig, SurfaceNamer namer) {
    List<String> methodNames = new LinkedList<>();
    for (FieldModel field : startingFieldConfig.getFieldPath()) {
      methodNames.add(0, namer.getFieldGetFunctionName(field));
    }
    return ImmutableList.copyOf(methodNames);
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
  public List<? extends FieldModel> getFields() {
    return properties;
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
        return "boolean";
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
    return schema.type().equals(Type.STRING) && schema.format() == null;
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
    return schema.getIdentifier().equals("Empty")
        && schema.type().equals(Type.OBJECT)
        && (schema.properties() == null || schema.properties().size() == 0);
  }

  @Override
  public OneofConfig getOneOfConfig(String fieldName) {
    return null;
  }

  @Override
  public int hashCode() {
    return 5 + 31 * schema.hashCode();
  }

  @Override
  public String toString() {
    return String.format("Discovery FieldModel (%s): {%s}", getApiSource(), schema.toString());
  }

  @Override
  public boolean equals(Object o) {
    return o != null
        && o instanceof DiscoveryField
        && ((DiscoveryField) o).schema.equals(this.schema);
  }
}
