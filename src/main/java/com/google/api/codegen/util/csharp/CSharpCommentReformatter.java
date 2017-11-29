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

package com.google.api.codegen.util.csharp;

import com.google.api.codegen.util.CommentReformatter;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

public class CSharpCommentReformatter implements CommentReformatter {

  private static final Escaper CSHARP_ESCAPER =
      Escapers.builder()
          .addEscape('&', "&amp;")
          .addEscape('<', "&lt;")
          .addEscape('>', "&gt;")
          .build();

  @Override
  public String reformat(String comment) {
    return CSHARP_ESCAPER.escape(comment).trim();
  }
}
