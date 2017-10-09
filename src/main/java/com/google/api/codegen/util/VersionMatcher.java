/* Copyright 2017 Google Inc
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
package com.google.api.codegen.util;

import java.util.regex.Pattern;

public class VersionMatcher {
  private static final Pattern VERSION_PATTERN =
      Pattern.compile(
          "^([vV]\\d+)" // Major version eg: v1
              + "([pP_]\\d+)?" // Point release eg: p2
              + "(([aA]lpha|[bB]eta)\\d*)?"); //  Release level eg: alpha3

  public static boolean isVersion(String str) {
    return VERSION_PATTERN.matcher(str).matches();
  }
}
