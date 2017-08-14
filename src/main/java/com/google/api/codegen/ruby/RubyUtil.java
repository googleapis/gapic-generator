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
package com.google.api.codegen.ruby;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class RubyUtil {
  private static final String LONGRUNNING_PACKAGE_NAME = "Google::Longrunning";
  private static final String RUBY_PACKAGE_SEPERATOR = "::";

  private static final Pattern VERSION_PATTERN = Pattern.compile("^[vV]\\d+");

  public static boolean isLongrunning(String packageName) {
    return packageName.equals(LONGRUNNING_PACKAGE_NAME);
  }

  public static boolean hasMajorVersion(String packageName) {
    List<String> parts = Arrays.asList(packageName.split(RUBY_PACKAGE_SEPERATOR));
    String lastPart = parts.get(parts.size() - 1);
    return VERSION_PATTERN.matcher(lastPart).matches();
  }
}
