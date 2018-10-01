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

@AutoValue
public abstract class StaticLangClientFileView implements ViewModel {
  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  public abstract Integer servicePort();

  public abstract FileHeaderView fileHeader();

  public abstract String domainLayerLocation();

  public abstract List<RetryConfigDefinitionView> retryPairDefinitions();

  public abstract List<StaticLangApiMethodView> apiMethods();

  public abstract String clientTypeName();

  public abstract String clientConstructorName();

  public abstract List<GrpcStubView> stubs();

  public abstract String callOptionsTypeName();

  public abstract String defaultClientOptionFunctionName();

  public abstract String defaultCallOptionFunctionName();

  public abstract String servicePhraseName();

  public abstract String serviceOriginalName();

  public abstract List<ApiCallSettingsView> callSettings();

  public abstract ServiceDocView serviceDoc();

  public abstract String serviceHostname();

  public abstract List<PageStreamingDescriptorClassView> pageStreamingDescriptorClasses();

  public abstract List<LongRunningOperationDetailView> lroDetailViews();

  public boolean hasLongRunningOperations() {
    return !lroDetailViews().isEmpty();
  }

  public static Builder newBuilder() {
    return new AutoValue_StaticLangClientFileView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder apiMethods(List<StaticLangApiMethodView> val);

    public abstract Builder fileHeader(FileHeaderView val);

    public abstract Builder domainLayerLocation(String val);

    public abstract Builder clientTypeName(String val);

    public abstract Builder clientConstructorName(String val);

    public abstract Builder stubs(List<GrpcStubView> val);

    public abstract Builder callOptionsTypeName(String val);

    public abstract Builder defaultClientOptionFunctionName(String val);

    public abstract Builder defaultCallOptionFunctionName(String val);

    public abstract Builder servicePhraseName(String val);

    public abstract Builder serviceOriginalName(String val);

    public abstract Builder outputPath(String val);

    public abstract Builder callSettings(List<ApiCallSettingsView> callSettings);

    public abstract Builder retryPairDefinitions(List<RetryConfigDefinitionView> val);

    public abstract Builder serviceDoc(ServiceDocView val);

    public abstract Builder serviceHostname(String val);

    public abstract Builder servicePort(Integer val);

    public abstract Builder templateFileName(String val);

    public abstract Builder pageStreamingDescriptorClasses(
        List<PageStreamingDescriptorClassView> val);

    public abstract Builder lroDetailViews(List<LongRunningOperationDetailView> val);

    public abstract StaticLangClientFileView build();
  }
}
