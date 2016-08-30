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
 * A read-only interface for mapping types to a corresponding String representation for a particular
 * language.
 */
public interface MethodTypeFormatter {

  /**
   * Returns method's name.
   */
  String getMethodName(Method method);

  /**
   * Takes a fully-qualified type name and returns its simple name.
   */
  String getTypeName(String typeName);

  /**
   * Renders the primitive value of the given type.
   */
  String renderPrimitiveValue(String typeName, String value);
}
