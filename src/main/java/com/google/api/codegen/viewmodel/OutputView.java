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
import javax.annotation.Nullable;

public interface OutputView {

  public enum Kind {
    COMMENT,
    DEFINE,
    ARRAY_LOOP,
    MAP_LOOP,
    PRINT,
    WRITE_FILE
  }

  Kind kind();

  @AutoValue
  abstract class DefineView implements OutputView {

    public abstract ImmutableList<String> modifiers();

    public abstract String variableTypeName();

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

      public abstract Builder modifiers(ImmutableList<String> val);

      public abstract Builder variableTypeName(String val);

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
  abstract class ArrayLoopView implements OutputView {
    public abstract String variableType();

    public abstract String variableName();

    public abstract VariableView collection();

    public abstract ImmutableList<OutputView> body();

    public Kind kind() {
      return Kind.ARRAY_LOOP;
    }

    public static Builder newBuilder() {
      return new AutoValue_OutputView_ArrayLoopView.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder variableType(String val);

      public abstract Builder variableName(String val);

      public abstract Builder collection(VariableView val);

      public abstract Builder body(ImmutableList<OutputView> val);

      public abstract ArrayLoopView build();
    }
  }

  @AutoValue
  abstract class MapLoopView implements OutputView {
    public abstract String keyType();

    @Nullable
    public abstract String keyVariableName();

    public abstract String valueType();

    @Nullable
    public abstract String valueVariableName();

    public abstract VariableView map();

    public abstract ImmutableList<OutputView> body();

    public Kind kind() {
      return Kind.MAP_LOOP;
    }

    public static Builder newBuilder() {
      return new AutoValue_OutputView_MapLoopView.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder keyType(String val);

      public abstract Builder keyVariableName(String val);

      public abstract Builder valueType(String val);

      public abstract Builder valueVariableName(String val);

      public abstract Builder map(VariableView val);

      public abstract Builder body(ImmutableList<OutputView> val);

      public abstract MapLoopView build();
    }
  }

  @AutoValue
  abstract class PrintView implements OutputView {

    // The first one is always the format
    // The later ones are args
    public ImmutableList<String> pieces();

    public Kind kind() {
      return Kind.PRINT;
    }

    public static Builder newBuilder() {
      return new AutoValue_OutputView_PrintView.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder pieces(ImmutableList<String> val);

      public abstract PrintView build();
    }
  }

  @AutoValue
  abstract class WriteFileView implements OutputView {
    public ImmutableList<String> fileNamePieces();

    public VariableView contents();

    public Kind kind() {
      return Kind.WRITE_FILE;
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder fileNamePieces(ImmutableList<String> val);

      public abstract Builder contents(VariableView val);

      public abstract WriteFileView build();
    }
  }

  @AutoValue
  abstract class VariableView {

    public abstract String variable();

    public abstract ImmutableList<String> accessors();

    public static Builder newBuilder() {
      return new AutoValue_OutputView_VariableView.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder variable(String val);

      public abstract Builder accessors(ImmutableList<String> val);

      public abstract VariableView build();
    }
  }
}
