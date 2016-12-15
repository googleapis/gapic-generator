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
package com.google.api.codegen.config;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Maps.EntryTransformer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.annotation.Nullable;
import org.yaml.snakeyaml.Yaml;

@AutoValue
public abstract class PackageMetadataConfig {

  @AutoValue
  public abstract static class VersionBound {
    public abstract String lower();

    @Nullable
    public abstract String upper();
  }

  protected abstract Map<String, VersionBound> gaxVersion();

  protected abstract Map<String, VersionBound> protoVersion();

  protected abstract Map<String, VersionBound> commonProtosVersion();

  protected abstract Map<String, VersionBound> packageVersion();

  protected abstract Map<String, String> packageName();

  /** The version of GAX that this package depends on. Configured per language. */
  public VersionBound gaxVersion(String language) {
    return getWithDefault(gaxVersion(), language);
  }

  /**
   * The version of the protocol buffer package that this package depends on. Map of target language
   * to name.
   */
  public VersionBound protoVersion(String language) {
    return getWithDefault(protoVersion(), language);
  }

  /**
   * The version of the googleapis common protos package that this package depends on. Configured
   * per language.
   */
  public VersionBound commonProtosVersion(String language) {
    return getWithDefault(commonProtosVersion(), language);
  }

  /** The version the client library package. E.g., "0.14.0". Configured per language. */
  public VersionBound packageVersion(String language) {
    return getWithDefault(packageVersion(), language);
  }

  /**
   * The base name of the client library package. E.g., "google-cloud-logging-v1". Configured per
   * language.
   */
  public String packageName(String language) {
    return getWithDefault(packageName(), language);
  }

  /** A single-word short name of the API. E.g., "logging". */
  public abstract String shortName();

  /** The major version of the API, as used in the package name. E.g., "v1". */
  public abstract String apiVersion();

  /** The path to the API protos in the googleapis repo. */
  public abstract String protoPath();

  /** The author of the client library. */
  public abstract String author();

  /** The email of the author of the client library. */
  public abstract String email();

  /** The homepage of the client library. */
  public abstract String homepage();

  /** The name of the license of the client library. */
  public abstract String license();

  private static Builder newBuilder() {
    return new AutoValue_PackageMetadataConfig.Builder();
  }

  @AutoValue.Builder
  protected abstract static class Builder {
    abstract Builder gaxVersion(Map<String, VersionBound> val);

    abstract Builder protoVersion(Map<String, VersionBound> val);

    abstract Builder commonProtosVersion(Map<String, VersionBound> val);

    abstract Builder packageName(Map<String, String> val);

    abstract Builder packageVersion(Map<String, VersionBound> val);

    abstract Builder shortName(String val);

    abstract Builder apiVersion(String val);

    abstract Builder protoPath(String val);

    abstract Builder author(String val);

    abstract Builder email(String val);

    abstract Builder homepage(String val);

    abstract Builder license(String val);

    abstract PackageMetadataConfig build();
  }

  /** Creates an PackageMetadataConfig with no content. Exposed for testing. */
  @VisibleForTesting
  public static PackageMetadataConfig createDummyPackageMetadataConfig() {
    return newBuilder()
        .gaxVersion(ImmutableMap.<String, VersionBound>of())
        .protoVersion(ImmutableMap.<String, VersionBound>of())
        .packageName(ImmutableMap.<String, String>of())
        .commonProtosVersion(ImmutableMap.<String, VersionBound>of())
        .packageVersion(ImmutableMap.<String, VersionBound>of())
        .shortName("")
        .apiVersion("")
        .protoPath("")
        .author("")
        .email("")
        .homepage("")
        .license("")
        .build();
  }

  @SuppressWarnings("unchecked")
  public static PackageMetadataConfig createFromFile(Path packageConfigPath) throws IOException {
    Yaml yaml = new Yaml();
    String contents = new String(Files.readAllBytes(packageConfigPath), StandardCharsets.UTF_8);
    Map<String, Object> configMap = (Map<String, Object>) yaml.load(contents);

    return newBuilder()
        .gaxVersion(
            createVersionMap((Map<String, Map<String, String>>) configMap.get("gax_version")))
        .protoVersion(
            createVersionMap((Map<String, Map<String, String>>) configMap.get("proto_version")))
        .commonProtosVersion(
            createVersionMap(
                (Map<String, Map<String, String>>) configMap.get("common_protos_version")))
        .packageVersion(
            createVersionMap((Map<String, Map<String, String>>) configMap.get("package_version")))
        .packageName((Map<String, String>) configMap.get("package_name"))
        .shortName((String) configMap.get("short_name"))
        .apiVersion((String) configMap.get("major_version"))
        .protoPath((String) configMap.get("proto_path"))
        .author((String) configMap.get("author"))
        .email((String) configMap.get("email"))
        .homepage((String) configMap.get("homepage"))
        .license((String) configMap.get("license"))
        .build();
  }

  private static Map<String, VersionBound> createVersionMap(
      Map<String, Map<String, String>> inputMap) {
    return Maps.transformEntries(
        inputMap,
        new EntryTransformer<String, Map<String, String>, VersionBound>() {
          @Override
          public VersionBound transformEntry(
              @Nullable String key, @Nullable Map<String, String> value) {
            return new AutoValue_PackageMetadataConfig_VersionBound(
                value.get("lower"), value.get("upper"));
          }
        });
  }

  private <T> T getWithDefault(Map<String, T> map, String index) {
    T candidate = map.get(index);
    return candidate == null ? map.get("default") : candidate;
  }
}
