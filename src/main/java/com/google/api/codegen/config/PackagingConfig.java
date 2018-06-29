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

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.packagegen.PackagingArtifactType;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.yaml.snakeyaml.Yaml;

/** Represents the packaging config necessary to run package generation for an API. */
@AutoValue
public abstract class PackagingConfig {

  /** A single-word short name of the API. E.g., "logging". */
  public abstract String apiName();

  /** The major version of the API, as used in the package name. E.g., "v1". */
  public abstract String apiVersion();

  /** The organization name of the API, e.g. "google-cloud". */
  public abstract String organizationName();

  /** The names of the proto packages that this package depends on. */
  public abstract List<String> protoPackageDependencies();

  /** The names of the proto packages that this package depends on for tests. */
  @Nullable
  public abstract List<String> protoPackageTestDependencies();

  /** The base name of the client library package. E.g., "google-cloud-logging-v1". */
  public String packageName() {
    if (!Strings.isNullOrEmpty(apiVersion())) {
      return Joiner.on("-").join(organizationName(), apiName(), apiVersion());
    } else {
      return Joiner.on("-").join(organizationName(), apiName());
    }
  }

  /** The artifact type that should be generated. */
  @Nullable
  public abstract PackagingArtifactType artifactType();

  /** The release level of the API. */
  @Nullable
  public abstract ReleaseLevel releaseLevel();

  /** The relative path to the API protos in the containing repo. */
  public abstract String protoPath();

  private static Builder newBuilder() {
    return new AutoValue_PackagingConfig.Builder();
  }

  @AutoValue.Builder
  protected abstract static class Builder {

    abstract Builder apiName(String val);

    abstract Builder apiVersion(String val);

    abstract Builder organizationName(String val);

    abstract Builder protoPackageDependencies(List<String> val);

    abstract Builder protoPackageTestDependencies(List<String> val);

    abstract Builder releaseLevel(ReleaseLevel val);

    abstract Builder artifactType(PackagingArtifactType val);

    abstract Builder protoPath(String val);

    abstract PackagingConfig build();
  }

  private static PackagingConfig createFromString(String yamlContents) {
    Yaml yaml = new Yaml();
    @SuppressWarnings("unchecked")
    Map<String, Object> configMap = (Map<String, Object>) yaml.load(yamlContents);

    Builder builder =
        newBuilder()
            .apiName((String) configMap.get("api_name"))
            .apiVersion(Strings.nullToEmpty((String) configMap.get("api_version")))
            .organizationName((String) configMap.get("organization_name"))
            .protoPackageDependencies(getProtoDeps(configMap, "proto_deps"))
            .releaseLevel(Configs.parseReleaseLevel((String) configMap.get("release_level")))
            .artifactType(PackagingArtifactType.of((String) configMap.get("artifact_type")))
            .protoPath((String) configMap.get("proto_path"));
    if (configMap.containsKey("test_proto_deps")) {
      builder.protoPackageTestDependencies(getProtoDeps(configMap, "test_proto_deps"));
    } else if (configMap.containsKey("proto_test_deps")) {
      // TODO delete this branch once artman always passes in test_proto_deps
      builder.protoPackageTestDependencies(getProtoDeps(configMap, "proto_test_deps"));
    }

    return builder.build();
  }

  public static PackagingConfig load(String path) throws IOException {
    String contents = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    return createFromString(contents);
  }

  public static PackagingConfig loadFromURL(URL url) throws IOException {
    String contents = Resources.toString(url, StandardCharsets.UTF_8);
    return createFromString(contents);
  }

  @SuppressWarnings("unchecked")
  private static List<String> getProtoDeps(Map<String, Object> configMap, String key) {
    List<?> deps = (List<?>) configMap.get(key);
    if (deps == null || deps.size() == 0) {
      return ImmutableList.of();
    }
    if (deps.get(0) instanceof String) {
      // TODO delete this branch once artman always passes in the structure instead of just names
      return (List<String>) deps;
    } else {
      ImmutableList.Builder<String> depStrings = ImmutableList.builder();
      for (Map<String, Object> packageData : (List<Map<String, Object>>) deps) {
        depStrings.add((String) packageData.get("name"));
      }
      return depStrings.build();
    }
  }
}
