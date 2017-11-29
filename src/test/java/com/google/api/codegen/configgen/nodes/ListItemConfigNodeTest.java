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
package com.google.api.codegen.configgen.nodes;

import com.google.api.codegen.configgen.nodes.metadata.Comment;
import com.google.api.codegen.configgen.nodes.metadata.DefaultComment;
import com.google.common.truth.Truth;
import org.junit.Test;

public class ListItemConfigNodeTest {
  @Test
  public void testChild() throws Exception {
    ListItemConfigNode node = new ListItemConfigNode(0);
    ConfigNode child = new ScalarConfigNode(0, "foo");
    Truth.assertThat(node.getChild().isPresent()).isFalse();
    Truth.assertThat(node.setChild(child)).isSameAs(node);
    Truth.assertThat(node.getChild()).isSameAs(child);
  }

  @Test
  public void testComment() throws Exception {
    ListItemConfigNode node = new ListItemConfigNode(0);
    Comment comment = new DefaultComment("Lorem ispum");
    Truth.assertThat(node.getComment().generate()).isEqualTo("");
    Truth.assertThat(node.setComment(comment)).isSameAs(node);
    Truth.assertThat(node.getComment()).isSameAs(comment);
  }
}
