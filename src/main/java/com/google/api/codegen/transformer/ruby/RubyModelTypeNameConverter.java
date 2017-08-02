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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.transformer.ModelTypeNameConverter;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

public class RubyModelTypeNameConverter extends ModelTypeNameConverter {

  /** A map from primitive types to its default value. */
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

  /** A map from primitive type to its corresponding ruby types */
  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "true, false")
          .put(Type.TYPE_DOUBLE, "Float")
          .put(Type.TYPE_FLOAT, "Float")
          .put(Type.TYPE_INT64, "Integer")
          .put(Type.TYPE_UINT64, "Integer")
          .put(Type.TYPE_SINT64, "Integer")
          .put(Type.TYPE_FIXED64, "Integer")
          .put(Type.TYPE_SFIXED64, "Integer")
          .put(Type.TYPE_INT32, "Integer")
          .put(Type.TYPE_UINT32, "Integer")
          .put(Type.TYPE_SINT32, "Integer")
          .put(Type.TYPE_FIXED32, "Integer")
          .put(Type.TYPE_SFIXED32, "Integer")
          .put(Type.TYPE_STRING, "String")
          .put(Type.TYPE_BYTES, "String")
          .build();

  private TypeNameConverter typeNameConverter;

  public RubyModelTypeNameConverter(String implicitPackageName) {
    this.typeNameConverter = new RubyTypeTable(implicitPackageName);
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    return typeNameConverter.getTypeNameInImplicitPackage(shortName);
  }

  @Override
  public TypeName getTypeName(TypeRef type) {
    if (type.isMap()) {
      return new TypeName("Hash");
    } else if (type.isRepeated()) {
      return new TypeName("Array");
    } else {
      return getTypeNameForElementType(type);
    }
  }

  /**
   * Returns the Ruby representation of a type. If the type is a primitive,
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
    return typeNameConverter.getTypeName(
        NamePath.dotted(elem.getFullName()).withUpperPieces().toDoubleColoned());
  }

  /**
   * Returns the Ruby representation of a zero value for that type, to be used in code sample doc.
   */
  @Override
  public TypedValue getSnippetZeroValue(TypeRef type) {
    // Don't call getTypeName; we don't need to import these.
    if (type.isMap()) {
      return TypedValue.create(new TypeName("Hash"), "{}");
    }
    if (type.isRepeated()) {
      return TypedValue.create(new TypeName("Array"), "[]");
    }
    if (PRIMITIVE_ZERO_VALUE.containsKey(type.getKind())) {
      return TypedValue.create(getTypeName(type), PRIMITIVE_ZERO_VALUE.get(type.getKind()));
    }
    if (type.isMessage()) {
      TypeName typeName = getTypeName(type);
      return TypedValue.create(typeName, "{}");
    }
    if (type.isEnum()) {
      return getEnumValue(type, type.getEnumType().getValues().get(0));
    }
    return TypedValue.create(new TypeName(""), "nil");
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
        return "\"" + value + "\"";
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
        "getTypeNameForTypedResourceName not supported by Ruby");
  }

  @Override
  public TypeName getTypeNameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName) {
    throw new UnsupportedOperationException(
        "getTypeNameForResourceNameElementType not supported by Ruby");
  }

  @Override
  public TypedValue getEnumValue(TypeRef type, EnumValue value) {
    return TypedValue.create(getTypeName(type), ":" + value.getSimpleName());
  }
}
