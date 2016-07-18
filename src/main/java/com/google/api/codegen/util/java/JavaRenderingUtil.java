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
package com.google.api.codegen.util.java;

import com.google.common.base.Splitter;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for Java to process text in the templates.
 */
public class JavaRenderingUtil {
  /**
   * Escaper for formatting javadoc strings.
   */
  private static final Escaper JAVADOC_ESCAPER =
      Escapers.builder()
          .addEscape('&', "&amp;")
          .addEscape('<', "&lt;")
          .addEscape('>', "&gt;")
          .addEscape('*', "&ast;")
          .build();

  /**
   * Splits given text into lines and returns an list of strings, each one representing a line.
   * Performs escaping of certain html characters.
   */
  public static List<String> getDocLines(String text) {
    // TODO: convert markdown to javadoc
    List<String> result = new ArrayList<>();
    text = JAVADOC_ESCAPER.escape(text);
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      result.add(line);
    }
    return result;
  }
}
