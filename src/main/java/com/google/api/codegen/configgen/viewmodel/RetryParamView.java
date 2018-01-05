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
package com.google.api.codegen.configgen.viewmodel;

import com.google.auto.value.AutoValue;

/** Represents the definition for retry/backoff parameters. */
@AutoValue
public abstract class RetryParamView {
  /** The name of the definition. */
  public abstract String name();

  /** The initial delay in retry in milliseconds. */
  public abstract String initialRetryDelayMillis();

  /** The multiplier for the retry delay. */
  public abstract String retryDelayMultiplier();

  /** The maximum delay in relay in milliseconds. */
  public abstract String maxRetryDelayMillis();

  /** The initial timeout in milliseconds. */
  public abstract String initialRpcTimeoutMillis();

  /** The multiplier for the timeout. */
  public abstract String rpcTimeoutMultiplier();

  /** The maximum timeout value in milliseconds. */
  public abstract String maxRpcTimeoutMillis();

  /** The maximum accumulated timeout in milliseconds. */
  public abstract String totalTimeoutMillis();

  public static Builder newBuilder() {
    return new AutoValue_RetryParamView.Builder();
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

    public abstract RetryParamView build();
  }
}
