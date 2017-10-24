/* Copyright 2016 Google Inc
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
public abstract class OptionalArrayMethodView implements ApiMethodView {

  public abstract ClientMethodType type();

  public abstract String apiClassName();

  public abstract String fullyQualifiedApiClassName();

  public abstract String apiVariableName();

  public abstract String apiModuleName();

  public abstract InitCodeView initCode();

  public abstract ApiMethodDocView doc();

  public abstract String name();

  public abstract String requestVariableName();

  public abstract String requestTypeName();

  public abstract String key();

  public abstract String grpcMethodName();

  public abstract GrpcStreamingType grpcStreamingType();

  public boolean isGrpcStreamingMethod() {
    return grpcStreamingType() == GrpcStreamingType.BidiStreaming
        || grpcStreamingType() == GrpcStreamingType.ClientStreaming
        || grpcStreamingType() == GrpcStreamingType.ServerStreaming;
  }

  public abstract List<DynamicLangDefaultableParamView> methodParams();

  public abstract List<RequestObjectParamView> requiredRequestObjectParams();

  public abstract List<RequestObjectParamView> optionalRequestObjectParams();

  public abstract List<RequestObjectParamView> optionalRequestObjectParamsNoPageToken();

  public abstract boolean hasRequestParameters();

  public abstract boolean hasRequiredParameters();

  public abstract boolean hasReturnValue();

  public abstract String stubName();

  @Nullable
  public abstract LongRunningOperationDetailView longRunningView();

  @Nullable
  public abstract PageStreamingDescriptorView pageStreamingView();

  public boolean isLongRunningOperation() {
    return longRunningView() != null;
  }

  public abstract boolean isSingularRequestMethod();

  public abstract String packageName();

  public abstract String localPackageName();

  public abstract boolean packageHasMultipleServices();

  /** The name of the service exported by the package. */
  public abstract String packageServiceName();

  @Nullable
  public abstract String apiVersion();

  public abstract String topLevelAliasedApiClassName();

  public abstract String versionAliasedApiClassName();

  public boolean hasApiVersion() {
    return apiVersion() != null;
  }

  public abstract Iterable<Iterable<String>> oneofParams();

  public static Builder newBuilder() {
    return new AutoValue_OptionalArrayMethodView.Builder();
  }

  public abstract Builder toBuilder();

  public boolean hasRequestStreaming() {
    return !isSingularRequestMethod();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder type(ClientMethodType val);

    public abstract Builder apiClassName(String val);

    public abstract Builder fullyQualifiedApiClassName(String val);

    public abstract Builder apiVariableName(String val);

    public abstract Builder apiModuleName(String val);

    public abstract Builder initCode(InitCodeView val);

    public abstract Builder doc(ApiMethodDocView val);

    public abstract Builder name(String val);

    public abstract Builder requestVariableName(String val);

    public abstract Builder requestTypeName(String val);

    public abstract Builder key(String val);

    public abstract Builder grpcMethodName(String val);

    public abstract Builder grpcStreamingType(GrpcStreamingType val);

    public abstract Builder methodParams(List<DynamicLangDefaultableParamView> val);

    public abstract Builder requiredRequestObjectParams(List<RequestObjectParamView> val);

    public abstract Builder optionalRequestObjectParams(List<RequestObjectParamView> val);

    public abstract Builder optionalRequestObjectParamsNoPageToken(
        List<RequestObjectParamView> val);

    public abstract Builder hasRequestParameters(boolean val);

    public abstract Builder hasRequiredParameters(boolean val);

    public abstract Builder hasReturnValue(boolean val);

    public abstract Builder stubName(String val);

    public abstract Builder longRunningView(LongRunningOperationDetailView val);

    public abstract Builder pageStreamingView(PageStreamingDescriptorView val);

    public abstract Builder isSingularRequestMethod(boolean val);

    public abstract Builder packageName(String val);

    public abstract Builder packageHasMultipleServices(boolean val);

    /** The name of the service exported by the package. */
    public abstract Builder packageServiceName(String val);

    public abstract Builder apiVersion(String val);

    public abstract Builder topLevelAliasedApiClassName(String val);

    public abstract Builder versionAliasedApiClassName(String val);

    public abstract Builder oneofParams(Iterable<Iterable<String>> val);

    public abstract Builder localPackageName(String val);

    public abstract OptionalArrayMethodView build();
  }
}
