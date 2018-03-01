/* Copyright 2017 Google LLC
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

/** ViewModel representing the file containing a ResourceName. */
@AutoValue
public abstract class StaticLangApiResourceNameFactoryFileView implements ViewModel {
  public abstract String resourceNameTypeName();

  public abstract String name();

  public abstract FileHeaderView fileHeader();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  public static StaticLangApiResourceNameFactoryFileView.Builder newBuilder() {
    return new AutoValue_StaticLangApiResourceNameFactoryFileView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String val);

    public abstract Builder resourceNameTypeName(String val);

    public abstract Builder templateFileName(String val);

    public abstract Builder outputPath(String val);

    public abstract Builder fileHeader(FileHeaderView val);

    public abstract StaticLangApiResourceNameFactoryFileView build();
  }
}
