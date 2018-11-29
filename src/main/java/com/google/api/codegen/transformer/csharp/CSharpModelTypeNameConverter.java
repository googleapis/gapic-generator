/* Copyright 2016 Google LLC
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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.transformer.ModelTypeNameConverter;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import java.util.List;

public class CSharpModelTypeNameConverter extends ModelTypeNameConverter {

  /** A map from primitive types in proto to C# counterparts. */
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
          .put(Type.TYPE_BYTES, "Google.Protobuf.ByteString.Empty")
          .build();

  private CSharpTypeTable typeNameConverter;
  private CSharpEnumNamer enumNamer;

  public CSharpModelTypeNameConverter(CSharpTypeTable typeTable) {
    this.typeNameConverter = typeTable;
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

  // TODO: Change this to use CSharpTypeTable.getTypeName
  @Override
  public TypeName getTypeName(ProtoElement elem) {
    // Handle nested types, construct the required type prefix
    ProtoElement parentEl = elem.getParent();
    String shortNamePrefix = "";
    while (parentEl != null && parentEl instanceof MessageType) {
      shortNamePrefix = parentEl.getSimpleName() + "+Types+" + shortNamePrefix;
      parentEl = parentEl.getParent();
    }
    String prefix = "";
    if (parentEl instanceof ProtoFile) {
      ProtoFile protoFile = (ProtoFile) parentEl;
      String namespace = protoFile.getProto().getOptions().getCsharpNamespace();
      if (Strings.isNullOrEmpty(namespace)) {
        for (String name : Splitter.on('.').split(parentEl.getFullName())) {
          prefix += Name.from(name).toUpperCamelAndDigits() + ".";
        }
      } else {
        prefix = namespace + ".";
      }
    }
    String shortName = shortNamePrefix + elem.getSimpleName();
    return typeNameConverter.getTypeName(prefix + shortName);
    // return new TypeName(prefix + shortName, shortName);
  }

  @Override
  public TypedValue getSnippetZeroValue(TypeRef type) {
    if (type.isMap()) {
      TypeName keyTypeName = getTypeNameForElementType(type.getMapKeyField().getType());
      TypeName valueTypeName = getTypeNameForElementType(type.getMapValueField().getType());
      TypeName mapTypeName = typeNameConverter.getTypeName("System.Collections.Generic.Dictionary");
      TypeName genericMapTypeName =
          new TypeName(
              mapTypeName.getFullName(),
              mapTypeName.getNickname(),
              "%s<%i, %i>",
              keyTypeName,
              valueTypeName);
      return TypedValue.create(genericMapTypeName, "new %s()");
    } else if (type.isRepeated()) {
      TypeName elementTypeName = getTypeNameForElementType(type);
      TypeName listTypeName = typeNameConverter.getTypeName("System.Collections.Generic.List");
      TypeName genericListTypeName =
          new TypeName(
              listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", elementTypeName);
      return TypedValue.create(genericListTypeName, "new %s()");
    } else if (type.isMessage()) {
      return TypedValue.create(getTypeName(type), "new %s()");
    } else if (type.isEnum()) {
      return getEnumValue(type, type.getEnumType().getValues().get(0));
    } else {
      if (type.getKind() == Type.TYPE_BYTES) {
        String clsName = typeNameConverter.getAndSaveNicknameFor("Google.Protobuf.ByteString");
        return TypedValue.create(getTypeName(type), clsName + ".Empty");
      } else {
        return TypedValue.create(getTypeName(type), PRIMITIVE_ZERO_VALUE.get(type.getKind()));
      }
    }
  }

  @Override
  public TypedValue getImplZeroValue(TypeRef type) {
    if (type.isMap()) {
      TypeName keyTypeName = getTypeNameForElementType(type.getMapKeyField().getType());
      TypeName valueTypeName = getTypeNameForElementType(type.getMapValueField().getType());
      TypeName emptyMapTypeName = typeNameConverter.getTypeName("Google.Api.Gax.EmptyDictionary");
      TypeName genericEmptyMapTypeName =
          new TypeName(
              emptyMapTypeName.getFullName(),
              emptyMapTypeName.getNickname(),
              "%s<%i, %i>",
              keyTypeName,
              valueTypeName);
      return TypedValue.create(genericEmptyMapTypeName, "%s.Instance");
    } else if (type.isRepeated()) {
      TypeName elementTypeName = getTypeNameForElementType(type);
      TypeName enumerableTypeName = typeNameConverter.getTypeName("System.Linq.Enumerable");
      TypeName emptyTypeName =
          new TypeName(
              enumerableTypeName.getFullName(),
              enumerableTypeName.getNickname(),
              "%s.Empty<%i>",
              elementTypeName);
      return TypedValue.create(emptyTypeName, "%s()");
    } else if (type.isMessage()) {
      return TypedValue.create(getTypeName(type), "new %s()");
    } else if (type.isEnum()) {
      return getEnumValue(type, type.getEnumType().getValues().get(0));
    } else {
      if (type.getKind() == Type.TYPE_BYTES) {
        String clsName = typeNameConverter.getAndSaveNicknameFor("Google.Protobuf.ByteString");
        return TypedValue.create(getTypeName(type), clsName + ".Empty");
      } else {
        return TypedValue.create(getTypeName(type), PRIMITIVE_ZERO_VALUE.get(type.getKind()));
      }
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
        String clsName = typeNameConverter.getAndSaveNicknameFor("Google.Protobuf.ByteString");
        return clsName + ".CopyFromUtf8(\"" + value + "\")";
      default:
        // Types that do not need to be modified (e.g. TYPE_INT32) are handled here
        return value;
    }
  }

  @Override
  public TypeName getTypeNameForTypedResourceName(
      FieldConfig fieldConfig, String typedResourceShortName) {
    return getTypeNameForTypedResourceName(
        fieldConfig, fieldConfig.getField().getType(), typedResourceShortName);
  }

  @Override
  public TypeName getTypeNameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName) {
    return getTypeNameForTypedResourceName(
        fieldConfig, fieldConfig.getField().getType().makeOptional(), typedResourceShortName);
  }

  private TypeName getTypeNameForTypedResourceName(
      FieldConfig fieldConfig, TypeModel type, String typedResourceShortName) {
    String nickName = typeNameConverter.getAndSaveNicknameFor(typedResourceShortName);
    TypeName simpleTypeName = new TypeName(typedResourceShortName, nickName);
    if (type.isMap()) {
      throw new IllegalArgumentException("Map type not supported for typed resource name");
    } else if (type.isRepeated()) {
      TypeName listTypeName =
          typeNameConverter.getTypeName("System.Collections.Generic.IEnumerable");
      return new TypeName(
          listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", simpleTypeName);
    } else {
      return simpleTypeName;
    }
  }

  @Override
  public TypedValue getEnumValue(TypeRef type, EnumValue value) {
    TypeName enumTypeName = getTypeName(type);
    List<String> enumTypeNameParts = Splitter.on('+').splitToList(enumTypeName.getNickname());
    String enumShortTypeName = enumTypeNameParts.get(enumTypeNameParts.size() - 1);
    String enumValueName = enumNamer.getEnumValueName(enumShortTypeName, value.getSimpleName());
    return TypedValue.create(enumTypeName, "%s." + enumValueName);
  }
}
