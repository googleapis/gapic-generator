/* Copyright 2017 Google LLC
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
import javax.annotation.Nullable;

@AutoValue
public abstract class BatchingDescriptorView {
  public abstract String methodName();

  public abstract String batchedFieldName();

  public abstract List<String> discriminatorFieldNames();

  public abstract String byteLengthFunctionName();

  @Nullable
  public abstract String subresponseFieldName();

  public static Builder newBuilder() {
    return new AutoValue_BatchingDescriptorView.Builder();
  }

  public boolean hasSubresponseField() {
    return subresponseFieldName() != null;
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder methodName(String val);

    public abstract Builder batchedFieldName(String val);

    public abstract Builder subresponseFieldName(String val);

    public abstract Builder discriminatorFieldNames(List<String> val);

    public abstract Builder byteLengthFunctionName(String val);

    public abstract BatchingDescriptorView build();
  }
}
