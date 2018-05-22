/* Copyright 2018 Google LLC
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
package com.google.api.codegen.viewmodel.ruby;

import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.util.NamePath;
import com.google.api.tools.framework.model.ProtoElement;

public class RubyTypeNameConverter {

  private final TypeModel type;

  public RubyTypeNameConverter(TypeModel type) {
    this.type = type;
  }

  public String getTypeName() {
    if (type.isMessage()) {
      ProtoElement elem = ((ProtoTypeRef) type).getProtoType().getMessageType();
      return NamePath.dotted(elem.getFullName()).withUpperPieces().toDoubleColoned();
    }

    if (type.isEnum()) {
      ProtoElement elem = ((ProtoTypeRef) type).getProtoType().getEnumType();
      return NamePath.dotted(elem.getFullName()).withUpperPieces().toDoubleColoned();
    }

    if (type.isBooleanType()) {
      return "true, false";
    }

    if (type.isStringType() || type.isBytesType()) {
      return "String";
    }

    if (type.isFloatType() || type.isDoubleType()) {
      return "Float";
    }

    return "Integer";
  }
}
