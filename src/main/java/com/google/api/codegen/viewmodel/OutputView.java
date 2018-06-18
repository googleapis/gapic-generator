/* Copyright 2018 Google LLC
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
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class OutputView {

  public enum Kind {
    PRINT
  }

  public abstract Kind kind();

  public abstract PrintView print();

  public static Builder newBuilder() {
    return new AutoValue_OutputView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder kind(Kind val);

    public abstract Builder print(PrintView val);

    public abstract OutputView build();
  }

  @AutoValue
  public abstract static class PrintView {

    public abstract String printSpec();

    public abstract ImmutableList<PrintArgView> printArgs();

    public static Builder newBuilder() {
      return new AutoValue_OutputView_PrintView.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder printSpec(String val);

      public abstract Builder printArgs(ImmutableList<PrintArgView> val);

      public abstract PrintView build();
    }
  }

  @AutoValue
  public abstract static class PrintArgView {

    public abstract String variable();

    public abstract ImmutableList<String> accessors();

    public static Builder newBuilder() {
      return new AutoValue_OutputView_PrintArgView.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder variable(String val);

      public abstract Builder accessors(ImmutableList<String> val);

      public abstract PrintArgView build();
    }
  }
}
