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
 * A class that provides helper methods for snippet files generating Go code to get data and
 * perform data transformations that are difficult or messy to do in the snippets themselves.
 */
public class GoContextCommon {
  /**
   * Converts the specified text into a comment block in the generated Go file.
   */
  public Iterable<String> getCommentLines(String text) {
    List<String> result = new ArrayList<>();
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      result.add(line.isEmpty() ? "//" : "// " + line);
    }
    return result;
  }

  public Iterable<String> getCommentLinesWrap(String text) {
    List<String> result = new ArrayList<>();
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      for (String wrapped : wrapLine(line, 70)) {
        result.add(line.isEmpty() ? "//" : "// " + wrapped);
      }
    }
    return result;
  }

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
