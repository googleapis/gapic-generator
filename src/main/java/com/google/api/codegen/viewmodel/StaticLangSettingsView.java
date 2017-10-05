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

import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class StaticLangSettingsView {

  @Nullable
  public abstract String releaseLevelAnnotation();

  public abstract SettingsDocView doc();

  public abstract String name();

  public abstract String serviceAddress();

  public abstract Integer servicePort();

  @Nullable
  public abstract String packagePath();

  public abstract Iterable<String> authScopes();

  public abstract List<ApiCallSettingsView> callSettings();

  public List<ApiCallSettingsView> unaryCallSettings() {
    ArrayList<ApiCallSettingsView> unaryCallSettings = new ArrayList<>();
    for (ApiCallSettingsView settingsView : callSettings()) {
      if (settingsView.type().serviceMethodType() == ServiceMethodType.UnaryMethod) {
        unaryCallSettings.add(settingsView);
      }
    }
    return unaryCallSettings;
  }

  public List<ApiCallSettingsView> longRunningCallSettings() {
    ArrayList<ApiCallSettingsView> unaryCallSettings = new ArrayList<>();
    for (ApiCallSettingsView settingsView : callSettings()) {
      if (settingsView.type().serviceMethodType() == ServiceMethodType.LongRunningMethod) {
        unaryCallSettings.add(settingsView);
      }
    }
    return unaryCallSettings;
  }

  public abstract List<PageStreamingDescriptorClassView> pageStreamingDescriptors();

  public abstract List<PagedListResponseFactoryClassView> pagedListResponseFactories();

  public abstract List<BatchingDescriptorClassView> batchingDescriptors();

  public abstract List<RetryCodesDefinitionView> retryCodesDefinitions();

  public abstract List<RetryParamsDefinitionView> retryParamsDefinitions();

  public abstract boolean hasDefaultServiceAddress();

  public abstract boolean hasDefaultServiceScopes();

  public abstract boolean hasDefaultInstance();

  @Nullable // Used in Java
  public abstract String stubInterfaceName();

  @Nullable // Used in Java
  public abstract String grpcStubClassName();

  public static Builder newBuilder() {
    return new AutoValue_StaticLangSettingsView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder releaseLevelAnnotation(String releaseAnnotation);

    public abstract Builder doc(SettingsDocView generateSettingsDoc);

    public abstract Builder name(String val);

    public abstract Builder serviceAddress(String val);

    public abstract Builder servicePort(Integer val);

    public abstract Builder packagePath(String val);

    public abstract Builder authScopes(Iterable<String> val);

    public abstract Builder callSettings(List<ApiCallSettingsView> callSettings);

    public abstract Builder pageStreamingDescriptors(
        List<PageStreamingDescriptorClassView> generateDescriptorClasses);

    public abstract Builder pagedListResponseFactories(List<PagedListResponseFactoryClassView> val);

    public abstract Builder batchingDescriptors(List<BatchingDescriptorClassView> val);

    public abstract Builder retryCodesDefinitions(List<RetryCodesDefinitionView> val);

    public abstract Builder retryParamsDefinitions(List<RetryParamsDefinitionView> val);

    public abstract Builder hasDefaultServiceAddress(boolean hasDefaultServiceAddress);

    public abstract Builder hasDefaultServiceScopes(boolean hasDefaultServiceScopes);

    public abstract Builder hasDefaultInstance(boolean hasDefaultInstance);

    public abstract Builder stubInterfaceName(String apiStubInterfaceName);

    public abstract Builder grpcStubClassName(String apiGrpcStubClassName);

    public abstract StaticLangSettingsView build();
  }
}
