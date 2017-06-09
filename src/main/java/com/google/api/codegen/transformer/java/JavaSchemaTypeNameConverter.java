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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Node;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.discovery.Schema.Type;
import com.google.api.codegen.transformer.SchemaTypeNameConverter;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.common.base.Strings;

/** The Schema TypeName converter for Java. */
public class JavaSchemaTypeNameConverter implements SchemaTypeNameConverter {

  /** The package prefix protoc uses if no java package option was provided. */
  private static final String DEFAULT_JAVA_PACKAGE_PREFIX = "com.google.discovery";

  private TypeNameConverter typeNameConverter;

  public JavaSchemaTypeNameConverter(String implicitPackageName) {
    this.typeNameConverter = new JavaTypeTable(implicitPackageName);
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
  public TypeName getTypeName(Schema schema) {
    if (schema.type() == Type.ARRAY) {
      TypeName listTypeName = typeNameConverter.getTypeName("java.util.List");
      TypeName elementTypeName = getTypeNameForElementType(schema, true);
      return new TypeName(
          listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", elementTypeName);
    } else {
      return getTypeNameForElementType(schema, false);
    }
  }

  @Override
  public TypedValue getEnumValue(Schema schema, String value) {
    return TypedValue.create(getTypeName(schema), "%s." + value);
  }

  @Override
  public TypeName getTypeNameForElementType(Schema type) {
    return getTypeNameForElementType(type, true);
  }

  /**
   * Returns the Java representation of a type, without cardinality. If the type is a Java
   * primitive, basicTypeName returns it in unboxed form.
   */
  private TypeName getTypeNameForElementType(Schema schema, boolean shouldBoxPrimitives) {
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
      TypeName listTypeName = typeNameConverter.getTypeName("java.util.List");
      TypeName elementTypeName = getTypeNameForElementType(schema.items(), true);
      return new TypeName(
          listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", elementTypeName);
    } else {
      String packageName = getSchemaPackage(schema);
      String shortName = "";
      if (!schema.id().isEmpty()) {
        shortName = schema.id();
      } else if (!schema.reference().isEmpty()) {
        shortName = schema.reference();
      } else if (schema.additionalProperties() != null
          && !schema.additionalProperties().reference().isEmpty()) {
        shortName = schema.additionalProperties().reference();
      } else {
        // TODO(andrealin): Treat nested schemas as inner classes.
        shortName = "Object";
        packageName = "java.lang";
      }
      String longName = packageName + "." + shortName;

      return new TypeName(longName, shortName);
    }
  }

  private static String getSchemaPackage(Schema schema) {
    String packageName;
    Node parent = schema.parent();
    while (parent != null && !(parent instanceof Document)) {
      parent = parent.parent();
    }
    if (parent == null) {
      packageName = DEFAULT_JAVA_PACKAGE_PREFIX;
    } else {
      packageName = getJavaPackage((Document) parent);
    }

    return packageName;
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
  public TypedValue getSnippetZeroValue(Schema schema) {
    // Don't call importAndGetShortestName; we don't need to import these.
    if (schema.type() == Schema.Type.ARRAY) {
      return TypedValue.create(typeNameConverter.getTypeName("java.util.ArrayList"), "new %s<>()");
    }
    if (getPrimitiveTypeName(schema) != null) {
      return TypedValue.create(getTypeName(schema), getPrimitiveZeroValue(schema));
    }
    if (schema.type() == Type.OBJECT) {
      return TypedValue.create(getTypeName(schema), "%s.newBuilder().build()");
    }
    return TypedValue.create(getTypeName(schema), "null");
  }

  @Override
  public TypedValue getImplZeroValue(Schema type) {
    return getSnippetZeroValue(type);
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

  public static String getJavaPackage(Document file) {
    String packageName = String.format("com.google.%s.%s", file.name(), file.version());
    if (Strings.isNullOrEmpty(packageName)) {
      return DEFAULT_JAVA_PACKAGE_PREFIX + "." + file.name();
    }
    return packageName;
  }
}
