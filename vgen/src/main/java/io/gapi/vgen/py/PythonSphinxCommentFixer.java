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
package io.gapi.vgen.py;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for formatting python comments to follow Sphinx style.
 */
public class PythonSphinxCommentFixer {

  private static final Pattern SINGLE_BACK_QUOTE_PATTERN = Pattern.compile("(?<!`)`(?!`)");
  private static final Pattern CLOUD_LINK_PATTERN =
      Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
  private static final Pattern PROTO_LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\[[^\\]]+\\]");

  /**
   * Returns a Sphinx-formatted comment string.
   */
  public static String sphinxify(String comment) {
    comment = SINGLE_BACK_QUOTE_PATTERN.matcher(comment).replaceAll("``");
    comment = sphinxifyProtoMarkdownLinks(comment);
    return sphinxifyCloudMarkdownLinks(comment);
  }

  /**
   * Returns a string with all proto markdown links formatted to Sphinx style.
   */
  private static String sphinxifyProtoMarkdownLinks(String comment) {
    StringBuffer sb = new StringBuffer();
    Matcher m = PROTO_LINK_PATTERN.matcher(comment);
    if (!m.find()) {
      return comment;
    }
    do {
      m.appendReplacement(sb, String.format("``%s``", m.group(1)));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }

  /**
   * Returns a string with all cloud markdown links formatted to Sphinx style.
   */
  private static String sphinxifyCloudMarkdownLinks(String comment) {
    StringBuffer sb = new StringBuffer();
    Matcher m = CLOUD_LINK_PATTERN.matcher(comment);
    if (!m.find()) {
      return comment;
    }
    do {
      m.appendReplacement(
          sb, String.format("`%s <https://cloud.google.com%s>`_", m.group(1), m.group(2)));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }
}
