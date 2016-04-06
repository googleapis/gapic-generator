/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http: *www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gapi.vgen.go;

import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that provides helper methods for snippet files generating Go code
 * to get data and perform data transformations that are difficult or messy to do
 * in the snippets themselves.
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
}
