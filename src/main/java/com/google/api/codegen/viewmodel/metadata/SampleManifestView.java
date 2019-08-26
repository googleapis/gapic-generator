/* Copyright 2019 Google LLC
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
package com.google.api.codegen.viewmodel.metadata;

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class SampleManifestView implements ViewModel {

  @AutoValue
  public abstract static class SampleEntry {

    public abstract String sample();

    public abstract String path();

    /** Only needed by Java and C#. */
    public abstract String className();

    public abstract String regionTag();

    public static SampleEntry.Builder newBuilder() {
      return new AutoValue_SampleManifestView_SampleEntry.Builder();
    }

    public static SampleEntry create(
        String sample, String path, String className, String regionTag) {
      return newBuilder()
          .sample(sample)
          .path(path)
          .regionTag(regionTag)
          .className(className)
          .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder sample(String val);

      public abstract Builder path(String val);

      public abstract Builder className(String val);

      public abstract Builder regionTag(String val);

      public abstract SampleEntry build();
    }
  }

  public abstract String schemaVersion();

  public abstract String environment();

  public abstract String bin();

  public abstract String basePath();

  /** Only needed by Java and C#. */
  public abstract String packageName();

  public abstract String invocation();

  public abstract ImmutableList<SampleEntry> sampleEntries();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  public static Builder newBuilder() {
    return new AutoValue_SampleManifestView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder schemaVersion(String val);

    public abstract Builder environment(String val);

    public abstract Builder bin(String val);

    public abstract Builder basePath(String val);

    public abstract Builder invocation(String val);

    public abstract Builder sampleEntries(ImmutableList<SampleEntry> val);

    public abstract Builder templateFileName(String val);

    public abstract Builder packageName(String val);

    public abstract Builder outputPath(String val);

    public abstract SampleManifestView build();
  }
}
