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

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.ResourceNameConfig;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.discovery.Schema.Type;
import com.google.api.codegen.transformer.SchemaTypeNameConverter;
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
  private final DiscoGapicNamer discoGapicNamer;

  public JavaSchemaTypeNameConverter(String implicitPackageName, JavaNameFormatter nameFormatter) {
    this.typeNameConverter = new JavaTypeTable(implicitPackageName);
    this.nameFormatter = nameFormatter;
    this.implicitPackageName = implicitPackageName;
    this.discoGapicNamer =
        new DiscoGapicNamer(new JavaSurfaceNamer(implicitPackageName, implicitPackageName));
  }

  private static String getPrimitiveTypeName(Schema schema) {
    if (schema == null) {
      return "java.lang.Void";
    }
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

  /** A map from primitive types in proto to zero values in Java. */
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
    if (primitiveType.equals("java.lang.Void")) {
      return "null";
    }
    throw new IllegalArgumentException("Schema is of unknown type.");
  }

  @Override
  public DiscoGapicNamer getDiscoGapicNamer() {
    return discoGapicNamer;
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    return typeNameConverter.getTypeNameInImplicitPackage(shortName);
  }

  @Override
  public TypeName getTypeName(Schema schema) {
    return getTypeName(schema, BoxingBehavior.NO_BOX_PRIMITIVES);
  }

  @Override
  public TypedValue getEnumValue(Schema schema, String value) {
    return TypedValue.create(getTypeName(schema), "%s." + value);
  }

  /**
   * Returns the Java representation of a type, without cardinality. If the type is a Java
   * primitive, basicTypeName returns it in unboxed form.
   *
   * @param schema The Schema to generate a TypeName from.
   *     <p>This method will be recursively called on the given schema's children.
   */
  private TypeName getTypeNameForElementType(Schema schema, BoxingBehavior boxingBehavior) {
    if (schema == null) {
      return new TypeName("java.lang.Void", "Void");
    }
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
      String packageName = implicitPackageName;
      Schema element = schema.items();
      if (!Strings.isNullOrEmpty(element.reference())) {
        String shortName =
            element.reference() != null ? element.reference() : element.getIdentifier();
        String longName = packageName + "." + shortName;
        return new TypeName(longName, shortName);
      } else {
        return getTypeName(schema.items(), BoxingBehavior.BOX_PRIMITIVES);
      }
    } else {
      String packageName =
          !implicitPackageName.isEmpty() ? implicitPackageName : DEFAULT_JAVA_PACKAGE_PREFIX;
      String shortName = "";
      if (!schema.id().isEmpty()) {
        shortName = schema.id();
      } else if (!schema.reference().isEmpty()) {
        shortName = schema.reference();
      } else if (schema.additionalProperties() != null
          && !schema.additionalProperties().reference().isEmpty()) {
        shortName = schema.additionalProperties().reference();
      } else {
        // This schema has a parent Schema.
        shortName = nameFormatter.publicClassName(Name.anyCamel(schema.key()));
      }
      String longName = packageName + "." + shortName;

      return new TypeName(longName, shortName);
    }
  }

  /**
   * Returns the Java representation of a type, with cardinality. If the type is a Java primitive,
   * basicTypeName returns it in unboxed form.
   *
   * @param schema The Schema to generate a TypeName from.
   *     <p>This method will be recursively called on the given schema's children.
   */
  @Override
  public TypeName getTypeName(Schema schema, BoxingBehavior boxingBehavior) {
    TypeName elementTypeName = getTypeNameForElementType(schema, BoxingBehavior.BOX_PRIMITIVES);
    if (schema == null) {
      return elementTypeName;
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
  public TypeName getTypeNameForElementType(Schema type) {
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
    }
    if (primitiveType.equals("int")
        || primitiveType.equals("long")
        || primitiveType.equals("double")
        || primitiveType.equals("float")) {
      return value;
    }
    if (primitiveType.equals("java.lang.String")) {
      return "\"" + value + "\"";
    }
    throw new IllegalArgumentException("Schema is of unknown type.");
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
  public TypedValue getSnippetZeroValue(Schema schema) {
    if (getPrimitiveTypeName(schema) != null) {
      return TypedValue.create(getTypeName(schema), getPrimitiveZeroValue(schema));
    }
    if (schema.type() == Type.OBJECT || schema.type() == Type.ARRAY) {
      return TypedValue.create(getTypeNameForElementType(schema), "%s.newBuilder().build()");
    }
    return TypedValue.create(getTypeName(schema), "null");
  }

  @Override
  public TypedValue getImplZeroValue(Schema type) {
    return getSnippetZeroValue(type);
  }

  private TypeName getTypeNameForTypedResourceName(
      ResourceNameConfig resourceNameConfig, FieldModel type, String typedResourceShortName) {
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
    return getTypeNameForTypedResourceName(
        fieldConfig.getResourceNameConfig(), fieldConfig.getField(), typedResourceShortName);
  }

  @Override
  public TypeName getTypeNameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName) {
    return getTypeNameForTypedResourceName(
        fieldConfig.getResourceNameConfig(), fieldConfig.getField(), typedResourceShortName);
  }
}
