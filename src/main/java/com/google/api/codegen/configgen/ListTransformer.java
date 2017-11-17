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
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.NullConfigNode;
import com.google.api.codegen.configgen.nodes.ScalarConfigNode;

/** Transforms an Iterable of elements into a linked list of ConfigNodes. */
public class ListTransformer {
  /**
   * Convenience method for transforming an Iterable of Strings into a linked list of ConfigNodes.
   */
  public static ConfigNode generateStringList(Iterable<String> elements, ConfigNode parentNode) {
    return generateList(
        elements,
        parentNode,
        new ElementTransformer<String>() {
          @Override
          public ConfigNode generateElement(String element) {
            return new ListItemConfigNode().setChild(new ScalarConfigNode(element));
          }
        });
  }

  /**
   * @param elements The data to transform into ConfigNodes
   * @param parentNode The parent of the generated list
   * @param elementTransformer Determines how to transform an individual element
   * @return The head of the list.
   */
  public static <T> ConfigNode generateList(
      Iterable<T> elements, ConfigNode parentNode, ElementTransformer<T> elementTransformer) {
    ConfigNode elementNode = new NullConfigNode();
    ConfigNode prev = null;
    for (T elem : elements) {
      ConfigNode node = elementTransformer.generateElement(elem);

      if (node == null) {
        continue;
      }

      if (prev == null) {
        parentNode.setChild(node);
      } else {
        prev.insertNext(node);
      }

      if (!elementNode.isPresent()) {
        elementNode = node;
      }

      prev = node;
    }
    return elementNode;
  }

  /** Transforms an element into a ConfigNode in the list. */
  public interface ElementTransformer<T> {
    ConfigNode generateElement(T element);
  }
}
