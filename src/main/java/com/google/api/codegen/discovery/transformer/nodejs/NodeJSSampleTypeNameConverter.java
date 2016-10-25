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
package com.google.api.codegen.discovery.transformer.nodejs;

import com.google.api.codegen.discovery.config.TypeInfo;
import com.google.api.codegen.discovery.transformer.SampleTypeNameConverter;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypedValue;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Field;

public class NodeJSSampleTypeNameConverter implements SampleTypeNameConverter {

  /** A map from primitive types in proto to NodeJS counterparts. */
  private static final ImmutableMap<Field.Kind, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_BOOL, "boolean")
          .put(Field.Kind.TYPE_INT32, "number")
          .put(Field.Kind.TYPE_INT64, "number")
          .put(Field.Kind.TYPE_UINT32, "number")
          .put(Field.Kind.TYPE_UINT64, "number")
          .put(Field.Kind.TYPE_FLOAT, "number")
          .put(Field.Kind.TYPE_DOUBLE, "number")
          .put(Field.Kind.TYPE_STRING, "String")
          .put(Field.Kind.TYPE_ENUM, "String")
          .build();

  /** A map from primitive types in proto to zero value in NodeJS */
  private static final ImmutableMap<Field.Kind, String> PRIMITIVE_ZERO_VALUE =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_BOOL, "false")
          .put(Field.Kind.TYPE_INT32, "0")
          .put(Field.Kind.TYPE_INT64, "''")
          .put(Field.Kind.TYPE_UINT32, "0")
          .put(Field.Kind.TYPE_UINT64, "''")
          .put(Field.Kind.TYPE_FLOAT, "0.0")
          .put(Field.Kind.TYPE_DOUBLE, "0.0")
          .put(Field.Kind.TYPE_STRING, "''")
          .put(Field.Kind.TYPE_ENUM, "''")
          .build();

  public NodeJSSampleTypeNameConverter() {}

  @Override
  public TypeName getServiceTypeName(String apiTypeName) {
    return new TypeName(apiTypeName);
  }

  @Override
  public TypeName getRequestTypeName(String apiTypeName, TypeInfo typeInfo) {
    // N/A
    return null;
  }

  @Override
  public TypeName getTypeName(TypeInfo typeInfo) {
    if (typeInfo.isMap()) {
      return new TypeName("Object");
    } else if (typeInfo.isArray()) {
      TypeName elementTypeName = getTypeNameForElementType(typeInfo);
      return new TypeName("", "", "%i[]", elementTypeName);
    } else {
      return getTypeNameForElementType(typeInfo);
    }
  }

  @Override
  public TypeName getTypeNameForElementType(TypeInfo typeInfo) {
    String primitiveTypeName = PRIMITIVE_TYPE_MAP.get(typeInfo.kind());
    if (primitiveTypeName != null) {
      return new TypeName(primitiveTypeName);
    }
    throw new IllegalArgumentException("unknown type kind: " + typeInfo.kind());
  }

  @Override
  public TypedValue getZeroValue(TypeInfo typeInfo) {
    // Don't call getTypeName; we don't need to import these.
    if (typeInfo.isMap()) {
      return TypedValue.create(new TypeName("Object"), "{}");
    }
    if (typeInfo.isArray()) {
      return TypedValue.create(new TypeName("Array"), "[]");
    }
    if (PRIMITIVE_ZERO_VALUE.containsKey(typeInfo.kind())) {
      return TypedValue.create(getTypeName(typeInfo), PRIMITIVE_ZERO_VALUE.get(typeInfo.kind()));
    }
    if (typeInfo.isMessage()) {
      return TypedValue.create(getTypeName(typeInfo), "{}");
    }
    throw new IllegalArgumentException("unknown type kind: " + typeInfo.kind());
  }
}
