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
import com.google.api.codegen.TargetLanguage;
import com.google.auto.value.AutoValue;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/** This class holds defaults which are mostly used for packaging files. */
@AutoValue
public abstract class ApiDefaultsConfig {

  /** The author of the client library. */
  public abstract String author();

  /** The email of the author of the client library. */
  public abstract String email();

  /** The homepage of the client library. */
  public abstract String homepage();

  /** The name of the license of the client library. */
  public abstract String licenseName();

  /** The development status of the client library. Configured per language. */
  public ReleaseLevel releaseLevel() {
    return ReleaseLevel.ALPHA;
  }

  protected abstract Map<TargetLanguage, VersionBound> generatedNonGAPackageVersionBound();

  private static Builder newBuilder() {
    return new AutoValue_ApiDefaultsConfig.Builder();
  }

  @AutoValue.Builder
  protected abstract static class Builder {

    abstract Builder author(String val);

    abstract Builder email(String val);

    abstract Builder homepage(String val);

    abstract Builder licenseName(String val);

    abstract Builder generatedNonGAPackageVersionBound(Map<TargetLanguage, VersionBound> val);

    abstract ApiDefaultsConfig build();
  }

  @SuppressWarnings("unchecked")
  private static ApiDefaultsConfig createFromString(String yamlContents) {
    Yaml yaml = new Yaml();
    Map<String, Object> configMap = (Map<String, Object>) yaml.load(yamlContents);

    Builder builder =
        newBuilder()
            .author((String) configMap.get("author"))
            .email((String) configMap.get("email"))
            .homepage((String) configMap.get("homepage"))
            .licenseName((String) configMap.get("license"))
            .generatedNonGAPackageVersionBound(
                Configs.createVersionMap(
                    (Map<String, Map<String, String>>) configMap.get("generated_package_version")));
    return builder.build();
  }

  public static ApiDefaultsConfig load() throws IOException {
    URL apiDefaultsUrl =
        ApiDefaultsConfig.class.getResource("/com/google/api/codegen/packaging/api_defaults.yaml");
    String contents = Resources.toString(apiDefaultsUrl, StandardCharsets.UTF_8);
    return createFromString(contents);
  }
}
