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
package com.google.api.codegen.viewmodel;

import com.google.api.codegen.viewmodel.ListMethodDetailView.Builder;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PageStreamingDescriptorView {
  public abstract String varName();

  public abstract String requestTokenFieldName();

  public abstract String responseTokenFieldName();

  public abstract String resourcesFieldName();

  public abstract String methodName();

  public static Builder newBuilder() {
    return new AutoValue_PageStreamingDescriptorView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder varName(String val);

    public abstract Builder requestTokenFieldName(String val);

    public abstract Builder responseTokenFieldName(String val);

    public abstract Builder resourcesFieldName(String val);

    public abstract Builder methodName(String val);

    public abstract PageStreamingDescriptorView build();
  }
}
