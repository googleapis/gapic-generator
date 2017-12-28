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

import com.google.api.codegen.viewmodel.RetryParamsDefinitionView.Builder;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class RetryCodesDefinitionView {
  public abstract String key();

  @Nullable // Used in C#
  public abstract String name();

  public abstract String retryFilterMethodName();

  public abstract ImmutableSet<String> codes();

  @Nullable // Used in C#
  public abstract List<String> codeNames();

  public static Builder newBuilder() {
    return new AutoValue_RetryCodesDefinitionView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder key(String val);

    public abstract Builder name(String val);

    public abstract Builder retryFilterMethodName(String val);

    public abstract Builder codes(ImmutableSet<String> val);

    public abstract Builder codeNames(List<String> val);

    public abstract RetryCodesDefinitionView build();
  }
}
