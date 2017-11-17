/* Copyright 2017 Google LLC
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

import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.util.Name;

/** A utility class used by a test generator to populate values for primitive fields. */
public class StandardValueProducer implements ValueProducer {
  @Override
  public String produce(TypeModel typeRef, Name identifier) {
    if (typeRef.isStringType()) {
      return identifier.toLowerCamel() + Integer.toString(identifier.hashCode());
    } else if (typeRef.isBooleanType()) {
      return identifier.hashCode() % 2 == 0 ? "true" : "false";
    } else if (typeRef.isBytesType()) {
      byte lowByte = (byte) (identifier.hashCode());
      return Byte.toString(lowByte);
    } else if (typeRef.getPrimitiveTypeName().contains("int")
        || typeRef.getPrimitiveTypeName().contains("fixed")) {
      return Integer.toString(identifier.hashCode());
    } else if (typeRef.isDoubleType() || typeRef.isFloatType()) {
      return Double.toString(identifier.hashCode() / 10);
    } else {
      throw new RuntimeException("Unknown type in ValueProducer: " + typeRef.getTypeName());
    }
  }
}
