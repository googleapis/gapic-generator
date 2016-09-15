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

import java.util.Collection;
import java.util.List;

@AutoValue
public abstract class StaticLangXCombinedSurfaceView implements ViewModel {
  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  public abstract Integer servicePort();

  public abstract List<String> imports();

  public abstract Iterable<String> authScopes();

  public abstract List<PathTemplateView> pathTemplates();

  public abstract List<PathTemplateGetterFunctionView> pathTemplateGetters();

  public abstract Collection<RetryPairDefinitionView> retryPairDefinitions();

  public abstract List<StaticLangApiMethodView> apiMethods();

  public abstract String clientName();

  public abstract String clientConstructorName();

  public abstract String callOptionsTypeName();

  public abstract String defaultClientOptionFunctionName();

  public abstract String defaultCallOptionFunctionName();

  public abstract String serviceName();

  public abstract String grpcClientTypeName();

  public abstract String grpcClientConstructorName();

  public abstract List<ApiCallSettingsView> callSettings();

  public abstract List<String> serviceDoc();

  public abstract String packageName();

  public abstract String serviceAddress();

  public abstract List<PageStreamingDescriptorClassView> pageStreamingDescriptorClasses();

  public static Builder newBuilder() {
    return new AutoValue_StaticLangXCombinedSurfaceView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder apiMethods(List<StaticLangApiMethodView> val);

    public abstract Builder imports(List<String> val);

    public abstract Builder authScopes(Iterable<String> val);

    public abstract Builder clientName(String val);

    public abstract Builder clientConstructorName(String val);

    public abstract Builder callOptionsTypeName(String val);

    public abstract Builder defaultClientOptionFunctionName(String val);

    public abstract Builder defaultCallOptionFunctionName(String val);

    public abstract Builder serviceName(String val);

    public abstract Builder grpcClientTypeName(String val);

    public abstract Builder grpcClientConstructorName(String val);

    public abstract Builder outputPath(String val);

    public abstract Builder packageName(String val);

    public abstract Builder callSettings(List<ApiCallSettingsView> callSettings);

    public abstract Builder pathTemplates(List<PathTemplateView> val);

    public abstract Builder retryPairDefinitions(Collection<RetryPairDefinitionView> val);

    public abstract Builder serviceDoc(List<String> val);

    public abstract Builder serviceAddress(String val);

    public abstract Builder pathTemplateGetters(List<PathTemplateGetterFunctionView> val);

    public abstract Builder servicePort(Integer val);

    public abstract Builder templateFileName(String val);

    public abstract Builder pageStreamingDescriptorClasses(
        List<PageStreamingDescriptorClassView> val);

    public abstract StaticLangXCombinedSurfaceView build();
  }
}
