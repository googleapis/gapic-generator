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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.io.File;
import java.util.List;

public class ModelToJavaTypeTable implements ModelTypeTable {
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

  /**
   * A map from primitive types in proto to zero value in Java
   */
  private static final ImmutableMap<Type, String> PRIMITIVE_ZERO_VALUE =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "false")
          .put(Type.TYPE_DOUBLE, "0.0")
          .put(Type.TYPE_FLOAT, "0.0F")
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
          .put(Type.TYPE_BYTES, "ByteString.copyFromUtf8(\"\")")
          .build();

  public ModelToJavaTypeTable() {
    javaTypeTable = new JavaTypeTable();
  }

  @Override
  public ModelTypeTable cloneEmpty() {
    return new ModelToJavaTypeTable();
  }

  @Override
  public void saveNicknameFor(String fullName) {
    getAndSaveNicknameFor(fullName);
  }

  @Override
  public String getFullNameFor(TypeRef type) {
    return getTypeName(type).getFullName();
  }

  @Override
  public String getFullNameForElementType(TypeRef type) {
    return getTypeNameForElementType(type, true).getFullName();
  }

  @Override
  public String getNicknameFor(TypeRef type) {
    return getTypeName(type).getNickname();
  }

  @Override
  public String getAndSaveNicknameFor(String fullName) {
    return javaTypeTable.getAndSaveNicknameFor(fullName);
  }

  /**
   * Returns the Java representation of a reference to a type.
   */
  @Override
  public String getAndSaveNicknameFor(TypeRef type) {
    return javaTypeTable.getAndSaveNicknameFor(getTypeName(type));
  }

  public TypeName getTypeName(TypeRef type) {
    if (type.isMap()) {
      TypeName mapTypeName = javaTypeTable.getTypeName("java.util.Map");
      TypeName keyTypeName = getTypeNameForElementType(type.getMapKeyField().getType(), true);
      TypeName valueTypeName = getTypeNameForElementType(type.getMapValueField().getType(), true);
      return new TypeName(
          mapTypeName.getFullName(),
          mapTypeName.getNickname(),
          "%s<%i, %i>",
          keyTypeName,
          valueTypeName);
    } else if (type.isRepeated()) {
      TypeName listTypeName = javaTypeTable.getTypeName("java.util.List");
      TypeName elementTypeName = getTypeNameForElementType(type, true);
      return new TypeName(
          listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", elementTypeName);
    } else {
      return getTypeNameForElementType(type, false);
    }
  }

  @Override
  public String getAndSaveNicknameForElementType(TypeRef type) {
    return javaTypeTable.getAndSaveNicknameFor(getTypeNameForElementType(type, true));
  }

  @Override
  public String getAndSaveNicknameForContainer(String containerFullName, String elementFullName) {
    TypeName containerTypeName = javaTypeTable.getTypeName(containerFullName);
    TypeName elementTypeName = javaTypeTable.getTypeName(elementFullName);
    TypeName completeTypeName =
        new TypeName(
            containerTypeName.getFullName(),
            containerTypeName.getNickname(),
            "%s<%i>",
            elementTypeName);
    return javaTypeTable.getAndSaveNicknameFor(completeTypeName);
  }

  /**
   * Returns the Java representation of a type, without cardinality. If the type is a Java
   * primitive, basicTypeName returns it in unboxed form.
   */
  private TypeName getTypeNameForElementType(TypeRef type, boolean shouldBoxPrimitives) {
    String primitiveTypeName = PRIMITIVE_TYPE_MAP.get(type.getKind());
    if (primitiveTypeName != null) {
      if (primitiveTypeName.contains(".")) {
        // Fully qualified type name, use regular type name resolver. Can skip boxing logic
        // because those types are already boxed.
        return javaTypeTable.getTypeName(primitiveTypeName);
      } else {
        if (shouldBoxPrimitives) {
          return new TypeName(JavaTypeTable.getBoxedTypeName(primitiveTypeName));
        } else {
          return new TypeName(primitiveTypeName);
        }
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

  @Override
  public List<String> getImports() {
    return javaTypeTable.getImports();
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
        return value + "F";
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

  /**
   * Returns the Java representation of a zero value for that type, to be used in code sample doc.
   *
   * Parametric types may use the diamond operator, since the return value will be used only in
   * initialization.
   */
  @Override
  public String getZeroValueAndSaveNicknameFor(TypeRef type) {
    // Don't call importAndGetShortestName; we don't need to import these.
    if (type.isMap()) {
      return "new HashMap<>()";
    }
    if (type.isRepeated()) {
      return "new ArrayList<>()";
    }
    if (PRIMITIVE_ZERO_VALUE.containsKey(type.getKind())) {
      return PRIMITIVE_ZERO_VALUE.get(type.getKind());
    }
    if (type.isMessage()) {
      return getAndSaveNicknameFor(type) + ".newBuilder().build()";
    }
    return "null";
  }

  private TypeName getTypeName(ProtoElement elem) {
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

    return new TypeName(name, shortName);
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
