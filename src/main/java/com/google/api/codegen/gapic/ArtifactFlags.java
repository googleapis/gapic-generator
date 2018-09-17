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
package com.google.api.codegen.gapic;

import com.google.api.codegen.ArtifactType;
import java.util.List;

public class ArtifactFlags {
  public static final String ARTIFACT_SURFACE = "surface";
  public static final String ARTIFACT_TEST = "test";

  private List<String> enabledArtifacts;
  private ArtifactType artifactType;

  public ArtifactFlags(List<String> enabledArtifacts, ArtifactType artifactType) {
    this.enabledArtifacts = enabledArtifacts;
    this.artifactType = artifactType;
  }

  public boolean surfaceGeneratorEnabled() {
    return enabledArtifacts.isEmpty() || enabledArtifacts.contains(ARTIFACT_SURFACE);
  }

  public boolean testGeneratorEnabled() {
    return enabledArtifacts.isEmpty() || enabledArtifacts.contains(ARTIFACT_TEST);
  }

  public boolean codeFilesEnabled() {
    return artifactType == ArtifactType.LEGACY_GAPIC_AND_PACKAGE
        || artifactType == ArtifactType.GAPIC_CODE;
  }

  public boolean packagingFilesEnabled() {
    return artifactType == ArtifactType.LEGACY_GAPIC_AND_PACKAGE
        || artifactType == ArtifactType.GAPIC_PACKAGE;
  }
}
