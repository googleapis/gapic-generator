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
package com.google.api.codegen.util.py;

import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class PythonRenderingUtil {
  public List<String> trimDocs(List<String> docLines) {
    // TODO (geigerj): "trimming" the docs means we don't support code blocks. Investigate
    // supporting code blocks here.
    ImmutableList.Builder<String> trimmedDocLines = ImmutableList.builder();
    for (int i = 0; i < docLines.size(); ++i) {
      String line = docLines.get(i).trim();
      if (line.equals("::")) {
        ++i;
      } else {
        trimmedDocLines.add(line);
      }
    }
    return trimmedDocLines.build();
  }

  public List<String> getDocLines(String text) {
    return CommonRenderingUtil.getDocLines(text);
  }

  public String sectionHeaderLine(String title) {
    return headerLine(title, '=');
  }

  public String subSectionHeaderLine(String title) {
    return headerLine(title, '-');
  }

  public String subSubSectionHeaderLine(String title) {
    return headerLine(title, '^');
  }

  @SuppressWarnings("InvalidPatternSyntax")
  private String headerLine(String title, char c) {
    return title.replaceAll(".", String.valueOf(c));
  }

  public String exampleLine(String str) {
    if (!Strings.isNullOrEmpty(str) && Character.isWhitespace(str.charAt(0))) {
      return "... " + str;
    }
    return ">>> " + str;
  }

  /** @see CommonRenderingUtil#padding(int) */
  public static String padding(int width) {
    return CommonRenderingUtil.padding(width);
  }

  /** @see CommonRenderingUtil#toInt(String) */
  public static int toInt(String value) {
    return CommonRenderingUtil.toInt(value);
  }
}
