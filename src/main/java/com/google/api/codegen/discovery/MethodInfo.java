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

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryImporter;
import com.google.auto.value.AutoValue;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
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
  public abstract List<String> nameComponents();

  /**
   * Returns a list of this method's fields.
   *
   * The list doesn't include the request body type, see {@link
   * #requestBodyType()}.
   */
  public abstract List<FieldInfo> fields();

  /**
   * Returns the type for this method's request.
   *
   * Apiary clients return a request type that's executed to produce a response.
   * This value contains the properties of that type.
   */
  @Nullable
  public abstract TypeInfo requestType();

  /**
   * Returns the type for method's request body, and null if it has none.
   *
   * Methods may contain any number of fields with one of them being an optional
   * message with additional properties. For convenience, that type is returned
   * here because it lacks a proper name.
   * @return
   */
  @Nullable
  public abstract TypeInfo requestBodyType();

  /**
   * Returns the type for this method's response, and null if it has none.
   */
  @Nullable
  public abstract TypeInfo responseType();

  /**
   * Returns true if the method is page streaming.
   *
   * True if the method's response type contains the field "nextPageToken".
   */
  public abstract boolean isPageStreaming();

  /**
   * Returns the response type's page streaming resource field, and null if it
   * has none.
   *
   * Always the first type within the response message that has a repeated
   * cardinality.
   */
  @Nullable
  public abstract FieldInfo pageStreamingResourceField();

  public static Builder newBuilder() {
    return new AutoValue_MethodInfo.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder nameComponents(List<String> val);

    public abstract Builder fields(List<FieldInfo> val);

    public abstract Builder requestType(TypeInfo val);

    public abstract Builder requestBodyType(TypeInfo val);

    public abstract Builder responseType(TypeInfo val);

    public abstract Builder isPageStreaming(boolean val);

    public abstract Builder pageStreamingResourceField(FieldInfo val);

    public abstract MethodInfo build();
  }
}
