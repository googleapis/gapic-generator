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

import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class BatchingDescriptorClassView {

  public abstract String name();

  public abstract String requestTypeName();

  public abstract String responseTypeName();

  @Nullable
  public abstract String subresponseTypeName();

  public abstract List<BatchingPartitionKeyView> partitionKeys();

  public abstract String batchedFieldGetFunction();

  public abstract String batchedFieldSetFunction();

  public abstract String batchedFieldCountGetFunction();

  @Nullable
  public abstract String subresponseByIndexGetFunction();

  @Nullable
  public abstract String subresponseSetFunction();

  public boolean hasSubresponse() {
    return subresponseTypeName() != null;
  }

  public static Builder newBuilder() {
    return new AutoValue_BatchingDescriptorClassView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String val);

    public abstract Builder requestTypeName(String val);

    public abstract Builder responseTypeName(String val);

    public abstract Builder subresponseTypeName(String val);

    public abstract Builder partitionKeys(List<BatchingPartitionKeyView> val);

    public abstract Builder batchedFieldGetFunction(String val);

    public abstract Builder batchedFieldSetFunction(String val);

    public abstract Builder batchedFieldCountGetFunction(String val);

    public abstract Builder subresponseByIndexGetFunction(String val);

    public abstract Builder subresponseSetFunction(String val);

    public abstract BatchingDescriptorClassView build();
  }
}
