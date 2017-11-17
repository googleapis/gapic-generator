/* Copyright 2017 Google LLC
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

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** Represents the request information of the page streaming method. */
@AutoValue
public abstract class PageStreamingRequestView {
  /** The name of the field in the request containing the page token. */
  @Nullable
  public abstract String tokenField();

  /** True if the tokenField property exists. */
  public boolean hasTokenField() {
    return tokenField() != null;
  }

  /**
   * The name of the field in the request specifying the maximum number of elements to be returned
   * in the response.
   */
  @Nullable
  public abstract String pageSizeField();

  /** True if the pageSizeField property exists. */
  public boolean hasPageSizeField() {
    return pageSizeField() != null;
  }

  public static Builder newBuilder() {
    return new AutoValue_PageStreamingRequestView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder tokenField(String val);

    public abstract Builder pageSizeField(String val);

    public abstract PageStreamingRequestView build();
  }
}
