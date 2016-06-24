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
package com.google.api.codegen.proto3;

import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.java.direct.JavaTypeTable;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.io.File;

public class Proto3JavaTypeTable {
  private JavaTypeTable javaTypeTable;

  /**
   * The package prefix protoc uses if no java package option was provided.
   */
  private static final String DEFAULT_JAVA_PACKAGE_PREFIX = "com.google.protos";

  /**
   * A map from primitive types in proto to Java counterparts.
   */
  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "boolean")
          .put(Type.TYPE_DOUBLE, "double")
          .put(Type.TYPE_FLOAT, "float")
          .put(Type.TYPE_INT64, "long")
          .put(Type.TYPE_UINT64, "long")
          .put(Type.TYPE_SINT64, "long")
          .put(Type.TYPE_FIXED64, "long")
          .put(Type.TYPE_SFIXED64, "long")
          .put(Type.TYPE_INT32, "int")
          .put(Type.TYPE_UINT32, "int")
          .put(Type.TYPE_SINT32, "int")
          .put(Type.TYPE_FIXED32, "int")
          .put(Type.TYPE_SFIXED32, "int")
          .put(Type.TYPE_STRING, "java.lang.String")
          .put(Type.TYPE_BYTES, "com.google.protobuf.ByteString")
          .build();

  public Proto3JavaTypeTable() {
    javaTypeTable = new JavaTypeTable();
  }

  public String importAndGetShortestName(String longName) {
    return javaTypeTable.importAndGetShortestName(longName);
  }

  /**
   * Returns the Java representation of a reference to a type.
   */
  public String importAndGetShortestName(TypeRef type) {
    if (type.isMap()) {
      String mapTypeName = javaTypeTable.importAndGetShortestName("java.util.Map");
      String keyTypeName = importAndGetShortestNameForElementType(type.getMapKeyField().getType());
      String valueTypeName =
          importAndGetShortestNameForElementType(type.getMapValueField().getType());
      return String.format(
          "%s<%s, %s>",
          mapTypeName,
          JavaTypeTable.getBoxedTypeName(keyTypeName),
          JavaTypeTable.getBoxedTypeName(valueTypeName));
    } else if (type.isRepeated()) {
      String listTypeName = javaTypeTable.importAndGetShortestName("java.util.List");
      String elementTypeName = importAndGetShortestNameForElementType(type);
      return String.format("%s<%s>", listTypeName, JavaTypeTable.getBoxedTypeName(elementTypeName));
    } else {
      return importAndGetShortestNameForElementType(type);
    }
  }

  public String importAndGetShortestName(ProtoElement elem) {
    // Construct the full name in Java
    String name = getJavaPackage(elem.getFile());
    if (!elem.getFile().getProto().getOptions().getJavaMultipleFiles()) {
      String outerClassName = elem.getFile().getProto().getOptions().getJavaOuterClassname();
      if (outerClassName.isEmpty()) {
        outerClassName = getFileClassName(elem.getFile());
      }
      name = name + "." + outerClassName;
    }
    String shortName = elem.getFullName().substring(elem.getFile().getFullName().length() + 1);
    name = name + "." + shortName;

    return javaTypeTable.importAndGetShortestName(name, shortName);
  }

  /**
   * Returns the Java representation of a type, without cardinality. If the type is a Java
   * primitive, basicTypeName returns it in unboxed form.
   */
  public String importAndGetShortestNameForElementType(TypeRef type) {
    String primitiveJavaTypeName = PRIMITIVE_TYPE_MAP.get(type.getKind());
    if (primitiveJavaTypeName != null) {
      if (primitiveJavaTypeName.contains(".")) {
        // Fully qualified type name, use regular type name resolver. Can skip boxing logic
        // because those types are already boxed.
        return javaTypeTable.importAndGetShortestName(primitiveJavaTypeName);
      }
      return primitiveJavaTypeName;
    }
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return importAndGetShortestName(type.getMessageType());
      case TYPE_ENUM:
        return importAndGetShortestName(type.getEnumType());
      default:
        throw new IllegalArgumentException("unknown type kind: " + type.getKind());
    }
  }

  private static String getJavaPackage(ProtoFile file) {
    String packageName = file.getProto().getOptions().getJavaPackage();
    if (Strings.isNullOrEmpty(packageName)) {
      return DEFAULT_JAVA_PACKAGE_PREFIX + "." + file.getFullName();
    }
    return packageName;
  }

  /**
   * Gets the class name for the given proto file.
   */
  private static String getFileClassName(ProtoFile file) {
    String baseName = Files.getNameWithoutExtension(new File(file.getSimpleName()).getName());
    return LanguageUtil.lowerUnderscoreToUpperCamel(baseName);
  }
}
