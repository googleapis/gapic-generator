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
package com.google.api.codegen.util.testing;

import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.model.TypeRef;

/**
 * A utility class used by the Java test generator which populates values for primitive fields.
 */
public class JavaTestValueGenerator extends TestValueGenerator {

  @Override
  protected String generateValue(TypeRef typeRef, Name identifier) {
    String typeName = typeRef.getPrimitiveTypeName();
    if (typeName == "string") {
      return identifier.toLowerCamel() + Integer.toString(identifier.hashCode());
    } else if (typeName == "bool") {
      return identifier.hashCode() % 2 == 0 ? "true" : "false";
    } else if (typeName.contains("int")) {
      return Integer.toString(identifier.hashCode());
    } else if (typeName == "byte") {
      byte lowByte = (byte) (identifier.hashCode());
      return Byte.toString(lowByte);
    } else {
      return Double.toString(identifier.hashCode() / 10);
    }
  }
}
