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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.transformer.ModelTypeNameConverter;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

public class CSharpModelTypeNameConverter implements ModelTypeNameConverter {

  /** A map from primitive types in proto to Java counterparts. */
  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "bool")
          .put(Type.TYPE_DOUBLE, "double")
          .put(Type.TYPE_FLOAT, "float")
          .put(Type.TYPE_INT64, "long")
          .put(Type.TYPE_UINT64, "ulong")
          .put(Type.TYPE_SINT64, "long")
          .put(Type.TYPE_FIXED64, "long")
          .put(Type.TYPE_SFIXED64, "long")
          .put(Type.TYPE_INT32, "int")
          .put(Type.TYPE_UINT32, "uint")
          .put(Type.TYPE_SINT32, "int")
          .put(Type.TYPE_FIXED32, "int")
          .put(Type.TYPE_SFIXED32, "int")
          .put(Type.TYPE_STRING, "string")
          .put(Type.TYPE_BYTES, "Google.Protobuf.ByteString")
          .build();

  /** A map from primitive types in proto to zero values in C#. */
  private static final ImmutableMap<Type, String> PRIMITIVE_ZERO_VALUE =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "false")
          .put(Type.TYPE_DOUBLE, "0.0")
          .put(Type.TYPE_FLOAT, "0.0f")
          .put(Type.TYPE_INT64, "0L")
          .put(Type.TYPE_UINT64, "0L")
          .put(Type.TYPE_SINT64, "0L")
          .put(Type.TYPE_FIXED64, "0L")
          .put(Type.TYPE_SFIXED64, "0L")
          .put(Type.TYPE_INT32, "0")
          .put(Type.TYPE_UINT32, "0")
          .put(Type.TYPE_SINT32, "0")
          .put(Type.TYPE_FIXED32, "0")
          .put(Type.TYPE_SFIXED32, "0")
          .put(Type.TYPE_STRING, "\"\"")
          .put(Type.TYPE_BYTES, "ByteString.CopyFromUtf8(\"\")")
          .build();

  private TypeNameConverter typeNameConverter;
  private CSharpEnumNamer enumNamer;

  public CSharpModelTypeNameConverter(String implicitPackageName) {
    this.typeNameConverter = new CSharpTypeTable(implicitPackageName);
    this.enumNamer = new CSharpEnumNamer();
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    return typeNameConverter.getTypeNameInImplicitPackage(shortName);
  }

  @Override
  public TypeName getTypeName(TypeRef type) {
    if (type.isMap()) {
      TypeName mapTypeName =
          typeNameConverter.getTypeName("System.Collections.Generic.IDictionary");
      TypeName keyTypeName = getTypeNameForElementType(type.getMapKeyField().getType());
      TypeName valueTypeName = getTypeNameForElementType(type.getMapValueField().getType());
      return new TypeName(
          mapTypeName.getFullName(),
          mapTypeName.getNickname(),
          "%s<%i, %i>",
          keyTypeName,
          valueTypeName);
    } else if (type.isRepeated()) {
      TypeName listTypeName =
          typeNameConverter.getTypeName("System.Collections.Generic.IEnumerable");
      TypeName elementTypeName = getTypeNameForElementType(type);
      return new TypeName(
          listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", elementTypeName);
    } else {
      return getTypeNameForElementType(type);
    }
  }

  @Override
  public TypeName getTypeNameForElementType(TypeRef type) {
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return getTypeName(type.getMessageType());
      case TYPE_ENUM:
        return getTypeName(type.getEnumType());
      default:
        String typeName = PRIMITIVE_TYPE_MAP.get(type.getKind());
        return typeNameConverter.getTypeName(typeName);
    }
  }

  @Override
  public TypeName getTypeName(ProtoElement elem) {
    // Handle nested types, construct the required type prefix
    ProtoElement parentEl = elem.getParent();
    String prefix = "";
    while (parentEl != null && parentEl instanceof MessageType) {
      prefix = parentEl.getSimpleName() + ".Types." + prefix;
      parentEl = parentEl.getParent();
    }
    String shortName = elem.getSimpleName();
    return new TypeName(prefix + shortName, shortName);
  }

  @Override
  public TypedValue getZeroValue(TypeRef type) {
    if (type.isMap()) {
      TypeName mapTypeName = typeNameConverter.getTypeName("System.Collections.Generic.Dictionary");
      TypeName keyTypeName = getTypeNameForElementType(type.getMapKeyField().getType());
      TypeName valueTypeName = getTypeNameForElementType(type.getMapValueField().getType());
      TypeName genericMapTypeName =
          new TypeName(
              mapTypeName.getFullName(),
              mapTypeName.getNickname(),
              "%s<%i, %i>",
              keyTypeName,
              valueTypeName);
      return TypedValue.create(genericMapTypeName, "new %s()");
    } else if (type.isRepeated()) {
      TypeName listTypeName = typeNameConverter.getTypeName("System.Collections.Generic.List");
      TypeName elementTypeName = getTypeNameForElementType(type);
      TypeName genericListTypeName =
          new TypeName(
              listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", elementTypeName);
      return TypedValue.create(genericListTypeName, "new %s()");
    } else if (type.isMessage()) {
      return TypedValue.create(getTypeName(type), "new %s()");
    } else if (type.isEnum()) {
      TypeName enumTypeName = getTypeName(type);
      EnumValue enumValue = type.getEnumType().getValues().get(0);
      String enumValueName =
          enumNamer.getEnumValueName(enumTypeName.getNickname(), enumValue.getSimpleName());
      return TypedValue.create(enumTypeName, "%s." + enumValueName);
    } else {
      return TypedValue.create(getTypeName(type), PRIMITIVE_ZERO_VALUE.get(type.getKind()));
    }
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
      case TYPE_FLOAT:
        return value + "f";
      case TYPE_INT64:
      case TYPE_UINT64:
        return value + "L";
      case TYPE_STRING:
        return "\"" + value + "\"";
      case TYPE_BYTES:
        return "ByteString.copyFromUtf8(\"" + value + "\")";
      default:
        // Types that do not need to be modified (e.g. TYPE_INT32) are handled here
        return value;
    }
  }

  @Override
  public TypeName getTypeNameForTypedResourceName(
      ProtoElement field, TypeRef type, String typedResourceShortName) {
    throw new UnsupportedOperationException("getTypeNameForTypedResourceName not supported by C#");
  }
}
