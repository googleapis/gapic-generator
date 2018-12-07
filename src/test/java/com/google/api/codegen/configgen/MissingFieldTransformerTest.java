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
package com.google.api.codegen.configgen;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class MissingFieldTransformerTest {
  @Test
  public void testPrepend() throws Exception {
    ConfigNode parent = new FieldConfigNode(0, "parent");
    Map<String, String> fields = ImmutableMap.of("B", "2", "C", "3");
    ListTransformer.generateList(
        fields.entrySet(),
        parent,
        (startLine, entry) ->
            FieldConfigNode.createStringPair(startLine, entry.getKey(), entry.getValue()));
    MissingFieldTransformer.prepend("A", parent).generate();
    List<String> fieldNames = Arrays.asList("A", "B", "C");
    int index = 0;
    for (ConfigNode node : NodeFinder.getChildren(parent)) {
      assertThat(node.getText()).isEqualTo(fieldNames.get(index++));
    }
  }

  @Test
  public void testInsert() throws Exception {
    ConfigNode parent = new FieldConfigNode(0, "parent");
    Map<String, String> fields = ImmutableMap.of("A", "1", "C", "3");
    ConfigNode fieldA =
        ListTransformer.generateList(
            fields.entrySet(),
            parent,
            (startLine, entry) ->
                FieldConfigNode.createStringPair(startLine, entry.getKey(), entry.getValue()));
    MissingFieldTransformer.insert("B", parent, fieldA).generate();
    List<String> fieldNames = Arrays.asList("A", "B", "C");
    int index = 0;
    for (ConfigNode node : NodeFinder.getChildren(parent)) {
      assertThat(node.getText()).isEqualTo(fieldNames.get(index++));
    }
  }

  @Test
  public void testAppend() throws Exception {
    ConfigNode parent = new FieldConfigNode(0, "parent");
    Map<String, String> fields = ImmutableMap.of("A", "1", "B", "2");
    ListTransformer.generateList(
        fields.entrySet(),
        parent,
        (startLine, entry) ->
            FieldConfigNode.createStringPair(startLine, entry.getKey(), entry.getValue()));
    MissingFieldTransformer.append("C", parent).generate();
    List<String> fieldNames = Arrays.asList("A", "B", "C");
    int index = 0;
    for (ConfigNode node : NodeFinder.getChildren(parent)) {
      assertThat(node.getText()).isEqualTo(fieldNames.get(index++));
    }
  }
}
