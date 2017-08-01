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

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PageStreamingResponseView {
  public abstract String tokenField();

  public abstract String resourcesField();

  public static Builder newBuilder() {
    return new AutoValue_PageStreamingResponseView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder tokenField(String val);

    public abstract Builder resourcesField(String val);

    public abstract PageStreamingResponseView build();
  }
}
