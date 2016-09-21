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
import com.google.common.collect.ImmutableMap;

import java.util.List;
import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = AutoValue_MethodInfo.Builder.class)
public abstract class MethodInfo {

  /**
   * Returns a list of the method-name's components.
   *
   * The method ID parsed from discovery is of the format
   * "adexchangebuyer.creatives.insert", where the API name is followed be a
   * list of resource names, and ends with the method's name.
   * To accommodate easy overrides, the returned list contains the
   * period-separated components of the method ID with the first component
   * removed.
   * For example: ["creatives", "insert"]
   */
  @JsonProperty("nameComponents")
  public abstract List<String> nameComponents();

  /**
   * Returns a map of field names to fields.
   *
   * The map doesn't include the request body type, see {@link
   * #requestBodyType()}.
   */
  @JsonProperty("fields")
  public abstract ImmutableMap<String, FieldInfo> fields();

  /**
   * Returns the type for this method's request.
   *
   * Apiary clients return a request type that's executed to produce a response.
   * This value contains the properties of that type.
   */
  @JsonProperty("requestType")
  @Nullable
  public abstract TypeInfo requestType();

  /**
   * Returns the type for method's request body, and null if it has none.
   *
   * Methods may contain any number of fields with one of them being an optional
   * message with additional properties. For convenience, that type is returned
   * here because it lacks a proper name.
   */
  @JsonProperty("requestBodyType")
  @Nullable
  public abstract TypeInfo requestBodyType();

  /**
   * Returns the type for this method's response, and null if it has none.
   */
  @JsonProperty("responseType")
  @Nullable
  public abstract TypeInfo responseType();

  /**
   * Returns true if the method is page streaming.
   *
   * True if the method's response type contains the field "nextPageToken".
   */
  @JsonProperty("isPageStreaming")
  public abstract boolean isPageStreaming();

  /**
   * Returns the response type's page streaming resource field, and null if it
   * has none.
   *
   * Always the first type within the response message that has a repeated
   * cardinality.
   */
  @JsonProperty("pageStreamingResourceField")
  @Nullable
  public abstract FieldInfo pageStreamingResourceField();

  public static Builder newBuilder() {
    return new AutoValue_MethodInfo.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    @JsonProperty("nameComponents")
    public abstract Builder nameComponents(List<String> val);

    @JsonProperty("fields")
    public abstract Builder fields(ImmutableMap<String, FieldInfo> val);

    @JsonProperty("requestType")
    public abstract Builder requestType(TypeInfo val);

    @JsonProperty("requestBodyType")
    public abstract Builder requestBodyType(TypeInfo val);

    @JsonProperty("responseType")
    public abstract Builder responseType(TypeInfo val);

    @JsonProperty("isPageStreaming")
    public abstract Builder isPageStreaming(boolean val);

    @JsonProperty("pageStreamingResourceField")
    public abstract Builder pageStreamingResourceField(FieldInfo val);

    public abstract MethodInfo build();
  }
}
