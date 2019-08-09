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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.yaml.snakeyaml.Yaml;

/** Represents the packaging config necessary to run package generation for an API. */
@AutoValue
public abstract class PackagingConfig {
  private static final Pattern COMMON_PKG_PATTERN =
      Pattern.compile("(?i)(?<org>(\\w+\\.){0,2})(?<name>\\w+)\\.(?<ver>\\w*\\d+[\\w\\d]*)(\\.|$)");
  private static final Pattern NAME_ONLY_PATTERN =
      Pattern.compile("(?i)(^|\\.)(?<name>[a-zA-Z]+)(\\.\\w*)$");

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

  @VisibleForTesting
  static Builder newBuilder() {
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

  // This should be eventually removed (as well as the whole PackagingConfig itself).
  //
  // Construct PackagingConfig when there is no explicit packaging config provided (i.e.
  // no --package_yaml2 command line argument).
  //
  // Parse interfaces.name value from gapic.yaml to calculate most of the
  // PackageConfig values. For the conditional dependencies portion of the PackageConfig
  // (proto_deps and test_proto_deps in package_yaml2) calculate it based on the
  // methods defined in the product config (gapic_yaml) (i.e. if there are IAM methods,
  // add IAM dependency).
  //
  // Note, so far IAM is the only possible conditional dependency, also IAM in general is very
  // common and is considered as "core" functionality so it makes sense to support it natively in
  // the gapic-generator (i.e. to determine in code if IAM dependency is needed instead of treating
  // it as a configuration parameter).
  //
  // This method calculates the package_yaml2 file values as follows:
  // 1) Take interfaceName from language_settings.name from gapic_yaml.
  // 2) Convert interfaceName string to lower case.
  // 3) Apply COMMON_PKG_PATTERN regexp to interfaceName.
  // 4) Set "name" capturing group as api_name value.
  // 5) Set "ver" capturing group as api_version value (or use "v1" if none found).
  // 6) Set "org" capturing group as organization_name (or use "google-cloud" if none found).
  // 7) Always add "google-common-protos" to proto_deps lists.
  // 8) Add "google-iam-v1" to proto_deps and test_proto_deps lists if interfaceConfigMap defines
  //    any IAM methods and it is not IAM client itself (i.e. IAM client should not depend on
  //    itself).
  // 9) Set release level to ALPHA or BETA if api_version contains "alpha" or "beta"
  //    substrings respectively. Set release level to UNSET_RELEASE_LEVEL otherwise (i.e. it will
  //    set UNSET_RELEASE_LEVEL even if the version is "v1"). This will make gapic-generator to
  //    choose the release level from gapic.yaml instead (if present).
  // 10) Always set artifact_type to GAPIC (this value is basically obsolete).
  // 11) Calculate proto_path as interfaceName without the last element (i.e. only package portion
  //    of the full interfaceName) where all '.' characters are replaced with '/' character.
  public static PackagingConfig loadFromProductConfig(
      Map<String, ? extends InterfaceConfig> interfaceConfigMap) {
    String name = "";
    String org = "google-cloud";
    String ver = "v1";
    String protoPath = "";

    ImmutableSet.Builder<String> depsBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<String> testDepsBuilder = ImmutableSet.builder();
    depsBuilder.add("google-common-protos");
    for (Map.Entry<String, ? extends InterfaceConfig> config : interfaceConfigMap.entrySet()) {
      String interfaceName = config.getKey().toLowerCase();
      if (name.isEmpty()) {
        int lastDotIndex = interfaceName.lastIndexOf('.');
        protoPath = lastDotIndex > 0 ? interfaceName.substring(0, lastDotIndex) : interfaceName;
        protoPath = protoPath.replace('.', '/');

        Matcher m = COMMON_PKG_PATTERN.matcher(interfaceName);
        if (!m.find()) {
          m = NAME_ONLY_PATTERN.matcher(interfaceName);
          name = !m.find() ? interfaceName : m.group("name");
        } else {
          name = m.group("name");
          org = m.group("org");
          ver = m.group("ver");
        }
      }

      if (config.getValue().hasIamMethods() && !interfaceName.contains("iam")) {
        depsBuilder.add("google-iam-v1");
        testDepsBuilder.add("google-iam-v1");
      }
    }

    org = org.replace('.', '-');
    while (org.endsWith("-")) {
      org = org.substring(0, org.length() - 1);
    }
    if (org.equals("google")) {
      org += "-cloud";
    }

    ReleaseLevel releaseLevel = ReleaseLevel.UNSET_RELEASE_LEVEL;
    if (ver.contains("alpha")) {
      releaseLevel = ReleaseLevel.ALPHA;
    } else if (ver.contains("beta")) {
      releaseLevel = ReleaseLevel.BETA;
    } else if (!ver.isEmpty()) {
      releaseLevel = ReleaseLevel.GA;
    }

    Builder builder =
        newBuilder()
            .apiName(name)
            .apiVersion(ver)
            .organizationName(org)
            .protoPackageDependencies(depsBuilder.build().asList())
            .protoPackageTestDependencies(testDepsBuilder.build().asList())
            .releaseLevel(releaseLevel)
            .artifactType(PackagingArtifactType.GAPIC)
            .protoPath(protoPath);

    return builder.build();
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
