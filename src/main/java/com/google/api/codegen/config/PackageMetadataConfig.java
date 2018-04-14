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
package com.google.api.codegen.config;

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.grpcmetadatagen.ArtifactType;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import org.yaml.snakeyaml.Yaml;

/**
 * PackageMetadataConfig represents the package metadata for an API library contained in the
 * {api}_pkg.yaml configuration file.
 */
@AutoValue
public abstract class PackageMetadataConfig {

  private static final String CONFIG_KEY_DEFAULT = "default";

  protected abstract Map<TargetLanguage, VersionBound> gaxVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> gaxGrpcVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> gaxHttpVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> grpcVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> protoVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> apiCommonVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> authVersionBound();

  protected abstract Map<TargetLanguage, VersionBound> generatedNonGAPackageVersionBound();

  @Nullable
  protected abstract Map<TargetLanguage, VersionBound> generatedGAPackageVersionBound();

  protected abstract Map<TargetLanguage, String> packageName();

  protected abstract Map<TargetLanguage, Map<String, VersionBound>> protoPackageDependencies();

  @Nullable
  protected abstract Map<TargetLanguage, Map<String, VersionBound>> protoPackageTestDependencies();

  protected abstract Map<TargetLanguage, ReleaseLevel> releaseLevel();

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

  /** The version the client library package. E.g., "0.14.0". Configured per language. */
  public VersionBound generatedPackageVersionBound(TargetLanguage language) {
    if (releaseLevel(language) == ReleaseLevel.GA
        && generatedGAPackageVersionBound() != null
        && generatedGAPackageVersionBound().containsKey(language)) {
      return generatedGAPackageVersionBound().get(language);
    } else {
      // Default to non-GA version since not all languages config GA version explicitly.
      return generatedNonGAPackageVersionBound().get(language);
    }
  }

  /** The version the auth library package that this package depends on. Configured per language. */
  public VersionBound authVersionBound(TargetLanguage language) {
    return authVersionBound().get(language);
  }

  /** The versions of the proto packages that this package depends on. Configured per language. */
  public Map<String, VersionBound> protoPackageDependencies(TargetLanguage language) {
    return protoPackageDependencies().get(language);
  }

  /**
   * The versions of the proto packages that this package depends on for tests. Configured per
   * language.
   */
  public Map<String, VersionBound> protoPackageTestDependencies(TargetLanguage language) {
    if (protoPackageTestDependencies() != null) {
      return protoPackageTestDependencies().get(language);
    }
    return null;
  }

  /** The development status of the client library. Configured per language. */
  public ReleaseLevel releaseLevel(TargetLanguage language) {
    ReleaseLevel level = releaseLevel().get(language);
    if (level == null) {
      level = ReleaseLevel.UNSET_RELEASE_LEVEL;
    }
    return level;
  }

  /**
   * The base name of the client library package. E.g., "google-cloud-logging-v1". Configured per
   * language.
   */
  public String packageName(TargetLanguage language) {
    return packageName().get(language);
  }

  @Nullable
  public abstract ArtifactType artifactType();

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

  /** The file name of the GAPIC API config yaml. */
  @Nullable
  public abstract String gapicConfigName();

  private static Builder newBuilder() {
    return new AutoValue_PackageMetadataConfig.Builder();
  }

  @AutoValue.Builder
  protected abstract static class Builder {
    abstract Builder gaxVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder gaxGrpcVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder gaxHttpVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder grpcVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder protoVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder apiCommonVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder authVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder packageName(Map<TargetLanguage, String> val);

    abstract Builder generatedNonGAPackageVersionBound(Map<TargetLanguage, VersionBound> val);

    @Nullable
    abstract Builder generatedGAPackageVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract Builder protoPackageDependencies(Map<TargetLanguage, Map<String, VersionBound>> val);

    abstract Builder protoPackageTestDependencies(
        Map<TargetLanguage, Map<String, VersionBound>> val);

    abstract Builder releaseLevel(Map<TargetLanguage, ReleaseLevel> val);

    abstract Builder shortName(String val);

    abstract Builder artifactType(ArtifactType val);

    abstract Builder apiVersion(String val);

