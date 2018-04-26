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
package com.google.api.codegen.viewmodel.metadata;

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.viewmodel.FileHeaderView;
import com.google.api.codegen.viewmodel.GrpcStubView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class VersionIndexView implements ViewModel {

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  @Nullable
  public abstract VersionIndexRequireView primaryService();

  public abstract String apiVersion();

  public abstract String packageVersion();

  @Nullable
  public abstract String toolkitVersion();

  @Nullable
  public abstract List<ModuleView> modules();

  public abstract List<VersionIndexRequireView> requireViews();

  @Nullable
  public abstract List<String> requireTypes();

  public abstract FileHeaderView fileHeader();

  @Nullable
  public abstract List<GrpcStubView> stubs();

  public abstract boolean isGcloud();

  public abstract VersionIndexType type();

  /* The path leading up to the version directory. */
  @Nullable
  public abstract String versionDirBasePath();

  /* The path following the version directory. */
  @Nullable
  public abstract String postVersionDirPath();

  public boolean hasPostVersionDirPath() {
    return !Strings.isNullOrEmpty(postVersionDirPath());
  }

  @Nullable
  public abstract String namespace();

  public abstract boolean packageHasEnums();

  @Nullable
  public abstract String packageName();

  public boolean hasMultipleServices() {
    return requireViews().size() > 1;
  }

  public static Builder newBuilder() {
    // Use v1 as the default version.
    return new AutoValue_VersionIndexView.Builder()
        .apiVersion("v1")
        .isGcloud(false)
        .packageHasEnums(false)
        .type(VersionIndexType.Unspecified);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder outputPath(String val);

    public abstract Builder templateFileName(String val);

    public abstract Builder primaryService(VersionIndexRequireView val);

    public abstract Builder apiVersion(String val);

    public abstract Builder packageVersion(String val);

    public abstract Builder toolkitVersion(String val);

    public abstract Builder modules(List<ModuleView> val);

    public abstract Builder requireViews(List<VersionIndexRequireView> val);

    public abstract Builder requireTypes(List<String> val);

    public abstract Builder fileHeader(FileHeaderView val);

    public abstract Builder stubs(List<GrpcStubView> vals);

    public abstract Builder isGcloud(boolean val);

    public abstract Builder type(VersionIndexType val);

    public abstract Builder versionDirBasePath(String val);

    public abstract Builder postVersionDirPath(String val);

    public abstract Builder namespace(String val);

    public abstract Builder packageHasEnums(boolean val);

    public abstract Builder packageName(String val);

    public abstract VersionIndexView build();
  }
}
