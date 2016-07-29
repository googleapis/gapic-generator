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

import java.util.List;

import javax.annotation.Nullable;

/**
 * View of a single api method. This is a union class that is capable of holding the
 * data for any type of static api method; the type is maintained as a value of the
 * ApiMethodType enum.
 */
@AutoValue
public abstract class StaticApiMethodView implements ApiMethodView {
  public abstract ApiMethodType type();

  public abstract String apiClassName();

  public abstract String apiVariableName();

  public abstract InitCodeView initCode();

  public abstract ApiMethodDocView doc();

  public abstract String apiRequestTypeName();

  public abstract String responseTypeName();

  public abstract String name();

  public abstract String settingsGetterName();

  public abstract List<RequestObjectParamView> methodParams();

  public abstract List<PathTemplateCheckView> pathTemplateChecks();

  public abstract boolean hasReturnValue();

  public abstract List<RequestObjectParamView> requestObjectParams();

  @Nullable
  public abstract ListMethodDetailView listMethod();

  @Nullable
  public abstract UnpagedListCallableMethodDetailView unpagedListCallableMethod();

  @Nullable
  public abstract CallableMethodDetailView callableMethod();

  @Nullable
  public abstract RequestObjectMethodDetailView requestObjectMethod();

  public static Builder newBuilder() {
    return new AutoValue_StaticApiMethodView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder type(ApiMethodType type);

    public abstract Builder apiClassName(String apiClassName);

    public abstract Builder apiVariableName(String apiVariableName);

    public abstract Builder initCode(InitCodeView initCode);

    public abstract Builder doc(ApiMethodDocView doc);

    public abstract Builder apiRequestTypeName(String requestTypeName);

    public abstract Builder responseTypeName(String responseTypeName);

    public abstract Builder name(String name);

    public abstract Builder settingsGetterName(String name);

    public abstract Builder methodParams(List<RequestObjectParamView> methodParams);

    public abstract Builder pathTemplateChecks(List<PathTemplateCheckView> pathTemplateChecks);

    public abstract Builder hasReturnValue(boolean hasReturnValue);

    public abstract Builder requestObjectParams(List<RequestObjectParamView> requestObjectParams);

    public abstract Builder listMethod(ListMethodDetailView details);

    public abstract Builder unpagedListCallableMethod(UnpagedListCallableMethodDetailView details);

    public abstract Builder callableMethod(CallableMethodDetailView details);

    public abstract Builder requestObjectMethod(RequestObjectMethodDetailView details);

    public abstract StaticApiMethodView build();
  }
}
