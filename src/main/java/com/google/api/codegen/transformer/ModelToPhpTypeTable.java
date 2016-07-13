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
package com.google.api.codegen.transformer;

import com.google.api.codegen.php.PhpTypeTable;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

public class ModelToPhpTypeTable implements ModelTypeTable {
  private PhpTypeTable phpTypeTable;

  /**
   * A map from primitive types in proto to PHP counterparts.
   */
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

  /**
   * A map from primitive types in proto to zero value in PHP
   */
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
          .put(Type.TYPE_STRING, "\"\"")
          .put(Type.TYPE_BYTES, "\"\"")
          .build();

  public ModelToPhpTypeTable() {
    phpTypeTable = new PhpTypeTable();
  }

  @Override
  public ModelTypeTable cloneEmpty() {
    return new ModelToPhpTypeTable();
  }

  @Override
  public String getFullNameFor(TypeRef type) {
    return getTypeAlias(type).getFullName();
  }

  @Override
  public String getNicknameFor(TypeRef type) {
    return getTypeAlias(type).getNickname();
  }

  @Override
  public void saveNicknameFor(String fullName) {
    getAndSaveNicknameFor(fullName);
  }

  @Override
  public String getAndSaveNicknameFor(String fullName) {
    return phpTypeTable.getAndSaveNicknameFor(fullName);
  }

  /**
   * Returns the PHP representation of a reference to a type.
   */
  @Override
  public String getAndSaveNicknameFor(TypeRef type) {
    return phpTypeTable.getAndSaveNicknameFor(getTypeAlias(type));
  }

  private TypeAlias getTypeAlias(TypeRef type) {
    if (type.isMap()) {
      return new TypeAlias("array");
    } else if (type.isRepeated()) {
      TypeAlias alias = getAliasForElementType(type);
      return new TypeAlias(alias.getFullName() + "[]", alias.getNickname() + "[]");
    } else {
      return getAliasForElementType(type);
    }
  }

  @Override
  public String getAndSaveNicknameForElementType(TypeRef type) {
    return phpTypeTable.getAndSaveNicknameFor(getAliasForElementType(type));
  }

  /**
   * Returns the PHP representation of a type, without cardinality. If the type is a primitive,
   * importAndGetShortestNameForElementType returns it in unboxed form.
   */
  private TypeAlias getAliasForElementType(TypeRef type) {
    String primitiveTypeName = PRIMITIVE_TYPE_MAP.get(type.getKind());
    if (primitiveTypeName != null) {
      if (primitiveTypeName.contains("\\")) {
        // Fully qualified type name, use regular type name resolver. Can skip boxing logic
        // because those types are already boxed.
        return phpTypeTable.getAlias(primitiveTypeName);
      } else {
        return new TypeAlias(primitiveTypeName);
      }
    }
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return getTypeAlias(type.getMessageType());
      case TYPE_ENUM:
        return getTypeAlias(type.getEnumType());
      default:
        throw new IllegalArgumentException("unknown type kind: " + type.getKind());
    }
  }

  @Override
  public String renderPrimitiveValue(TypeRef keyType, String key) {
    throw new NotImplementedException("PhpIdentifierNamer.renderPrimitiveValue");
  }

  /**
   * Returns the PHP representation of a zero value for that type, to be used in code sample doc.
   *
   * Parametric types may use the diamond operator, since the return value will be used only in
   * initialization.
   */
  @Override
  public String getZeroValueAndSaveNicknameFor(TypeRef type) {
    // Don't call getTypeName; we don't need to import these.
    if (type.isMap()) {
      return "[]";
    }
    if (type.isRepeated()) {
      return "[]";
    }
    if (PRIMITIVE_ZERO_VALUE.containsKey(type.getKind())) {
      return PRIMITIVE_ZERO_VALUE.get(type.getKind());
    }
    if (type.isMessage()) {
      return "new " + getAndSaveNicknameFor(type) + "()";
    }
    return "null";
  }

  @Override
  public List<String> getImports() {
    return phpTypeTable.getImports();
  }

  /**
   * Gets the full name of the message or enum type in PHP.
   */
  private TypeAlias getTypeAlias(ProtoElement elem) {
    // Construct the fully-qualified PHP class name
    int fullNamePrefixLength = elem.getFile().getFullName().length() + 1;
    String nickname = elem.getFullName().substring(fullNamePrefixLength);
    String fullName = getPhpPackage(elem.getFile()) + "\\" + nickname;
    return new TypeAlias(fullName, nickname);
  }

  /**
   * Gets the PHP package for the given proto file.
   */
  private static String getPhpPackage(ProtoFile file) {
    return file.getProto().getPackage().replaceAll("\\.", "\\\\");
  }
}
