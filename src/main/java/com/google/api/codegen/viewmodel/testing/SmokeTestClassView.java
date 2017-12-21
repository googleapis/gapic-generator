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
package com.google.api.codegen.viewmodel.testing;

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.FileHeaderView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

@AutoValue
public abstract class SmokeTestClassView implements ViewModel {

  public abstract FileHeaderView fileHeader();

  public abstract String name();

  public abstract String apiClassName();

  @Nullable
  public abstract String apiVariableName();

  public abstract String apiSettingsClassName();

  @Nullable
  public abstract ApiMethodView apiMethod();

  @Nullable
  public abstract TestCaseView method();

  public boolean hasMethod() {
    return method() != null;
  }

  public abstract boolean requireProjectId();

  @Nullable
  public abstract String projectIdVariableName();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  @Nullable
  public abstract String apiName();

  @Nullable
  public abstract String apiVersion();

  public static Builder newBuilder() {
    return new AutoValue_SmokeTestClassView.Builder().requireProjectId(false);
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder fileHeader(FileHeaderView val);

    public abstract Builder name(String val);

    public abstract Builder apiClassName(String val);

    public abstract Builder apiVariableName(String val);

    public abstract Builder apiSettingsClassName(String val);

    public abstract Builder outputPath(String val);

    public abstract Builder templateFileName(String val);

    public abstract Builder apiMethod(ApiMethodView val);

    public abstract Builder method(TestCaseView val);

    public abstract Builder requireProjectId(boolean val);

    public abstract Builder projectIdVariableName(String val);

    public abstract Builder apiName(String val);

    public abstract Builder apiVersion(String val);

    public abstract SmokeTestClassView build();
  }
}
