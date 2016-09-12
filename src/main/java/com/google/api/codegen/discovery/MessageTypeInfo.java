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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = AutoValue_MessageTypeInfo.Builder.class)
public abstract class MessageTypeInfo {

  /**
   * Returns the message's type name.
   *
   * If the message is a method's request type, the type name will be
   * "request$".
   */
  @JsonProperty("typeName")
  public abstract String typeName();

  /**
   * Returns an empty string.
   *
   * This value is intended only for overrides. If not empty, it should be
   * specified as the fully qualified type name of the message type and be used
   * in place of any heuristic to determine the import path and nickname of the
   * message type.
   */
  @JsonProperty("typeNameOverride")
  public abstract String typeNameOverride();

  /**
   * Returns a map of message field names to fields.
   */
  @JsonProperty("fields")
  public abstract Map<String, FieldInfo> fields();

  public static Builder newBuilder() {
    return new AutoValue_MessageTypeInfo.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    @JsonProperty("typeName")
    public abstract Builder typeName(String val);

    @JsonProperty("typeNameOverride")
    public abstract Builder typeNameOverride(String val);

    @JsonProperty("fields")
    public abstract Builder fields(Map<String, FieldInfo> val);

    public abstract MessageTypeInfo build();
  }
}
