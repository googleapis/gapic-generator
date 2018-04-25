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
package com.google.api.codegen.util.php;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Utility class for PHP to manipulate package strings. */
public class PhpPackageUtil {

  private static String PACKAGE_SEPARATOR = "\\";
  private static String PACKAGE_SPLIT_REGEX = "[\\\\]";
  private static String PACKAGE_VERSION_REGEX = "V\\d+.*";

  public static String[] splitPackageName(String packageName) {
    if (packageName.startsWith(PACKAGE_SEPARATOR)) {
      // Remove leading "\" before splitting
      packageName = packageName.substring(PACKAGE_SEPARATOR.length());
    }
    return packageName.trim().split(PACKAGE_SPLIT_REGEX);
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

  /**
   * Remove the base package name, returning a package name which begins with the version. If no
   * version is present in the input packageName, returns null.
   */
  public static String removeBasePackageName(String packageName) {
    ArrayList<String> packageComponents = new ArrayList<>();
    List<String> pieces = Arrays.asList(PhpPackageUtil.splitPackageName(packageName));
    boolean foundVersion = false;
    for (String packageElement : Lists.reverse(pieces)) {
      packageComponents.add(packageElement);
      if (isPackageVersion(packageElement)) {
        foundVersion = true;
        break;
      }
    }
    if (foundVersion) {
      return buildPackageName(Lists.reverse(packageComponents));
    } else {
      // If we did not find a version, then the whole package name is considered
      // the base package name, and we return null.
      return null;
    }
  }

  /** Get the base package name, which includes everything before the version. */
  public static String getBasePackageName(String packageName) {
    ArrayList<String> packageComponents = new ArrayList<>();
    for (String packageElement : PhpPackageUtil.splitPackageName(packageName)) {
      if (isPackageVersion(packageElement)) {
        break;
      }
      packageComponents.add(packageElement);
    }
    return buildPackageName(packageComponents);
  }
}
