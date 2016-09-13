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
package com.google.api.codegen.discovery.transformer.java;

import com.google.api.codegen.discovery.config.SampleConfig;
import com.google.api.codegen.discovery.config.TypeInfo;
import com.google.api.codegen.discovery.transformer.SampleTypeNameConverter;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Field;

/**
 * Maps SampleConfig and TypeInfo instances to Java specific TypeName instances.
 */
class JavaSampleTypeNameConverter implements SampleTypeNameConverter {

  /**
   * A map from primitive types in proto to Java counterparts.
   */
  private static final ImmutableMap<Field.Kind, String> PRIMIVITVE_TYPE_MAP =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_BOOL, "boolean")
          .put(Field.Kind.TYPE_INT32, "int")
          .put(Field.Kind.TYPE_INT64, "long")
          .put(Field.Kind.TYPE_UINT32, "int")
          .put(Field.Kind.TYPE_UINT64, "long")
          .put(Field.Kind.TYPE_FLOAT, "float")
          .put(Field.Kind.TYPE_DOUBLE, "double")
          .put(Field.Kind.TYPE_STRING, "java.lang.String")
          .put(Field.Kind.TYPE_ENUM, "java.lang.String")
          .build();

  /**
   * A map from primitive types in proto to zero value in Java.
   */
  private static final ImmutableMap<Field.Kind, String> PRIMITIVE_ZERO_VALUE =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_BOOL, "false")
          .put(Field.Kind.TYPE_INT32, "0")
          .put(Field.Kind.TYPE_INT64, "0L")
          .put(Field.Kind.TYPE_UINT32, "0")
          .put(Field.Kind.TYPE_UINT64, "0L")
          .put(Field.Kind.TYPE_FLOAT, "0.0F")
          .put(Field.Kind.TYPE_DOUBLE, "0.0")
          .put(Field.Kind.TYPE_STRING, "\"\"")
          .put(Field.Kind.TYPE_ENUM, "\"\"")
          .build();

  private TypeNameConverter typeNameConverter;

  public JavaSampleTypeNameConverter() {
    this.typeNameConverter = new JavaTypeTable();
  }

  @Override
  public TypeName getServiceTypeName(SampleConfig sampleConfig) {
    return typeNameConverter.getTypeName(sampleConfig.apiTypeName());
  }

  @Override
  public TypeName getTypeName(TypeInfo typeInfo) {
    String typeName = "";
    if (typeInfo.isMessage()) {
      typeName = typeInfo.message().typeName();
    }
    return getTypeName(typeInfo, typeName);
  }

  private TypeName getTypeName(TypeInfo typeInfo, String typeName) {
    if (typeInfo.isMessage()) {
      // {apiName}.{resource1}.{resource2}...{messageTypeName}
      return typeNameConverter.getTypeName(typeName);
    }
    if (typeInfo.isMap()) {
      TypeName mapTypeName = typeNameConverter.getTypeName("java.util.Map");
      TypeName keyTypeName = getTypeNameForElementType(typeInfo.mapKey(), true);
      TypeName valueTypeName = getTypeNameForElementType(typeInfo.mapValue(), true);
      return new TypeName(
          mapTypeName.getFullName(),
          mapTypeName.getNickname(),
          "%s<%i, %i>",
          keyTypeName,
          valueTypeName);
    }
    if (typeInfo.isArray()) {
      TypeName listTypeName = typeNameConverter.getTypeName("java.util.List");
      TypeName elementTypeName = getTypeNameForElementType(typeInfo, true);
      return new TypeName(
          listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", elementTypeName);
    }
    return getTypeNameForElementType(typeInfo, false);
  }

  private TypeName getTypeNameForElementType(TypeInfo type, boolean shouldBoxPrimitives) {
    String primitiveTypeName = PRIMIVITVE_TYPE_MAP.get(type.kind());
    if (primitiveTypeName != null) {
      if (primitiveTypeName.contains(".")) {
        // For fully-qualified type names, use the regular resolver. These types are already boxed,
        // and so we can skip boxing logic.
        return typeNameConverter.getTypeName(primitiveTypeName);
      }
      if (shouldBoxPrimitives) {
        return new TypeName(JavaTypeTable.getBoxedTypeName(primitiveTypeName));
      }
      return new TypeName(primitiveTypeName);
    }
    switch (type.kind()) {
      case TYPE_MESSAGE:
        return getTypeName(type);
      default:
        throw new IllegalArgumentException("unknown type kind: " + type.kind());
    }
  }

  /**
   * Returns the zero value for typeInfo.
   */
  @Override
  public TypedValue getZeroValue(TypeInfo typeInfo) {
    if (typeInfo.isMap()) {
      return TypedValue.create(typeNameConverter.getTypeName("java.util.HashMap"), "new %s<>()");
    }
    if (typeInfo.isArray()) {
      return TypedValue.create(typeNameConverter.getTypeName("java.util.ArrayList"), "new %s<>()");
    }
    if (PRIMITIVE_ZERO_VALUE.containsKey(typeInfo.kind())) {
      return TypedValue.create(getTypeName(typeInfo), PRIMITIVE_ZERO_VALUE.get(typeInfo.kind()));
    }
    throw new IllegalArgumentException("unknown type kind: " + typeInfo.kind());
  }
}
