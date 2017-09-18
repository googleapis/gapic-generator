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
package com.google.api.codegen.configgen.viewmodel;

import com.google.api.codegen.ResourceNameTreatment;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

/** Represents the method configuration. */
@AutoValue
public abstract class MethodView implements Comparable<MethodView> {
  /** The simple name of the method. */
  public abstract String name();

  /** The configuration for parameter flattening. */
  @Nullable
  public abstract FlatteningView flattening();

  /** True if the flattening property exists. */
  public boolean hasFlattening() {
    return flattening() != null;
  }

  /** The fields that are always required for a request to be valid. */
  public abstract List<String> requiredFields();

  /** Turns on or off generation of a method whose sole parameter is a request object. */
  public abstract boolean requestObjectMethod();

  /** The resource name treatment. */
  @Nullable
  public abstract ResourceNameTreatment resourceNameTreatment();

  /** The configuration for paging. */
  @Nullable
  public abstract PageStreamingView pageStreaming();

  /** True of the pageStreaming property exists. */
  public boolean hasPageStreaming() {
    return pageStreaming() != null;
  }

  /** The configuration for retryable codes. */
  public abstract String retryCodesName();

  /** The configuration for retry/backoff parameters. */
  public abstract String retryParamsName();

  /**
   * The entires of the map from the field names of the request type to the entity names of the
   * collection.
   */
  public abstract List<FieldNamePatternView> fieldNamePatterns();

  /** The default timeout for a non-retrying call. */
  public abstract String timeoutMillis();

  public static Builder newBuilder() {
    return new AutoValue_MethodView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String val);

    public abstract Builder flattening(FlatteningView val);

    public abstract Builder requiredFields(List<String> val);

    public abstract Builder requestObjectMethod(boolean val);

    public abstract Builder resourceNameTreatment(ResourceNameTreatment val);

    public abstract Builder pageStreaming(PageStreamingView val);

    public abstract Builder retryCodesName(String val);

    public abstract Builder retryParamsName(String val);

    public abstract Builder fieldNamePatterns(List<FieldNamePatternView> val);

    public abstract Builder timeoutMillis(String val);

    public abstract MethodView build();
  }

  @Override
  public int compareTo(MethodView other) {
    return this.name().compareTo(other.name());
  }
}
