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
package io.gapi.vgen.ruby;

import com.google.common.base.Splitter;

import java.lang.Character;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for formatting source comments to follow RDoc style.
 * @todo extract the common interface with PythonSphinxCommentFixer.
 */
public class RDocCommentFixer {

  private static final Pattern BACK_QUOTE_PATTERN = Pattern.compile(
      "(?<!`)``?(?!`)");
  private static final Pattern CLOUD_LINK_PATTERN = Pattern.compile(
      "\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
  private static final Pattern PROTO_LINK_PATTERN = Pattern.compile(
      "\\[([^\\]]+)\\]\\[[^\\]]*\\]");
  private static final Pattern HEADLINE_PATTERN = Pattern.compile(
      "^#+", Pattern.MULTILINE);

  /**
   * Returns a Sphinx-formatted comment string.
   */
  public static String rdocify(String comment) {
    comment = BACK_QUOTE_PATTERN.matcher(comment).replaceAll("+");
    comment = rdocifyProtoMarkdownLinks(comment);
    comment = rdocifyCloudMarkdownLinks(comment);
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
        result += (isFirstSegment ? "" : "::") +
            Character.toUpperCase(firstChar) + name.substring(1);
      }
      isFirstSegment = false;
    }
    return result;
  }

  /**
   * Returns a string with all proto markdown links formatted to RDoc style.
   */
  private static String rdocifyProtoMarkdownLinks(String comment) {
    StringBuffer sb = new StringBuffer();
    Matcher m = PROTO_LINK_PATTERN.matcher(comment);
    if (!m.find()) {
      return comment;
    }
    do {
      m.appendReplacement(sb, String.format("%s", protoToRubyDoc(m.group(1))));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }

  /**
   * Returns a string with all cloud markdown links formatted to Sphinx style.
   */
  private static String rdocifyCloudMarkdownLinks(String comment) {
    StringBuffer sb = new StringBuffer();
    Matcher m = CLOUD_LINK_PATTERN.matcher(comment);
    if (!m.find()) {
      return comment;
    }
    do {
      String url = m.group(2);
      if (url.indexOf("http") != 0) {
        url = "https://cloud.google.com" + url;
      }
      m.appendReplacement(
          sb, String.format("{%s}[%s]", m.group(1), url));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }

  private static String rdocifyHeadline(String comment) {
    StringBuffer sb = new StringBuffer();
    Matcher m = HEADLINE_PATTERN.matcher(comment);
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
