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
package com.google.api.codegen.discovery.transformer.php;

import com.google.api.codegen.discovery.config.TypeInfo;
import com.google.api.codegen.discovery.transformer.SampleTypeNameConverter;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypedValue;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Field;

public class PhpSampleTypeNameConverter implements SampleTypeNameConverter {

  /** A map from primitive types in proto to PHP counterparts. */
  private static final ImmutableMap<Field.Kind, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_BOOL, "Boolean")
          .put(Field.Kind.TYPE_INT32, "Integer")
          .put(Field.Kind.TYPE_INT64, "String")
          .put(Field.Kind.TYPE_UINT32, "Integer")
          .put(Field.Kind.TYPE_UINT64, "String")
          .put(Field.Kind.TYPE_FLOAT, "Float")
          .put(Field.Kind.TYPE_DOUBLE, "Float")
          .put(Field.Kind.TYPE_STRING, "String")
          .put(Field.Kind.TYPE_ENUM, "String")
          .build();

  /** A map from primitive types in proto to zero value in PHP */
  private static final ImmutableMap<Field.Kind, String> PRIMITIVE_ZERO_VALUE =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_BOOL, "false")
          .put(Field.Kind.TYPE_INT32, "0")
          .put(Field.Kind.TYPE_INT64, "'0'")
          .put(Field.Kind.TYPE_UINT32, "0")
          .put(Field.Kind.TYPE_UINT64, "'0'")
          .put(Field.Kind.TYPE_FLOAT, "0.0")
          .put(Field.Kind.TYPE_DOUBLE, "0.0")
          .put(Field.Kind.TYPE_STRING, "''")
          .put(Field.Kind.TYPE_ENUM, "''")
          .build();

  private final String apiTypeName;

  public PhpSampleTypeNameConverter(String apiTypeName) {
    this.apiTypeName = apiTypeName;
  }

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
    // This method is only ever called for the request body type, which should
    // always be a message.
    if (typeInfo.isMessage()) {
      return new TypeName(apiTypeName + "_" + typeInfo.message().typeName());
    }
    return getTypeNameForElementType(typeInfo);
  }

  @Override
  public TypeName getTypeNameForElementType(TypeInfo typeInfo) {
    throw new IllegalArgumentException("unknown type kind: " + typeInfo.kind());
  }

  @Override
  public TypedValue getZeroValue(TypeInfo typeInfo) {
    // Don't call getTypeName; we don't need to import these.
    if (typeInfo.isMap()) {
      return TypedValue.create(new TypeName("Array"), "[]");
    }
    if (typeInfo.isArray()) {
      return TypedValue.create(new TypeName("Array"), "[]");
    }
    if (PRIMITIVE_ZERO_VALUE.containsKey(typeInfo.kind())) {
      return TypedValue.create(
          new TypeName(PRIMITIVE_TYPE_MAP.get(typeInfo.kind())),
          PRIMITIVE_ZERO_VALUE.get(typeInfo.kind()));
    }
    if (typeInfo.isMessage()) {
      return TypedValue.create(new TypeName("Object"), "new stdClass()");
    }
    throw new IllegalArgumentException("unknown type kind: " + typeInfo.kind());
  }
}
