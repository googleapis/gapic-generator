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

public interface OutputView {

  public enum Kind {
    COMMENT,
    DEFINE,
    LOOP,
    PRINT
  }

  Kind kind();

  @AutoValue
  abstract class DefineView implements OutputView {
    public abstract String variableType(); // TODO: Replace with appropriate type type

    public abstract String variableName();

    public abstract VariableView reference();

    public Kind kind() {
      return Kind.DEFINE;
    }

    public static Builder newBuilder() {
      return new AutoValue_OutputView_DefineView.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder variableType(String val);

      public abstract Builder variableName(String val);

      public abstract Builder reference(VariableView val);

      public abstract DefineView build();
    }
  }

  @AutoValue
  abstract class CommentView implements OutputView {
    public abstract ImmutableList<String> lines();

    public Kind kind() {
      return Kind.COMMENT;
    }

    public static Builder newBuilder() {
      return new AutoValue_OutputView_CommentView.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder lines(ImmutableList<String> val);

      public abstract CommentView build();
    }
  }

  @AutoValue
  abstract class LoopView implements OutputView {
    public abstract String variableType(); // TODO: Replace with appropriate type type

    public abstract String variableName();

    public abstract VariableView collection();

    public abstract ImmutableList<OutputView> body();

    public Kind kind() {
      return Kind.LOOP;
    }

    public static Builder newBuilder() {
      return new AutoValue_OutputView_LoopView.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder variableType(String val);

      public abstract Builder variableName(String val);

      public abstract Builder collection(VariableView val);

      public abstract Builder body(ImmutableList<OutputView> val);

      public abstract LoopView build();
    }
  }

  @AutoValue
  abstract class PrintView implements OutputView {

    public abstract String format();

    public abstract ImmutableList<VariableView> args();

    public Kind kind() {
      return Kind.PRINT;
    }

    public static Builder newBuilder() {
      return new AutoValue_OutputView_PrintView.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder format(String val);

      public abstract Builder args(ImmutableList<VariableView> val);

      public abstract PrintView build();
    }
  }

  @AutoValue
  abstract class VariableView {

    public abstract String variable();

    public abstract ImmutableList<AccessorView> accessors();

    public static Builder newBuilder() {
      return new AutoValue_OutputView_VariableView.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder variable(String val);

      public abstract Builder accessors(ImmutableList<AccessorView> val);

      public abstract VariableView build();
    }
  }
}
