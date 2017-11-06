/* Copyright 2017 Google LLC
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

import com.google.api.codegen.util.CommentReformatter;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

public class JavaCommentReformatter implements CommentReformatter {
  /** Escaper for formatting javadoc strings. */
  private static final Escaper JAVADOC_ESCAPER =
      Escapers.builder()
          .addEscape('&', "&amp;")
          .addEscape('<', "&lt;")
          .addEscape('>', "&gt;")
          .addEscape('*', "&#42;")
          .addEscape('@', "{@literal @}")
          .build();

  @Override
  public String reformat(String comment) {
    // TODO: convert markdown to javadoc
    // https://github.com/googleapis/toolkit/issues/331
    return JAVADOC_ESCAPER.escape(comment).trim();
  }
}
