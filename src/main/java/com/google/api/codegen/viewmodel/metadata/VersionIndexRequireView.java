/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.viewmodel.metadata;

import com.google.api.codegen.viewmodel.ServiceDocView;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

@AutoValue
public abstract class VersionIndexRequireView {

  public abstract String clientName();

  @Nullable
  public abstract String serviceName();

  @Nullable
  public abstract String localName();

  @Nullable
  public abstract ServiceDocView doc();

  public abstract String fileName();

  @Nullable
  public abstract String topLevelNamespace();

  public static Builder newBuilder() {
    return new AutoValue_VersionIndexRequireView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder clientName(String val);

    public abstract Builder serviceName(String val);

    public abstract Builder localName(String val);

    public abstract Builder doc(ServiceDocView val);

    public abstract Builder fileName(String val);

    public abstract Builder topLevelNamespace(String val);

    public abstract VersionIndexRequireView build();
  }
}