    abstract Builder protoPath(String val);

    abstract Builder author(String val);

    abstract Builder email(String val);

    abstract Builder homepage(String val);

    abstract Builder licenseName(String val);

    abstract Builder gapicConfigName(String val);

    abstract PackageMetadataConfig build();
  }

  /** Creates an PackageMetadataConfig with no content. Exposed for testing. */
  @VisibleForTesting
  public static PackageMetadataConfig createDummyPackageMetadataConfig() {
    return newBuilder()
        .gaxVersionBound(ImmutableMap.<TargetLanguage, VersionBound>of())
        .gaxGrpcVersionBound(ImmutableMap.<TargetLanguage, VersionBound>of())
        .gaxHttpVersionBound(ImmutableMap.<TargetLanguage, VersionBound>of())
        .grpcVersionBound(ImmutableMap.<TargetLanguage, VersionBound>of())
        .protoVersionBound(ImmutableMap.<TargetLanguage, VersionBound>of())
        .packageName(ImmutableMap.<TargetLanguage, String>of())
        .authVersionBound(ImmutableMap.<TargetLanguage, VersionBound>of())
        .apiCommonVersionBound(ImmutableMap.<TargetLanguage, VersionBound>of())
        .generatedNonGAPackageVersionBound(ImmutableMap.<TargetLanguage, VersionBound>of())
        .generatedGAPackageVersionBound(ImmutableMap.<TargetLanguage, VersionBound>of())
        .protoPackageDependencies(ImmutableMap.<TargetLanguage, Map<String, VersionBound>>of())
        .releaseLevel(ImmutableMap.<TargetLanguage, ReleaseLevel>of())
        .shortName("")
        .artifactType(ArtifactType.GRPC)
        .apiVersion("")
        .protoPath("")
        .author("")
        .email("")
        .homepage("")
        .licenseName("")
        .gapicConfigName("")
        .build();
  }

  @SuppressWarnings("unchecked")
  public static PackageMetadataConfig createFromString(String yamlContents) {
    Yaml yaml = new Yaml();
    Map<String, Object> configMap = (Map<String, Object>) yaml.load(yamlContents);

    Builder builder =
        newBuilder()
            .gaxVersionBound(
                createVersionMap((Map<String, Map<String, String>>) configMap.get("gax_version")))
            .gaxGrpcVersionBound(
                createVersionMap(
                    (Map<String, Map<String, String>>) configMap.get("gax_grpc_version")))
            .gaxHttpVersionBound(
                createVersionMap(
                    (Map<String, Map<String, String>>) configMap.get("gax_http_version")))
            .grpcVersionBound(
                createVersionMap((Map<String, Map<String, String>>) configMap.get("grpc_version")))
            .protoVersionBound(
                createVersionMap((Map<String, Map<String, String>>) configMap.get("proto_version")))
            .authVersionBound(
                createVersionMap((Map<String, Map<String, String>>) configMap.get("auth_version")))
            .generatedNonGAPackageVersionBound(
                createVersionMap(
                    (Map<String, Map<String, String>>) configMap.get("generated_package_version")))
            .apiCommonVersionBound(
                createVersionMap(
                    (Map<String, Map<String, String>>) configMap.get("api_common_version")))
            .releaseLevel(
                createReleaseLevelMap((Map<String, String>) configMap.get("release_level")))
            .protoPackageDependencies(createProtoPackageDependencies(configMap, "proto_deps"))
            .packageName(buildMapWithDefault((Map<String, String>) configMap.get("package_name")))
            .shortName((String) configMap.get("short_name"))
            .artifactType(ArtifactType.of((String) configMap.get("artifact_type")))
            .apiVersion((String) configMap.get("major_version"))
            .protoPath((String) configMap.get("proto_path"))
            .author((String) configMap.get("author"))
            .email((String) configMap.get("email"))
            .homepage((String) configMap.get("homepage"))
            .licenseName((String) configMap.get("license"))
            .gapicConfigName((String) configMap.get("gapic_config_name"));
    if (configMap.containsKey("proto_test_deps")) {
      builder.protoPackageTestDependencies(
          createProtoPackageDependencies(configMap, "proto_test_deps"));
    }

    if (configMap.containsKey("generated_ga_package_version")) {
      builder.generatedGAPackageVersionBound(
          createVersionMap(
              (Map<String, Map<String, String>>) configMap.get("generated_ga_package_version")));
    }
    return builder.build();
  }

