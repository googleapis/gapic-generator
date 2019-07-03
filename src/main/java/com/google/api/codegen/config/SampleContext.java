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
package com.google.api.codegen.config;

import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/** The context of transforming a sample into a view model. */
@AutoValue
public abstract class SampleContext {

  public abstract ImmutableList<CallingForm> availableCallingForms();

  public abstract SampleSpec.SampleType sampleType();

  public abstract SampleConfig sampleConfig();

  public Builder newBuilder() {
    return new AutoValue_SampleContext.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder availableCallingForms(ImmutableList<CallingForm> val);

    public abstract Builder sampleType(SampleSpec.SampleType val);

    public abstract Builder SampleConfig(SampleConfig val);

    public abstract SampleContext build();
  }

  public SampleContext create(
      MethodContext methodContext, SampleSpec.SampleType type, TargetLanguage language) {
    return newBuilder()
        .availableCallingForms(CallingForm.getCallingForms(methodContext, language))
        .sampleType(type)
        .build();
  }
}
