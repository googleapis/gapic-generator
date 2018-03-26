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
import java.util.List;

// This bundles the samples together with the corresponding API structure for use while transforming
@AutoValue
public abstract class ApiAndSamples<ApiViewT, MethodViewT> {

  public abstract List<SampleInfo<MethodViewT>> samples();

  public abstract ApiViewT api();

  public abstract Builder<ApiViewT, MethodViewT> toBuilder();

  public static <ApiViewT, MethodViewT> Builder<ApiViewT, MethodViewT> newBuilder() {
    return new AutoValue_ApiAndSamples.Builder<>();
  }

  @AutoValue.Builder
  public abstract static class Builder<ApiViewT, MethodViewT> {
    public abstract Builder<ApiViewT, MethodViewT> samples(List<SampleInfo<MethodViewT>> val);

    public abstract Builder<ApiViewT, MethodViewT> api(ApiViewT val);

    public abstract ApiAndSamples<ApiViewT, MethodViewT> build();
  }
}
