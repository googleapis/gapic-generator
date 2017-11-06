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
package com.google.api.codegen.util.py;

import com.google.api.codegen.CommentPatterns;
import com.google.api.codegen.util.CommentReformatter;
import com.google.api.codegen.util.CommentTransformer;
import com.google.api.codegen.util.LinkPattern;
import com.google.common.base.Splitter;

public class PythonCommentReformatter implements CommentReformatter {

  private CommentTransformer transformer =
      CommentTransformer.newBuilder()
          .replace(CommentPatterns.BACK_QUOTE_PATTERN, "``")
          .transform(LinkPattern.PROTO.toFormat("``$TITLE``"))
          .transform(LinkPattern.ABSOLUTE.toFormat("`$TITLE <$URL>`_"))
          .transform(
              LinkPattern.RELATIVE
                  .withUrlPrefix(CommentTransformer.CLOUD_URL_PREFIX)
                  .toFormat("`$TITLE <$URL>`_"))
          .build();

  @Override
  public String reformat(String comment) {
    boolean inCodeBlock = false;
    boolean first = true;
    Iterable<String> lines = Splitter.on("\n").split(comment);
    StringBuffer sb = new StringBuffer();
    for (String line : lines) {
      if (inCodeBlock) {
        // Code blocks are either empty or indented
        if (!(line.trim().isEmpty()
            || CommentPatterns.CODE_BLOCK_PATTERN.matcher(line).matches())) {
          inCodeBlock = false;
          line = transformer.transform(line);
        }

      } else if (CommentPatterns.CODE_BLOCK_PATTERN.matcher(line).matches()) {
        inCodeBlock = true;
        line = "::\n\n" + line;

      } else {
        line = transformer.transform(line);
      }

      if (!first) {
        sb.append("\n");
      }
      first = false;
      sb.append(line.replace("\"", "\\\""));
    }
    return sb.toString().trim();
  }
}
