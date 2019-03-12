/* Copyright 2019 Google LLC
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
import java.util.List;

@AutoValue
public abstract class SampleFunctionDocView {

  public abstract ImmutableList<List<String>> paramDocLines();

  /** Description of the sample function. */
  public abstract ImmutableList<String> mainDocLines();

  public static Builder newBuilder() {
    return new AutoValue_SampleFunctionDocView.Builder();
  }

  public boolean isEmpty() {
    return mainDocLines().isEmpty() && paramDocLines().isEmpty();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder paramDocLines(ImmutableList<List<String>> val);

    public abstract Builder mainDocLines(ImmutableList<String> val);

    public abstract SampleFunctionDocView build();
  }
}
