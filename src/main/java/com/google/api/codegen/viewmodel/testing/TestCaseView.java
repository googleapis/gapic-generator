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
package com.google.api.codegen.viewmodel.testing;

import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class TestCaseView {

  public abstract String clientMethodName();

  public abstract InitCodeView initCode();

  @Nullable // Use in C#
  public abstract InitCodeView requestObjectInitCode();

  public abstract ClientMethodType clientMethodType();

  public abstract MockRpcResponseView mockResponse();

  public abstract List<ClientTestAssertView> asserts();

  public abstract String requestTypeName();

  public abstract String responseTypeName();

  public abstract String callerResponseTypeName();

  public abstract String fullyQualifiedRequestTypeName();

  public abstract String fullyQualifiedResponseTypeName();

  public abstract List<PageStreamingResponseView> pageStreamingResponseViews();

  @Nullable
  public abstract GrpcStreamingView grpcStreamingView();

  public abstract String name();

  public abstract String nameWithException();

  public abstract String serviceConstructorName();

  public abstract String fullyQualifiedServiceClassName();

  /**
   * In Ruby, the initializer of the service class is aliased. This name is the aliased method used
   * to initialize a service class.
   */
  public abstract String fullyQualifiedAliasedServiceClassName();

  public abstract String mockServiceVarName();

  public abstract boolean hasRequestParameters();

  public abstract boolean hasReturnValue();

  public abstract GrpcStreamingType grpcStreamingType();

  public abstract String mockGrpcStubTypeName();

  public abstract String createStubFunctionName();

  public abstract String grpcStubCallString();

  public abstract boolean clientHasDefaultInstance();

  public abstract String grpcMethodName();

  public static Builder newBuilder() {
    return new AutoValue_TestCaseView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder clientMethodName(String val);

    public abstract Builder name(String val);

    public abstract Builder nameWithException(String val);

    public abstract Builder serviceConstructorName(String val);

    public abstract Builder fullyQualifiedServiceClassName(String val);

    public abstract Builder fullyQualifiedAliasedServiceClassName(String val);

    public abstract Builder mockServiceVarName(String val);

    public abstract Builder initCode(InitCodeView val);

    public abstract Builder requestObjectInitCode(InitCodeView val);

    public abstract Builder clientMethodType(ClientMethodType val);

    public abstract Builder mockResponse(MockRpcResponseView val);

    public abstract Builder asserts(List<ClientTestAssertView> val);

    public abstract Builder requestTypeName(String val);

    public abstract Builder responseTypeName(String val);

    public abstract Builder callerResponseTypeName(String val);

    public abstract Builder fullyQualifiedRequestTypeName(String val);

    public abstract Builder fullyQualifiedResponseTypeName(String val);

    public abstract Builder pageStreamingResponseViews(List<PageStreamingResponseView> val);

    public abstract Builder grpcStreamingView(GrpcStreamingView val);

    public abstract Builder hasRequestParameters(boolean val);

    public abstract Builder hasReturnValue(boolean val);

    public abstract Builder grpcStreamingType(GrpcStreamingType val);

    public abstract Builder mockGrpcStubTypeName(String val);

    public abstract Builder createStubFunctionName(String val);

    public abstract Builder grpcStubCallString(String val);

    public abstract Builder clientHasDefaultInstance(boolean val);

    public abstract Builder grpcMethodName(String val);

    public abstract TestCaseView build();
  }
}
