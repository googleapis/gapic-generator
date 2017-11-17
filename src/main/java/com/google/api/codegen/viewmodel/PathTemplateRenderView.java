/* Copyright 2017 Google LLC
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
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class PathTemplateRenderView {
  public enum PieceKind {
    LITERAL,
    VARIABLE
  }

  @AutoValue
  public abstract static class Piece {
    public abstract String value();

    public abstract PieceKind kind();

    public static Builder builder() {
      return new AutoValue_PathTemplateRenderView_Piece.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder value(String val);

      public abstract Builder kind(PieceKind val);

      public abstract Piece build();
    }
  }

  public abstract ImmutableList<Piece> pieces();

  public static Builder builder() {
    return new AutoValue_PathTemplateRenderView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder pieces(ImmutableList<Piece> val);

    public abstract PathTemplateRenderView build();
  }
}
