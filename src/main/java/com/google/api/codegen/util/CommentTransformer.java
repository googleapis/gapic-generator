/* Copyright 2017 Google LLC
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
import com.google.common.collect.ImmutableList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommentTransformer {

  public static String CLOUD_URL_PREFIX = "https://cloud.google.com";

  private ImmutableList<Transformation> transformations;

  private CommentTransformer(ImmutableList<Transformation> transformations) {
    this.transformations = transformations;
  }

  public String transform(String comment) {
    for (Transformation transformation : transformations) {
      comment = transformation.apply(comment);
    }
    return comment;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private ImmutableList.Builder<Transformation> transformations = ImmutableList.builder();

    private Builder() {}

    public Builder replace(Pattern pattern, final String replacement) {
      transformations.add(
          new Transformation(
              pattern,
              new Function<String, String>() {
                @Override
                public String apply(String s) {
                  return replacement;
                }
              }));
      return this;
    }

    public Builder scopedReplace(Pattern pattern, final String target, final String replacement) {
      return transform(
          new Transformation(
              pattern,
              new Function<String, String>() {
                @Override
                public String apply(String matchedString) {
                  return matchedString.replace(target, replacement);
                }
              }));
    }

    public Builder transform(Transformation transformation) {
      transformations.add(transformation);
      return this;
    }

    public CommentTransformer build() {
      return new CommentTransformer(transformations.build());
    }
  }

  public static class Transformation {
    private Pattern pattern;
    private Function<String, String> replacementFunction;

    public Transformation(Pattern pattern, Function<String, String> replacementFunction) {
      this.pattern = pattern;
      this.replacementFunction = replacementFunction;
    }

    public String apply(String comment) {
      StringBuffer sb = new StringBuffer();
      Matcher m = pattern.matcher(comment);
      while (m.find()) {
        m.appendReplacement(sb, replacementFunction.apply(m.group()));
      }
      m.appendTail(sb);
      return sb.toString();
    }
  }
}
