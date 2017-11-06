/* Copyright 2016 Google LLC
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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.transformer.ModelTypeNameConverter;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.js.JSTypeTable;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

public class NodeJSModelTypeNameConverter extends ModelTypeNameConverter {

  /** A map from primitive types in proto to NodeJS counterparts. */
  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "boolean")
          .put(Type.TYPE_DOUBLE, "number")
          .put(Type.TYPE_FLOAT, "number")
          .put(Type.TYPE_INT64, "number")
          .put(Type.TYPE_UINT64, "number")
          .put(Type.TYPE_SINT64, "number")
          .put(Type.TYPE_FIXED64, "number")
          .put(Type.TYPE_SFIXED64, "number")
          .put(Type.TYPE_INT32, "number")
          .put(Type.TYPE_UINT32, "number")
          .put(Type.TYPE_SINT32, "number")
          .put(Type.TYPE_FIXED32, "number")
          .put(Type.TYPE_SFIXED32, "number")
          .put(Type.TYPE_STRING, "string")
          .put(Type.TYPE_BYTES, "string")
          .build();

  /** A map from primitive types in proto to zero value in NodeJS */
  private static final ImmutableMap<Type, String> PRIMITIVE_ZERO_VALUE =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "false")
          .put(Type.TYPE_DOUBLE, "0.0")
          .put(Type.TYPE_FLOAT, "0.0")
          .put(Type.TYPE_INT64, "0")
          .put(Type.TYPE_UINT64, "0")
          .put(Type.TYPE_SINT64, "0")
          .put(Type.TYPE_FIXED64, "0")
          .put(Type.TYPE_SFIXED64, "0")
          .put(Type.TYPE_INT32, "0")
          .put(Type.TYPE_UINT32, "0")
          .put(Type.TYPE_SINT32, "0")
          .put(Type.TYPE_FIXED32, "0")
          .put(Type.TYPE_SFIXED32, "0")
          .put(Type.TYPE_STRING, "\'\'")
          .put(Type.TYPE_BYTES, "\'\'")
          .build();

  private TypeNameConverter typeNameConverter;

  public NodeJSModelTypeNameConverter(String implicitPackageName) {
    this.typeNameConverter = new JSTypeTable(implicitPackageName);
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    return typeNameConverter.getTypeNameInImplicitPackage(shortName);
  }

  @Override
  public TypeName getTypeName(TypeRef type) {
    if (type.isMap()) {
      return new TypeName("Object");
    } else if (type.isRepeated()) {
      TypeName elementTypeName = getTypeNameForElementType(type);
      return new TypeName("", "", "%i[]", elementTypeName);
    } else {
      return getTypeNameForElementType(type);
    }
  }

  /**
   * Returns the NodeJS representation of a type, without cardinality. If the type is a primitive,
   * getTypeNameForElementType returns it in unboxed form.
   */
  @Override
  public TypeName getTypeNameForElementType(TypeRef type) {
    String primitiveTypeName = PRIMITIVE_TYPE_MAP.get(type.getKind());
    if (primitiveTypeName != null) {
      return new TypeName(primitiveTypeName);
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
  public TypeName getTypeName(ProtoElement elem) {
    return typeNameConverter.getTypeName(elem.getFullName());
  }

  /**
   * Returns the NodeJS representation of a zero value for that type, to be used in code sample doc.
   */
  @Override
  public TypedValue getSnippetZeroValue(TypeRef type) {
    // Don't call getTypeName; we don't need to import these.
    if (type.isMap()) {
      return TypedValue.create(new TypeName("Object"), "{}");
    }
    if (type.isRepeated()) {
      return TypedValue.create(new TypeName("Array"), "[]");
    }
    if (PRIMITIVE_ZERO_VALUE.containsKey(type.getKind())) {
      return TypedValue.create(getTypeName(type), PRIMITIVE_ZERO_VALUE.get(type.getKind()));
    }
    if (type.isMessage()) {
      return TypedValue.create(getTypeName(type), "{}");
    }
    if (type.isEnum()) {
      return getEnumValue(type, type.getEnumType().getValues().get(0));
    }
    return TypedValue.create(new TypeName(""), "null");
  }

  @Override
  public TypedValue getImplZeroValue(TypeRef type) {
    return getSnippetZeroValue(type);
  }

  @Override
  public String renderPrimitiveValue(TypeRef type, String value) {
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
      case TYPE_STRING:
      case TYPE_BYTES:
        return "\'" + value + "\'";
      default:
        // Types that do not need to be modified (e.g. TYPE_INT32) are handled
        // here
        return value;
    }
  }

  @Override
  public TypeName getTypeNameForTypedResourceName(
      FieldConfig fieldConfig, String typedResourceShortName) {
    throw new UnsupportedOperationException(
        "getTypeNameForTypedResourceName not supported by NodeJS");
  }

  @Override
  public TypeName getTypeNameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName) {
    throw new UnsupportedOperationException(
        "getTypeNameForResourceNameElementType not supported by NodeJS");
  }

  @Override
  public TypedValue getEnumValue(TypeRef type, EnumValue value) {
    return TypedValue.create(getTypeName(type), String.format("'%s'", value.getSimpleName()));
  }
}
