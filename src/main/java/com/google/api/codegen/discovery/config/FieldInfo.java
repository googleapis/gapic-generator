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
import com.google.protobuf.Field.Cardinality;

@AutoValue
@JsonDeserialize(builder = AutoValue_FieldInfo.Builder.class)
public abstract class FieldInfo {

  /**
   * Returns the field's name.
   *
   * <p>Always lower camel case. For example: "projectId"
   */
  @JsonProperty("name")
  public abstract String name();

  /** Returns the field's type. */
  @JsonProperty("type")
  public abstract TypeInfo type();

  /** Returns the field's cardinality. */
  @JsonProperty("cardinality")
  public abstract Cardinality cardinality();

  /** Returns the example value of the field, or empty string if none. */
  @JsonProperty("example")
  public abstract String example();

  /** Returns the description of the field. */
  @JsonProperty("description")
  public abstract String description();

  public static Builder newBuilder() {
    return new AutoValue_FieldInfo.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    @JsonProperty("name")
    public abstract Builder name(String val);

    @JsonProperty("type")
    public abstract Builder type(TypeInfo val);

    @JsonProperty("cardinality")
    public abstract Builder cardinality(Cardinality cardinality);

    @JsonProperty("example")
    public abstract Builder example(String val);

    @JsonProperty("description")
    public abstract Builder description(String val);

    public abstract FieldInfo build();
  }
}
