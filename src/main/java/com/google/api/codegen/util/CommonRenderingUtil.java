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
package com.google.api.codegen.util;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility class to process text in the templates. */
public class CommonRenderingUtil {
  private static Pattern singleQuoteStringPattern = Pattern.compile("'([^\\\']*)'");
  private static Pattern doubleQuoteStringPattern = Pattern.compile("\"([^\\\"]*)\"");

  /** Strips the surrounding quotes from the given string */
  public static String stripQuotes(String value) {
    Matcher singleQuoteMatcher = singleQuoteStringPattern.matcher(value);
    Matcher doubleQuoteMatcher = doubleQuoteStringPattern.matcher(value);
    if (singleQuoteMatcher.matches()) {
      value = singleQuoteMatcher.group(1);
    } else if (doubleQuoteMatcher.matches()) {
      value = doubleQuoteMatcher.group(1);
    }
    return value;
  }

  /** Returns the input text split on newlines. */
  public static List<String> getDocLines(String text) {
    // TODO: Convert markdown to language-specific doc format.
    // https://github.com/googleapis/toolkit/issues/331
    List<String> result = Splitter.on(String.format("%n")).splitToList(text);
    return result.size() == 1 && result.get(0).isEmpty() ? ImmutableList.<String>of() : result;
  }

  /**
   * Returns the input text split on newlines and maxWidth.
   *
   * <p>maxWidth includes the ending newline.
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

  /** Returns the index on which to insert a newline given maxWidth. */
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
   * <p>The set includes whitespace characters, '(', and '['.
   */
  private static boolean isLineWrapChar(char c) {
    return Character.isWhitespace(c) || "([".indexOf(c) >= 0;
  }

  /**
   * Creates a whitespace string of the specified width.
   *
   * @param width number of spaces
   * @return padding whitespace
   */
  public static String padding(int width) {
    return Strings.repeat(" ", width);
  }

  /**
   * Helper function for referencing integers from templates.
   *
   * @param value value
   * @return int value
   */
  public static int toInt(String value) {
    return Integer.valueOf(value);
  }

  /** Checks for the existence of a key (the given key will be coerced to a String). */
  public static <K> boolean hasKey(Map<String, String> map, K key) {
    return key != null ? map.containsKey(key.toString()) : false;
  }

  /** Gets the value of a key (the given key will be coerced to a String). */
  public static <K> String getValueForKey(Map<String, String> map, K key) {
    return key != null ? map.get(key.toString()) : "";
  }
}
