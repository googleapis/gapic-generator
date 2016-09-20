/* Copyright 2016 Google Inc
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

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to process text in the templates.
 */
public class CommonRenderingUtil {

  /**
   * Returns the input text split on newlines.
   */
  public static List<String> getDocLines(String text) {
    // TODO: Convert markdown to language-specific doc format.
    // https://github.com/googleapis/toolkit/issues/331
    List<String> result = new ArrayList<>();
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      result.add(line);
    }
    return result;
  }

  /**
   * Returns the input text split on newlines and maxWidth.
   *
   * maxWidth includes the ending newline.
   */
  public static List<String> getDocLines(String text, int maxWidth) {
    maxWidth = maxWidth - 1;
    List<String> lines = new ArrayList<>();
    for (String line : text.trim().split("\n")) {
      line = line.trim();
      while (line.length() > maxWidth) {
        int split = lineWrapIndex(line, maxWidth);
        lines.add(line.substring(0, split).trim());
        line = line.substring(split).trim();
      }
      if (!line.isEmpty()) {
        lines.add(line);
      }
    }
    return lines;
  }

  /**
   * Returns the index on which to insert a newline given maxWidth.
   */
  private static int lineWrapIndex(String line, int maxWidth) {
    for (int i = maxWidth; i > 0; i--) {
      if (isLineWrapChar(line.charAt(i))) {
        return i;
      }
    }
    for (int i = maxWidth + 1; i < line.length(); i++) {
      if (isLineWrapChar(line.charAt(i))) {
        return i;
      }
    }
    return line.length();
  }

  /**
   * Returns true if c is a character that should be wrapped on.
   *
   * The set includes whitespace characters, '(', and '['.
   */
  private static boolean isLineWrapChar(char c) {
    return Character.isWhitespace(c) || "([".indexOf(c) >= 0;
  }
}
