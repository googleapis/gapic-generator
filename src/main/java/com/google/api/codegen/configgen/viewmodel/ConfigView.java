/* Copyright 2017 Google LLC
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
package com.google.api.codegen.configgen.viewmodel;

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.List;

/** Represents the GAPIC config being generated. */
@AutoValue
public abstract class ConfigView implements ViewModel {
  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  /** The type of the config's proto. */
  public abstract String type();

  /** The version of the config schema. */
  public abstract String configSchemaVersion();

  /** The settings of generated code in a specific language. */
  public abstract List<LanguageSettingView> languageSettings();

  /** The configuration for the license header to put on generated files. */
  public abstract LicenseView license();

  /** The API interface configurations. */
  public abstract List<InterfaceView> interfaces();

  /** The list of resource name generation configurations. */
  public abstract List<ResourceNameGenerationView> resourceNameGeneration();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  public static Builder newBuilder() {
    return new AutoValue_ConfigView.Builder()
        .resourceNameGeneration(new ArrayList<ResourceNameGenerationView>());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder templateFileName(String val);

    public abstract Builder outputPath(String val);

    public abstract Builder type(String val);

    public abstract Builder configSchemaVersion(String val);

    public abstract Builder languageSettings(List<LanguageSettingView> val);

    public abstract Builder license(LicenseView val);

    public abstract Builder interfaces(List<InterfaceView> val);

    public abstract Builder resourceNameGeneration(List<ResourceNameGenerationView> val);

    public abstract ConfigView build();
  }
}
