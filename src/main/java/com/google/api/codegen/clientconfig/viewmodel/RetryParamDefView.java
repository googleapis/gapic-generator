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

/** Represents a retry param definition. */
@AutoValue
public abstract class RetryParamDefView {
  public abstract String name();

  public abstract String initialRetryDelayMillis();

  public abstract String retryDelayMultiplier();

  public abstract String maxRetryDelayMillis();

  public abstract String initialRpcTimeoutMillis();

  public abstract String rpcTimeoutMultiplier();

  public abstract String maxRpcTimeoutMillis();

  public abstract String totalTimeoutMillis();

  public static Builder newBuilder() {
    return new AutoValue_RetryParamDefView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String val);

    public abstract Builder initialRetryDelayMillis(String val);

    public abstract Builder retryDelayMultiplier(String val);

    public abstract Builder maxRetryDelayMillis(String val);

    public abstract Builder initialRpcTimeoutMillis(String val);

    public abstract Builder rpcTimeoutMultiplier(String val);

    public abstract Builder maxRpcTimeoutMillis(String val);

    public abstract Builder totalTimeoutMillis(String val);

    public abstract RetryParamDefView build();
  }
}
