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
package com.google.api.codegen.java;

import com.google.common.base.CharMatcher;

public class JavaUtil {

  private JavaUtil() {
    // Utility class
  }

  public static String getGapicArtifactDirectoryName(String packageName) {
    return "gapic-" + CharMatcher.is('.').replaceFrom(packageName, '-');
  }

  public static String getSampleArtifactDirectoryName(String packageName) {
    return "sample-" + CharMatcher.is('.').replaceFrom(packageName, '-');
  }
}
