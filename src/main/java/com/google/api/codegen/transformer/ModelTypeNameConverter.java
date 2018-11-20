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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.ProtoField;
import com.google.api.codegen.config.ProtoInterfaceModel;
import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypedValue;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

/** ModelTypeNameConverter maps TypeRef instances to TypeName instances. */
public abstract class ModelTypeNameConverter implements TypeNameConverter {
  /** Provides a TypeName for the given TypeRef. */
  public abstract TypeName getTypeName(TypeRef type);

  /** Provides a TypeName for the given TypeRef. */
  @Override
  public TypeName getTypeName(InterfaceModel interfaceModel) {
    return getTypeName(((ProtoInterfaceModel) interfaceModel).getInterface());
  }

  /** Provides a TypedValue for the given enum TypeRef. */
  public abstract TypedValue getEnumValue(TypeRef type, EnumValue value);

  /** Provides a TypeName for the element type of the given TypeRef. */
  public abstract TypeName getTypeNameForElementType(TypeRef type);

  @Override
  public TypeName getTypeName(TypeModel type) {
    return getTypeName(((ProtoTypeRef) type).getProtoType());
  }

  @Override
  /* Provides a TypeName for the given FieldConfig and resource short name. */
  public abstract TypeName getTypeNameForTypedResourceName(
      FieldConfig fieldConfig, String typedResourceShortName);

  @Override
  /*
   * Provides a TypeName for the given FieldConfig and resource short name, using the inner type if
   * the underlying field is repeated.
   */
  public abstract TypeName getTypeNameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName);

  /** Provides a TypeName for the given ProtoElement. */
  public abstract TypeName getTypeName(ProtoElement elem);

  @Override
  /* Provides a TypeName for the given short name, using the default package. */
  public abstract TypeName getTypeNameInImplicitPackage(String shortName);

  /**
   * Provides a TypedValue containing the zero value of the given type, plus the TypeName of the
   * type; suitable for use within code snippets.
   */
  public abstract TypedValue getSnippetZeroValue(TypeRef type);

  /**
   * Provides a TypedValue containing the zero value of the given type, for use internally within
   * the vkit layer; plus the TypeName of the type. This will often return the same value as {@link
   * #getSnippetZeroValue(TypeRef)}.
   */
  public abstract TypedValue getImplZeroValue(TypeRef type);

  /** Renders the given value if it is a primitive type. */
  public abstract String renderPrimitiveValue(TypeRef type, String value);

  /** Renders the given value if it is a primitive type. */
  String renderValueAsString(String value) {
    return renderPrimitiveValue(TypeRef.of(Type.TYPE_STRING), value);
  }

  // Overriden interface methods call the overloaded method on the underlying Field object.

  /** Provides a TypeName for the given FieldModel. */
  @Override
  public TypeName getTypeName(FieldModel type) {
    return getTypeName((((ProtoField) type).getType().getProtoType()));
  }

  /** Provides a TypedValue for the given enum FieldModel. */
  @Override
  public TypedValue getEnumValue(FieldModel type, EnumValue value) {
    return getEnumValue((((ProtoField) type).getType().getProtoType()), value);
  }

  /** Provides a TypeName for the element type of the given FieldModel. */
  @Override
  public TypeName getTypeNameForElementType(FieldModel type) {
    return getTypeNameForElementType((((ProtoField) type).getType().getProtoType()));
  }

  /**
   * Provides a TypedValue containing the zero value of the given type, plus the TypeName of the
   * type; suitable for use within code snippets.
   */
  @Override
  public TypedValue getSnippetZeroValue(FieldModel type) {
    return getSnippetZeroValue((((ProtoField) type).getType().getProtoType()));
  }

  /**
   * Provides a TypedValue containing the zero value of the given type, for use internally within
   * the vkit layer; plus the TypeName of the type. This will often return the same value as {@link
   * #getSnippetZeroValue(FieldModel)}.
   */
  @Override
  public TypedValue getImplZeroValue(FieldModel type) {
    return getImplZeroValue((((ProtoField) type).getType().getProtoType()));
  }

  /** Renders the given value if it is a primitive type. */
  @Override
  public String renderPrimitiveValue(FieldModel type, String value) {
    return renderPrimitiveValue((((ProtoField) type).getType().getProtoType()), value);
  }
}
