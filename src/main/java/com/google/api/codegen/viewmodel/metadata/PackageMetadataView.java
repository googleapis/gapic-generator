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
import com.google.api.codegen.config.VersionBound;
import com.google.api.codegen.grpcmetadatagen.ArtifactType;
import com.google.api.codegen.viewmodel.FileHeaderView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

  @Nullable
  public abstract ArtifactType artifactType();

  @Nullable
  public abstract String gapicConfigName();

  @Nullable
  public abstract String identifier();

  @Nullable
  public abstract VersionBound packageVersionBound();

  @Nullable
  public abstract VersionBound gaxVersionBound();

  @Nullable
  public abstract VersionBound gaxGrpcVersionBound();

  @Nullable
  public abstract VersionBound grpcVersionBound();

  @Nullable
  public abstract VersionBound protoVersionBound();

  @Nullable
  public abstract List<PackageDependencyView> protoPackageDependencies();

  @Nullable
  public abstract List<PackageDependencyView> additionalDependencies();

  public List<PackageDependencyView> dependencies() {
    List<PackageDependencyView> dependencies = new ArrayList<PackageDependencyView>();
    if (protoPackageDependencies() != null) {
      dependencies.addAll(protoPackageDependencies());
    }
    if (additionalDependencies() != null) {
      dependencies.addAll(additionalDependencies());
    }
    Collections.sort(dependencies);
    return dependencies;
  }

  @Nullable
  public abstract List<PackageDependencyView> protoPackageTestDependencies();

  @Nullable
  public abstract VersionBound authVersionBound();

  @Nullable
  public abstract String serviceName();

  public abstract String fullName();

  public abstract String shortName();

  public abstract String discoveryApiName();

  public abstract String protoPackageName();

  @Nullable
  public abstract String grpcPackageName();

  public abstract String gapicPackageName();

  public abstract String majorVersion();

  public abstract String protoPath();

  @Nullable
  public abstract String versionPath();

  @Nullable
  public abstract String versionNamespace();

  public abstract String author();

  public abstract String email();

  public abstract String homepage();

  public abstract String licenseName();

  @Nullable
  public abstract String developmentStatus();

  public abstract boolean hasMultipleServices();

  public abstract boolean hasSmokeTests();

  // TODO(landrito) Currently only Ruby/Python supports using fileHeaderView. Switch all metadata
  // gen to use this field.
  @Nullable
  public abstract FileHeaderView fileHeader();

  @Nullable
  public abstract String credentialsClassName();

  // Python-specific configuration
  @Nullable
  public abstract List<String> namespacePackages();

  @Nullable
  public abstract List<String> apiModules();

  @Nullable
  public abstract List<String> typeModules();

  @Nullable
  public abstract List<String> clientModules();

  @Nullable
  public abstract ReadmeMetadataView readmeMetadata();

  @Nullable
  public abstract String sampleAppName();

  @Nullable
  public abstract String sampleAppPackage();

  @Nullable
  public abstract String smokeTestProjectVariable();

  @Nullable
  public abstract String smokeTestKeyfileVariable();

  @Nullable
  public abstract String smokeTestJsonKeyVariable();

  @Nullable
  public abstract String projectVariable();

  @Nullable
  public abstract String keyfileVariable();

  @Nullable
  public abstract String jsonKeyVariable();

  public abstract boolean publishProtos();

  /** Overrides for packaging / publish settings */
  @Nullable
  public abstract Map<String, String> packageSettings();

  public static Builder newBuilder() {
    return new AutoValue_PackageMetadataView.Builder().hasSmokeTests(false);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder outputPath(String val);

    public abstract Builder templateFileName(String val);

    public abstract Builder identifier(String val);

    public abstract Builder gapicConfigName(String val);

    public abstract Builder artifactType(ArtifactType val);

    public abstract Builder packageVersionBound(VersionBound val);

    public abstract Builder gaxVersionBound(VersionBound val);

    public abstract Builder gaxGrpcVersionBound(VersionBound val);

    public abstract Builder grpcVersionBound(VersionBound val);

    public abstract Builder protoVersionBound(VersionBound val);

    public abstract Builder protoPackageDependencies(List<PackageDependencyView> val);

    @Nullable
    public abstract Builder protoPackageTestDependencies(List<PackageDependencyView> val);

    /** Additional dependencies. Used for conditionally added dependencies. */
    public abstract Builder additionalDependencies(List<PackageDependencyView> val);

    public abstract Builder authVersionBound(VersionBound val);

    public abstract Builder serviceName(String val);

    /** The full name of the API, including branding. E.g., "Stackdriver Logging". */
    public abstract Builder fullName(String val);

    /** A single-word short name of the API. E.g., "logging". */
    public abstract Builder shortName(String val);

    /** The API name used for generating the URL for the Discovery doc and APIs Explorer link. */
    public abstract Builder discoveryApiName(String val);

    /** The base name of the proto client library package. E.g., "proto-google-cloud-logging-v1". */
    public abstract Builder protoPackageName(String val);

    /** The base name of the gRPC client library package. E.g., "grpc-google-cloud-logging-v1". */
    public abstract Builder grpcPackageName(String val);

    /** The base name of the GAPIC client library package. E.g., "gapic-google-cloud-logging-v1". */
    public abstract Builder gapicPackageName(String val);

    /** The major version of the API, as used in the package name. E.g., "v1". */
    public abstract Builder majorVersion(String val);

    /** The path to the API protos in the googleapis repo. */
    public abstract Builder protoPath(String val);

    /* The path to the generated version index file. */
    public abstract Builder versionPath(String val);

    /** The namespace of the services found within this package. */
    public abstract Builder versionNamespace(String val);

    /** The author of the package. */
    public abstract Builder author(String val);

    /** The email of the author of the package. */
    public abstract Builder email(String val);

    /** The URL of the homepage of the author of the package. */
    public abstract Builder homepage(String val);

    /** The name of the license that the package is licensed under. */
    public abstract Builder licenseName(String val);

    /** The developement status of the package. E.g., "alpha". */
    public abstract Builder developmentStatus(String val);

    /** Whether the package contains multiple service objects */
    public abstract Builder hasMultipleServices(boolean val);

    /** The packages that should be declared as (Python) namespace packages. */
    public abstract Builder namespacePackages(List<String> val);

    /** The names of the GAPIC modules defining service objects. */
    public abstract Builder apiModules(List<String> val);

    /** The names of the GAPIC modules defining service types. */
    public abstract Builder typeModules(List<String> val);

    /** The names of the GAPIC modules defining clients. */
    public abstract Builder clientModules(List<String> vals);

    /** Whether a smoketest was generated for the package. */
    public abstract Builder hasSmokeTests(boolean val);

    /** File header information such as copyright lines and license lines */
    public abstract Builder fileHeader(FileHeaderView val);

    /** Name of the class that handles authentication for this package */
    public abstract Builder credentialsClassName(String val);

    public abstract Builder readmeMetadata(ReadmeMetadataView val);

    /** Class name of the sample application. */
    public abstract Builder sampleAppName(String s);

    /** Package name of the sample application. */
    public abstract Builder sampleAppPackage(String s);

    /** Environment variable to determine the Google Cloud project used to run smoke tests. */
    public abstract Builder smokeTestProjectVariable(String s);

    /**
     * Environment variable to determine the Google Cloud service account keyfile used to run smoke
     * tests.
     */
    public abstract Builder smokeTestKeyfileVariable(String s);

    /**
     * Environment variable to determine the contents of the Google Cloud service account keyfile
     * used to run smoke tests.
     */
    public abstract Builder smokeTestJsonKeyVariable(String s);

    /**
     * Environment variable to determine the Google Cloud project used to configure default
     * credentials.
     */
    public abstract Builder projectVariable(String s);

    /**
     * Environment variable to determine the Google Cloud service account keyfile used to configure
     * default credentials.
     */
    public abstract Builder keyfileVariable(String s);

    /**
     * Environment variable to determine the contents of the Google Cloud service account keyfile
     * used to configure default credentials.
     */
    public abstract Builder jsonKeyVariable(String s);

    /** Whether .proto files should be included in the package. */
    public abstract Builder publishProtos(boolean val);

    public abstract Builder packageSettings(Map<String, String> val);

    public abstract PackageMetadataView build();
  }
}
