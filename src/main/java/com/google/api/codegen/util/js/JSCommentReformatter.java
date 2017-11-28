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
package com.google.api.codegen.util.js;

import com.google.api.codegen.util.CommentReformatter;
import com.google.api.codegen.util.CommentTransformer;
import com.google.api.codegen.util.LinkPattern;
import com.google.api.tools.framework.model.ProtoElement;
import java.util.regex.Pattern;

public class JSCommentReformatter implements CommentReformatter {
  public static final Pattern CLOSE_COMMENT_PATTERN = Pattern.compile("\\*/");

  private CommentTransformer transformer =
      CommentTransformer.newBuilder()
          // TODO(landrito): Fix the linking semantics to follow the packageName.typeName
          // links like the getLinkedElementName method below. We will probably need to pass this
          // class something that can lookup the linked proto and converts that to the correct link.
          .transform(LinkPattern.PROTO.toFormat("$TITLE"))
          .transform(
              LinkPattern.RELATIVE
                  .withUrlPrefix(CommentTransformer.CLOUD_URL_PREFIX)
                  .toFormat("[$TITLE]($URL)"))
          .replace(CLOSE_COMMENT_PATTERN, "&#42;/")
          .build();

  @Override
  public String reformat(String comment) {
    return transformer.transform(comment).trim();
  }

  public String getLinkedElementName(ProtoElement element) {
    String simpleName = element.getSimpleName();
    String packageName = element.getFile().getFullName();
    return String.format("[%s]{@link %s.%s}", simpleName, packageName, simpleName);
  }
}
