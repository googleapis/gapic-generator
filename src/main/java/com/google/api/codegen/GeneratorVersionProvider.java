/* Copyright 2017 Google LLC
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
package com.google.api.codegen;

import java.io.IOException;
import java.util.Properties;

public class GeneratorVersionProvider {

  private static final String DEFAULT_VERSION = "";

  public static String getGeneratorVersion() {
    String version = DEFAULT_VERSION;
    Properties properties = new Properties();
    try {
      properties.load(
          GeneratorVersionProvider.class
              .getResourceAsStream("/com/google/api/codegen/codegen.properties"));
      version = properties.getProperty("version");
    } catch (IOException e) {
      e.printStackTrace(System.err);
    }
    return version;
  }
}
