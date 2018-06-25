/* Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.config;

import com.google.api.codegen.gapic.ServiceMessages;
import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Oneof;
import com.google.api.tools.framework.model.TypeRef;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos;
import java.util.List;
import java.util.regex.Pattern;

/** A type declaration wrapper around TypeRef. */
@AutoValue
public abstract class ProtoTypeRef implements TypeModel {

  public abstract TypeRef getProtoType();

  /* Create a MethodModel object from a non-null Method object. */
  public static ProtoTypeRef create(TypeRef typeRef) {
    return new AutoValue_ProtoTypeRef(typeRef);
  }

  @Override
  public boolean isMap() {
    return getProtoType().isMap();
  }

  @Override
  public TypeModel getMapKeyType() {
    return create(getProtoType().getMapKeyField().getType());
  }

  @Override
  public TypeModel getMapValueType() {
    return create(getProtoType().getMapValueField().getType());
  }

  @Override
  public boolean isMessage() {
    return getProtoType().isMessage();
  }

  @Override
  public boolean isRepeated() {
    return getProtoType().isRepeated();
  }

  @Override
  public boolean isEnum() {
    return getProtoType().isEnum();
  }

  @Override
  public boolean isPrimitive() {
    return getProtoType().isPrimitive();
  }

  @Override
  public boolean isEmptyType() {
    return ServiceMessages.s_isEmptyType(getProtoType());
  }

  /**
   * Validates that the provided value matches the provided type. Throws an IllegalArgumentException
   * if the provided type is not supported or doesn't match the value.
   */
  public void validateValue(String value) {
    switch (getProtoType().getKind()) {
      case TYPE_ENUM:
        for (EnumValue enumValue : getProtoType().getEnumType().getValues()) {
          if (enumValue.getSimpleName().equals(value)) {
            return;
          }
        }
        break;
      case TYPE_BOOL:
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
          return;
        }
        break;
      case TYPE_DOUBLE:
      case TYPE_FLOAT:
        if (Pattern.matches("[+-]?([0-9]*[.])?[0-9]+", value)) {
          return;
        }
        break;
      case TYPE_INT64:
      case TYPE_UINT64:
      case TYPE_SINT64:
      case TYPE_FIXED64:
      case TYPE_SFIXED64:
      case TYPE_INT32:
      case TYPE_UINT32:
      case TYPE_SINT32:
      case TYPE_FIXED32:
      case TYPE_SFIXED32:
        if (Pattern.matches("[+-]?[0-9]+", value)) {
          return;
        }
        break;
      case TYPE_STRING:
      case TYPE_BYTES:
        if (!value.contains("\\") && !value.contains("\"") && !value.contains("'")) {
          return;
        }
        break;
      default:
        // Throw an exception if a value is unsupported for the given type.
        throw new IllegalArgumentException(
            "Tried to assign value for unsupported type " + getProtoType() + "; value " + value);
    }
    throw new IllegalArgumentException(
        "Could not assign value '" + value + "' to type " + getProtoType());
  }

  @Override
  @Memoized
  public List<? extends FieldModel> getFields() {
    return getProtoType()
        .getMessageType()
        .getFields()
        .stream()
        .map(ProtoField::new)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public FieldModel getField(String key) {
    for (FieldModel field : getFields()) {
      if (field.getSimpleName().equals(key)) {
        return field;
      }
    }
    return null;
  }

  @Override
  public TypeModel makeOptional() {
    return create(getProtoType().makeOptional());
  }

  @Override
  public String getPrimitiveTypeName() {
    return getProtoType().getPrimitiveTypeName();
  }

  @Override
  public boolean isBooleanType() {
    return getProtoType().getKind() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL;
  }

  @Override
  public boolean isStringType() {
    return getProtoType().getKind() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;
  }

  @Override
  public boolean isFloatType() {
    return getProtoType().getKind() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT;
  }

  @Override
  public boolean isBytesType() {
    return getProtoType().getKind() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES;
  }

  @Override
  public boolean isDoubleType() {
    return getProtoType().getKind() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE;
  }

  @Override
  public String getTypeName() {
    return getProtoType().getKind().toString();
  }

  /**
   * Returns oneof config for the field of a given type, or null if the field is not a part of any
   * oneofs
   */
  public OneofConfig getOneOfConfig(String fieldName) {
    MessageType message = this.getProtoType().getMessageType();
    for (Oneof oneof : message.getOneofs()) {
      for (Field field : oneof.getFields()) {
        if (field.getSimpleName().equals(fieldName)) {
          return new AutoValue_OneofConfig(
              Name.from(oneof.getName()), message, new ProtoField(field));
        }
      }
    }
    return null;
  }
}
