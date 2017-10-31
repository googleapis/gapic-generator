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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;

@AutoValue
public abstract class FileHeaderView {
  public abstract ImmutableList<String> copyrightLines();

  public abstract ImmutableList<String> licenseLines();

  public abstract String packageName();

  public abstract String examplePackageName();

  public abstract String localPackageName();

  public abstract ImmutableList<String> modules();

  @Nullable
  public abstract String version();

  public boolean hasVersion() {
    String version = version();
    return version != null && version.length() > 0;
  }

  public abstract String generatorVersion();

  public boolean hasGeneratorVersion() {
    String generatorVersion = generatorVersion();
    return generatorVersion != null && generatorVersion.length() > 0;
  }

  public abstract String localExamplePackageName();

  public abstract ImportSectionView importSection();

  public static Builder newBuilder() {
    return new AutoValue_FileHeaderView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder copyrightLines(ImmutableList<String> val);

    public abstract Builder licenseLines(ImmutableList<String> val);

    public abstract Builder packageName(String val);

    public abstract Builder examplePackageName(String val);

    public abstract Builder localPackageName(String val);

    public abstract Builder modules(ImmutableList<String> val);

    public abstract Builder version(String val);

    public abstract Builder generatorVersion(String val);

    public abstract Builder localExamplePackageName(String val);

    public abstract Builder importSection(ImportSectionView val);

    public abstract FileHeaderView build();
  }
}
