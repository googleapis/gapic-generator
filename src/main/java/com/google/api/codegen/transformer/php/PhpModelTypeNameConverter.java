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
package com.google.api.codegen.transformer.php;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.transformer.ModelTypeNameConverter;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class PhpModelTypeNameConverter extends ModelTypeNameConverter {

  /** The maximum depth of nested messages supported by PHP type name determination. */
  private static final int MAX_NESTED_DEPTH = 20;

  /** A map from primitive types in proto to PHP counterparts. */
  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "bool")
          .put(Type.TYPE_DOUBLE, "float")
          .put(Type.TYPE_FLOAT, "float")
          .put(Type.TYPE_INT64, "int")
          .put(Type.TYPE_UINT64, "int")
          .put(Type.TYPE_SINT64, "int")
          .put(Type.TYPE_FIXED64, "int")
          .put(Type.TYPE_SFIXED64, "int")
          .put(Type.TYPE_INT32, "int")
          .put(Type.TYPE_UINT32, "int")
          .put(Type.TYPE_SINT32, "int")
          .put(Type.TYPE_FIXED32, "int")
          .put(Type.TYPE_SFIXED32, "int")
          .put(Type.TYPE_STRING, "string")
          .put(Type.TYPE_BYTES, "string")
          .build();

  /** A map from primitive types in proto to zero value in PHP */
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

  /** A map from protobuf message type names to PHP types for special case messages. */
  private static final ImmutableMap<String, String> TYPE_NAME_MAP =
      ImmutableMap.<String, String>builder()
          .put("google.protobuf.Empty", "\\Google\\Protobuf\\GPBEmpty")
          .build();

  private TypeNameConverter typeNameConverter;

  public PhpModelTypeNameConverter(String implicitPackageName) {
    this.typeNameConverter = new PhpTypeTable(implicitPackageName);
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    return typeNameConverter.getTypeNameInImplicitPackage(shortName);
  }

  @Override
  public TypeName getTypeName(TypeRef type) {
    if (type.isMap()) {
      return new TypeName("array");
    } else if (type.isRepeated()) {
      TypeName elementTypeName = getTypeNameForElementType(type);
      return new TypeName("", "", "%i[]", elementTypeName);
    } else {
      return getTypeNameForElementType(type);
    }
  }

  /**
   * Returns the PHP representation of a type, without cardinality. If the type is a primitive,
   * getTypeNameForElementType returns it in unboxed form.
   */
  @Override
  public TypeName getTypeNameForElementType(TypeRef type) {
    String primitiveTypeName = PRIMITIVE_TYPE_MAP.get(type.getKind());
    if (primitiveTypeName != null) {
      if (primitiveTypeName.contains("\\")) {
        // Fully qualified type name, use regular type name resolver. Can skip boxing logic
        // because those types are already boxed.
        return typeNameConverter.getTypeName(primitiveTypeName);
      } else {
        return new TypeName(primitiveTypeName);
      }
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

  /**
   * This function determines the PHP type name for a proto message. For example, the following
   * proto definition: <code>
   * package example;
   * message Top {
   *  message Nested {
   *    message DeepNested {}
   *  }
   * }
   * </code> will generate these PHP types:
   *
   * <ul>
   *   <li>\Example\Top
   *   <li>\Example\Top\Nested
   *   <li>\Example\Top\Nested\DeepNested
   * </ul>
   */
  @Override
  public TypeName getTypeName(ProtoElement elem) {
    String fullName = elem.getFullName();
    if (TYPE_NAME_MAP.containsKey(fullName)) {
      return typeNameConverter.getTypeName(TYPE_NAME_MAP.get(fullName));
    }

    LinkedList<String> components = new LinkedList<>();

    while (elem != null && !(elem instanceof ProtoFile)) {
      components.addFirst(elem.getSimpleName());
      elem = elem.getParent();
    }

    // Create the type name based on protobuf PHP namespace
    if (elem != null && elem instanceof ProtoFile) {
      ProtoFile protoFile = (ProtoFile) elem;
      String namespace = protoFile.getProto().getOptions().getPhpNamespace();
      if (!Strings.isNullOrEmpty(namespace)) {
        components.addFirst(namespace);
        fullName =
            components
                .stream()
                .map(PhpModelTypeNameConverter::makeNamespacePieces)
                .collect(Collectors.joining(""));
        return typeNameConverter.getTypeName(fullName);
      }
    }

    // Create the type name based on the element's full name
    fullName =
        Arrays.stream(fullName.split("\\."))
            .map(PhpModelTypeNameConverter::makeNamespacePieces)
            .collect(Collectors.joining(""));
    return typeNameConverter.getTypeName(fullName);
  }

  /**
   * Returns the PHP representation of a zero value for that type, to be used in code sample doc.
   */
  @Override
  public TypedValue getSnippetZeroValue(TypeRef type) {
    // Don't call getTypeName; we don't need to import these.
    if (type.isMap()) {
      return TypedValue.create(new TypeName("array"), "[]");
    }
    if (type.isRepeated()) {
      return TypedValue.create(new TypeName("array"), "[]");
    }
    if (PRIMITIVE_ZERO_VALUE.containsKey(type.getKind())) {
      return TypedValue.create(getTypeName(type), PRIMITIVE_ZERO_VALUE.get(type.getKind()));
    }
    if (type.isMessage()) {
      return TypedValue.create(getTypeName(type), "new %s()");
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
        return '\'' + value + '\'';
      default:
        // Types that do not need to be modified (e.g. TYPE_INT32) are handled
        // here
        return value;
    }
  }

  @Override
  public TypeName getTypeNameForTypedResourceName(
      FieldConfig fieldConfig, String typedResourceShortName) {
    throw new UnsupportedOperationException("getTypeNameForTypedResourceName not supported by PHP");
  }

  @Override
  public TypeName getTypeNameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName) {
    throw new UnsupportedOperationException(
        "getTypeNameForResourceNameElementType not supported by PHP");
  }

  @Override
  public TypedValue getEnumValue(TypeRef type, EnumValue value) {
    return TypedValue.create(getTypeName(type), "%s::" + value.getSimpleName());
  }

  private static String makeNamespacePieces(String piece) {
    return "\\" + piece.substring(0, 1).toUpperCase() + piece.substring(1);
  }
}
