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

import com.google.api.codegen.SampleValueSet;
import com.google.auto.value.AutoValue;
import java.util.List;

/** Contains the view model for a single set of sample values for a single method. */
@AutoValue
public abstract class SampleValueSetView {
  /** The id as specified in the config */
  public abstract String id();

  /** The title as specified in the config */
  public abstract String title();

  /** The description as specified in the config */
  public abstract String description();

  /** The parameters as specified in the config */
  public abstract List<String> parameters();

  public static SampleValueSetView of(SampleValueSet config) {
    SampleValueSetView.Builder builder = new AutoValue_SampleValueSetView.Builder();
    return builder
        .id(config.getId())
        .title(config.getTitle())
        .description(config.getDescription())
        .parameters(config.getParametersList())
        .build();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder id(String val);

    public abstract Builder title(String val);

    public abstract Builder description(String val);

    public abstract Builder parameters(List<String> params);

    public abstract SampleValueSetView build();
  }
}
