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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.config.DiscoveryField;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.discovery.Schema.Type;
import com.google.api.codegen.transformer.SchemaTypeNameConverter;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.java.JavaNameFormatter;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.common.base.Strings;

/** The Schema TypeName converter for Java. */
public class JavaSchemaTypeNameConverter extends SchemaTypeNameConverter {

  /** The package prefix protoc uses if no java package option was provided. */
  private static final String DEFAULT_JAVA_PACKAGE_PREFIX = "com.google.discovery";

  private final TypeNameConverter typeNameConverter;
  private final JavaNameFormatter nameFormatter;
  private final String implicitPackageName;
  private final DiscoGapicNamer discoGapicNamer = new DiscoGapicNamer();
  private final JavaSurfaceNamer namer;

  public JavaSchemaTypeNameConverter(String implicitPackageName, JavaNameFormatter nameFormatter) {
    this.typeNameConverter = new JavaTypeTable(implicitPackageName);
    this.nameFormatter = nameFormatter;
    this.implicitPackageName = implicitPackageName;
    this.namer = new JavaSurfaceNamer(implicitPackageName, implicitPackageName);
  }

  private static String getPrimitiveTypeName(Schema schema) {
    switch (schema.type()) {
      case INTEGER:
        return "int";
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
        return "java.lang.String";
      default:
        return null;
    }
  }

  /**
   * A map from primitive types in proto to zero values in Java. Returns 'Void' if input is null.
   */
  private static String getPrimitiveZeroValue(Schema schema) {
    String primitiveType = getPrimitiveTypeName(schema);
    if (primitiveType == null) {
      return null;
    }
    if (primitiveType.equals("boolean")) {
      return "false";
    }
    if (primitiveType.equals("int")
        || primitiveType.equals("long")
        || primitiveType.equals("double")
        || primitiveType.equals("float")) {
      return "0";
    }
    if (primitiveType.equals("java.lang.String")) {
      return "\"\"";
    }
    throw new IllegalArgumentException("Schema is of unknown type.");
  }

  @Override
  public DiscoGapicNamer getDiscoGapicNamer() {
    return discoGapicNamer;
  }

  @Override
  public SurfaceNamer getNamer() {
    return namer;
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    return typeNameConverter.getTypeNameInImplicitPackage(shortName);
  }

  @Override
  public TypeName getTypeName(DiscoveryField field) {
    return getTypeName(field, BoxingBehavior.NO_BOX_PRIMITIVES);
  }

  @Override
  public TypedValue getEnumValue(DiscoveryField field, String value) {
    return TypedValue.create(getTypeName(field), "%s." + value);
  }

  @Override
  public TypeName getTypeNameForElementType(TypeModel type) {
    if (type.isStringType()) {
      return typeNameConverter.getTypeName("java.lang.String");
    } else if (type.isEmptyType()) {
      return typeNameConverter.getTypeName("com.google.api.gax.httpjson.EmptyMessage");
    }

    return getTypeNameForElementType((DiscoveryField) type);
  }

  /**
   * Returns the Java representation of a type, without cardinality. If the type is a Java
   * primitive, basicTypeName returns it in unboxed form.
   *
   * @param fieldModel The Schema to generate a TypeName from.
   *     <p>This method will be recursively called on the given schema's children.
   */
  private TypeName getTypeNameForElementType(
      DiscoveryField fieldModel, BoxingBehavior boxingBehavior) {
    if (fieldModel == null) {
      return new TypeName("com.google.api.gax.httpjson.EmptyMessage", "EmptyMessage");
    }
    Schema schema = fieldModel.getOriginalDiscoveryField();
    String primitiveTypeName = getPrimitiveTypeName(schema);
    if (primitiveTypeName != null) {
      if (primitiveTypeName.contains(".")) {
        // Fully qualified type name, use regular type name resolver. Can skip boxing logic
        // because those types are already boxed.
        return typeNameConverter.getTypeName(primitiveTypeName);
      } else {
        switch (boxingBehavior) {
          case BOX_PRIMITIVES:
            return new TypeName(JavaTypeTable.getBoxedTypeName(primitiveTypeName));
          case NO_BOX_PRIMITIVES:
            return new TypeName(primitiveTypeName);
          default:
            throw new IllegalArgumentException(
                String.format("Unhandled boxing behavior: %s", boxingBehavior.name()));
        }
      }
    }
    if (schema.type().equals(Type.ARRAY)) {
      return getTypeName(
          DiscoveryField.create(schema.dereference().items(), fieldModel.getDiscoApiModel()),
          BoxingBehavior.BOX_PRIMITIVES);
    } else {
      String packageName =
          !implicitPackageName.isEmpty() ? implicitPackageName : DEFAULT_JAVA_PACKAGE_PREFIX;
      String shortName = "";
      if (schema.additionalProperties() != null
          && !Strings.isNullOrEmpty(schema.additionalProperties().reference())) {
        shortName = schema.additionalProperties().reference();
      } else {
        shortName = namer.publicClassName(Name.anyCamel(fieldModel.getSimpleName()));
      }
      String longName = packageName + "." + shortName;

      return new TypeName(longName, shortName);
    }
  }

