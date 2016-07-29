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
public abstract class BundlingConfigView {
  public abstract int elementCountThreshold();

  public abstract long requestByteThreshold();

  public abstract int elementCountLimit();

  public abstract long requestByteLimit();

  public abstract long delayThresholdMillis();

  public static Builder newBuilder() {
    return new AutoValue_BundlingConfigView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder elementCountThreshold(int val);

    public abstract Builder requestByteThreshold(long val);

    public abstract Builder elementCountLimit(int val);

    public abstract Builder requestByteLimit(long val);

    public abstract Builder delayThresholdMillis(long val);

    public abstract BundlingConfigView build();
  }
}
