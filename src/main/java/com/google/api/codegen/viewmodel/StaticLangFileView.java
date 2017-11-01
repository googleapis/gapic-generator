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
package com.google.api.codegen.viewmodel;

import com.google.api.codegen.SnippetSetRunner;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class StaticLangFileView<ClassViewT> implements ViewModel {
  @Override
  public abstract String templateFileName();

  public abstract FileHeaderView fileHeader();

  public abstract ClassViewT classView();

  @Override
  public abstract String outputPath();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  public static <ClassViewT> Builder<ClassViewT> newBuilder() {
    return new AutoValue_StaticLangFileView.Builder<>();
  }

  @AutoValue.Builder
  public abstract static class Builder<ClassViewT> {
    public abstract Builder<ClassViewT> templateFileName(String val);

    public abstract Builder<ClassViewT> fileHeader(FileHeaderView val);

    public abstract Builder<ClassViewT> outputPath(String val);

    public abstract Builder<ClassViewT> classView(ClassViewT val);

    public abstract StaticLangFileView<ClassViewT> build();
  }
}
