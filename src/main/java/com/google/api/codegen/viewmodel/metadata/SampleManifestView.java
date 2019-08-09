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

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SampleManifestView {

  @AutoValue
  public abstract class SampleEntry {

    public abstract String sample();

    public abstract String path();

    public abstract String regionTag();

    public static SamplePathPair newBuilder() {
      return new AutoValue_SampleManifestView.SampleEntry.Builder();
    }

    @AutoValue.Builder
    public abstract class Builder {

      public abstract Builder sample(String val);

      public abstract Builder path(String val);

      public abstract Builder regionTag(String val);
    }
  }

  public abstract String schemaVersion();

  public abstract String environment();

  public abstract String basePath();

  public abstract ImmutableList<SampleEntry> sampleEntries();

  public Builder newBuilder() {
    return new AutoValue_SampleManifestView.Builder();
  }

  @AutoValue.Builder
  public abstract class Builder {

    public abstract Builder schemaVersion(String val);

    public abstract Builder environment(String val);

    public abstract Builder basePath(String val);

    public abstract Builder sampleEntries(ImmutableList<SampleEntry> val);
  }
}
