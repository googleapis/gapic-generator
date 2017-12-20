/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ResourceIdParamView {
  public abstract String name();

  public abstract String docName();

  public abstract String templateKey();

  public static Builder newBuilder() {
    return new AutoValue_ResourceIdParamView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String val);

    public abstract Builder docName(String val);

    public abstract Builder templateKey(String val);

    public abstract ResourceIdParamView build();
  }
}
