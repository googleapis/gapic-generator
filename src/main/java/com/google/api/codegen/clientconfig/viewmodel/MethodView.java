/* Copyright 2017 Google LLC
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
package com.google.api.codegen.clientconfig.viewmodel;

import com.google.auto.value.AutoValue;
import java.util.List;

/** Represents a method. */
@AutoValue
public abstract class MethodView {
  public abstract String name();

  public abstract String timeoutMillis();

  public abstract boolean isRetryingSupported();

  public abstract String retryCodesName();

  public abstract String retryParamsName();

  public abstract List<PairView> batching();

  public static Builder newBuilder() {
    return new AutoValue_MethodView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String val);

    public abstract Builder timeoutMillis(String val);

    public abstract Builder isRetryingSupported(boolean val);

    public abstract Builder retryCodesName(String val);

    public abstract Builder retryParamsName(String val);

    public abstract Builder batching(List<PairView> val);

    public abstract MethodView build();
  }
}
