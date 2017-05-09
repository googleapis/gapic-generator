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
package com.google.api.codegen.util;

import com.google.api.codegen.CommentPatterns;
import com.google.common.base.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommentReformatter {

  public static String CLOUD_URL_PREFIX = "https://cloud.google.com";

  private String comment;

  private CommentReformatter(String comment) {
    this.comment = comment;
  }

  public static CommentReformatter of(String comment) {
    return new CommentReformatter(comment);
  }

  public CommentReformatter reformat(
      Pattern pattern, Function<Matcher, String> replacementFunction) {
    comment = reformatPattern(comment, pattern, replacementFunction);
    return this;
  }

  public CommentReformatter replace(Pattern pattern, String replacement) {
    comment = pattern.matcher(comment).replaceAll(replacement);
    return this;
  }

  public CommentReformatter reformatAbsoluteMarkdownLinks(String linkFormat) {
    comment =
        reformatPattern(
            comment, CommentPatterns.ABSOLUTE_LINK_PATTERN, reformatLinkFunction(linkFormat, ""));
    return this;
  }

  public CommentReformatter reformatCloudMarkdownLinks(String linkFormat) {
    comment =
        reformatPattern(
            comment,
            CommentPatterns.CLOUD_LINK_PATTERN,
            reformatLinkFunction(linkFormat, CLOUD_URL_PREFIX));
    return this;
  }

  @Override
  public String toString() {
    return comment;
  }

  public static Function<Matcher, String> reformatLinkFunction(
      final String linkFormat, final String urlPrefix) {
    return new Function<Matcher, String>() {
      @Override
      public String apply(Matcher matcher) {
        String url = urlPrefix + matcher.group(2);
        return Matcher.quoteReplacement(String.format(linkFormat, matcher.group(1), url));
      }
    };
  }

  private static String reformatPattern(
      String comment, Pattern pattern, Function<Matcher, String> replacementFunction) {
    StringBuffer sb = new StringBuffer();
    Matcher m = pattern.matcher(comment);
    if (!m.find()) {
      return comment;
    }
    do {
      m.appendReplacement(sb, replacementFunction.apply(m));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
  }
}
