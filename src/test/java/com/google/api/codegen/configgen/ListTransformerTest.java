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
package com.google.api.codegen.configgen;

import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.ScalarConfigNode;
import com.google.common.truth.Truth;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class ListTransformerTest {
  @Test
  public void testGenerateList() throws Exception {
    List<Integer> elements = Arrays.asList(1, 2);
    ConfigNode parent = new FieldConfigNode(0, "parent");
    ConfigNode listNode =
        ListTransformer.generateList(
            elements,
            parent,
            (startLine, element) ->
                new ListItemConfigNode(startLine)
                    .setChild(new ScalarConfigNode(startLine, String.valueOf(element))));
    int index = 0;
    for (ConfigNode node : NodeFinder.getChildren(parent)) {
      Truth.assertThat(node.getChild().getText()).isEqualTo(String.valueOf(elements.get(index++)));
    }
  }

  @Test
  public void testGenerateStringList() throws Exception {
    List<String> elements = Arrays.asList("1", "2");
    ConfigNode parent = new FieldConfigNode(0, "parent");
    ConfigNode listNode = ListTransformer.generateStringList(elements, parent);
    int index = 0;
    for (ConfigNode node : NodeFinder.getChildren(parent)) {
      Truth.assertThat(node.getChild().getText()).isEqualTo(elements.get(index++));
    }
  }
}
