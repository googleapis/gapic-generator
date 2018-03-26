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
import javax.annotation.Nullable;

/** Information for a single sample for a given method, to be included in other view models. */
@AutoValue
public abstract class SampleInfo<MethodViewT> {

  /** The class name for the runnable sample */
  @Nullable
  public abstract String className();

  /** The client library method for which this is a sample */
  public abstract MethodViewT method();

  /** The calling form exemplified in this sample */
  public abstract String callingForm();

  /** The sample value set used in this sample */
  public abstract SampleValueSetView valueSet();

  public static <MethodViewT> Builder<MethodViewT> newBuilder() {
    return new AutoValue_SampleInfo.Builder<>();
  }

  public abstract Builder<MethodViewT> toBuilder();

  @AutoValue.Builder
  public abstract static class Builder<MethodViewT> {
    public abstract Builder<MethodViewT> className(String val);

    public abstract Builder<MethodViewT> method(MethodViewT val);

    public abstract Builder<MethodViewT> callingForm(String val);

    public abstract Builder<MethodViewT> valueSet(SampleValueSetView val);

    public abstract SampleInfo<MethodViewT> build();
  }
}
