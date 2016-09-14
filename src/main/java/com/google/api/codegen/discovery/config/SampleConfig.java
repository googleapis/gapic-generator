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
   * Returns the API's name.
   *
   * For example: "adexchangebuyer"
   */
  @JsonProperty("apiName")
  public abstract String apiName();

  /**
   * Returns the API's version.
   */
  @JsonProperty("apiVersion")
  public abstract String apiVersion();

  /**
   * Returns the API's type name.
   *
   * The type name of the message in the target language, but not
   * fully-qualified. To produce a fully qualified name, it may be necessary to
   * use {@link #packagePrefix()}}.
   *
   * For example: "Adexchangebuyer"
   */
  @JsonProperty("apiTypeName")
  public abstract String apiTypeName();

  /**
   * Returns the language specific package prefix for API types.
   */
  @JsonProperty("packagePrefix")
  public abstract String packagePrefix();

  /**
   * Returns a map of method names to methods.
   */
  @JsonProperty("methods")
  public abstract Map<String, MethodInfo> methods();

  public static Builder newBuilder() {
    return new AutoValue_SampleConfig.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    @JsonProperty("apiTitle")
    public abstract Builder apiTitle(String val);

    @JsonProperty("apiName")
    public abstract Builder apiName(String val);

    @JsonProperty("apiVersion")
    public abstract Builder apiVersion(String val);

    @JsonProperty("apiTypeName")
    public abstract Builder apiTypeName(String val);

    @JsonProperty("packagePrefix")
    public abstract Builder packagePrefix(String val);

    @JsonProperty("methods")
    public abstract Builder methods(Map<String, MethodInfo> val);

    public abstract SampleConfig build();
  }
}
