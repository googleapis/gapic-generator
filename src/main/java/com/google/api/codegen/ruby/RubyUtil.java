/* Copyright 2017 Google LLC
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
package com.google.api.codegen.ruby;

import com.google.api.codegen.util.VersionMatcher;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RubyUtil {
  private static final String LONGRUNNING_PACKAGE_NAME = "Google::Longrunning";

  private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("(.+?)::([vV][0-9]+)(::.*)?");

  public static boolean isLongrunning(String packageName) {
    return packageName.equals(LONGRUNNING_PACKAGE_NAME);
  }

  public static boolean hasMajorVersion(String packageName) {
    Matcher m = getVersionMatcher(packageName);
    if (m.matches()) {
      return VersionMatcher.isVersion(m.group(2));
    }
    return false;
  }

  /**
   * Get a matches to check for the presence of a version number. If a match is found, the gem name
   * is in group 1 and the version is in group 2.
   *
   * @param packageName
   * @return
   */
  public static Matcher getVersionMatcher(String packageName) {
    return IDENTIFIER_PATTERN.matcher(packageName);
  }

  public static String getSentence(List<String> lines) {
    StringBuilder builder = new StringBuilder();
    for (String line : lines) {
      if (findSentenceEnd(line.trim(), builder)) {
        break;
      }
    }
    return builder.toString().trim();
  }

  private static boolean findSentenceEnd(String line, StringBuilder builder) {
    int startIndex = 0;
    while (startIndex < line.length()) {
      int dotIndex = line.indexOf(".", startIndex);
      if (dotIndex < 0) {
        builder.append(line.substring(startIndex)).append(" ");
        return false;
      }

      builder.append(line.substring(startIndex, dotIndex + 1));
      startIndex = dotIndex + 1;
      if (startIndex == line.length() || line.charAt(startIndex) == ' ') {
        return true;
      }
    }

    return builder.length() > 0;
  }
}
