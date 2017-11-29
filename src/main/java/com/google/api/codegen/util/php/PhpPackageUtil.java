/* Copyright 2016 Google LLC
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
package com.google.api.codegen.util.php;

import com.google.common.base.Joiner;
import java.util.Arrays;

/** Utility class for PHP to manipulate package strings. */
public class PhpPackageUtil {

  private static String PACKAGE_SEPARATOR = "\\";
  private static String PACKAGE_SPLIT_REGEX = "[\\\\]";
  private static String[] PACKAGE_PREFIX = {"Google", "Cloud"};
  private static String PACKAGE_VERSION_REGEX = "V\\d+.*";

  public static String[] getStandardPackagePrefix() {
    return PACKAGE_PREFIX;
  }

  public static String[] splitPackageName(String packageName) {
    return packageName.split(PACKAGE_SPLIT_REGEX);
  }

  public static String[] splitPackageNameWithoutStandardPrefix(String packageName) {
    String[] result = splitPackageName(packageName);
    int packageStartIndex = 0;
    // Skip common package prefix only when it is an exact match in sequence.
    for (int i = 0; i < PACKAGE_PREFIX.length && i < result.length; i++) {
      if (result[i].equals(PACKAGE_PREFIX[i])) {
        packageStartIndex++;
      } else {
        break;
      }
    }
    return Arrays.copyOfRange(result, packageStartIndex, result.length);
  }

  public static String buildPackageName(Iterable<String> components) {
    return Joiner.on(PACKAGE_SEPARATOR).join(components);
  }

  public static String buildPackageName(String... components) {
    return buildPackageName(Arrays.asList(components));
  }

  public static String getFullyQualifiedName(String packageName, String objectName) {
    return Joiner.on(PACKAGE_SEPARATOR).join(packageName, objectName);
  }

  public static boolean isPackageVersion(String versionString) {
    return versionString.matches(PACKAGE_VERSION_REGEX);
  }
}
