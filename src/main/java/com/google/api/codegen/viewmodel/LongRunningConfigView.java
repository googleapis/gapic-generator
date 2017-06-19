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
package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;
import org.joda.time.Duration;

@AutoValue
public abstract class LongRunningConfigView {
  public abstract String operationResultTypeName();

  public abstract String operationMetadataTypeName();

  public abstract Duration operationInitialPollDelay();

  public abstract double operationPollDelayMultiplier();

  public abstract Duration operationMaxPollDelay();

  public abstract Duration operationTotalPollTimeout();

  public static LongRunningConfigView.Builder newBuilder() {
    return new AutoValue_LongRunningConfigView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract LongRunningConfigView.Builder operationResultTypeName(String val);

    public abstract LongRunningConfigView.Builder operationMetadataTypeName(String val);

    public abstract LongRunningConfigView.Builder operationInitialPollDelay(Duration initialDelay);

    public abstract LongRunningConfigView.Builder operationPollDelayMultiplier(double multiplier);

    public abstract LongRunningConfigView.Builder operationMaxPollDelay(Duration maxDelay);

    public abstract LongRunningConfigView.Builder operationTotalPollTimeout(Duration totalTimeout);

    public abstract LongRunningConfigView build();
  }
}
