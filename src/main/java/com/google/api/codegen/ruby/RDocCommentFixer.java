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
package com.google.api.codegen.ruby;

import com.google.api.codegen.CommentPatterns;
import com.google.common.base.Splitter;
import java.util.regex.Matcher;

/** Utility class for formatting source comments to follow RDoc style. */
public class RDocCommentFixer {

  /** Returns a RDoc-formatted comment string. */
  public static String rdocify(String comment) {
    comment = CommentPatterns.BACK_QUOTE_PATTERN.matcher(comment).replaceAll("+");
    comment = rdocifyProtoMarkdownLinks(comment);
    comment = rdocifyCloudMarkdownLinks(comment);
    comment = rdocifyAbsoluteMarkdownLinks(comment);
    comment = rdocifyHeadline(comment);
    return cleanupTrailingWhitespaces(comment);
  }

  private static String protoToRubyDoc(String comment) {
    boolean messageFound = false;
    boolean isFirstSegment = true;
    String result = "";
    for (String name : Splitter.on(".").splitToList(comment)) {
      char firstChar = name.charAt(0);
      if (Character.isUpperCase(firstChar)) {
        messageFound = true;
        result += (isFirstSegment ? "" : "::") + name;
      } else if (messageFound) {
        // Lowercase segment after message is found is field.
        // In Ruby, it is referred as "Message#field" format.
        result += "#" + name;
      } else {
        result +=
            (isFirstSegment ? "" : "::") + Character.toUpperCase(firstChar) + name.substring(1);
      }
      isFirstSegment = false;
    }
    return result;
  }

  /** Returns a string with all proto markdown links formatted to RDoc style. */
  private static String rdocifyProtoMarkdownLinks(String comment) {
    StringBuffer sb = new StringBuffer();
    Matcher m = CommentPatterns.PROTO_LINK_PATTERN.matcher(comment);
    if (!m.find()) {
      return comment;
    }
    do {
      // proto display name may contain '$' which needs to be escaped using Matcher.quoteReplacement
      m.appendReplacement(
          sb, Matcher.quoteReplacement(String.format("%s", protoToRubyDoc(m.group(1)))));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }

  /** Returns a string with all cloud markdown links formatted to RDoc style. */
  private static String rdocifyCloudMarkdownLinks(String comment) {
    StringBuffer sb = new StringBuffer();
    Matcher m = CommentPatterns.CLOUD_LINK_PATTERN.matcher(comment);
    if (!m.find()) {
      return comment;
    }
    do {
      String url = "https://cloud.google.com" + m.group(2);
      // cloud markdown links may contain '$' which needs to be escaped using Matcher.quoteReplacement
      m.appendReplacement(sb, Matcher.quoteReplacement(String.format("{%s}[%s]", m.group(1), url)));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }

  /** Returns a string with all absolute markdown links formatted to RDoc style. */
  private static String rdocifyAbsoluteMarkdownLinks(String comment) {
    StringBuffer sb = new StringBuffer();
    Matcher m = CommentPatterns.ABSOLUTE_LINK_PATTERN.matcher(comment);
    if (!m.find()) {
      return comment;
    }
    do {
      // absolute markdown links may contain '$' which needs to be escaped using Matcher.quoteReplacement
      m.appendReplacement(
          sb, Matcher.quoteReplacement(String.format("{%s}[%s]", m.group(1), m.group(2))));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }

  private static String rdocifyHeadline(String comment) {
    StringBuffer sb = new StringBuffer();
    Matcher m = CommentPatterns.HEADLINE_PATTERN.matcher(comment);
    if (!m.find()) {
      return comment;
    }
    do {
      m.appendReplacement(sb, m.group().replace("#", "="));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }

  private static String cleanupTrailingWhitespaces(String comment) {
    return comment.trim();
  }
}
