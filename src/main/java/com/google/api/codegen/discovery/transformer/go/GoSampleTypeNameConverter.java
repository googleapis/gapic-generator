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
package com.google.api.codegen.discovery.transformer.go;

import com.google.api.codegen.discovery.config.TypeInfo;
import com.google.api.codegen.discovery.transformer.SampleTypeNameConverter;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypedValue;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Field;

public class GoSampleTypeNameConverter implements SampleTypeNameConverter {

  /** A map from primitive types in proto to Go counterparts. */
  private static final ImmutableMap<Field.Kind, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_BOOL, "bool")
          .put(Field.Kind.TYPE_INT32, "int32")
          .put(Field.Kind.TYPE_INT64, "int64")
          .put(Field.Kind.TYPE_UINT32, "uint32")
          .put(Field.Kind.TYPE_UINT64, "uint64")
          .put(Field.Kind.TYPE_FLOAT, "float32")
          .put(Field.Kind.TYPE_DOUBLE, "float64")
          .put(Field.Kind.TYPE_STRING, "string")
          .put(Field.Kind.TYPE_ENUM, "string")
          .build();

  /** A map from primitive types in proto to zero value in Go. */
  private static final ImmutableMap<Field.Kind, String> PRIMITIVE_ZERO_VALUE =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_BOOL, "false")
          .put(Field.Kind.TYPE_INT32, "int64(0)")
          .put(Field.Kind.TYPE_INT64, "int64(0)")
          .put(Field.Kind.TYPE_UINT32, "uint64(0)")
          .put(Field.Kind.TYPE_UINT64, "uint64(0)")
          .put(Field.Kind.TYPE_FLOAT, "float32(0.0)")
          .put(Field.Kind.TYPE_DOUBLE, "0.0")
          .put(Field.Kind.TYPE_STRING, "\"\"")
          .put(Field.Kind.TYPE_ENUM, "\"\"")
          .build();

  String packagePrefix;

  public GoSampleTypeNameConverter(String packagePrefix) {
    this.packagePrefix = packagePrefix;
  }

  @Override
  public TypeName getServiceTypeName(String apiTypeName) {
    // N/A
    return null;
  }

  @Override
  public TypeName getRequestTypeName(String apiTypeName, TypeInfo typeInfo) {
    return getTypeName(typeInfo);
  }

  @Override
  public TypeName getTypeName(TypeInfo typeInfo) {
    if (typeInfo.isMessage()) {
      String localName = GoSampleNamer.getServicePackageName(packagePrefix);
      return new TypeName(packagePrefix + ";;;", localName + "." + typeInfo.message().typeName());
    }
    if (typeInfo.isMap()) {
      TypeName keyTypeName = getTypeNameForElementType(typeInfo.mapKey());
      TypeName valueTypeName = getTypeNameForElementType(typeInfo.mapValue());
      return new TypeName("", "", "map[%s]%s", keyTypeName, valueTypeName);
    }
    if (typeInfo.isArray()) {
      TypeName elementTypeName = getTypeNameForElementType(typeInfo);
      return new TypeName("", "", "[]%i", elementTypeName);
    }
    return getTypeNameForElementType(typeInfo);
  }

  @Override
  public TypeName getTypeNameForElementType(TypeInfo typeInfo) {
    String primitiveTypeName = PRIMITIVE_TYPE_MAP.get(typeInfo.kind());
    if (primitiveTypeName != null) {
      return new TypeName(primitiveTypeName);
    }
    throw new IllegalArgumentException("unsupported type kind: " + typeInfo.kind());
  }

  @Override
  public TypedValue getZeroValue(TypeInfo typeInfo) {
    // Don't call getTypeName; we don't need to import these.
    if (typeInfo.isMap() || typeInfo.isArray()) {
      return TypedValue.create(getTypeName(typeInfo), "%s{}");
    }
    if (PRIMITIVE_ZERO_VALUE.containsKey(typeInfo.kind())) {
      return TypedValue.create(
          new TypeName(PRIMITIVE_TYPE_MAP.get(typeInfo.kind())),
          PRIMITIVE_ZERO_VALUE.get(typeInfo.kind()));
    }
    throw new IllegalArgumentException("unknown type kind: " + typeInfo.kind());
  }
}
