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
package com.google.api.codegen.util.java;

import com.google.api.codegen.util.CommonRenderingUtil;
import java.util.Arrays;
import java.util.List;

/** Utility class for Java to process text in the templates. */
public class JavaRenderingUtil {
  /**
   * Splits given text into lines and returns an list of strings, each one representing a line.
   * Performs escaping of certain HTML characters.
   */
  public static List<String> getDocLines(String text) {
    return CommonRenderingUtil.getDocLines(new JavaCommentReformatter().reformat(text));
  }

  /**
   * Splits given text into lines and returns an list of strings, each one representing a line,
   * without escaping HTML characters.
   */
  public static List<String> splitLines(String text) {
    return CommonRenderingUtil.getDocLines(text);
  }

  public List<String> getMultilineHeading(String heading) {
    final char[] array = new char[heading.length()];
    Arrays.fill(array, '=');
    String eqsString = new String(array);
    return Arrays.asList(eqsString, heading, eqsString);
  }
}
