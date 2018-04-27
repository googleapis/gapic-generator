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

/**
 * ViewModel representing the file containing a message.
 *
 * <p>For example, this can be used to represent a class file containing a Discovery Document's
 * schemas.
 */
@AutoValue
public abstract class StaticLangApiMessageFileView implements ViewModel {
  public abstract StaticLangApiMessageView schema();

  public abstract FileHeaderView fileHeader();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  public static StaticLangApiMessageFileView.Builder newBuilder() {
    return new AutoValue_StaticLangApiMessageFileView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract StaticLangApiMessageFileView.Builder schema(StaticLangApiMessageView val);

    public abstract StaticLangApiMessageFileView.Builder templateFileName(String val);

    public abstract StaticLangApiMessageFileView.Builder outputPath(String val);

    public abstract StaticLangApiMessageFileView.Builder fileHeader(FileHeaderView val);

    public abstract StaticLangApiMessageFileView build();
  }
}
