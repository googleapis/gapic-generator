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

import java.util.ArrayList;
import java.util.List;

import com.google.api.codegen.discovery.MessageTypeInfo;
import com.google.api.codegen.discovery.SampleConfig;
import com.google.api.codegen.discovery.TypeInfo;
import com.google.api.codegen.discovery.transformer.ProtobufTypeNameConverter;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Field;

/**
 * Maps Field.Kind instances to Java specific TypeName instances.
 */
class JavaProtobufTypeNameConverter implements ProtobufTypeNameConverter {

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

  public JavaProtobufTypeNameConverter() {
    this.typeNameConverter = new JavaTypeTable();
  }

  @Override
  public TypeName getServiceTypeName(SampleConfig sampleConfig) {
    return typeNameConverter.getTypeName(
        "com.google.api.services" + Name.lowerCamel(sampleConfig.apiName()).toUpperCamel());
  }

  @Override
  public TypeName getTypeName(TypeInfo typeInfo) {
    if (typeInfo.isMessage()) {
      // {apiName}.{resource1}.{resource2}...{messageTypeName}
      MessageTypeInfo messageTypeInfo = typeInfo.message();
      String resources[] = messageTypeInfo.packagePath().split("\\.");
      for (int i = 0; i < resources.length; i++) {
        // TODO(garrettjones): Should I do this differently?
        resources[i] = Name.lowerCamel(resources[i]).toUpperCamel();
      }
      return typeNameConverter.getTypeName(
          "com.google.api.services." + String.join(".", resources) + "." + messageTypeInfo.name());
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
    throw new IllegalArgumentException("unknown type kind: " + type.kind());
  }

  @Override
  public TypedValue getZeroValue(TypeInfo typeInfo) {
    // TODO(saicheems)
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

  @Override
  public String renderPrimitiveValue(Field.Kind kind, String value) {
    // TODO(saicheems)
    return null;
  }
}
