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

import com.google.api.codegen.discovery.TypeInfo;
import com.google.api.codegen.discovery.transformer.ProtobufTypeNameConverter;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypedValue;
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
          .put(Field.Kind.TYPE_DOUBLE, "double")
          .put(Field.Kind.TYPE_FLOAT, "float")
          .put(Field.Kind.TYPE_INT64, "long")
          .put(Field.Kind.TYPE_UINT64, "long")
          .put(Field.Kind.TYPE_SINT64, "long")
          .put(Field.Kind.TYPE_FIXED64, "long")
          .put(Field.Kind.TYPE_SFIXED64, "long")
          .put(Field.Kind.TYPE_INT32, "int")
          .put(Field.Kind.TYPE_UINT32, "int")
          .put(Field.Kind.TYPE_SINT32, "int")
          .put(Field.Kind.TYPE_FIXED32, "int")
          .put(Field.Kind.TYPE_SFIXED32, "int")
          .put(Field.Kind.TYPE_STRING, "java.lang.String")
          .put(Field.Kind.TYPE_BYTES, "com.google.protobuf.ByteString")
          .build();

  /**
   * A map from primitive types in proto to zero value in Java.
   */
  private static final ImmutableMap<Field.Kind, String> PRIMITIVE_ZERO_VALUE =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_BOOL, "false")
          .put(Field.Kind.TYPE_DOUBLE, "0.0")
          .put(Field.Kind.TYPE_FLOAT, "0.0F")
          .put(Field.Kind.TYPE_INT64, "0L")
          .put(Field.Kind.TYPE_UINT64, "0L")
          .put(Field.Kind.TYPE_SINT64, "0L")
          .put(Field.Kind.TYPE_FIXED64, "0L")
          .put(Field.Kind.TYPE_SFIXED64, "0L")
          .put(Field.Kind.TYPE_INT32, "0")
          .put(Field.Kind.TYPE_UINT32, "0")
          .put(Field.Kind.TYPE_SINT32, "0")
          .put(Field.Kind.TYPE_FIXED32, "0")
          .put(Field.Kind.TYPE_SFIXED32, "0")
          .put(Field.Kind.TYPE_STRING, "\"\"")
          .put(Field.Kind.TYPE_BYTES, "ByteString.copyFromUtf8(\"\")")
          .build();

  @Override
  public String getTypeName(TypeInfo type) {
    // TODO(saicheems)
    return PRIMIVITVE_TYPE_MAP.get(type.kind());
  }

  @Override
  public TypedValue getZeroValue(Field.Kind kind) {
    // TODO(saicheems)
    return null;
  }

  @Override
  public String renderPrimitiveValue(Field.Kind kind, String value) {
    // TODO(saicheems)
    return null;
  }
}
