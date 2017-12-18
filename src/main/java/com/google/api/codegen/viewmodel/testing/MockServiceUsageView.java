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
package com.google.api.codegen.viewmodel.testing;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

@AutoValue
public abstract class MockServiceUsageView {
  public abstract String className();

  public abstract String varName();

  public abstract String implName();

  /** Used in Go. The function to register the GRPC server. */
  @Nullable
  public abstract String registerFunctionName();

  public static Builder newBuilder() {
    return new AutoValue_MockServiceUsageView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder className(String modifier);

    public abstract Builder varName(String name);

    public abstract Builder implName(String name);

    public abstract Builder registerFunctionName(String val);

    public abstract MockServiceUsageView build();
  }
}
