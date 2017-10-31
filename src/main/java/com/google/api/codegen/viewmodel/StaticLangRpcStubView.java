/* Copyright 2017 Google LLC
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
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class StaticLangRpcStubView {
  public abstract ServiceDocView doc();

  @Nullable
  public abstract String releaseLevelAnnotation();

  public abstract String name();

  public abstract String settingsClassName();

  public abstract List<ApiCallableView> apiCallables();

  public abstract List<DirectCallableView> directCallables();

  public abstract List<StaticLangApiMethodView> callableMethods();

  public abstract boolean hasDefaultInstance();

  public abstract boolean hasLongRunningOperations();

  public abstract String parentName();

  @Nullable
  // Base URL for HTTP calls.
  public abstract String baseUrl();

  public static StaticLangRpcStubView.Builder newBuilder() {
    return new AutoValue_StaticLangRpcStubView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder doc(ServiceDocView val);

    public abstract Builder releaseLevelAnnotation(String val);

    public abstract Builder name(String val);

    public abstract Builder settingsClassName(String val);

    public abstract Builder apiCallables(List<ApiCallableView> val);

    public abstract Builder directCallables(List<DirectCallableView> val);

    public abstract Builder callableMethods(List<StaticLangApiMethodView> val);

    public abstract Builder hasDefaultInstance(boolean val);

    public abstract Builder hasLongRunningOperations(boolean val);

    public abstract Builder parentName(String apiStubInterfaceName);

    public abstract Builder baseUrl(String baseUrl);

    public abstract StaticLangRpcStubView build();
  }
}
