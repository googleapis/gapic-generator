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

import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.VersionMatcher;
import java.util.List;

public class RubyUtil {
  private static final String LONGRUNNING_PACKAGE_NAME = "Google::Longrunning";

  public static boolean isLongrunning(String packageName) {
    return packageName.equals(LONGRUNNING_PACKAGE_NAME);
  }

  public static boolean hasMajorVersion(String packageName) {
    return VersionMatcher.isVersion(NamePath.doubleColoned(packageName).getHead());
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
