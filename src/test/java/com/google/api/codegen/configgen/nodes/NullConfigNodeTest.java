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

public class NullConfigNodeTest {
  @Test
  public void testIsPresent() throws Exception {
    NullConfigNode node = new NullConfigNode();
    assertThat(node.isPresent()).isFalse();
  }

  @Test
  public void testGetStartLine() throws Exception {
    NullConfigNode node = new NullConfigNode();
    assertThat(node.getStartLine()).isEqualTo(0);
  }

  @Test
  public void testGetText() throws Exception {
    NullConfigNode node = new NullConfigNode();
    assertThat(node.getText()).isEqualTo("");
  }

  @Test
  public void testGetNext() throws Exception {
    NullConfigNode node = new NullConfigNode();
    assertThat(node.getNext().isPresent()).isFalse();
  }

  @Test
  public void testGetChild() throws Exception {
    NullConfigNode node = new NullConfigNode();
    assertThat(node.getChild().isPresent()).isFalse();
  }

  @Test
  public void testSetChild() throws Exception {
    NullConfigNode node = new NullConfigNode();
    ConfigNode child = new ScalarConfigNode(0, "foo");
    assertThat(node.setChild(child)).isSameAs(node);
  }

  @Test
  public void testInsertNext() throws Exception {
    NullConfigNode node = new NullConfigNode();
    ConfigNode next = new ScalarConfigNode(0, "foo");
    assertThat(node.insertNext(next)).isSameAs(next);
  }
}
