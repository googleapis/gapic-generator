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
package com.google.api.codegen.gapic;

import com.google.auto.value.AutoValue;
import java.util.List;

/*
 * A context class which stores config data used by GapicProviderFactory.
 */
@AutoValue
public abstract class GapicGeneratorConfig {
  public static final String ARTIFACT_SURFACE = "surface";
  public static final String ARTIFACT_TEST = "test";

  public abstract List<String> enabledArtifacts();

  public abstract String id();

  public boolean enableSurfaceGenerator() {
    return enabledArtifacts().isEmpty() || enabledArtifacts().contains(ARTIFACT_SURFACE);
  }

  public boolean enableTestGenerator() {
    return enabledArtifacts().isEmpty() || enabledArtifacts().contains(ARTIFACT_TEST);
  }

  public static Builder newBuilder() {
    return new AutoValue_GapicGeneratorConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder enabledArtifacts(List<String> val);

    public abstract Builder id(String val);

    public abstract GapicGeneratorConfig build();
  }
}
