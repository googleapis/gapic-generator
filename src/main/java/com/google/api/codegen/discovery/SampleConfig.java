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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = AutoValue_SampleConfig.Builder.class)
public abstract class SampleConfig {

  /**
   * Returns the API's title.
   *
   * A printable representation of the API's title.
   * For example: "Ad Exchange Buyer API"
   */
  @JsonProperty("apiTitle")
  public abstract String apiTitle();

  /**
   * Returns a period joined concatenation of the API's name and version.
   *
   * For example: "adexchangebuyer.v1.4"
   */
  @JsonProperty("apiNameVersion")
  public abstract String apiNameVersion();

  /**
   * Returns the API's type name.
   *
   * This value will always be lower-case and contain only alphanumeric
   * characters.
   * For example: "adexchangebuyer"
   */
  @JsonProperty("apiTypeName")
  public abstract String apiTypeName();

  /**
   * Returns the sample's method.
   */
  @JsonProperty("methodInfo")
  public abstract MethodInfo methodInfo();

  public static Builder newBuilder() {
    return new AutoValue_SampleConfig.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    @JsonProperty("apiTitle")
    public abstract Builder apiTitle(String val);

    @JsonProperty("apiNameVersion")
    public abstract Builder apiNameVersion(String val);

    @JsonProperty("apiTypeName")
    public abstract Builder apiTypeName(String val);

    @JsonProperty("methodInfo")
    public abstract Builder methodInfo(MethodInfo val);

    public abstract SampleConfig build();
  }
}
