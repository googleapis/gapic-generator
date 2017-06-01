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

import com.google.api.codegen.util.CommentReformatter;
import com.google.api.codegen.util.CommentTransformer;
import com.google.api.codegen.util.LinkPattern;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.collect.ImmutableSet;

public class JSCommentReformatter implements CommentReformatter {

  private CommentTransformer transformer =
      CommentTransformer.newBuilder()
          .transform(LinkPattern.PROTO.toFormat("{@link $TITLE}"))
          .transform(
              LinkPattern.RELATIVE
                  .withUrlPrefix(CommentTransformer.CLOUD_URL_PREFIX)
                  .toFormat("[$TITLE]($URL)"))
          .build();

  @Override
  public String reformat(String comment) {
    return transformer.transform(comment).trim();
  }

  public String getLinkedElementName(ProtoElement element) {
    if (isExternalFile(element.getFile())) {
      String fullName = element.getFullName();
      return String.format("[%s]{@link external:\"%s\"}", fullName, fullName);
    } else {
      String simpleName = element.getSimpleName();
      return String.format("[%s]{@link %s}", simpleName, simpleName);
    }
  }

  public boolean isExternalFile(ProtoFile file) {
    String filePath = file.getSimpleName();
    for (String commonPath : COMMON_PROTO_PATHS) {
      if (filePath.startsWith(commonPath)) {
        return true;
      }
    }
    return false;
  }

  private static final ImmutableSet<String> COMMON_PROTO_PATHS =
      ImmutableSet.of(
          "google/api",
          "google/bytestream",
          "google/logging/type",
          "google/longrunning",
          "google/protobuf",
          "google/rpc",
          "google/type");
}
