/* Copyright 2019 Google LLC
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
package com.google.api.codegen.util;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.Yaml;

public class SampleConfigSanitizer {

  private SampleConfigSanitizer() {
    // utility class
  }

  private static final String TYPE_FIELD_NAME = "type";
  private static final String SCHEMA_VERSION_FIELD_NAME = "schema_version";
  private static final String VALID_TYPE_VALUE =
      "com.google.api.codegen.samplegen.v1p2.SampleConfigProto";
  private static final String VALID_SCHEMA_VERSION = "1.2.0";

  // Filter out sample configure files without proper type definition.
  public static List<String> sanitize(List<String> sampleConfigFileNames) {
    return sampleConfigFileNames
        .stream()
        .filter(SampleConfigSanitizer::isValidSampleConfig)
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  private static boolean isValidSampleConfig(String sampleConfigFileName) {
    try (Reader reader = new FileReader(sampleConfigFileName)) {
      Yaml yaml = new Yaml();
      for (Object rawData : yaml.loadAll(reader)) {
        Map<String, Object> data = (Map<String, Object>) rawData;
        if (!VALID_TYPE_VALUE.equals(data.get(TYPE_FIELD_NAME))
            || !VALID_SCHEMA_VERSION.equals(data.get(SCHEMA_VERSION_FIELD_NAME))) {
          return false;
        }
      }
      return true;
    } catch (IOException e) {
      throw new IllegalArgumentException(
          String.format("sample config file not found: %s", sampleConfigFileName));
    }
  }
}
