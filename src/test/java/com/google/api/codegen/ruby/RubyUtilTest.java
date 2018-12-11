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
package com.google.api.codegen.ruby;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class RubyUtilTest {
  @Test
  public void testGetSentence_noDot() {
    String sentence =
        RubyUtil.getSentence(
            ImmutableList.of("Lorem ipsum dolor sit amet,", "consectetur adipiscing elit"));
    assertThat(sentence).isEqualTo("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
  }

  @Test
  public void testGetSentence_endingSentence() {
    String sentence = RubyUtil.getSentence(ImmutableList.of("Lorem ipsum dolor sit amet."));
    assertThat(sentence).isEqualTo("Lorem ipsum dolor sit amet.");
  }

  @Test
  public void testGetSentence_middleSentence() {
    String sentence =
        RubyUtil.getSentence(
            ImmutableList.of(
                "Lorem ipsum dolor sit amet. Vivamus condimentum rhoncus est volutpat venenatis."));
    assertThat(sentence).isEqualTo("Lorem ipsum dolor sit amet.");
  }

  @Test
  public void testGetSentence_multipleSentences() {
    String sentence =
        RubyUtil.getSentence(
            ImmutableList.of(
                "Lorem ipsum dolor sit amet.",
                "Vivamus condimentum rhoncus est volutpat venenatis."));
    assertThat(sentence).isEqualTo("Lorem ipsum dolor sit amet.");
  }

  @Test
  public void testGetSentence_middleDot() {
    String sentence =
        RubyUtil.getSentence(
            ImmutableList.of("Lorem ipsum.dolor.sit amet,", "consectetur adipiscing elit."));
    assertThat(sentence).isEqualTo("Lorem ipsum.dolor.sit amet, consectetur adipiscing elit.");
  }

  @Test
  public void testGetSentence_startingBlank() {
    String sentence =
        RubyUtil.getSentence(
            ImmutableList.of("", "Lorem ipsum dolor sit amet,", "consectetur adipiscing elit"));
    assertThat(sentence).isEqualTo("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
  }

  @Test
  public void testGetSentence_middleBlank() {
    String sentence =
        RubyUtil.getSentence(
            ImmutableList.of("Lorem ipsum dolor sit amet", " ", "consectetur adipiscing elit"));
    assertThat(sentence).isEqualTo("Lorem ipsum dolor sit amet");
  }

  @Test
  public void testHasMajorVersion() {
    assertThat(RubyUtil.hasMajorVersion("One::Two::Three::V1")).isTrue();
  }

  @Test
  public void testHasMajorVersion_notVersion() {
    assertThat(RubyUtil.hasMajorVersion("One::Two::Three::Version")).isFalse();
  }

  @Test
  public void testHasMajorVersion_pointVersion() {
    assertThat(RubyUtil.hasMajorVersion("One::Two::Three::V1p2beta4")).isTrue();
  }

  @Test
  public void testHasMajorVersion_alphaVersion() {
    assertThat(RubyUtil.hasMajorVersion("One::Two::Three::V9alpha4")).isTrue();
  }
}
