/* Copyright 2018 Google LLC
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
import javax.annotation.Nullable;

/** A ViewModel for standalone samples for dynamic languages. */
@AutoValue
public abstract class DynamicLangSampleView implements ViewModel {

  /**
   * A placeholder class intended for different languages to subclass and put language-specific
   * information
   */
  public static class SampleExtraInfo {}

  public abstract String templateFileName();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  public abstract String outputPath();

  public abstract String className();

  /** The client library method illustrated in this sample. */
  public abstract OptionalArrayMethodView libraryMethod();

  public abstract String gapicPackageName();

  @Nullable
  public abstract SampleExtraInfo extraInfo();

  public static Builder newBuilder() {
    return new AutoValue_DynamicLangSampleView.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder templateFileName(String val);

    public abstract Builder outputPath(String val);

    public abstract Builder className(String val);

    public abstract Builder libraryMethod(OptionalArrayMethodView val);

    public abstract Builder gapicPackageName(String val);

    public abstract Builder extraInfo(SampleExtraInfo extraInfo);

    public abstract DynamicLangSampleView build();
  }
}
