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
package com.google.api.codegen.discovery.viewmodel;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

@AutoValue
public abstract class SampleFieldView {

  @Nullable
  public abstract String name();

  @Nullable
  public abstract String typeName();

  public abstract String defaultValue();

  public abstract String example();

  public abstract String description();

  @Nullable
  public abstract Boolean required();

  @Nullable
  public abstract String setterFuncName();

  // Only intended for use in Go, where some slices must be exploded depending
  // on the context. It's easiest to deal with this at the snippet level.
  @Nullable
  public abstract Boolean isArray();

  public static Builder newBuilder() {
    return new AutoValue_SampleFieldView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String val);

    public abstract Builder typeName(String val);

    public abstract Builder defaultValue(String val);

    public abstract Builder example(String val);

    public abstract Builder description(String val);

    public abstract Builder required(Boolean val);

    public abstract Builder setterFuncName(String val);

    public abstract Builder isArray(Boolean val);

    public abstract SampleFieldView build();
  }
}
