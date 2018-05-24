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
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Utility methods used by classes which read packaging-related config (e.g. {@link
 * ApiDefaultsConfig}, {@link DependenciesConfig}, {@link PackagingConfig}, and {@link
 * PackageMetadataConfig}.
 */
class Configs {

  private static final String CONFIG_KEY_DEFAULT = "default";

  private Configs() {}

  static Map<TargetLanguage, VersionBound> createVersionMap(
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

  static ReleaseLevel parseReleaseLevel(String releaseLevelName) {
    if (releaseLevelName == null) {
      return null;
    }
    return Enum.valueOf(ReleaseLevel.class, releaseLevelName.toUpperCase());
  }

  /**
   * Transforms entries of the input map into TargetLanguage, taking into account an optional
   * default setting.
   */
  static <V> Map<TargetLanguage, V> buildMapWithDefault(Map<String, V> inputMap) {
    Map<TargetLanguage, V> outputMap = new HashMap<>();
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
