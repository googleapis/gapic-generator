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
package com.google.api.codegen.configgen.nodes;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class BaseConfigNodeTest {
  private static class TestConfigNode extends BaseConfigNode {
    TestConfigNode(int startLine, String text) {
      super(startLine, text);
    }
  }

  @Test
  public void testIsPresent() throws Exception {
    TestConfigNode node = new TestConfigNode(0, "foo");
    assertThat(node.isPresent()).isTrue();
  }

  @Test
  public void testGetStartLine() throws Exception {
    TestConfigNode node = new TestConfigNode(1, "foo");
    assertThat(node.getStartLine()).isEqualTo(1);
  }

  @Test
  public void testGetText() throws Exception {
    TestConfigNode node = new TestConfigNode(0, "foo");
    assertThat(node.getText()).isEqualTo("foo");
  }

  @Test
  public void testChild() throws Exception {
    TestConfigNode node = new TestConfigNode(0, "foo");
    ConfigNode child = new ScalarConfigNode(0, "bar");
    assertThat(node.setChild(child)).isSameAs(node);
    assertThat(node.getChild().isPresent()).isFalse();
  }

  @Test
  public void testNext() throws Exception {
    TestConfigNode node = new TestConfigNode(0, "foo");
    ConfigNode next = new ScalarConfigNode(0, "bar");
    assertThat(node.getNext().isPresent()).isFalse();
    assertThat(node.insertNext(next)).isSameAs(node);
    assertThat(node.getNext()).isSameAs(next);
  }
}
