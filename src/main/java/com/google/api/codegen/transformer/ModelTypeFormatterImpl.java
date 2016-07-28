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

import com.google.api.tools.framework.model.TypeRef;

/**
 * Default implementation of ModelTypeFormatter.
 */
public class ModelTypeFormatterImpl implements ModelTypeFormatter {
  private ModelTypeNameConverter typeNameConverter;

  /**
   * Standard constructor.
   */
  public ModelTypeFormatterImpl(ModelTypeNameConverter typeNameConverter) {
    this.typeNameConverter = typeNameConverter;
  }

  @Override
  public String getFullNameFor(TypeRef type) {
    return typeNameConverter.getTypeName(type).getFullName();
  }

  @Override
  public String getNicknameFor(TypeRef type) {
    return typeNameConverter.getTypeName(type).getNickname();
  }

  @Override
  public String renderPrimitiveValue(TypeRef type, String value) {
    return typeNameConverter.renderPrimitiveValue(type, value);
  }
}
