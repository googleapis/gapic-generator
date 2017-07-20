/* Copyright 2017 Google Inc
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
package com.google.api.codegen.viewmodel;

import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.viewmodel.ApiCallableView.Builder;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class DirectCallableView {

  public abstract String requestTypeName();

  public abstract String responseTypeName();

  public abstract GrpcStreamingType grpcStreamingType();

  public abstract String name();

  public abstract String protoMethodName();

  public abstract String fullServiceName();

  public abstract String interfaceTypeName();

  public abstract String createCallableFunctionName();

  public static Builder newBuilder() {
    return new AutoValue_DirectCallableView.Builder()
        .grpcStreamingType(GrpcStreamingType.NonStreaming);
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder requestTypeName(String name);

    public abstract Builder responseTypeName(String name);

    public abstract Builder grpcStreamingType(GrpcStreamingType val);

    public abstract Builder name(String directCallableName);

    public abstract Builder protoMethodName(String val);

    public abstract Builder fullServiceName(String val);

    public abstract Builder interfaceTypeName(String val);

    public abstract Builder createCallableFunctionName(String val);

    public abstract DirectCallableView build();
  }
}
