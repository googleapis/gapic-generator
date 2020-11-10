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

import com.google.api.codegen.SnippetSetRunner;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class DynamicLangXApiView implements ViewModel {
  public abstract String templateFileName();

  public abstract FileHeaderView fileHeader();

  public abstract String protoFilename();

  public abstract ServiceDocView doc();

  public abstract String name();

  public abstract String serviceHostname();

  public abstract Integer servicePort();

  public abstract String serviceTitle();

  public abstract Iterable<String> authScopes();

  public abstract List<PathTemplateView> pathTemplates();

  public abstract List<FormatResourceFunctionView> formatResourceFunctions();

  public abstract List<ParseResourceFunctionView> parseResourceFunctions();

  public boolean hasFormatOrParseResourceFunctions() {
    return formatResourceFunctions().size() > 0 || parseResourceFunctions().size() > 0;
  }

  public abstract List<PathTemplateGetterFunctionView> pathTemplateGetterFunctions();

  public abstract List<PageStreamingDescriptorView> pageStreamingDescriptors();

  public abstract List<BatchingDescriptorView> batchingDescriptors();

  public abstract List<LongRunningOperationDetailView> longRunningDescriptors();

  public abstract List<GrpcStreamingDetailView> grpcStreamingDescriptors();

  public abstract String clientConfigPath();

  @Nullable
  public abstract String clientConfigName();

  public abstract String interfaceKey();

  public abstract String grpcClientTypeName();

  public abstract List<GrpcStubView> stubs();

  public abstract String outputPath();

  public abstract List<ApiMethodView> apiMethods();

  public abstract boolean hasPageStreamingMethods();

  public abstract boolean hasBatchingMethods();

  public abstract boolean hasLongRunningOperations();

  public boolean hasGrpcStreamingMethods() {
    return grpcStreamingDescriptors().size() > 0;
  }

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

  @Nullable
  public abstract List<String> validDescriptorsNames();

  /**
   * The name of the class that controls the credentials information of an api. It is currently only
   * used by Ruby.
   */
  @Nullable
  public abstract String fullyQualifiedCredentialsClassName();

  @Nullable
  public abstract String defaultCredentialsInitializerCall();

  @Nullable
  public abstract String servicePhraseName();

  @Nullable
  public abstract String gapicPackageName();

  @Nullable
  public abstract String apiVersion();

  @Nullable
  public abstract String grpcTransportClassName();

  @Nullable
  public abstract String grpcTransportImportName();

  public abstract boolean isRestOnlyTransport();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  public abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_DynamicLangXApiView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder templateFileName(String val);

    public abstract Builder fileHeader(FileHeaderView val);

    public abstract Builder protoFilename(String simpleName);

    public abstract Builder doc(ServiceDocView doc);

    public abstract Builder name(String val);

    public abstract Builder serviceHostname(String val);

    public abstract Builder servicePort(Integer val);

    public abstract Builder serviceTitle(String val);

    public abstract Builder authScopes(Iterable<String> val);

    public abstract Builder pathTemplates(List<PathTemplateView> val);

    public abstract Builder formatResourceFunctions(List<FormatResourceFunctionView> val);

    public abstract Builder parseResourceFunctions(List<ParseResourceFunctionView> val);

    public abstract Builder pathTemplateGetterFunctions(List<PathTemplateGetterFunctionView> val);

    public abstract Builder pageStreamingDescriptors(List<PageStreamingDescriptorView> val);

    public abstract Builder batchingDescriptors(List<BatchingDescriptorView> val);

    public abstract Builder longRunningDescriptors(List<LongRunningOperationDetailView> val);

    public abstract Builder grpcStreamingDescriptors(List<GrpcStreamingDetailView> val);

    public abstract Builder clientConfigPath(String val);

    public abstract Builder clientConfigName(String var);

    public abstract Builder interfaceKey(String val);

    public abstract Builder grpcClientTypeName(String val);

    public abstract Builder stubs(List<GrpcStubView> val);

    public abstract Builder outputPath(String val);

    public abstract Builder apiMethods(List<ApiMethodView> val);

    public abstract Builder hasPageStreamingMethods(boolean val);

    public abstract Builder hasBatchingMethods(boolean val);

    public abstract Builder hasLongRunningOperations(boolean val);

    public abstract Builder hasDefaultServiceAddress(boolean val);

    public abstract Builder hasDefaultServiceScopes(boolean val);

    public abstract Builder validDescriptorsNames(List<String> strings);

    public abstract Builder fullyQualifiedCredentialsClassName(String val);

    public abstract Builder defaultCredentialsInitializerCall(String val);

    public abstract Builder servicePhraseName(String val);

    public abstract Builder gapicPackageName(String val);

    public abstract Builder apiVersion(String val);

    public abstract Builder grpcTransportClassName(String val);

    public abstract Builder grpcTransportImportName(String val);

    public abstract Builder isRestOnlyTransport(boolean val);

    public abstract DynamicLangXApiView build();
  }
}
