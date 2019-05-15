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

import com.google.api.codegen.SampleValueSet;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.auto.value.AutoValue;

/** SampleConfig represents configurations of a sample. */
@AutoValue
public abstract class SampleConfig {

  public abstract String regionTag();

  public abstract CallingForm callingForm();

  public abstract SampleValueSet valueSet();

  public abstract SampleSpec.SampleType type();

  public static SampleConfig create(
      String regionTag,
      CallingForm callingForm,
      SampleValueSet valueSet,
      SampleSpec.SampleType type) {
    return newBuilder()
        .regionTag(regionTag)
        .callingForm(callingForm)
        .valueSet(valueSet)
        .type(type)
        .build();
  }

  public static Builder newBuilder() {
    return new AutoValue_SampleConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder regionTag(String val);

    public abstract Builder callingForm(CallingForm val);

    public abstract Builder valueSet(SampleValueSet val);

    public abstract Builder type(SampleSpec.SampleType val);

    public abstract SampleConfig build();
  }
}
