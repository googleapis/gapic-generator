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
package com.google.api.codegen.ruby;

import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.VersionMatcher;

public class RubyUtil {
  private static final String GOOGLE_CLOUD_PACKAGE_NAME = "Google::Cloud";
  private static final String LONGRUNNING_PACKAGE_NAME = "Google::Longrunning";

  public static boolean isLongrunning(String packageName) {
    return packageName.equals(LONGRUNNING_PACKAGE_NAME);
  }

  public static boolean isGoogleCloudPackage(String packageName) {
    return packageName.startsWith(GOOGLE_CLOUD_PACKAGE_NAME);
  }

  public static boolean hasMajorVersion(String packageName) {
    return VersionMatcher.isVersion(NamePath.doubleColoned(packageName).getHead());
  }
}
