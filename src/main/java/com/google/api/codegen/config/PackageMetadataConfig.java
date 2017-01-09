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

import com.google.api.codegen.TargetLanguage;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * PackageMetadataConfig represents the package metadata for an API library contained in the
 * {api}_pkg.yaml configuration file.
 */
@AutoValue
public abstract class PackageMetadataConfig {

  protected abstract Map<TargetLanguage, VersionBound> gaxVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> protoVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> commonProtosVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> packageVersionBound();

  protected abstract Map<TargetLanguage, String> packageName();

  /** The version of GAX that this package depends on. Configured per language. */
  public VersionBound gaxVersionBound(TargetLanguage language) {
    return gaxVersionBound().get(language);
  }

  /**
   * The version of the protocol buffer package that this package depends on. Map of target language
   * to name.
   */
  public VersionBound protoVersionBound(TargetLanguage language) {
    return protoVersionBound().get(language);
  }

  /**
   * The version of the googleapis common protos package that this package depends on. Configured
   * per language.
   */
  public VersionBound commonProtosVersionBound(TargetLanguage language) {
    return commonProtosVersionBound().get(language);
  }

  /** The version the client library package. E.g., "0.14.0". Configured per language. */
  public VersionBound packageVersionBound(TargetLanguage language) {
    return packageVersionBound().get(language);
  }

  /**
   * The base name of the client library package. E.g., "google-cloud-logging-v1". Configured per
   * language.
   */
  public String packageName(TargetLanguage language) {
    return packageName().get(language);
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
  public abstract String licenseName();

  private static Builder newBuilder() {
    return new AutoValue_PackageMetadataConfig.Builder();
  }

  @AutoValue.Builder
  protected abstract static class Builder {
    abstract Builder gaxVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder protoVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder commonProtosVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder packageName(Map<TargetLanguage, String> val);

    abstract Builder packageVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder shortName(String val);

    abstract Builder apiVersion(String val);

    abstract Builder protoPath(String val);

    abstract Builder author(String val);

    abstract Builder email(String val);

    abstract Builder homepage(String val);

    abstract Builder licenseName(String val);

    abstract PackageMetadataConfig build();
  }

  /** Creates an PackageMetadataConfig with no content. Exposed for testing. */
  @VisibleForTesting
  public static PackageMetadataConfig createDummyPackageMetadataConfig() {
    return newBuilder()
        .gaxVersionBound(ImmutableMap.<TargetLanguage, VersionBound>of())
        .protoVersionBound(ImmutableMap.<TargetLanguage, VersionBound>of())
        .packageName(ImmutableMap.<TargetLanguage, String>of())
        .commonProtosVersionBound(ImmutableMap.<TargetLanguage, VersionBound>of())
        .packageVersionBound(ImmutableMap.<TargetLanguage, VersionBound>of())
        .shortName("")
        .apiVersion("")
        .protoPath("")
        .author("")
        .email("")
        .homepage("")
        .licenseName("")
        .build();
  }

  @SuppressWarnings("unchecked")
  public static PackageMetadataConfig createFromString(String yamlContents) {
    Yaml yaml = new Yaml();
    Map<String, Object> configMap = (Map<String, Object>) yaml.load(yamlContents);

    return newBuilder()
        .gaxVersionBound(
            createVersionMap((Map<String, Map<String, String>>) configMap.get("gax_version")))
        .protoVersionBound(
            createVersionMap((Map<String, Map<String, String>>) configMap.get("proto_version")))
        .commonProtosVersionBound(
            createVersionMap(
                (Map<String, Map<String, String>>) configMap.get("common_protos_version")))
        .packageVersionBound(
            createVersionMap((Map<String, Map<String, String>>) configMap.get("package_version")))
        .packageName(createPackageNameMap((Map<String, String>) configMap.get("package_name")))
        .shortName((String) configMap.get("short_name"))
        .apiVersion((String) configMap.get("major_version"))
        .protoPath((String) configMap.get("proto_path"))
        .author((String) configMap.get("author"))
        .email((String) configMap.get("email"))
        .homepage((String) configMap.get("homepage"))
        .licenseName((String) configMap.get("license"))
        .build();
  }

  private static Map<TargetLanguage, VersionBound> createVersionMap(
      Map<String, Map<String, String>> inputMap) {
    ImmutableMap.Builder<TargetLanguage, VersionBound> versionMap = new ImmutableMap.Builder<>();
    for (Map.Entry<String, Map<String, String>> entry : inputMap.entrySet()) {
      putWithDefault(
          versionMap,
          entry.getKey(),
          VersionBound.create(entry.getValue().get("lower"), entry.getValue().get("upper")));
    }
    return versionMap.build();
  }

  private static Map<TargetLanguage, String> createPackageNameMap(Map<String, String> inputMap) {
    ImmutableMap.Builder<TargetLanguage, String> packageNameMap = new ImmutableMap.Builder<>();
    for (Map.Entry<String, String> entry : inputMap.entrySet()) {
      putWithDefault(packageNameMap, entry.getKey(), entry.getValue());
    }
    return packageNameMap.build();
  }

  private static <V> void putWithDefault(
      ImmutableMap.Builder<TargetLanguage, V> builder, String languageName, V value) {
    if (languageName.equals("default")) {
      for (TargetLanguage language : TargetLanguage.values()) {
        builder.put(language, value);
      }
    } else {
      builder.put(TargetLanguage.fromString(languageName), value);
    }
  }
}
