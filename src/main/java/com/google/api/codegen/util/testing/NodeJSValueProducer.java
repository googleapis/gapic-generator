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
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

/**
 * A utility class used by the NodeJS test generator which populates values for primitive fields.
 */
public class NodeJSValueProducer implements ValueProducer {
  @Override
  public String produce(TypeRef typeRef, Name identifier) {
    Type type = typeRef.getKind();
    if (type == Type.TYPE_STRING) {
      return identifier.toLowerCamel() + Integer.toString(identifier.hashCode());
    } else if (type == Type.TYPE_BOOL) {
      return identifier.hashCode() % 2 == 0 ? "true" : "false";
    } else if (type == Type.TYPE_BYTES) {
      byte lowByte = (byte) (identifier.hashCode());
      return Byte.toString(lowByte);
    } else if (typeRef.getPrimitiveTypeName().contains("int")) {
      return Integer.toString(identifier.hashCode());
    } else if (type == Type.TYPE_DOUBLE
        || type == Type.TYPE_FLOAT
        || typeRef.getPrimitiveTypeName().contains("fixed")) {
      return Double.toString(identifier.hashCode() / 10);
    } else {
      throw new RuntimeException("Unknown type in ValueProducer.");
    }
  }
}
