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

import com.google.api.codegen.config.*;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;

/** Default implementation of ModelTypeFormatter. */
public class ModelTypeFormatterImpl implements ModelTypeFormatter {
  private ModelTypeNameConverter typeNameConverter;

  public ModelTypeFormatterImpl(ModelTypeNameConverter typeNameConverter) {
    this.typeNameConverter = typeNameConverter;
  }

  @Override
  public String getImplicitPackageFullNameFor(String shortName) {
    return typeNameConverter.getTypeNameInImplicitPackage(shortName).getFullName();
  }

  @Override
  public String getFullNameFor(TypeRef type) {
    return typeNameConverter.getTypeName(type).getFullName();
  }

  @Override
  public String getFullNameFor(ProtoElement element) {
    return typeNameConverter.getTypeName(element).getFullName();
  }

  @Override
  public String getFullNameFor(InterfaceModel interfaceModel) {
    return typeNameConverter
        .getTypeName(((ProtoInterfaceModel) interfaceModel).getInterface())
        .getFullName();
  }

  @Override
  public String getFullNameForElementType(TypeRef type) {
    return typeNameConverter.getTypeNameForElementType(type).getFullName();
  }

  @Override
  public String getFullNameFor(TypeModel type) {
    return typeNameConverter.getTypeName(type).getFullName();
  }

  @Override
  public String getFullNameForMessageType(TypeModel type) {
    return typeNameConverter
        .getTypeName(((ProtoTypeRef) type).getProtoType().getMessageType())
        .getFullName();
  }

  @Override
  public String getNicknameFor(TypeRef type) {
    return typeNameConverter.getTypeName(type).getNickname();
  }

  @Override
  public String renderPrimitiveValue(TypeRef type, String value) {
    return typeNameConverter.renderPrimitiveValue(type, value);
  }

  @Override
  public String getFullNameFor(FieldModel type) {
    return getFullNameFor(type.getProtoTypeRef());
  }

  @Override
  public String getFullNameForElementType(FieldModel type) {
    return getFullNameForElementType(type.getProtoTypeRef());
  }

  @Override
  public String getNicknameFor(FieldModel type) {
    return getNicknameFor(type.getProtoTypeRef());
  }

  @Override
  public String getNicknameFor(TypeModel type) {
    return getNicknameFor(((ProtoTypeRef) type).getProtoType());
  }

  @Override
  public String renderPrimitiveValue(FieldModel type, String key) {
    return renderPrimitiveValue(type.getProtoTypeRef(), key);
  }

  @Override
  public String renderPrimitiveValue(TypeModel type, String key) {
    return renderPrimitiveValue(((ProtoTypeRef) type).getProtoType(), key);
  }
}
