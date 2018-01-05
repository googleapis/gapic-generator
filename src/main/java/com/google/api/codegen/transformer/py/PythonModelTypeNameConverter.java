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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.transformer.ModelTypeNameConverter;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.tools.framework.model.EnumType;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PythonModelTypeNameConverter extends ModelTypeNameConverter {
  private static final String GOOGLE_PREFIX = "google";

  private static final String GOOGLE_CLOUD_PREFIX = GOOGLE_PREFIX + ".cloud";

  /** A map from primitive type to its corresponding Python types */
  private static final Map<Type, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_DOUBLE, "float")
          .put(Type.TYPE_FLOAT, "float")
          .put(Type.TYPE_INT32, "int")
          .put(Type.TYPE_INT64, "long")
          .put(Type.TYPE_UINT32, "int")
          .put(Type.TYPE_UINT64, "long")
          .put(Type.TYPE_SINT32, "int")
          .put(Type.TYPE_SINT64, "long")
          .put(Type.TYPE_FIXED32, "int")
          .put(Type.TYPE_FIXED64, "long")
          .put(Type.TYPE_SFIXED32, "int")
          .put(Type.TYPE_SFIXED64, "long")
          .put(Type.TYPE_BOOL, "bool")
          .put(Type.TYPE_STRING, "str")
          .put(Type.TYPE_BYTES, "bytes")
          .build();

  /** A map from primitive types to its default value. */
  private static final Map<Type, String> PRIMITIVE_ZERO_VALUE =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "False")
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
          .put(Type.TYPE_BYTES, "b\'\'")
          .build();

  // TODO (geigerj): Read this from configuration?
  private final Iterable<String> COMMON_PROTOS =
      ImmutableSet.of(
          "google.iam",
          "google.protobuf",
          "google.api",
          "google.longrunning",
          "google.rpc",
          "google.type",
          "google.logging.type");

  private final TypeNameConverter typeNameConverter;
  private String protoNamespace;

  public PythonModelTypeNameConverter(String implicitPackageName) {
    typeNameConverter = new PythonTypeTable(implicitPackageName);
    if (implicitPackageName.endsWith(".gapic")) {
      protoNamespace = implicitPackageName.replace(".gapic", ".proto");
    } else {
      protoNamespace = String.format("%s.proto", implicitPackageName);
    }
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    return typeNameConverter.getTypeNameInImplicitPackage(shortName);
  }

  @Override
  public TypeName getTypeName(TypeRef type) {
    if (type.isMap()) {
      return new TypeName("dict");
    } else if (type.isRepeated()) {
      return new TypeName("list");
    } else {
      return getTypeNameForElementType(type);
    }
  }

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
    List<String> path = getClassNamePath(elem);

    if (elem instanceof EnumType) {
      path.add(0, "enums");
      String shortName = Joiner.on(".").join(path);
      return getTypeNameInImplicitPackage(shortName);
    }

    path.add(0, getPbFileName(elem.getFile().getSimpleName()));
    path.add(0, protoPackageToPythonPackage(elem.getFile().getProto().getPackage()));
    String fullName = Joiner.on(".").join(path);
    return typeNameConverter.getTypeName(fullName);
  }

  private List<String> getClassNamePath(ProtoElement elem) {
    List<String> path = new LinkedList<>();
    for (ProtoElement elt = elem; elt.getParent() != null; elt = elt.getParent()) {
      path.add(0, elt.getSimpleName());
    }
    return path;
  }

  @Override
  public TypedValue getEnumValue(TypeRef type, EnumValue value) {
    return TypedValue.create(getTypeName(type), "%s." + value.getSimpleName());
  }

  @Override
  public TypedValue getSnippetZeroValue(TypeRef type) {
    // Don't call getTypeName; we don't need to import these.
    if (type.isMap()) {
      return TypedValue.create(new TypeName("dict"), "{}");
    }
    if (type.isRepeated()) {
      return TypedValue.create(new TypeName("list"), "[]");
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
    return TypedValue.create(new TypeName(""), "None");
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
        return Name.from(value.toLowerCase()).toUpperCamel();
      case TYPE_STRING:
        return "'" + value + "'";
      case TYPE_BYTES:
        return "b'" + value + "'";
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
        "getTypeNameForTypedResourceName not supported by Python");
  }

  @Override
  public TypeName getTypeNameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName) {
    throw new UnsupportedOperationException(
        "getTypeNameForResourceNameElementType not supported by Python");
  }

  private String protoPackageToPythonPackage(String protoPackage) {
    if (!protoPackage.startsWith(GOOGLE_PREFIX)) {
      return protoPackage;
    }

    for (String commonProto : COMMON_PROTOS) {
      if (protoPackage.startsWith(commonProto)) {
        return protoPackage;
      }
    }

    return protoNamespace;
  }

  private String getPbFileName(String filename) {
    return filename.substring(filename.lastIndexOf("/") + 1, filename.length() - ".proto".length())
        + "_pb2";
  }
}
