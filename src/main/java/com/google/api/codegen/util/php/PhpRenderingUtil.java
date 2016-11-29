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
package com.google.api.codegen.util.php;

import com.google.common.base.Splitter;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for PHP to process text in the templates. It is necessary to escape special
 * characters used in DocBlocks, including @ and *.
 *
 * <p>TODO(michaelbausor): update these escape chars once PHP documentation style is finalized. See
 * issue https://github.com/googleapis/toolkit/issues/768
 */
public class PhpRenderingUtil {
  /** Escaper for formatting PHP doc strings. */
  private static final Escaper PHP_ESCAPER =
      Escapers.builder().addEscape('*', "&#42;").addEscape('@', "&#64;").build();

  /**
   * Splits given text into lines and returns an list of strings, each one representing a line.
   * Performs escaping of '@' and '*' characters.
   */
  public static List<String> getDocLines(String text) {
    List<String> result = new ArrayList<>();
    text = PHP_ESCAPER.escape(text);
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      result.add(line);
    }
    return result;
  }
}
