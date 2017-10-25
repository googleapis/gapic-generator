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
package com.google.api.codegen.util.ruby;

import com.google.api.codegen.CommentPatterns;
import com.google.api.codegen.util.CommentReformatter;
import com.google.api.codegen.util.CommentTransformer;
import com.google.api.codegen.util.CommentTransformer.Transformation;
import com.google.api.codegen.util.LinkPattern;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.util.regex.Matcher;

public class RubyCommentReformatter implements CommentReformatter {
  private static final String BULLET = "* ";

  private static Transformation PROTO_TO_RUBY_DOC_TRANSFORMATION =
      new Transformation(
          CommentPatterns.PROTO_LINK_PATTERN,
          new Function<String, String>() {
            @Override
            public String apply(String matchedString) {
              Matcher matcher = CommentPatterns.PROTO_LINK_PATTERN.matcher(matchedString);
              matcher.find();
              String title = matcher.group(1);
              String ref = matcher.group(2);
              if (ref == null || ref.equals(title)) {
                return String.format("{%s}", Matcher.quoteReplacement(protoToRubyDoc(title)));
              }
              return String.format(
                  "{%s %s}",
                  Matcher.quoteReplacement(protoToRubyDoc(ref)),
                  Matcher.quoteReplacement(protoToRubyDoc(title, false)));
            }
          });

  private CommentTransformer transformer =
      CommentTransformer.newBuilder()
          .replace(CommentPatterns.BACK_QUOTE_PATTERN, "+")
          .transform(PROTO_TO_RUBY_DOC_TRANSFORMATION)
          .transform(
              LinkPattern.RELATIVE
                  .withUrlPrefix(CommentTransformer.CLOUD_URL_PREFIX)
                  .toFormat("[$TITLE]($URL)"))
          .transform(LinkPattern.ABSOLUTE.toFormat("[$TITLE]($URL)"))
          .scopedReplace(CommentPatterns.HEADLINE_PATTERN, "#", "=")
          .build();

  @Override
  public String reformat(String comment) {
    StringBuffer sb = new StringBuffer();
    int listIndent = 0;
    boolean followsListItem = false;
    boolean followsBlankLine = false;
    for (String line : Splitter.on("\n").split(comment)) {
      Matcher listMatcher = CommentPatterns.UNORDERED_LIST_PATTERN.matcher(line);
      boolean matchesList = listMatcher.lookingAt();
      Matcher indentMatcher = CommentPatterns.INDENT_PATTERN.matcher(line);
      indentMatcher.lookingAt();
      int indent = indentMatcher.group().length();
      if (matchesList) {
        line = listMatcher.replaceFirst(BULLET);
      }
      if (indent < listIndent && (matchesList || followsBlankLine)) {
        listIndent -= BULLET.length();
      } else if (followsListItem && (!matchesList || indent > listIndent)) {
        listIndent += BULLET.length();
      }
      if (listIndent > 0) {
        line = line.trim();
        sb.append(Strings.repeat(" ", listIndent));
      }
      sb.append(transformer.transform(line)).append("\n");
      followsListItem = matchesList;
      followsBlankLine = line.isEmpty();
    }
    return sb.toString().trim();
  }

  private static String protoToRubyDoc(String comment) {
    return protoToRubyDoc(comment, true);
  }

  private static String protoToRubyDoc(String comment, boolean changeCase) {
    boolean messageFound = false;
    boolean isFirstSegment = true;
    StringBuilder builder = new StringBuilder();
    for (String name : Splitter.on(".").split(comment)) {
      char firstChar = name.charAt(0);
      if (Character.isUpperCase(firstChar)) {
        builder.append(isFirstSegment ? "" : "::").append(name);
        messageFound = true;
      } else if (messageFound) {
        // Lowercase segment after message is found is field.
        // In Ruby, it is referred as "Message#field" format.
        builder.append("#").append(name);
      } else {
        builder
            .append(isFirstSegment ? "" : "::")
            .append(changeCase ? Character.toUpperCase(firstChar) : firstChar)
            .append(name.substring(1));
      }
      isFirstSegment = false;
    }
    return builder.toString();
  }
}
