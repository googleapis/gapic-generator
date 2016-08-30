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
package com.google.api.codegen.discovery.transformer;

import com.google.protobuf.Method;

/**
 * Default implementation of MethodTypeFormatter.
 */
public class MethodTypeFormatterImpl implements MethodTypeFormatter {

  private TypeNameConverter typeNameConverter;

  public MethodTypeFormatterImpl(TypeNameConverter typeNameConverter) {
    this.typeNameConverter = typeNameConverter;
  }

  @Override
  public String getMethodName(Method method) {
    return "";
  }

  @Override
  public String getTypeName(String typeName) {
    return "";
  }

  @Override
  public String renderPrimitiveValue(String typeName, String value) {
    return "";
  }
}
