/* Copyright 2017 Google Inc
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
package com.google.api.codegen.config;

import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Oneof;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class OneofConfig {
  public abstract Name groupName();

  public abstract MessageType parentType();

  public abstract FieldModel field();

  /**
   * Returns oneof config for the field of a given type, or null if the field is not a part of any
   * oneofs
   */
  public static OneofConfig of(TypeModel typeModel, String fieldName) {
    if (!typeModel.isMessage()) {
      return null;
    }
    MessageType message = ((ProtoTypeRef) typeModel).getProtoType().getMessageType();
    for (Oneof oneof : message.getOneofs()) {
      for (Field field : oneof.getFields()) {
        if (field.getSimpleName().equals(fieldName)) {
          return new AutoValue_OneofConfig(
              Name.from(oneof.getName()), message, new ProtoField(field));
        }
      }
    }
    return null;
  }
}
