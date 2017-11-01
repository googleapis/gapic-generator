/* Copyright 2016 Google LLC
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

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ResourceNameParamView {

  public abstract int index();

  public abstract String nameAsParam();

  public abstract String nameAsProperty();

  public abstract String docName();

  public static Builder newBuilder() {
    return new AutoValue_ResourceNameParamView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder index(int val);

    public abstract Builder nameAsParam(String val);

    public abstract Builder nameAsProperty(String val);

    public abstract Builder docName(String val);

    public abstract ResourceNameParamView build();
  }
}
