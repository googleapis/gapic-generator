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

@AutoValue
public abstract class LroClientFactoryView {
  public abstract String lroClientFactoryName();

  public abstract String serviceName();

  public abstract List<LroClientInstanceView> clientInstances();

  public static Builder newBuilder() {
    return new AutoValue_LroClientFactoryView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder serviceName(String val);

    public abstract Builder lroClientFactoryName(String val);

    public abstract Builder clientInstances(List<LroClientInstanceView> val);

    public abstract LroClientFactoryView build();
  }

  @AutoValue
  public abstract static class LroClientInstanceView {
    public abstract String deleteGlobalOperationCallable();

    public abstract String getGlobalOperationCallable();

    public abstract String operationDeleteRequestSetter();

    public abstract String deleteOperationRequestName();

    public abstract String operationGetRequestSetter();

    public abstract String getOperationRequestName();

    public Builder newBuilder() {
      return new AutoValue_LroClientFactoryView_LroClientInstanceView.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder deleteGlobalOperationCallable(String val);

      public abstract Builder getGlobalOperationCallable(String val);

      public abstract Builder operationDeleteRequestSetter(String val);

      public abstract Builder deleteOperationRequestName(String val);

      public abstract Builder operationGetRequestSetter(String val);

      public abstract Builder getOperationRequestName(String val);

      public abstract LroClientInstanceView build();
    }
  }
}
