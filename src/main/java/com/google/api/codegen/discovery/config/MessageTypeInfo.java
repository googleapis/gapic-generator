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
import java.util.Map;

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

    @JsonProperty("fields")
    public abstract Builder fields(Map<String, FieldInfo> val);

    public abstract MessageTypeInfo build();
  }
}
