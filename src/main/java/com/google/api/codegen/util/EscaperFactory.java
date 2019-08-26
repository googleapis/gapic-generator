/* Copyright 2019 Google LLC
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

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

/** The factory class that creates escapers for different languages. */
public class EscaperFactory {

  private static final Escaper doubleQuoteEscaper =
      newBaseEscapersBuilder().addEscape('"', "\\\"").build();
  private static final Escaper singleQuoteEscaper =
      newBaseEscapersBuilder().addEscape('\'', "\\'").build();
  private static final Escaper cliEscaper =
      newBaseEscapersBuilder().addEscape('`', "\\`").addEscape('"', "\\\"").build();

  public static Escaper getSingleQuoteEscaper() {
    return singleQuoteEscaper;
  }

  public static Escaper getDoubleQuoteEscaper() {
    return doubleQuoteEscaper;
  }

  public static Escaper getCliEscaper() {
    return cliEscaper;
  }

  public static Escapers.Builder newBaseEscapersBuilder() {
    return Escapers.builder()
        .addEscape('\\', "\\\\")
        .addEscape('\b', "\\b")
        .addEscape('\n', "\\n")
        .addEscape('\r', "\\r")
        .addEscape('\t', "\\t");
  }
}
