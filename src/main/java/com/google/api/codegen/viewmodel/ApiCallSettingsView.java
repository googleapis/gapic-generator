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
import javax.annotation.Nullable;

@AutoValue
public abstract class ApiCallSettingsView {
  public abstract ApiCallableImplType type();

  public abstract String methodName();

  public abstract String asyncMethodName();

  public abstract String requestTypeName();

  public abstract String responseTypeName();

  public abstract String resourceTypeName();

  public abstract String pagedListResponseTypeName();

  public abstract GrpcStreamingType grpcStreamingType();

  public boolean isStreaming() {
    return grpcStreamingType() != GrpcStreamingType.NonStreaming;
  }

  public abstract String memberName();

  public abstract String settingsGetFunction();

  public abstract String grpcTypeName();

  public abstract String grpcMethodConstant();

  public abstract String pageStreamingDescriptorName();

  public abstract String pagedListResponseFactoryName();

  public abstract String batchingDescriptorName();

  public abstract String retryCodesName();

  public abstract String retryParamsName();

  @Nullable
  public abstract RetryCodesDefinitionView retryCodesView();

  @Nullable
  public abstract RetryParamsDefinitionView retryParamsView();

  @Nullable
  public abstract BatchingConfigView batchingConfig();

  @Nullable
  public abstract LongRunningOperationDetailView operationMethod();

  public abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_ApiCallSettingsView.Builder()
        .grpcStreamingType(GrpcStreamingType.NonStreaming);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder type(ApiCallableImplType type);

    public abstract Builder methodName(String apiMethodName);

    public abstract Builder asyncMethodName(String apiAsyncMethodName);

    public abstract Builder requestTypeName(String val);

    public abstract Builder responseTypeName(String val);

    public abstract Builder resourceTypeName(String val);

    public abstract Builder pagedListResponseTypeName(String val);

    public abstract Builder grpcStreamingType(GrpcStreamingType val);

    public abstract Builder memberName(String val);

    public abstract Builder settingsGetFunction(String val);

    public abstract Builder grpcTypeName(String val);

    public abstract Builder grpcMethodConstant(String val);

    public abstract Builder pageStreamingDescriptorName(String val);

    public abstract Builder pagedListResponseFactoryName(String val);

    public abstract Builder batchingDescriptorName(String val);

    public abstract Builder batchingConfig(BatchingConfigView val);

    public abstract Builder retryCodesName(String val);

    public abstract Builder retryParamsName(String val);

    public abstract Builder retryCodesView(RetryCodesDefinitionView val);

    public abstract Builder retryParamsView(RetryParamsDefinitionView val);

    public abstract Builder operationMethod(LongRunningOperationDetailView val);

    public abstract ApiCallSettingsView build();
  }
}
