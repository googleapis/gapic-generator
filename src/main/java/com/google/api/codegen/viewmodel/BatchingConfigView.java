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
import javax.annotation.Nullable;

@AutoValue
public abstract class BatchingConfigView {
  public abstract int elementCountThreshold();

  public abstract long requestByteThreshold();

  public abstract long delayThresholdMillis();

  @Nullable
  public abstract Long flowControlElementLimit();

  @Nullable
  public abstract Long flowControlByteLimit();

  public abstract String flowControlLimitExceededBehavior();

  public boolean hasFlowControlElementLimit() {
    return flowControlElementLimit() != null;
  }

  public boolean hasFlowControlByteLimit() {
    return flowControlByteLimit() != null;
  }

  public static Builder newBuilder() {
    return new AutoValue_BatchingConfigView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder elementCountThreshold(int val);

    public abstract Builder requestByteThreshold(long val);

    public abstract Builder delayThresholdMillis(long val);

    public abstract Builder flowControlElementLimit(Long val);

    public abstract Builder flowControlByteLimit(Long val);

    public abstract Builder flowControlLimitExceededBehavior(String val);

    public abstract BatchingConfigView build();
  }
}
