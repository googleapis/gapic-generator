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
package com.google.api.codegen.py;

import com.google.api.codegen.CommentPatterns;
import com.google.common.base.Splitter;
import java.util.regex.Matcher;

/** Utility class for formatting Python comments to follow Sphinx style. */
public class PythonSphinxCommentFixer {

  /** Returns a Sphinx-formatted comment string. */
  public static String sphinxify(String comment) {
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
          line = applyTransformations(line);
        }

      } else if (CommentPatterns.CODE_BLOCK_PATTERN.matcher(line).matches()) {
        inCodeBlock = true;
        line = "::\n\n" + line;

      } else {
        line = applyTransformations(line);
      }

      if (!first) {
        sb.append("\n");
      }
      first = false;
      sb.append(line.replace("\"", "\\\""));
    }
    return sb.toString().trim();
  }

  private static String applyTransformations(String line) {
    line = CommentPatterns.BACK_QUOTE_PATTERN.matcher(line).replaceAll("``");
    line = sphinxifyProtoMarkdownLinks(line);
    line = sphinxifyAbsoluteMarkdownLinks(line);
    return sphinxifyCloudMarkdownLinks(line);
  }

  /** Returns a string with all proto markdown links formatted to Sphinx style. */
  private static String sphinxifyProtoMarkdownLinks(String comment) {
    StringBuffer sb = new StringBuffer();
    Matcher m = CommentPatterns.PROTO_LINK_PATTERN.matcher(comment);
    if (!m.find()) {
      return comment;
    }
    do {
      // proto display name may contain '$' which needs to be escaped using Matcher.quoteReplacement
      m.appendReplacement(sb, Matcher.quoteReplacement(String.format("``%s``", m.group(1))));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }

  /** Returns a string with all absolute markdown links formatted to Sphinx style. */
  private static String sphinxifyAbsoluteMarkdownLinks(String comment) {
    StringBuffer sb = new StringBuffer();
    Matcher m = CommentPatterns.ABSOLUTE_LINK_PATTERN.matcher(comment);
    if (!m.find()) {
      return comment;
    }
    do {
      // absolute markdown links may contain '$' which needs to be escaped using Matcher.quoteReplacement
      m.appendReplacement(
          sb, Matcher.quoteReplacement(String.format("`%s <%s>`_", m.group(1), m.group(2))));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }

  /** Returns a string with all cloud markdown links formatted to Sphinx style. */
  private static String sphinxifyCloudMarkdownLinks(String comment) {
    StringBuffer sb = new StringBuffer();
    Matcher m = CommentPatterns.CLOUD_LINK_PATTERN.matcher(comment);
    if (!m.find()) {
      return comment;
    }
    do {
      // cloud markdown links may contain '$' which needs to be escaped using Matcher.quoteReplacement
      m.appendReplacement(
          sb,
          Matcher.quoteReplacement(
              String.format("`%s <https://cloud.google.com%s>`_", m.group(1), m.group(2))));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }
}
