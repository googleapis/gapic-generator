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

import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.util.Name;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class StaticLangApiView {
  protected abstract InterfaceContext context();

  private InterfaceConfig interfaceConfig() {
    return context().getInterfaceConfig();
  }

  @Nullable
  public abstract ServiceDocView doc();

  @Nullable
  public abstract String releaseLevelAnnotation();

  public Name name() {
    return Name.anyCamel(interfaceConfig().getName(), "Client");
  }

  // include in subclass: CSharpApiView
  public Name implName() {
    return Name.upperCamel(interfaceConfig().getName(), "ClientImpl");
  }

  @Nullable // Used in C#
  public abstract String grpcServiceName();

  @Nullable // Used in C#
  public abstract String grpcTypeNameOuter();

  @Nullable // Used in C#
  public abstract String grpcTypeNameInner();

  @Nullable // Used in C#
  public abstract List<ReroutedGrpcView> reroutedGrpcClients();

  public abstract String settingsClassName();

  public abstract List<ApiCallableView> apiCallableMembers();

  public abstract List<PathTemplateView> pathTemplates();

  public abstract List<FormatResourceFunctionView> formatResourceFunctions();

  public abstract List<ParseResourceFunctionView> parseResourceFunctions();

  public abstract List<StaticLangApiMethodView> apiMethods();

  @Nullable // Used in C#
  public abstract List<StaticLangApiMethodView> apiMethodsImpl();

  @Nullable // Used in C#
  public abstract List<ModifyMethodView> modifyMethods();

  public abstract boolean hasDefaultInstance();

  public abstract boolean hasLongRunningOperations();

  // Used in C#
  public abstract boolean apiHasUnaryMethod();

  // Used in C#
  public abstract boolean apiHasServerStreamingMethod();

  // Used in C#
  public abstract boolean apiHasClientStreamingMethod();

  // Used in C#
  public abstract boolean apiHasBidiStreamingMethod();

  @Nullable // Used in Java
  public abstract String stubInterfaceName();

  @Nullable // Used in Java
  public abstract String stubSettingsClassName();

  @Nullable // Used in Java
  public abstract List<StaticLangPagedResponseView> pagedResponseViews();

  public static Builder newBuilder() {
    return new AutoValue_StaticLangApiView.Builder()
        .apiHasUnaryMethod(false)
        .apiHasServerStreamingMethod(false)
        .apiHasClientStreamingMethod(false)
        .apiHasBidiStreamingMethod(false);
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder context(InterfaceContext context);

    public abstract Builder doc(ServiceDocView val);

    public abstract Builder releaseLevelAnnotation(String val);

    // public abstract Builder name(String val);

    // public abstract Builder implName(String val);

    public abstract Builder grpcServiceName(String val);

    public abstract Builder grpcTypeNameInner(String val);

    public abstract Builder grpcTypeNameOuter(String val);

    public abstract Builder reroutedGrpcClients(List<ReroutedGrpcView> val);

    public abstract Builder settingsClassName(String val);

    public abstract Builder apiCallableMembers(List<ApiCallableView> val);

    public abstract Builder pathTemplates(List<PathTemplateView> val);

    public abstract Builder formatResourceFunctions(List<FormatResourceFunctionView> val);

    public abstract Builder parseResourceFunctions(List<ParseResourceFunctionView> val);

    public abstract Builder apiMethods(List<StaticLangApiMethodView> val);

    public abstract Builder apiMethodsImpl(List<StaticLangApiMethodView> val);

    public abstract Builder modifyMethods(List<ModifyMethodView> val);

    public abstract Builder hasDefaultInstance(boolean val);

    public abstract Builder hasLongRunningOperations(boolean val);

    public abstract Builder apiHasUnaryMethod(boolean val);

    public abstract Builder apiHasServerStreamingMethod(boolean val);

    public abstract Builder apiHasClientStreamingMethod(boolean val);

    public abstract Builder apiHasBidiStreamingMethod(boolean val);

    public abstract Builder stubInterfaceName(String apiStubInterfaceName);

    public abstract Builder stubSettingsClassName(String apiSettingsStubInterfaceName);

    public abstract Builder pagedResponseViews(List<StaticLangPagedResponseView> val);

    public abstract StaticLangApiView build();
  }
}
