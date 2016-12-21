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

import com.google.api.codegen.SnippetSetRunner;
import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
public abstract class DynamicLangXApiView implements ViewModel {
  public abstract String templateFileName();

  public abstract String packageName();

  public abstract String protoFilename();

  public abstract ServiceDocView doc();

  public abstract String name();

  public abstract String serviceAddress();

  public abstract Integer servicePort();

  public abstract String serviceTitle();

  public abstract Iterable<String> authScopes();

  public abstract List<PathTemplateView> pathTemplates();

  public abstract List<FormatResourceFunctionView> formatResourceFunctions();

  public abstract List<ParseResourceFunctionView> parseResourceFunctions();

  public abstract List<PathTemplateGetterFunctionView> pathTemplateGetterFunctions();

  public abstract List<PageStreamingDescriptorView> pageStreamingDescriptors();

  public abstract List<LongRunningOperationDetailView> longRunningDescriptors();

  public abstract List<String> methodKeys();

  public abstract String clientConfigPath();

  public abstract String interfaceKey();

  public abstract String grpcClientTypeName();

  public abstract List<GrpcStubView> stubs();

  public abstract List<ImportTypeView> imports();

  public abstract String outputPath();

  public abstract List<ApiMethodView> apiMethods();

  public abstract boolean hasLongRunningOperations();

  public abstract boolean hasDefaultServiceAddress();

  public abstract boolean hasDefaultServiceScopes();

  public boolean missingDefaultServiceAddress() {
    return !hasDefaultServiceAddress();
  }

  public boolean missingDefaultServiceScopes() {
    return !hasDefaultServiceScopes();
  }

  public boolean hasMissingDefaultOptions() {
    return missingDefaultServiceAddress() || missingDefaultServiceScopes();
  }

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  public static Builder newBuilder() {
    return new AutoValue_DynamicLangXApiView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder templateFileName(String val);

    public abstract Builder packageName(String val);

    public abstract Builder protoFilename(String simpleName);

    public abstract Builder doc(ServiceDocView doc);

    public abstract Builder name(String val);

    public abstract Builder serviceAddress(String val);

    public abstract Builder servicePort(Integer val);

    public abstract Builder serviceTitle(String val);

    public abstract Builder authScopes(Iterable<String> val);

    public abstract Builder pathTemplates(List<PathTemplateView> val);

    public abstract Builder formatResourceFunctions(List<FormatResourceFunctionView> val);

    public abstract Builder parseResourceFunctions(List<ParseResourceFunctionView> val);

    public abstract Builder pathTemplateGetterFunctions(List<PathTemplateGetterFunctionView> val);

    public abstract Builder pageStreamingDescriptors(List<PageStreamingDescriptorView> val);

    public abstract Builder longRunningDescriptors(List<LongRunningOperationDetailView> val);

    public abstract Builder methodKeys(List<String> val);

    public abstract Builder clientConfigPath(String val);

    public abstract Builder interfaceKey(String val);

    public abstract Builder grpcClientTypeName(String val);

    public abstract Builder stubs(List<GrpcStubView> val);

    public abstract Builder imports(List<ImportTypeView> val);

    public abstract Builder outputPath(String val);

    public abstract Builder apiMethods(List<ApiMethodView> val);

    public abstract Builder hasLongRunningOperations(boolean val);

    public abstract Builder hasDefaultServiceAddress(boolean val);

    public abstract Builder hasDefaultServiceScopes(boolean val);

    public abstract DynamicLangXApiView build();
  }
}
