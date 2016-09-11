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
package com.google.api.codegen.discovery;

import javax.annotation.Nullable;

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryImporter;
import com.google.auto.value.AutoValue;
import com.google.gson.FieldNamingPolicy;
import com.google.protobuf.Field;
import com.google.protobuf.Field.Cardinality;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

@AutoValue
public abstract class TypeInfo {

  /**
   * Returns this type's kind.
   */
  public abstract Field.Kind kind();

  /**
   * Returns true if this type is a map.
   */
  public abstract boolean isMap();

  /**
   * Returns the type for the key of a map type, and null if this type is not a map.
   */
  @Nullable
  public abstract TypeInfo mapKey();

  /**
   * Returns the type for the value of a map type, and null if this type is not a
   * map.
   */
  @Nullable
  public abstract TypeInfo mapValue();

  /**
   * Returns true if this type is an array.
   */
  public abstract boolean isArray();

  /**
   * Returns true if this type is a message.
   */
  public abstract boolean isMessage();

  /**
   * Returns the type for a message, and null if this type is not a message.
   */
  @Nullable
  public abstract MessageTypeInfo message();

  public static Builder newBuilder() {
    return new AutoValue_TypeInfo.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder kind(Field.Kind val);

    public abstract Builder isMap(boolean val);

    public abstract Builder mapKey(TypeInfo val);

    public abstract Builder mapValue(TypeInfo val);

    public abstract Builder isArray(boolean val);

    public abstract Builder isMessage(boolean val);

    public abstract Builder message(MessageTypeInfo val);

    public abstract TypeInfo build();
  }
}
