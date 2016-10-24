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
import javax.annotation.Nullable;

@AutoValue
public abstract class StaticLangXApiView implements ViewModel {
  @Override
  public abstract String templateFileName();

  public abstract String packageName();

  public abstract ServiceDocView doc();

  public abstract String name();

  @Nullable // Used in C#
  public abstract String implName();

  @Nullable // Used in C#
  public abstract String grpcServiceName();

  @Nullable // Used in C#
  public abstract String grpcTypeName();

  public abstract String settingsClassName();

  public abstract List<ApiCallableView> apiCallableMembers();

  public abstract List<PathTemplateView> pathTemplates();

  public abstract List<FormatResourceFunctionView> formatResourceFunctions();

  public abstract List<ParseResourceFunctionView> parseResourceFunctions();

  public abstract List<StaticLangApiMethodView> apiMethods();

  @Nullable // Used in C#
  public abstract List<StaticLangApiMethodView> apiMethodsImpl();

  public abstract boolean hasDefaultInstance();

  public abstract List<ImportTypeView> imports();

  @Override
  public abstract String outputPath();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  public static Builder newBuilder() {
    return new AutoValue_StaticLangXApiView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder templateFileName(String val);

    public abstract Builder packageName(String val);

    public abstract Builder doc(ServiceDocView val);

    public abstract Builder name(String val);

    public abstract Builder implName(String val);

    public abstract Builder grpcServiceName(String val);

    public abstract Builder grpcTypeName(String val);

    public abstract Builder settingsClassName(String val);

    public abstract Builder apiCallableMembers(List<ApiCallableView> val);

    public abstract Builder pathTemplates(List<PathTemplateView> val);

    public abstract Builder formatResourceFunctions(List<FormatResourceFunctionView> val);

    public abstract Builder parseResourceFunctions(List<ParseResourceFunctionView> val);

    public abstract Builder apiMethods(List<StaticLangApiMethodView> val);

    public abstract Builder apiMethodsImpl(List<StaticLangApiMethodView> val);

    public abstract Builder hasDefaultInstance(boolean val);

    public abstract Builder imports(List<ImportTypeView> val);

    public abstract Builder outputPath(String val);

    public abstract StaticLangXApiView build();
  }
}