  /**
   * Returns the Java representation of a type, with cardinality. If the type is a Java primitive,
   * basicTypeName returns it in unboxed form.
   *
   * @param field The Schema to generate a TypeName from.
   *     <p>This method will be recursively called on the given schema's children.
   */
  @Override
  public TypeName getTypeName(DiscoveryField field, BoxingBehavior boxingBehavior) {
    TypeName elementTypeName = getTypeNameForElementType(field, BoxingBehavior.BOX_PRIMITIVES);
    if (field == null) {
      return elementTypeName;
    }
    Schema schema = field.getDiscoveryField();

    if (schema.isMap()) {
      TypeName mapTypeName = typeNameConverter.getTypeName("java.util.Map");
      TypeName keyTypeName = typeNameConverter.getTypeName("java.lang.String");
      TypeName valueTypeName =
          getTypeNameForElementType(
              DiscoveryField.create(schema.additionalProperties(), field.getDiscoApiModel()),
              BoxingBehavior.BOX_PRIMITIVES);
      return new TypeName(
          mapTypeName.getFullName(),
          mapTypeName.getNickname(),
          "%s<%i, %i>",
          keyTypeName,
          valueTypeName);
    }
    if (schema.repeated() || schema.type().equals(Type.ARRAY)) {
      TypeName listTypeName = typeNameConverter.getTypeName("java.util.List");
      return new TypeName(
          listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", elementTypeName);
    } else {
      return elementTypeName;
    }
  }

  @Override
  public TypeName getTypeNameForElementType(DiscoveryField type) {
    return getTypeNameForElementType(type, BoxingBehavior.BOX_PRIMITIVES);
  }

  @Override
  public String renderPrimitiveValue(Schema schema, String value) {
    String primitiveType = getPrimitiveTypeName(schema);
    if (primitiveType == null) {
      throw new IllegalArgumentException(
          "Initial values are only supported for primitive types, got type "
              + schema
              + ", with value "
              + value);
    }
    if (primitiveType.equals("boolean")) {
      return value.toLowerCase();
    } else if (primitiveType.equals("long")) {
      return value + "L";
    } else if (primitiveType.equals("float")) {
      return value + "F";
    }
    if (primitiveType.equals("int") || primitiveType.equals("double")) {
      return value;
    }
    if (primitiveType.equals("java.lang.String")) {
      return "\"" + value + "\"";
    }
    throw new IllegalArgumentException("Schema is of unknown type.");
  }

  @Override
  public String renderPrimitiveValue(TypeModel type, String value) {
    if (type.isStringType()) {
      return "\"" + value + "\"";
    }
    return renderPrimitiveValue(((DiscoveryField) type).getDiscoveryField(), value);
  }

  @Override
  public String renderValueAsString(String value) {
    return "\"" + value + "\"";
  }

  /**
   * Returns the Java representation of a zero value for that type, to be used in code sample doc.
   *
   * <p>Parametric types may use the diamond operator, since the return value will be used only in
   * initialization.
   */
  @Override
  public TypedValue getSnippetZeroValue(DiscoveryField field) {
    Schema schema = field.getDiscoveryField();
    if (getPrimitiveTypeName(schema) != null) {
      return TypedValue.create(getTypeName(field), getPrimitiveZeroValue(schema));
    }
    if (field.isMap()) {
      return TypedValue.create(typeNameConverter.getTypeName("java.util.HashMap"), "new %s<>()");
    }
    if (field.isRepeated()) {
      return TypedValue.create(typeNameConverter.getTypeName("java.util.ArrayList"), "new %s<>()");
    }
    if (schema.type() == Type.OBJECT) {
      return TypedValue.create(getTypeNameForElementType(field), "%s.newBuilder().build()");
    }
    return TypedValue.create(getTypeName(field), "null");
  }

  /**
   * Returns the Java representation of a zero value for that type, to be used in code sample doc.
   *
   * <p>Parametric types may use the diamond operator, since the return value will be used only in
   * initialization.
   */
  @Override
  public TypedValue getSnippetZeroValue(TypeModel typeModel) {
    if (typeModel.isEmptyType()) {
      return TypedValue.create(getTypeNameForElementType(typeModel), "%s.newBuilder().build()");
    }
    return getSnippetZeroValue((DiscoveryField) typeModel);
  }

  @Override
  public TypedValue getImplZeroValue(DiscoveryField type) {
    return getSnippetZeroValue(type);
  }

  private TypeName getTypeNameForTypedResourceName(FieldModel type, String typedResourceShortName) {
    String packageName = implicitPackageName;
    String longName = packageName + "." + typedResourceShortName;

    TypeName simpleTypeName = new TypeName(longName, typedResourceShortName);

    if (type.isMap()) {
      throw new IllegalArgumentException("Map type not supported for typed resource name");
    } else if (type.isRepeated()) {
      TypeName listTypeName = typeNameConverter.getTypeName("java.util.List");
      return new TypeName(
          listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", simpleTypeName);
    } else {
      return simpleTypeName;
    }
  }

  @Override
  public TypeName getTypeNameForTypedResourceName(
      FieldConfig fieldConfig, String typedResourceShortName) {
    return getTypeNameForTypedResourceName(fieldConfig.getField(), typedResourceShortName);
  }

  @Override
  public TypeName getTypeNameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName) {
    return getTypeNameForTypedResourceName(fieldConfig.getField(), typedResourceShortName);
  }
}
