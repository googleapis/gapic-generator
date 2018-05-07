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

import com.google.api.gax.retrying.RetrySettings.Builder;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;
import org.threeten.bp.Duration;

@AutoValue
public abstract class RetryParamsDefinitionView {
  public abstract String key();

  @Nullable // Used in C#
  public abstract String name();

  public abstract String retryBackoffMethodName();

  public abstract String timeoutBackoffMethodName();

  public abstract Duration totalTimeout();

  public abstract Duration initialRetryDelay();

  public abstract double retryDelayMultiplier();

  public abstract Duration maxRetryDelay();

  public abstract Duration initialRpcTimeout();

  public abstract double rpcTimeoutMultiplier();

  public abstract Duration maxRpcTimeout();

  public static Builder newBuilder() {
    return new AutoValue_RetryParamsDefinitionView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder key(String val);

    public abstract Builder name(String val);

    public abstract Builder retryBackoffMethodName(String val);

    public abstract Builder timeoutBackoffMethodName(String val);

    public abstract Builder initialRetryDelay(Duration initialDelay);

    public abstract Builder retryDelayMultiplier(double multiplier);

    public abstract Builder maxRetryDelay(Duration maxDelay);

    public abstract Builder initialRpcTimeout(Duration initialTimeout);

    public abstract Builder rpcTimeoutMultiplier(double multiplier);

    public abstract Builder maxRpcTimeout(Duration maxTimeout);

    public abstract Builder totalTimeout(Duration totalTimeout);

    public abstract RetryParamsDefinitionView build();
  }
}
