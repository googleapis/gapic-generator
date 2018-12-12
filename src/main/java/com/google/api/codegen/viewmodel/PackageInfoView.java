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
package com.google.api.codegen.viewmodel;

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class PackageInfoView implements ViewModel {
  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  public abstract FileHeaderView fileHeader();

  public abstract String serviceTitle();

  @Nullable // Used in C#
  public abstract String serviceDescription();

  @Nullable // Used in C#
  public abstract String version();

  @Nullable // Used in C#
  public abstract String tags();

  @Nullable // User in C#
  public abstract PackageMetadataView packageMetadata();

  public abstract List<ServiceDocView> serviceDocs();

  public abstract List<String> authScopes();

  public abstract String domainLayerLocation();

  public abstract ReleaseLevel releaseLevel();

  // If we can infer from the package name that this is a beta version API.
  public abstract boolean isInferredBeta();

  @Nullable
  public abstract String importPath();

  @Nullable
  public abstract List<String> packageDoc();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  public static Builder newBuilder() {
    return new AutoValue_PackageInfoView.Builder().isInferredBeta(false);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder templateFileName(String val);

    public abstract Builder outputPath(String val);

    public abstract Builder fileHeader(FileHeaderView val);

    public abstract Builder serviceTitle(String val);

    public abstract Builder serviceDescription(String val);

    public abstract Builder version(String val);

    public abstract Builder tags(String val);

    public abstract Builder packageMetadata(PackageMetadataView val);

    public abstract Builder serviceDocs(List<ServiceDocView> val);

    public abstract Builder authScopes(List<String> val);

    public abstract Builder domainLayerLocation(String val);

    public abstract Builder releaseLevel(ReleaseLevel val);

    public abstract Builder isInferredBeta(boolean val);

    public abstract Builder importPath(String val);

    public abstract Builder packageDoc(List<String> val);

    public abstract PackageInfoView build();
  }
}
