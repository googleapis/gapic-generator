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

import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ModifyMethodView {
  public abstract String name();

  public abstract String requestTypeName();

  public abstract GrpcStreamingType grpcStreamingType();

  public static Builder builder() {
    return new AutoValue_ModifyMethodView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String val);

    public abstract Builder requestTypeName(String val);

    public abstract Builder grpcStreamingType(GrpcStreamingType val);

    public abstract ModifyMethodView build();
  }
}
