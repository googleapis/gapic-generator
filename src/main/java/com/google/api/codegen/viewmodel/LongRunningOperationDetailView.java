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

@AutoValue
public abstract class LongRunningOperationDetailView {

  public abstract String constructorName();

  public abstract String clientReturnTypeName();

  public abstract String operationPayloadTypeName();

  public abstract boolean isEmptyOperation();

  public abstract String metadataTypeName();

  public abstract boolean implementsDelete();

  public abstract boolean implementsCancel();

  public abstract String methodName();

  public abstract long initialPollDelay();

  public abstract double pollDelayMultiplier();

  public abstract long maxPollDelay();

  public abstract long totalPollTimeout();

  public static Builder newBuilder() {
    return new AutoValue_LongRunningOperationDetailView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder constructorName(String val);

    public abstract Builder clientReturnTypeName(String val);

    public abstract Builder operationPayloadTypeName(String val);

    public abstract Builder isEmptyOperation(boolean val);

    public abstract Builder metadataTypeName(String val);

    public abstract Builder implementsDelete(boolean val);

    public abstract Builder implementsCancel(boolean val);

    public abstract Builder methodName(String val);

    public abstract Builder initialPollDelay(long initialDelay);

    public abstract Builder pollDelayMultiplier(double multiplier);

    public abstract Builder maxPollDelay(long maxDelay);

    public abstract Builder totalPollTimeout(long totalTimeout);

    public abstract LongRunningOperationDetailView build();
  }
}
