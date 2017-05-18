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

import com.google.common.base.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommentTransformer {

  public static String CLOUD_URL_PREFIX = "https://cloud.google.com";

  private String comment;

  private CommentTransformer(String comment) {
    this.comment = comment;
  }

  public static CommentTransformer of(String comment) {
    return new CommentTransformer(comment);
  }

  public CommentTransformer replace(Pattern pattern, String replacement) {
    comment = pattern.matcher(comment).replaceAll(replacement);
    return this;
  }

  public CommentTransformer transformProtoMarkdownLinks(String linkFormat) {
    return transform(ProtoLinkPattern.PROTO.createTransformation(linkFormat, ""));
  }

  public CommentTransformer transformAbsoluteMarkdownLinks(String linkFormat) {
    return transform(ProtoLinkPattern.ABSOLUTE.createTransformation(linkFormat, ""));
  }

  public CommentTransformer transformCloudMarkdownLinks(String linkFormat) {
    return transform(ProtoLinkPattern.CLOUD.createTransformation(linkFormat, CLOUD_URL_PREFIX));
  }

  public CommentTransformer transform(Transformation transformation) {
    comment = transformation.apply(comment);
    return this;
  }

  @Override
  public String toString() {
    return comment;
  }

  public static class Transformation {
    private Pattern pattern;
    private Function<Matcher, String> replacementFunction;

    public Transformation(Pattern pattern, Function<Matcher, String> replacementFunction) {
      this.pattern = pattern;
      this.replacementFunction = replacementFunction;
    }

    public String apply(String comment) {
      StringBuffer sb = new StringBuffer();
      Matcher m = pattern.matcher(comment);
      while (m.find()) {
        m.appendReplacement(sb, replacementFunction.apply(m));
      }
      m.appendTail(sb);
      return sb.toString();
    }
  }
}
