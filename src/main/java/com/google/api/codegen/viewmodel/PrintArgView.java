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
public abstract class PrintArgView {

  public abstract ImmutableList<ArgSegmentView> segments();

  public static Builder newBuilder() {
    return new AutoValue_PrintArgView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder segments(ImmutableList<ArgSegmentView> val);

    public abstract PrintArgView build();
  }

  public interface ArgSegmentView {

    enum Kind {
      TEXT,
      VARIABLE
    }

    Kind kind();
  }

  @AutoValue
  public abstract static class TextSegmentView implements ArgSegmentView {

    @Override
    public Kind kind() {
      return Kind.TEXT;
    }

    public abstract String text();

    public static Builder newBuilder() {
      return new AutoValue_PrintArgView_TextSegmentView.Builder();
    }

    public static TextSegmentView of(String text) {
      return newBuilder().text(text).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder text(String val);

      public abstract TextSegmentView build();
    }
  }

  @AutoValue
  public abstract static class VariableSegmentView implements ArgSegmentView {

    @Override
    public Kind kind() {
      return Kind.VARIABLE;
    }

    public abstract OutputView.VariableView variable();

    public static Builder newBuilder() {
      return new AutoValue_PrintArgView_VariableSegmentView.Builder();
    }

    public static VariableSegmentView of(OutputView.VariableView variable) {
      return newBuilder().variable(variable).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder variable(OutputView.VariableView val);

      public abstract VariableSegmentView build();
    }
  }
}
