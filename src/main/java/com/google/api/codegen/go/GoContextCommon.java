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
package com.google.api.codegen.go;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class that provides helper methods for snippet files generating Go code to get data and perform
 * data transformations that are difficult or messy to do in the snippets themselves.
 */
public class GoContextCommon {
  /** Converts the specified text into a comment block in the generated Go file. */
  public List<String> getCommentLines(String text) {
    List<String> result = new ArrayList<>();
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      result.add(line.isEmpty() ? "//" : "// " + line);
    }
    return result;
  }

  /**
   * Converts and wraps the specified text into a comment block in the generated Go file.
   *
   * <p>Lines are wrapped to 70 characters. See documentation of wrapLine for more details.
   */
  public List<String> getWrappedCommentLines(String text) {
    List<String> result = new ArrayList<>();
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      for (String wrapped : wrapLine(line, 70)) {
        result.add(line.isEmpty() ? "//" : "// " + wrapped);
      }
    }
    return result;
  }

  /**
   * wrapLine splits `line` into a list of lines around `length` long at whitespaces.
   *
   * <p>The current implementation - first splits the line into words - adds the words, separated by
   * a single space, to the current line until the line is longer than `length` - and then breaks
   * the line.
   *
   * <p>This implementation has a corner case of `line` containing a word that is by itself longer
   * than `length`. This issue crops up occasionally since many comments contain URLs. Consequently,
   * each wrapped line, except possibly the last, will be slightly longer than `length`. In
   * practice, this is not a serious problem, but this algorithm should be changed if stronger
   * guarantees are required.
   */
  private List<String> wrapLine(String line, int length) {
    if (line.length() <= length) {
      return Collections.<String>singletonList(line);
    }
    List<String> result = new ArrayList<>(line.length() / length + 1);
    StringBuilder current = new StringBuilder();
    for (String word : Splitter.onPattern("\\s").omitEmptyStrings().split(line)) {
      current.append(" ");
      current.append(word);
      if (current.length() >= length) {
        result.add(current.substring(1));
        current.setLength(0);
      }
    }
    if (current.length() > 0) {
      result.add(current.substring(1));
    }
    return result;
  }
}
