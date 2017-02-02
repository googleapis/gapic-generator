/* Copyright 2016 Google Inc
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
package com.google.api.codegen.nodejs;

import com.google.api.codegen.CommentPatterns;
import java.util.regex.Matcher;

/** Utility class for formatting source comments to follow JSDoc style. */
public class JSDocCommentFixer {

  /** Returns a JSDoc-formatted comment string. */
  public static String jsdocify(String comment) {
    comment = jsdocifyProtoMarkdownLinks(comment);
    comment = jsdocifyCloudMarkdownLinks(comment);
    return comment.trim();
  }

  /** Returns a string with all proto markdown links formatted to JSDoc style. */
  private static String jsdocifyProtoMarkdownLinks(String comment) {
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
  private static String jsdocifyCloudMarkdownLinks(String comment) {
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
}
