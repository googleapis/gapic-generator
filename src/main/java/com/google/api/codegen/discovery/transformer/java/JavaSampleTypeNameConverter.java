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

import com.google.api.codegen.discovery.config.MessageTypeInfo;
import com.google.api.codegen.discovery.config.TypeInfo;
import com.google.api.codegen.discovery.transformer.SampleTypeNameConverter;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Field;

/** Maps SampleConfig and TypeInfo instances to Java specific TypeName instances. */
class JavaSampleTypeNameConverter implements SampleTypeNameConverter {

  /** A map from primitive types in proto to Java counterparts. */
  private static final ImmutableMap<Field.Kind, String> PRIMIVITVE_TYPE_MAP =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_UNKNOWN, "java.lang.Object")
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

  /** A map from primitive types in proto to zero value in Java. */
  private static final ImmutableMap<Field.Kind, String> PRIMITIVE_ZERO_VALUE =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_UNKNOWN, "new Object()")
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

  private final TypeNameConverter typeNameConverter;
  private final String packagePrefix;

  public JavaSampleTypeNameConverter(String apiName, String apiVersion, boolean versionModule) {
    this.typeNameConverter = new JavaTypeTable("");
    StringBuilder stringBuilder = new StringBuilder("com.google.api.services.").append(apiName);
    if (versionModule) {
      stringBuilder.append('.');
      stringBuilder.append(apiVersion);
    }
    this.packagePrefix = stringBuilder.toString();
  }

  @Override
  public TypeName getServiceTypeName(String apiTypeName) {
    return typeNameConverter.getTypeName(Joiner.on('.').join(packagePrefix, apiTypeName));
  }

  @Override
  public TypeName getRequestTypeName(String apiTypeName, TypeInfo typeInfo) {
    MessageTypeInfo type = typeInfo.message();
    return new TypeName(
        Joiner.on('.').join(packagePrefix, "model", apiTypeName, type.typeName()),
        Joiner.on('.').join(apiTypeName, type.typeName()));
  }

  @Override
  public TypeName getTypeName(TypeInfo typeInfo) {
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

  @Override
  public TypeName getTypeNameForElementType(TypeInfo typeInfo) {
    if (typeInfo.isMessage()) {
      MessageTypeInfo messageInfo = typeInfo.message();
      return typeNameConverter.getTypeName(
          Joiner.on('.').join(packagePrefix, "model", messageInfo.typeName()));
    }
    // Maps are special-cased so we return Map.Entry types.
    if (typeInfo.isMap()) {
      TypeName mapTypeName = new TypeName("java.util.Map", "Map.Entry");
      TypeName keyTypeName = getTypeNameForElementType(typeInfo.mapKey(), true);
      TypeName valueTypeName = getTypeNameForElementType(typeInfo.mapValue(), true);
      return new TypeName(
          mapTypeName.getFullName(),
          mapTypeName.getNickname(),
          "%s<%i, %i>",
          keyTypeName,
          valueTypeName);
    }
    return getTypeNameForElementType(typeInfo, true);
  }

  private TypeName getTypeNameForElementType(TypeInfo typeInfo, boolean shouldBoxPrimitives) {
    String primitiveTypeName = PRIMIVITVE_TYPE_MAP.get(typeInfo.kind());
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
    switch (typeInfo.kind()) {
      case TYPE_MESSAGE:
        return getTypeNameForElementType(typeInfo);
      default:
        throw new IllegalArgumentException("unknown type kind: " + typeInfo.kind());
    }
  }

  /** Returns the zero value for typeInfo. */
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
