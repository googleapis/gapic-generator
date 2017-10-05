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
package com.google.api.codegen.viewmodel.testing;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** Parameter used for the instantiation of a client. */
@AutoValue
public abstract class ClientInitParamView {
  /** The value of the parameter. */
  public abstract String value();

  /** Keyword for optional parameters */
  @Nullable
  public abstract String key();

  public static Builder newBuilder() {
    return new AutoValue_ClientInitParamView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder value(String val);

    public abstract Builder key(String val);

    public abstract ClientInitParamView build();
  }
}
