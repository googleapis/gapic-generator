/* Copyright 2017 Google LLC
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
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class MethodDescriptorView {

  public abstract String requestTypeName();

  public abstract String responseTypeName();

  public abstract boolean hasResponse();

  public abstract GrpcStreamingType grpcStreamingType();

  public abstract String name();

  public abstract String protoMethodName();

  public abstract String fullServiceName();

  public abstract String transportSettingsVar();

  public abstract List<HeaderRequestParamView> headerRequestParams();

  public boolean hasHeaderRequestParams() {
    return headerRequestParams() != null && !headerRequestParams().isEmpty();
  }

  @Nullable
  public abstract HttpMethodView httpMethod();

  public static Builder newBuilder() {
    return new AutoValue_MethodDescriptorView.Builder()
        .grpcStreamingType(GrpcStreamingType.NonStreaming);
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder requestTypeName(String name);

    public abstract Builder responseTypeName(String name);

    public abstract Builder hasResponse(boolean val);

    public abstract Builder grpcStreamingType(GrpcStreamingType val);

    public abstract Builder name(String directCallableName);

    public abstract Builder protoMethodName(String val);

    public abstract Builder fullServiceName(String val);

    public abstract Builder transportSettingsVar(String val);

    public abstract Builder headerRequestParams(List<HeaderRequestParamView> val);

    public abstract Builder httpMethod(HttpMethodView val);

    public abstract MethodDescriptorView build();
  }
}
