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
package com.google.api.codegen.viewmodel.testing;

import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.ServiceMethodType;
import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
public abstract class ClientTestCaseView {
  public abstract String name();

  public abstract String nameWithException();

  public abstract String surfaceMethodName();

  public abstract boolean hasReturnValue();

  public abstract String requestTypeName();

  public abstract String responseTypeName();

  public abstract List<PageStreamingResponseView> pageStreamingResponseViews();

  public abstract MockGrpcResponseView mockResponse();

  public abstract ClientMethodType clientMethodType();

  public abstract InitCodeView initCode();

  public abstract List<ClientTestAssertView> asserts();

  public abstract String mockServiceVarName();

  public abstract String serviceConstructorName();

  public abstract GrpcStreamingType grpcStreamingType();

  public abstract ServiceMethodType serviceMethodType();

  public static Builder newBuilder() {
    return new AutoValue_ClientTestCaseView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder surfaceMethodName(String val);

    public abstract Builder name(String val);

    public abstract Builder nameWithException(String val);

    public abstract Builder hasReturnValue(boolean val);

    public abstract Builder requestTypeName(String val);

    public abstract Builder responseTypeName(String val);

    public abstract Builder pageStreamingResponseViews(List<PageStreamingResponseView> val);

    public abstract Builder clientMethodType(ClientMethodType val);

    public abstract Builder initCode(InitCodeView val);

    public abstract Builder asserts(List<ClientTestAssertView> val);

    public abstract Builder mockResponse(MockGrpcResponseView val);

    public abstract Builder mockServiceVarName(String val);

    public abstract Builder serviceConstructorName(String val);

    public abstract Builder grpcStreamingType(GrpcStreamingType val);

    public abstract Builder serviceMethodType(ServiceMethodType val);

    public abstract ClientTestCaseView build();
  }
}
