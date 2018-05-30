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
package com.google.api.codegen.config;

import com.google.api.codegen.common.TargetLanguage;
import com.google.auto.value.AutoValue;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/** This class holds dependency version information for the dependencies of the generated code. */
@AutoValue
public abstract class DependenciesConfig {

  protected abstract Map<String, Object> configMap();

  /** A map from language to the version of the given package in that language. */
  @SuppressWarnings("unchecked")
  public Map<String, Map<String, String>> getPackageVersions(String packageName) {
    return (Map<String, Map<String, String>>) configMap().get(packageName + "_version");
  }

  protected abstract Map<TargetLanguage, VersionBound> gaxVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> gaxGrpcVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> gaxHttpVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> grpcVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> protoVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> apiCommonVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> authVersionBound();

  /** The version of GAX that this package depends on. Configured per language. */
  public VersionBound gaxVersionBound(TargetLanguage language) {
    return gaxVersionBound().get(language);
  }

  /** The version of GAX Grpc that this package depends on. Configured per language. */
  public VersionBound gaxGrpcVersionBound(TargetLanguage language) {
    return gaxGrpcVersionBound().get(language);
  }

  /** The version of GAX Grpc that this package depends on. Configured per language. */
  public VersionBound gaxHttpVersionBound(TargetLanguage language) {
    return gaxHttpVersionBound().get(language);
  }

  /** The version of api-common that this package depends on. Only used by Java */
  public VersionBound apiCommonVersionBound(TargetLanguage language) {
    return apiCommonVersionBound().get(language);
  }

  /** The version of gRPC that this package depends on. Configured per language. */
  public VersionBound grpcVersionBound(TargetLanguage language) {
    return grpcVersionBound().get(language);
  }

  /**
   * The version of the protocol buffer package that this package depends on. Map of target language
   * to name.
   */
  public VersionBound protoVersionBound(TargetLanguage language) {
    return protoVersionBound().get(language);
  }

  /** The version the auth library package that this package depends on. Configured per language. */
  public VersionBound authVersionBound(TargetLanguage language) {
    return authVersionBound().get(language);
  }

  private static Builder newBuilder() {
    return new AutoValue_DependenciesConfig.Builder();
  }

  @AutoValue.Builder
  protected abstract static class Builder {
    abstract Builder configMap(Map<String, Object> val);

    abstract Builder gaxVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder gaxGrpcVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder gaxHttpVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder grpcVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder protoVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder apiCommonVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder authVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract DependenciesConfig build();
  }

  @SuppressWarnings("unchecked")
  public static DependenciesConfig createFromString(String yamlContents) {
    Yaml yaml = new Yaml();
    Map<String, Object> configMap = (Map<String, Object>) yaml.load(yamlContents);

    Builder builder =
        newBuilder()
            .configMap(configMap)
            .gaxVersionBound(
                Configs.createVersionMap(
                    (Map<String, Map<String, String>>) configMap.get("gax_version")))
            .gaxGrpcVersionBound(
                Configs.createVersionMap(
                    (Map<String, Map<String, String>>) configMap.get("gax_grpc_version")))
            .gaxHttpVersionBound(
                Configs.createVersionMap(
                    (Map<String, Map<String, String>>) configMap.get("gax_http_version")))
            .grpcVersionBound(
                Configs.createVersionMap(
                    (Map<String, Map<String, String>>) configMap.get("grpc_version")))
            .protoVersionBound(
                Configs.createVersionMap(
                    (Map<String, Map<String, String>>) configMap.get("proto_version")))
            .authVersionBound(
                Configs.createVersionMap(
                    (Map<String, Map<String, String>>) configMap.get("auth_version")))
            .apiCommonVersionBound(
                Configs.createVersionMap(
                    (Map<String, Map<String, String>>) configMap.get("api_common_version")));
    return builder.build();
  }

  public static DependenciesConfig load() throws IOException {
    URL dependenciesUrl =
        DependenciesConfig.class.getResource("/com/google/api/codegen/packaging/dependencies.yaml");
    return loadFromURL(dependenciesUrl);
  }

  public static DependenciesConfig loadFromURL(URL url) throws IOException {
    String contents = Resources.toString(url, StandardCharsets.UTF_8);
    return createFromString(contents);
  }
}
