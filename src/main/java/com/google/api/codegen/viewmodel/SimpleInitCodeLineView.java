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

import com.google.api.codegen.metacode.InitCodeLineType;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class SimpleInitCodeLineView implements InitCodeLineView {

  @Override
  public abstract InitCodeLineType lineType();

  public abstract String typeName();

  @Override
  public abstract String identifier();

  @Nullable
  /** Used only in standalone samples. Generated as comments to describe the variable. */
  public abstract List<String> descriptions();

  public abstract List<String> doc();

  /** @return if a newline should be added above this line */
  public abstract boolean needsLeadingNewline();

  public abstract InitValueView initValue();

  public static Builder newBuilder() {
    return new AutoValue_SimpleInitCodeLineView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder lineType(InitCodeLineType val);

    public abstract Builder typeName(String val);

    public abstract Builder identifier(String val);

    public abstract Builder initValue(InitValueView val);

    public abstract Builder descriptions(List<String> val);

    public abstract Builder doc(List<String> val);

    public abstract Builder needsLeadingNewline(boolean val);

    public abstract SimpleInitCodeLineView build();
  }
}
