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

import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.ResourceNameConfig;
import com.google.api.codegen.config.ResourceNameType;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.transformer.SchemaTypeNameConverter;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import java.io.File;
import javax.print.Doc;

/** The SchemaTypeTable for Java. */
public class JavaSchemaTypeNameConverter implements SchemaTypeNameConverter {

  /** The package prefix protoc uses if no java package option was provided. */
  private static final String DEFAULT_JAVA_PACKAGE_PREFIX = "com.google.protos";

  /** A map from primitive types in proto to Java counterparts. */
  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "boolean")
          .put(Type.TYPE_DOUBLE, "double")
          .put(Type.TYPE_FLOAT, "float")
          .put(Type.TYPE_INT64, "long")
          .put(Type.TYPE_UINT64, "long")
          .put(Type.TYPE_SINT64, "long")
          .put(Type.TYPE_FIXED64, "long")
          .put(Type.TYPE_SFIXED64, "long")
          .put(Type.TYPE_INT32, "int")
          .put(Type.TYPE_UINT32, "int")
          .put(Type.TYPE_SINT32, "int")
          .put(Type.TYPE_FIXED32, "int")
          .put(Type.TYPE_SFIXED32, "int")
          .put(Type.TYPE_STRING, "java.lang.String")
          .put(Type.TYPE_BYTES, "com.google.protobuf.ByteString")
          .build();

  /** A map from primitive types in proto to zero values in Java. */
  private static final ImmutableMap<Type, String> PRIMITIVE_ZERO_VALUE =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "false")
          .put(Type.TYPE_DOUBLE, "0.0")
          .put(Type.TYPE_FLOAT, "0.0F")
          .put(Type.TYPE_INT64, "0L")
          .put(Type.TYPE_UINT64, "0L")
          .put(Type.TYPE_SINT64, "0L")
          .put(Type.TYPE_FIXED64, "0L")
          .put(Type.TYPE_SFIXED64, "0L")
          .put(Type.TYPE_INT32, "0")
          .put(Type.TYPE_UINT32, "0")
          .put(Type.TYPE_SINT32, "0")
          .put(Type.TYPE_FIXED32, "0")
          .put(Type.TYPE_SFIXED32, "0")
          .put(Type.TYPE_STRING, "\"\"")
          .put(Type.TYPE_BYTES, "ByteString.copyFromUtf8(\"\")")
          .build();

  private TypeNameConverter typeNameConverter;

  public JavaSchemaTypeNameConverter(String implicitPackageName) {
    this.typeNameConverter = new JavaTypeTable(implicitPackageName);
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    return typeNameConverter.getTypeNameInImplicitPackage(shortName);
  }

  @Override
  public TypeName getTypeName(Schema type) {
    if (type.isMap()) {
      TypeName mapTypeName = typeNameConverter.getTypeName("java.util.Map");
      TypeName keyTypeName = getTypeNameForElementType(type.getMapKeyField().getType(), true);
      TypeName valueTypeName = getTypeNameForElementType(type.getMapValueField().getType(), true);
      return new TypeName(
          mapTypeName.getFullName(),
          mapTypeName.getNickname(),
          "%s<%i, %i>",
          keyTypeName,
          valueTypeName);
    } else if (type.isRepeated()) {
      TypeName listTypeName = typeNameConverter.getTypeName("java.util.List");
      TypeName elementTypeName = getTypeNameForElementType(type, true);
      return new TypeName(
          listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", elementTypeName);
    } else {
      return getTypeNameForElementType(type, false);
    }
  }

  @Override
  public TypedValue getEnumValue(Schema type) {
    return null;
  }

  @Override
  public TypedValue getEnumValue(Schema type, EnumValue value) {
    return TypedValue.create(getTypeName(type), "%s." + value.getSimpleName());
  }

  @Override
  public TypeName getTypeNameForElementType(Schema type) {
    return getTypeNameForElementType(type, true);
  }

  /**
   * Returns the Java representation of a type, without cardinality. If the type is a Java
   * primitive, basicTypeName returns it in unboxed form.
   */
  private TypeName getTypeNameForElementType(Schema type, boolean shouldBoxPrimitives) {
    String primitiveTypeName = PRIMITIVE_TYPE_MAP.get(type.getKind());
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
    }
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return getTypeName(type.getMessageType());
      case TYPE_ENUM:
        return getTypeName(type.getEnumType());
      default:
        throw new IllegalArgumentException("unknown type kind: " + type.getKind());
    }
  }

  @Override
  public String renderPrimitiveValue(Schema type, String value) {
    Type primitiveType = type.getKind();
    if (!PRIMITIVE_TYPE_MAP.containsKey(primitiveType)) {
      throw new IllegalArgumentException(
          "Initial values are only supported for primitive types, got type "
              + type
              + ", with value "
              + value);
    }
    switch (primitiveType) {
      case TYPE_BOOL:
        return value.toLowerCase();
      case TYPE_FLOAT:
        return value + "F";
      case TYPE_INT64:
      case TYPE_UINT64:
        return value + "L";
      case TYPE_STRING:
        return "\"" + value + "\"";
      case TYPE_BYTES:
        return "ByteString.copyFromUtf8(\"" + value + "\")";
      default:
        // Types that do not need to be modified (e.g. TYPE_INT32) are handled here
        return value;
    }
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
    if (PRIMITIVE_ZERO_VALUE.containsKey(schema.type())) {
      return TypedValue.create(getTypeName(schema), PRIMITIVE_ZERO_VALUE.get(schema.getKind()));
    }
    if (schema.isMessage()) {
      return TypedValue.create(getTypeName(schema), "%s.newBuilder().build()");
    }
    if (schema.isEnum()) {
      return getEnumValue(schema, schema.getEnumType().getValues().get(0));
    }
    return TypedValue.create(getTypeName(schema), "null");
  }

  @Override
  public TypedValue getImplZeroValue(Schema type) {
    return getSnippetZeroValue(type);
  }

  @Override
  public TypeName getTypeName(Schema elem) {
    String packageName = getProtoElementPackage(elem);
    String shortName = getShortName(elem);
    String longName = packageName + "." + shortName;

    return new TypeName(longName, shortName);
  }

  private TypeName getTypeNameForTypedResourceName(
      ResourceNameConfig resourceNameConfig, Schema type, String typedResourceShortName) {
    String packageName = getResourceNamePackage(resourceNameConfig);
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

  private static String getResourceNamePackage(ResourceNameConfig resourceNameConfig) {
    ResourceNameType resourceNameType = resourceNameConfig.getResourceNameType();
    switch (resourceNameType) {
      case ANY:
        return "com.google.api.resourcenames";
      case FIXED:
      case SINGLE:
      case ONEOF:
        return getJavaPackage(resourceNameConfig.getAssignedProtoFile());
      case NONE:
      default:
        throw new IllegalArgumentException("Unexpected ResourceNameType: " + resourceNameType);
    }
  }

  @Override
  public TypeName getTypeNameForTypedResourceName(
      FieldConfig fieldConfig, String typedResourceShortName) {
    return getTypeNameForTypedResourceName(
        fieldConfig.getResourceNameConfig(),
        fieldConfig.getField().getType(),
        typedResourceShortName);
  }

  @Override
  public TypeName getTypeNameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName) {
    return getTypeNameForTypedResourceName(
        fieldConfig.getResourceNameConfig(),
        fieldConfig.getField().getType().makeOptional(),
        typedResourceShortName);
  }

  public static String getJavaPackage(Document file) {
    String packageName = file.getProto().getOptions().getJavaPackage();
    if (Strings.isNullOrEmpty(packageName)) {
      return DEFAULT_JAVA_PACKAGE_PREFIX + "." + file.getFullName();
    }
    return packageName;
  }

  /** Gets the class name for the given proto file. */
  private static String getFileClassName(Document file) {
    String baseName = Files.getNameWithoutExtension(new File(file.getSimpleName()).getName());
    return LanguageUtil.lowerUnderscoreToUpperCamel(baseName);
  }
}
