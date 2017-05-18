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
import com.google.api.codegen.util.CommentTransformer.Transformation;
import com.google.common.base.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An enumeration of link formats found in proto files. Each element of the enumeration is
 * associated with a {@link Pattern} object that must contain exactly two groups matching the link
 * title and link url.
 */
public enum ProtoLinkPattern {
  ABSOLUTE(CommentPatterns.ABSOLUTE_LINK_PATTERN),
  CLOUD(CommentPatterns.CLOUD_LINK_PATTERN),
  PROTO(CommentPatterns.PROTO_LINK_PATTERN);

  private Pattern pattern;

  ProtoLinkPattern(Pattern pattern) {
    this.pattern = pattern;
  }

  public Pattern getPattern() {
    return pattern;
  }

  public Transformation createTransformation(final String linkFormat, final String urlPrefix) {
    return new Transformation(
        getPattern(),
        new Function<Matcher, String>() {
          @Override
          public String apply(Matcher matcher) {
            String title = matcher.group(1);
            String url = urlPrefix + matcher.group(2);
            return Matcher.quoteReplacement(String.format(linkFormat, title, url));
          }
        });
  }
}
