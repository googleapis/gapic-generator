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

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class RequestObjectParamView {
  public abstract String name();

  public abstract String elementTypeName();

  public abstract String typeName();

  public abstract String setCallName();

  public abstract boolean isMap();

  public abstract boolean isArray();

  public static Builder newBuilder() {
    return new AutoValue_RequestObjectParamView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder name(String val);

    public abstract Builder elementTypeName(String val);

    public abstract Builder typeName(String val);

    public abstract Builder setCallName(String val);

    public abstract Builder isMap(boolean val);

    public abstract Builder isArray(boolean val);

    public abstract RequestObjectParamView build();
  }
}
