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
package com.google.api.codegen.discovery.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.protobuf.Field;
import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = AutoValue_TypeInfo.Builder.class)
public abstract class TypeInfo {

  /** Returns this type's kind. */
  @JsonProperty("kind")
  public abstract Field.Kind kind();

  /** Returns true if this type is a map. */
  @JsonProperty("isMap")
  public abstract boolean isMap();

  /** Returns the type for the key of a map type, and null if this type is not a map. */
  @JsonProperty("mapKey")
  @Nullable
  public abstract TypeInfo mapKey();

  /** Returns the type for the value of a map type, and null if this type is not a map. */
  @Nullable
  @JsonProperty("mapValue")
  public abstract TypeInfo mapValue();

  /** Returns true if this type is an array. */
  @JsonProperty("isArray")
  public abstract boolean isArray();

  /** Returns true if this type is a message. */
  @JsonProperty("isMessage")
  public abstract boolean isMessage();

  /** Returns the type for a message, and null if this type is not a message. */
  @JsonProperty("message")
  @Nullable
  public abstract MessageTypeInfo message();

  public static Builder newBuilder() {
    return new AutoValue_TypeInfo.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    @JsonProperty("kind")
    public abstract Builder kind(Field.Kind val);

    @JsonProperty("isMap")
    public abstract Builder isMap(boolean val);

    @JsonProperty("mapKey")
    public abstract Builder mapKey(TypeInfo val);

    @JsonProperty("mapValue")
    public abstract Builder mapValue(TypeInfo val);

    @JsonProperty("isArray")
    public abstract Builder isArray(boolean val);

    @JsonProperty("isMessage")
    public abstract Builder isMessage(boolean val);

    @JsonProperty("message")
    public abstract Builder message(MessageTypeInfo val);

    public abstract TypeInfo build();
  }
}