  @SuppressWarnings("unchecked")
  private static Map<TargetLanguage, Map<String, VersionBound>> createProtoPackageDependencies(
      Map<String, Object> configMap, String key) {
    Map<TargetLanguage, Map<String, VersionBound>> packageDependencies = new HashMap<>();
    List<String> packages = (List<String>) configMap.get(key);
    if (packages == null) {
      return packageDependencies;
    }

    for (String packageName : packages) {
      Map<String, Map<String, String>> config =
          (Map<String, Map<String, String>>) configMap.get(packageName + "_version");
      if (config == null) {
        throw new IllegalArgumentException(
            "'" + packageName + "' in proto_deps was not found in dependency list.");
      }

      Map<TargetLanguage, Map<String, String>> versionMap = buildMapWithDefault(config);
      for (Entry<TargetLanguage, Map<String, String>> entry : versionMap.entrySet()) {
        if (entry.getValue() == null) {
          continue;
        }
        if (!packageDependencies.containsKey(entry.getKey())) {
          packageDependencies.put(entry.getKey(), new HashMap<String, VersionBound>());
        }

        String packageNameForLanguage = entry.getValue().get("name_override");
        if (packageNameForLanguage == null) {
          packageNameForLanguage = packageName;
        }
        VersionBound version =
            VersionBound.create(entry.getValue().get("lower"), entry.getValue().get("upper"));
        packageDependencies.get(entry.getKey()).put(packageNameForLanguage, version);
      }
    }

    return packageDependencies;
  }

  private static Map<TargetLanguage, VersionBound> createVersionMap(
      Map<String, Map<String, String>> inputMap) {
    Map<TargetLanguage, Map<String, String>> intermediate = buildMapWithDefault(inputMap);
    // Convert parsed YAML map into VersionBound object
    return Maps.transformValues(
        intermediate,
        new Function<Map<String, String>, VersionBound>() {
          @Override
          @Nullable
          public VersionBound apply(@Nullable Map<String, String> versionMap) {
            if (versionMap == null) {
              return null;
            }
            return VersionBound.create(versionMap.get("lower"), versionMap.get("upper"));
          }
        });
  }

  private static Map<TargetLanguage, ReleaseLevel> createReleaseLevelMap(
      Map<String, String> inputMap) {
    Map<TargetLanguage, String> intermediate = buildMapWithDefault(inputMap);
    // Convert parsed YAML map into ReleaseLevel enum
    return Maps.transformValues(
        intermediate,
        new Function<String, ReleaseLevel>() {
          @Override
          @Nullable
          public ReleaseLevel apply(@Nullable String releaseLevelName) {
            if (releaseLevelName == null) {
              return null;
            }
            return Enum.valueOf(ReleaseLevel.class, releaseLevelName.toUpperCase());
          }
        });
  }

  /**
   * Transforms entries of the input map into TargetLanguage, taking into account an optional
   * default setting.
   */
  private static <V> Map<TargetLanguage, V> buildMapWithDefault(Map<String, V> inputMap) {
    Map<TargetLanguage, V> outputMap = new HashMap<>();

    // TODO(andrealin): should this just return null?
    if (inputMap == null) {
      return outputMap;
    }
    Set<TargetLanguage> configuredLanguages = new HashSet<>();
    V defaultValue = null;
    for (Map.Entry<String, V> entry : inputMap.entrySet()) {
      if (entry.getKey().equals(CONFIG_KEY_DEFAULT)) {
        defaultValue = entry.getValue();
      } else {
        TargetLanguage targetLanguage = TargetLanguage.fromString(entry.getKey());
        configuredLanguages.add(targetLanguage);
        outputMap.put(targetLanguage, entry.getValue());
      }

      for (TargetLanguage language : TargetLanguage.values()) {
        if (!configuredLanguages.contains(language)) {
          outputMap.put(language, defaultValue);
        }
      }
    }
    return outputMap;
  }
}
