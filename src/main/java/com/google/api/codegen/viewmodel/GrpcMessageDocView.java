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

@AutoValue
public abstract class GrpcMessageDocView implements GrpcElementDocView {
  public abstract String name();

  public abstract String fullName();

  public abstract List<String> lines();

  public abstract List<ParamDocView> properties();

  public abstract List<GrpcElementDocView> elementDocs();

  public abstract String fileUrl();

  public abstract String packageName();

  public static Builder newBuilder() {
    return new AutoValue_GrpcMessageDocView.Builder();
  }

  public String type() {
    return GrpcMessageDocView.class.getSimpleName();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String val);

    public abstract Builder fullName(String val);

    public abstract Builder lines(List<String> val);

    public abstract Builder properties(List<ParamDocView> val);

    public abstract Builder elementDocs(List<GrpcElementDocView> val);

    public abstract Builder fileUrl(String val);

    public abstract Builder packageName(String val);

    public abstract GrpcMessageDocView build();
  }
}
