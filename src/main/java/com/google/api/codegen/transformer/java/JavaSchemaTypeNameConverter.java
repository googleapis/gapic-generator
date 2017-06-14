/* Copyright 2017 Google Inc
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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.discovery.Schema.Type;
import com.google.api.codegen.transformer.SchemaTypeNameConverter;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.java.JavaNameFormatter;
import com.google.api.codegen.util.java.JavaTypeTable;

/** The Schema TypeName converter for Java. */
public class JavaSchemaTypeNameConverter implements SchemaTypeNameConverter {

  /** The package prefix protoc uses if no java package option was provided. */
  private static final String DEFAULT_JAVA_PACKAGE_PREFIX = "com.google.discovery";

  private TypeNameConverter typeNameConverter;
  private JavaNameFormatter nameFormatter;
  private String implicitPackageName;

  public JavaSchemaTypeNameConverter(String implicitPackageName, JavaNameFormatter nameFormatter) {
    this.typeNameConverter = new JavaTypeTable(implicitPackageName);
    this.nameFormatter = nameFormatter;
    this.implicitPackageName = implicitPackageName;
  }

  private static String getPrimitiveTypeName(Schema schema) {
    switch (schema.type()) {
      case INTEGER:
        switch (schema.format()) {
          case INT32:
            return "int";
          case UINT32:
          default:
            return "long";
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
    if (primitiveType.equals("String")) {
      return "\"\"";
    }
    throw new IllegalArgumentException("Schema is of unknown type.");
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    return typeNameConverter.getTypeNameInImplicitPackage(shortName);
  }

  @Override
  public TypeName getTypeName(String escapedSchemaName, Schema schema) {
    if (schema.type() == Type.ARRAY) {
      TypeName listTypeName = typeNameConverter.getTypeName("java.util.List");
      TypeName elementTypeName = getTypeNameForElementType(escapedSchemaName, schema, null, true);
      return new TypeName(
          listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", elementTypeName);
    } else {
      return getTypeNameForElementType(escapedSchemaName, schema, null, false);
    }
  }

  @Override
  public TypedValue getEnumValue(String escapedSchemaName, Schema schema, String value) {
    return TypedValue.create(getTypeName(escapedSchemaName, schema), "%s." + value);
  }

  @Override
  public TypeName getTypeNameForElementType(
      String escapedSchemaName, Schema type, String parentName) {
    return getTypeNameForElementType(escapedSchemaName, type, parentName, true);
  }

  /**
   * Returns the Java representation of a type, without cardinality. If the type is a Java
   * primitive, basicTypeName returns it in unboxed form.
   *
   * @param schema The Schema to generate a TypeName from.
   * @param parentName The TypeName of the parent Schema that encloses this Schema.
   *     <p>This method will be recursively called on the given schema's children.
   */
  private TypeName getTypeNameForElementType(
      String escapedSchemaName, Schema schema, String parentName, boolean shouldBoxPrimitives) {
    String primitiveTypeName = getPrimitiveTypeName(schema);
    if (primitiveTypeName != null) {
      if (primitiveTypeName.contains(".")) {
        // Fully qualified type name, use regular type name resolver. Can skip boxing logic
        // because those types are already boxed.
        return typeNameConverter.getTypeName(primitiveTypeName);
      } else {
        if (shouldBoxPrimitives) {
          return new TypeName(JavaTypeTable.getBoxedTypeName(primitiveTypeName));
        } else {
          return new TypeName(primitiveTypeName);
        }
      }
    } else if (schema.type() == Type.ARRAY) {
      // TODO(andrealin): ensure that this handles arrays of arrays.
      TypeName listTypeName = typeNameConverter.getTypeName("java.util.List");
      TypeName elementTypeName =
          getTypeNameForElementType(escapedSchemaName, schema.items(), parentName, true);
      return new TypeName(
          listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", elementTypeName);
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
        shortName = nameFormatter.publicClassName(Name.anyCamel(escapedSchemaName));
        packageName =
            packageName + "." + nameFormatter.publicClassName(Name.anyCamel(parentName)).toString();
      }
      String longName = packageName + "." + shortName;

      return new TypeName(longName, shortName);
    }
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
    if (primitiveType.equals("String")) {
      return "\"" + value + "\"";
    }
    throw new IllegalArgumentException("Schema is of unknown type.");
  }

  /**
   * Returns the Java representation of a zero value for that type, to be used in code sample doc.
   *
   * <p>Parametric types may use the diamond operator, since the return value will be used only in
   * initialization.
   */
  @Override
  public TypedValue getSnippetZeroValue(String escapedSchemaName, Schema schema) {
    if (schema.type() == Schema.Type.ARRAY) {
      return TypedValue.create(typeNameConverter.getTypeName("java.util.ArrayList"), "new %s<>()");
    }
    if (getPrimitiveTypeName(schema) != null) {
      return TypedValue.create(
          getTypeName(escapedSchemaName, schema), getPrimitiveZeroValue(schema));
    }
    if (schema.type() == Type.OBJECT) {
      return TypedValue.create(getTypeName(escapedSchemaName, schema), "%s.newBuilder().build()");
    }
    return TypedValue.create(getTypeName(escapedSchemaName, schema), "null");
  }

  @Override
  public TypedValue getImplZeroValue(String escapedSchemaName, Schema type) {
    return getSnippetZeroValue(escapedSchemaName, type);
  }

  @Override
  public TypeName getTypeNameForTypedResourceName(
      FieldConfig fieldConfig, String typedResourceShortName) {
    // TODO(andrealin)
    return null;
  }

  @Override
  public TypeName getTypeNameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName) {
    // TODO(andrealin)
    return null;
  }
}
