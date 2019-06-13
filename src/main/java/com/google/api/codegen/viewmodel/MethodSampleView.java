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

/** A view model for methods samples. */
@AutoValue
public abstract class MethodSampleView {

  /** The value set used in this sample. */
  public abstract SampleValueSetView valueSet();

  /** The calling form used in this sample. */
  public abstract CallingForm callingForm();

  /** The initialization code constructed from this samples value set and calling form. */
  public abstract InitCodeView sampleInitCode();

  /** The response printing code. */
  public abstract ImmutableList<OutputView> outputs();

  /** The region tag to be used for this sample. */
  public abstract String regionTag();

  /** The name of the sample function. */
  public abstract String sampleFunctionName();

  /** The documentation of the sample function. */
  public abstract SampleFunctionDocView sampleFunctionDoc();

  public abstract ImportSectionView sampleImports();

  /** Whether the output has multiple write-to-file statements. */
  public abstract boolean hasMultipleFileOutputs();

  /** Used by Node.js and C#. */
  public abstract boolean usesAsyncAwaitPattern();

  public abstract String title();

  public abstract String descriptionLine();

  public abstract ImmutableList<String> additionalDescriptionLines();

  public static Builder newBuilder() {
    return new AutoValue_MethodSampleView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder valueSet(SampleValueSetView val);

    public abstract Builder callingForm(CallingForm val);

    public abstract Builder sampleInitCode(InitCodeView val);

    public abstract Builder outputs(ImmutableList<OutputView> val);

    public abstract Builder regionTag(String val);

    public abstract Builder sampleFunctionName(String val);

    public abstract Builder sampleImports(ImportSectionView val);

    public abstract Builder sampleFunctionDoc(SampleFunctionDocView val);

    public abstract Builder hasMultipleFileOutputs(boolean hasMultipleFileOutputs);

    public abstract Builder usesAsyncAwaitPattern(boolean val);

    public abstract Builder title(String val);

    public abstract Builder descriptionLine(String val);

    public abstract Builder additionalDescriptionLines(ImmutableList<String> val);

    public abstract MethodSampleView build();
  }
}
