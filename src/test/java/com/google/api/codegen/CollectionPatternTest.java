/* Copyright 2016 Google LLC
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
package com.google.api.codegen;

import com.google.api.codegen.configgen.CollectionPattern;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.FieldSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.LiteralSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.PathSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.WildcardSegment;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CollectionPatternTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  private CollectionPattern constructCollectionPattern(String fieldPath, String resourcePath) {
    return constructCollectionPattern(fieldPath, resourcePath, null);
  }

  private CollectionPattern constructCollectionPattern(
      String fieldPath, String resourcePath, String customVerb) {
    ImmutableList.Builder<PathSegment> subPathSegmentBuilder = ImmutableList.<PathSegment>builder();
    for (String seg : resourcePath.split("/")) {
      if (seg.equals("*")) {
        subPathSegmentBuilder.add(new WildcardSegment(false));
      } else if (seg.equals("**")) {
        subPathSegmentBuilder.add(new WildcardSegment(true));
      } else {
        subPathSegmentBuilder.add(new LiteralSegment(seg, false));
      }
    }
    if (customVerb != null) {
      subPathSegmentBuilder.add(new LiteralSegment(customVerb, true));
    }
    return CollectionPattern.create(new FieldSegment(fieldPath, subPathSegmentBuilder.build()));
  }

  @Test
  public void collectionPattern() {
    CollectionPattern collectionPattern = constructCollectionPattern("name", "shelves/*/books/*");
    Truth.assertThat(collectionPattern.getSimpleName()).isEqualTo("book");
    Truth.assertThat(collectionPattern.getTemplatizedResourcePath())
        .isEqualTo("shelves/{shelf}/books/{book}");
  }

  @Test
  public void collectionPatternPathWildcard() {
    CollectionPattern collectionPattern = constructCollectionPattern("name", "shelves/*/books/**");
    Truth.assertThat(collectionPattern.getSimpleName()).isEqualTo("book_path");
    Truth.assertThat(collectionPattern.getTemplatizedResourcePath())
        .isEqualTo("shelves/{shelf}/books/{book_path=**}");
  }

  @Test
  public void collectionPatternStartWithWildcard() {
    CollectionPattern collectionPattern = constructCollectionPattern("name", "*/shelves/*/books/*");
    Truth.assertThat(collectionPattern.getSimpleName()).isEqualTo("book");
    Truth.assertThat(collectionPattern.getTemplatizedResourcePath())
        .isEqualTo("{unknown}/shelves/{shelf}/books/{book}");
  }

  @Test
  public void collectionPatternEndWithLiteral() {
    CollectionPattern collectionPattern = constructCollectionPattern("name", "shelves/*/books");
    Truth.assertThat(collectionPattern.getSimpleName()).isEqualTo("shelf");
    Truth.assertThat(collectionPattern.getTemplatizedResourcePath())
        .isEqualTo("shelves/{shelf}/books");
  }

  @Test
  public void collectionPatternCustomVerb() {
    CollectionPattern collectionPattern =
        constructCollectionPattern("name", "shelves/*/books/*", "reader");
    Truth.assertThat(collectionPattern.getSimpleName()).isEqualTo("book");
    Truth.assertThat(collectionPattern.getTemplatizedResourcePath())
        .isEqualTo("shelves/{shelf}/books/{book}:reader");
  }

  @Test
  public void collectionPatternFailConstantSegment() {
    thrown.expect(IllegalArgumentException.class);
    constructCollectionPattern("name", "shelves");
  }

  @Test
  public void collectionPatternFailConstantPathSegment() {
    thrown.expect(IllegalArgumentException.class);
    constructCollectionPattern("name", "shelves/books");
  }

  @Test
  public void collectionPatternFailRawWildcard() {
    thrown.expect(IllegalArgumentException.class);
    constructCollectionPattern("name", "*");
  }

  @Test
  public void collectionPatternFailRawPathWildcard() {
    thrown.expect(IllegalArgumentException.class);
    constructCollectionPattern("name", "**");
  }

  @Test
  public void collectionPatternFailNoLiteral() {
    thrown.expect(IllegalArgumentException.class);
    constructCollectionPattern("name", "*/*");
  }
}
