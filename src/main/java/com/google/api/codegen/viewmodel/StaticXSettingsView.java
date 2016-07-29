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
public abstract class StaticXSettingsView implements ViewModel {
  public abstract String templateFileName();

  public abstract String packageName();

  public abstract SettingsDocView doc();

  public abstract String name();

  public abstract String serviceAddress();

  public abstract Integer servicePort();

  public abstract Iterable<String> authScopes();

  public abstract List<ApiCallSettingsView> callSettings();

  public abstract List<PageStreamingDescriptorClassView> pageStreamingDescriptors();

  public abstract List<BundlingDescriptorClassView> bundlingDescriptors();

  public abstract List<RetryCodesDefinitionView> retryCodesDefinitions();

  public abstract List<RetryParamsDefinitionView> retryParamsDefinitions();

  public abstract List<String> imports();

  public abstract String outputPath();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  public static Builder newBuilder() {
    return new AutoValue_StaticXSettingsView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder templateFileName(String val);

    public abstract Builder packageName(String val);

    public abstract Builder doc(SettingsDocView generateSettingsDoc);

    public abstract Builder name(String val);

    public abstract Builder serviceAddress(String val);

    public abstract Builder servicePort(Integer val);

    public abstract Builder authScopes(Iterable<String> val);

    public abstract Builder callSettings(List<ApiCallSettingsView> callSettings);

    public abstract Builder pageStreamingDescriptors(
        List<PageStreamingDescriptorClassView> generateDescriptorClasses);

    public abstract Builder bundlingDescriptors(List<BundlingDescriptorClassView> val);

    public abstract Builder retryCodesDefinitions(List<RetryCodesDefinitionView> val);

    public abstract Builder retryParamsDefinitions(List<RetryParamsDefinitionView> val);

    public abstract Builder imports(List<String> val);

    public abstract Builder outputPath(String val);

    public abstract StaticXSettingsView build();
  }
}
