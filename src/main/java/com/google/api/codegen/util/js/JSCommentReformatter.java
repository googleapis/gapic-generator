/* Copyright 2017 Google Inc
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
package com.google.api.codegen.util.js;

import com.google.api.codegen.CommentPatterns;
import com.google.api.codegen.util.CommentReformatter;
import com.google.api.codegen.util.CommentReformatting;
import com.google.common.base.Function;
import java.util.regex.Matcher;

public class JSCommentReformatter implements CommentReformatter {

  private static Function<Matcher, String> PROTO_TO_JS_DOC =
      new Function<Matcher, String>() {
        @Override
        public String apply(Matcher matcher) {
          return Matcher.quoteReplacement(String.format("{@link %s}", matcher.group(1)));
        }
      };

  @Override
  public String reformat(String comment) {
    comment =
        CommentReformatting.reformatPattern(
            comment, CommentPatterns.PROTO_LINK_PATTERN, PROTO_TO_JS_DOC);
    comment = CommentReformatting.reformatCloudMarkdownLinks(comment, "[%s](%s)");
    return comment.trim();
  }
}
