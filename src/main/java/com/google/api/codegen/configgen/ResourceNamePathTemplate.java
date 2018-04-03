/* Copyright 2018 Google LLC
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
package com.google.api.codegen.configgen;

import com.google.api.codegen.discogapic.transformer.DiscoGapicParser;
import com.google.api.pathtemplate.PathTemplate;
import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper around {@link com.google.api.pathtemplate.PathTemplate} that overrides the equals() and
 * hashCode() methods so that two PathTemplates with the same order of PathTemplate.Segments of type
 * SegmentKind.LITERAL are the same.
 */
public class ResourceNamePathTemplate {
  private final PathTemplate pathTemplate;
  private final String pathTemplateString;

  // The list of the static segments, not including the wildcard strings, of the PathTemplate.
  private final List<String> literalSegments;

  private ResourceNamePathTemplate(PathTemplate pathTemplate) {
    this.pathTemplate = pathTemplate;
    this.pathTemplateString = pathTemplate.toString();

    String[] pieces = pathTemplateString.split(DiscoGapicParser.PATH_DELIMITER);

    this.literalSegments = new ArrayList<>(pieces.length);
    for (String piece : pieces) {
      if (!piece.contains("{")) {
        this.literalSegments.add(piece);
      }
    }
  }

  public static ResourceNamePathTemplate create(String pathTemplateString) {
    PathTemplate pathTemplate = PathTemplate.create(pathTemplateString);
    return new ResourceNamePathTemplate(pathTemplate);
  }

  /** Two instances of this class are equal if the have the same static segments. */
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof ResourceNamePathTemplate)) {
      return false;
    } else {
      ResourceNamePathTemplate otherTemplate = (ResourceNamePathTemplate) other;
      return this.literalSegments.equals(otherTemplate.literalSegments);
    }
  }

  @Override
  public int hashCode() {
    return this.literalSegments.hashCode();
  }
}
