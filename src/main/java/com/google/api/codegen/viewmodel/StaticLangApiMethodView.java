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
import java.util.List;
import javax.annotation.Nullable;

/**
 * View of a single api method. This is a union class that is capable of holding the data for any
 * type of static api method; the type is maintained as a value of the ClientMethodType enum.
 */
@AutoValue
public abstract class StaticLangApiMethodView
    implements ApiMethodView, Comparable<StaticLangApiMethodView> {
  public abstract ClientMethodType type();

  public abstract String apiClassName();

  public abstract String apiVariableName();

  public abstract InitCodeView initCode();

  public abstract ApiMethodDocView doc();

  public abstract String serviceRequestTypeName();

  public abstract String serviceRequestTypeConstructor();

  public abstract String serviceResponseTypeName();

  public abstract String responseTypeName();

  public abstract String visibility();

  public abstract String name();

  public abstract String exampleName();

  public abstract String callableName();

  public abstract String settingsGetterName();

  public abstract String modifyMethodName();

  public abstract List<RequestObjectParamView> methodParams();

  @Nullable // Used in C#
  public abstract List<RequestObjectParamView> forwardingMethodParams();

  public abstract List<PathTemplateCheckView> pathTemplateChecks();

  public abstract boolean hasReturnValue();

  public abstract List<RequestObjectParamView> requestObjectParams();

  public abstract String stubName();

  public abstract GrpcStreamingType grpcStreamingType();

  public boolean isStreaming() {
    return grpcStreamingType() != GrpcStreamingType.NonStreaming;
  }

  @Nullable
  public abstract ListMethodDetailView listMethod();

  @Nullable
  public abstract UnpagedListCallableMethodDetailView unpagedListCallableMethod();

  @Nullable
  public abstract CallableMethodDetailView callableMethod();

  @Nullable
  public abstract RequestObjectMethodDetailView requestObjectMethod();

  @Nullable
  public abstract LongRunningOperationDetailView operationMethod();

  public abstract String releaseLevelAnnotation();

  public abstract List<HeaderRequestParamView> headerRequestParams();

  public abstract String serviceConstructorName();

  public static Builder newBuilder() {
    return new AutoValue_StaticLangApiMethodView.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder type(ClientMethodType type);

    public abstract Builder apiClassName(String apiClassName);

    public abstract Builder apiVariableName(String apiVariableName);

    public abstract Builder initCode(InitCodeView initCode);

    public abstract Builder doc(ApiMethodDocView doc);

    public abstract Builder serviceRequestTypeName(String requestTypeName);

    public abstract Builder serviceRequestTypeConstructor(String requestTypeConstructor);

    public abstract Builder serviceResponseTypeName(String val);

    public abstract Builder responseTypeName(String responseTypeName);

    public abstract Builder visibility(String visibility);

    public abstract Builder name(String name);

    public abstract Builder exampleName(String name);

    public abstract Builder callableName(String name);

    public abstract Builder settingsGetterName(String name);

    public abstract Builder modifyMethodName(String name);

    public abstract Builder methodParams(List<RequestObjectParamView> methodParams);

    public abstract Builder forwardingMethodParams(List<RequestObjectParamView> methodParams);

    public abstract Builder pathTemplateChecks(List<PathTemplateCheckView> pathTemplateChecks);

    public abstract Builder hasReturnValue(boolean hasReturnValue);

    public abstract Builder requestObjectParams(List<RequestObjectParamView> requestObjectParams);

    public abstract Builder listMethod(ListMethodDetailView details);

    public abstract Builder unpagedListCallableMethod(UnpagedListCallableMethodDetailView details);

    public abstract Builder callableMethod(CallableMethodDetailView details);

    public abstract Builder requestObjectMethod(RequestObjectMethodDetailView details);

    public abstract Builder operationMethod(LongRunningOperationDetailView details);

    public abstract Builder stubName(String stubName);

    public abstract Builder grpcStreamingType(GrpcStreamingType val);

    public abstract Builder releaseLevelAnnotation(String value);

    public abstract Builder headerRequestParams(List<HeaderRequestParamView> val);

    public abstract Builder serviceConstructorName(String val);

    public abstract StaticLangApiMethodView build();
  }

  @Override
  public int compareTo(StaticLangApiMethodView o) {
    return this.name().compareTo(o.name());
  }
}
