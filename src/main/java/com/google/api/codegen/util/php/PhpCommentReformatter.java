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
package com.google.api.codegen.util.php;

import com.google.api.codegen.util.CommentReformatter;
import com.google.api.codegen.util.CommentTransformer;
import com.google.api.codegen.util.LinkPattern;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.regex.Pattern;

public class PhpCommentReformatter implements CommentReformatter {
  public static final Pattern CLOSE_COMMENT_PATTERN = Pattern.compile("\\*/");
  public static final List<String> VALID_COMMENT_TAGS =
      ImmutableList.<String>builder().add("see").build();
  public static final Pattern AT_SYMBOL_PATTERN =
      Pattern.compile(String.format("@(?!%s)", Joiner.on("\\s|").join(VALID_COMMENT_TAGS) + "\\s"));

  private CommentTransformer transformer =
      CommentTransformer.newBuilder()
          .replace(CLOSE_COMMENT_PATTERN, "&#42;/")
          .replace(AT_SYMBOL_PATTERN, "&#64;")
          .transform(
              LinkPattern.RELATIVE
                  .withUrlPrefix(CommentTransformer.CLOUD_URL_PREFIX)
                  .toFormat("[$TITLE]($URL)"))
          .build();

  @Override
  public String reformat(String comment) {
    return transformer.transform(comment).trim();
  }
}
