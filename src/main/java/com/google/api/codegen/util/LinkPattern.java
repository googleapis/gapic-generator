/* Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.util;

import com.google.api.codegen.util.CommentTransformer.Transformation;
import com.google.common.base.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An collection of known link patterns found in proto files. The LinkPattern class provides an easy
 * way to create Transformation objects for these link patterns into a particular format.
 *
 * <p>The format may contain standard Java string format tokens, or the special tokens $TITLE and
 * $URL.
 *
 * <p>An example creating a Transformation for a relative link would be:
 *
 * <pre>
 * <code>
 * Transformation transformation = LinkPattern.RELATIVE
 *    .withUrlPrefix("https://cloud.google.com")
 *    .toFormat("[$TITLE]($URL)"));
 * </code>
 * </pre>
 */
public class LinkPattern {
  public static LinkPattern ABSOLUTE = new LinkPattern(CommentPatterns.ABSOLUTE_LINK_PATTERN, "");
  public static LinkPattern RELATIVE = new LinkPattern(CommentPatterns.RELATIVE_LINK_PATTERN, "");
  public static LinkPattern PROTO = new LinkPattern(CommentPatterns.PROTO_LINK_PATTERN, "");

  private Pattern pattern;
  private String urlPrefix;

  private LinkPattern(Pattern pattern, String urlPrefix) {
    this.pattern = pattern;
    this.urlPrefix = urlPrefix;
  }

  public LinkPattern withUrlPrefix(String urlPrefix) {
    return new LinkPattern(pattern, urlPrefix);
  }

  public Transformation toFormat(String linkFormat) {
    final String finalLinkFormat =
        linkFormat.replaceAll("\\$TITLE", "%1\\$s").replaceAll("\\$URL", "%2\\$s");
    return new Transformation(
        pattern,
        new Function<String, String>() {
          @Override
          public String apply(String matchedString) {
            Matcher matcher = pattern.matcher(matchedString);
            matcher.find();
            String title = matcher.group(1);
            String url = urlPrefix + matcher.group(2);
            return Matcher.quoteReplacement(String.format(finalLinkFormat, title, url));
          }
        });
  }
}
