/* Copyright 2017 Google Inc
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
package com.google.api.codegen.config;

import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Oneof;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A type declaration wrapper around TypeRef. */
public class ProtoTypeRef implements TypeModel {
  private final TypeRef typeRef;
  private List<ProtoField> fields;

  /* Create a MethodModel object from a non-null Method object. */
  public ProtoTypeRef(TypeRef typeRef) {
    Preconditions.checkNotNull(typeRef);
    this.typeRef = typeRef;
  }

  public TypeRef getProtoType() {
    return typeRef;
  }

  @Override
  public boolean isMap() {
    return typeRef.isMap();
  }

  @Override
  public FieldModel getMapKeyField() {
    return new ProtoField(typeRef.getMapKeyField());
  }

  @Override
  public FieldModel getMapValueField() {
    return new ProtoField(typeRef.getMapValueField());
  }

  @Override
  public boolean isMessage() {
    return typeRef.isMessage();
  }

  @Override
  public boolean isRepeated() {
    return typeRef.isRepeated();
  }

  @Override
  public boolean isEnum() {
    return typeRef.isEnum();
  }

  @Override
  public boolean isPrimitive() {
    return typeRef.isPrimitive();
  }

  @Override
  public boolean isEmptyType() {
    return ServiceMessages.s_isEmptyType(typeRef);
  }

  /**
   * Validates that the provided value matches the provided type. Throws an IllegalArgumentException
   * if the provided type is not supported or doesn't match the value.
   */
  public void validateValue(String value) {
    DescriptorProtos.FieldDescriptorProto.Type descType = typeRef.getKind();
    switch (descType) {
      case TYPE_ENUM:
        for (EnumValue enumValue : typeRef.getEnumType().getValues()) {
          if (enumValue.getSimpleName().equals(value)) {
            return;
          }
        }
        break;
      case TYPE_BOOL:
        String lowerCaseValue = value.toLowerCase();
        if (lowerCaseValue.equals("true") || lowerCaseValue.equals("false")) {
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
        Matcher matcher = Pattern.compile("([^\\\"']*)").matcher(value);
        if (matcher.matches()) {
          return;
        }
        break;
      default:
        // Throw an exception if a value is unsupported for the given type.
        throw new IllegalArgumentException(
            "Tried to assign value for unsupported type " + typeRef + "; value " + value);
    }
    throw new IllegalArgumentException("Could not assign value '" + value + "' to type " + typeRef);
  }

  @Override
  public List<? extends FieldModel> getFields() {
    if (this.fields != null) {
      return this.fields;
    }
    ImmutableList.Builder<ProtoField> fields = ImmutableList.builder();
    for (Field field : typeRef.getMessageType().getFields()) {
      fields.add(new ProtoField(field));
    }

    this.fields = fields.build();
    return this.fields;
  }

  @Override
  public TypeModel makeOptional() {
    return new ProtoTypeRef(typeRef.makeOptional());
  }

  @Override
  public String getPrimitiveTypeName() {
    return typeRef.getPrimitiveTypeName();
  }

  @Override
  public boolean isBooleanType() {
    return typeRef.getKind() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL;
  }

  @Override
  public boolean isStringType() {
    return typeRef.getKind() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;
  }

  @Override
  public boolean isFloatType() {
    return typeRef.getKind() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT;
  }

  @Override
  public boolean isBytesType() {
    return typeRef.getKind() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES;
  }

  @Override
  public boolean isDoubleType() {
    return typeRef.getKind() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE;
  }

  @Override
  public String getTypeName() {
    return typeRef.getKind().toString();
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

  @Override
  public int hashCode() {
    return 5 + 31 * typeRef.hashCode();
  }

  @Override
  public String toString() {
    return String.format("Protobuf FieldModel (%s): {%s}", typeRef.toString());
  }

  @Override
  public boolean equals(Object o) {
    return o != null && o instanceof ProtoField && ((ProtoTypeRef) o).typeRef.equals(this.typeRef);
  }
}
