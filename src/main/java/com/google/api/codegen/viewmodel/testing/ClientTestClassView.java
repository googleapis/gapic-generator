/* Copyright 2016 Google LLC
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

import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class ClientTestClassView {

  public abstract String name();

  public abstract String apiClassName();

  public abstract String apiSettingsClassName();

  @Nullable
  public abstract String apiVariableName();

  @Nullable
  public abstract String apiName();

  public abstract List<MockServiceUsageView> mockServices();

  public abstract List<TestCaseView> testCases();

  public abstract boolean apiHasLongRunningMethods();

  @Nullable
  public abstract String packageServiceName();

  public abstract boolean missingDefaultServiceAddress();

  public abstract boolean missingDefaultServiceScopes();

  @Nullable
  public abstract String apiVersion();

  @Nullable
  public abstract String mockCredentialsClassName();

  @Nullable
  public abstract String fullyQualifiedCredentialsClassName();

  @Nullable
  public abstract List<ClientInitParamView> clientInitOptionalParams();

  public static Builder newBuilder() {
    return new AutoValue_ClientTestClassView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String val);

    public abstract Builder apiClassName(String val);

    public abstract Builder apiSettingsClassName(String val);

    public abstract Builder apiVariableName(String val);

    public abstract Builder apiName(String val);

    public abstract Builder mockServices(List<MockServiceUsageView> val);

    public abstract Builder testCases(List<TestCaseView> val);

    public abstract Builder apiHasLongRunningMethods(boolean val);

    /** The name of the property of the api export that exports this service. Used in Node.js. */
    public abstract Builder packageServiceName(String val);

    public abstract Builder missingDefaultServiceAddress(boolean val);

    public abstract Builder missingDefaultServiceScopes(boolean val);

    public abstract Builder apiVersion(String val);

    public abstract Builder mockCredentialsClassName(String val);

    public abstract Builder fullyQualifiedCredentialsClassName(String val);

    public abstract Builder clientInitOptionalParams(List<ClientInitParamView> val);

    public abstract ClientTestClassView build();
  }
}
