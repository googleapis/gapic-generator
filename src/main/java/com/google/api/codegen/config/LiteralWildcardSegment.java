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
package com.google.api.codegen.config;

import com.google.api.codegen.Inflector;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.LiteralSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.WildcardSegment;
import com.google.auto.value.AutoValue;

/**
 * Class to hold a LiteralSegment and WildcardSegment pair
 */
@AutoValue
public abstract class LiteralWildcardSegment {

  public static LiteralWildcardSegment create(
      LiteralSegment literalSegment, WildcardSegment wildcardSegment) {
    return new AutoValue_LiteralWildcardSegment(literalSegment, wildcardSegment);
  }

  public abstract LiteralSegment literalSegment();

  public abstract WildcardSegment wildcardSegment();

  public String collectionName() {
    return literalSegment().getLiteral();
  }

  public String paramName() {
    String paramName = Inflector.singularize(collectionName());
    if (wildcardSegment().isUnbounded()) {
      paramName += "_path";
    }
    return paramName;
  }

  public String resourcePathString() {
    StringBuilder builder = new StringBuilder();
    builder.append(collectionName());
    builder.append("/{");
    builder.append(paramName());
    if (wildcardSegment().isUnbounded()) {
      builder.append("=**");
    }
    builder.append("}");
    return builder.toString();
  }
}
