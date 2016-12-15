/* Copyright 2016 Google Inc
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

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.config.PackageMetadataConfig.VersionBound;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class PackageMetadataView implements ViewModel {

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  public abstract String identifier();

  public abstract VersionBound packageVersion();

  public abstract VersionBound gaxVersion();

  public abstract VersionBound protoVersion();

  public abstract VersionBound commonProtosVersion();

  public abstract String serviceName();

  public abstract String fullName();

  public abstract String shortName();

  public abstract String packageName();

  public abstract String majorVersion();

  public abstract String protoPath();

  public abstract String author();

  public abstract String email();

  public abstract String homepage();

  public abstract String license();

  @Nullable
  public abstract List<String> namespacePackages();

  public static Builder newBuilder() {
    return new AutoValue_PackageMetadataView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder outputPath(String val);

    public abstract Builder templateFileName(String val);

    public abstract Builder identifier(String val);

    public abstract Builder packageVersion(VersionBound val);

    public abstract Builder gaxVersion(VersionBound val);

    public abstract Builder protoVersion(VersionBound val);

    public abstract Builder commonProtosVersion(VersionBound val);

    public abstract Builder serviceName(String val);

    /** The full name of the API, including branding. E.g., "Stackdriver Logging". */
    public abstract Builder fullName(String val);

    /** A single-word short name of the API. E.g., "logging". */
    public abstract Builder shortName(String val);

    /** The base name of the client library package. E.g., "google-cloud-logging-v1". */
    public abstract Builder packageName(String val);

    /** The major version of the API, as used in the package name. E.g., "v1". */
    public abstract Builder majorVersion(String val);

    /** The path to the API protos in the googleapis repo. */
    public abstract Builder protoPath(String val);

    public abstract Builder author(String val);

    public abstract Builder email(String val);

    public abstract Builder homepage(String val);

    public abstract Builder license(String val);

    public abstract Builder namespacePackages(List<String> val);

    public abstract PackageMetadataView build();
  }
}
