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
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.collect.ImmutableSet;
import java.util.regex.Matcher;

public class JSCommentReformatter implements CommentReformatter {
  @Override
  public String reformat(String comment) {
    comment = reformatProtoMarkdownLinks(comment);
    comment = reformatCloudMarkdownLinks(comment);
    return comment.trim();
  }

  /** Returns a string with all proto markdown links formatted to JSDoc style. */
  private String reformatProtoMarkdownLinks(String comment) {
    StringBuffer sb = new StringBuffer();
    Matcher m = CommentPatterns.PROTO_LINK_PATTERN.matcher(comment);
    if (!m.find()) {
      return comment;
    }
    do {
      // proto display name may contain '$' which needs to be escaped using Matcher.quoteReplacement
      m.appendReplacement(sb, Matcher.quoteReplacement(String.format("{@link %s}", m.group(1))));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }

  /** Returns a string with all cloud markdown links formatted to JSDoc style. */
  private String reformatCloudMarkdownLinks(String comment) {
    StringBuffer sb = new StringBuffer();
    Matcher m = CommentPatterns.CLOUD_LINK_PATTERN.matcher(comment);
    if (!m.find()) {
      return comment;
    }
    do {
      String url = "https://cloud.google.com" + m.group(2);
      // cloud markdown links may contain '$' which needs to be escaped using Matcher.quoteReplacement
      m.appendReplacement(sb, Matcher.quoteReplacement(String.format("[%s](%s)", m.group(1), url)));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
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
