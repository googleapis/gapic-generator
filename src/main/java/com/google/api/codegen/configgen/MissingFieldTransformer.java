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

import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;

/** Generates a FieldConfigNode if missing from its parent. */
public class MissingFieldTransformer {
  private final String name;
  private final ConfigNode parent;
  private final ConfigNode prev;

  /**
   * Creates a MissingFieldTransformer that prepends a FieldConfigNode to the given parent if one
   * with the given name could not be found in the parent.
   *
   * <p>Example: <code>
   * // parent contains nodeB and nodeC
   * MissingFieldTransformer.prepend("nodeA", parent).generate();
   * // parent contains nodeA, nodeB, and nodeC
   * </code>
   */
  public static MissingFieldTransformer prepend(String name, ConfigNode parent) {
    return new MissingFieldTransformer(name, parent, null);
  }

  /**
   * Creates a MissingFieldTransformer that inserts a FieldConfigNode after the given prev node if
   * one with the given name could not be found in the given parent.
   *
   * <p>Example: <code>
   * // parent contains nodeA and nodeC
   * MissingFieldTransformer.insert("nodeB", parent, nodeA).generate();
   * // parent contains nodeA, nodeB, and nodeC
   * </code>
   */
  public static MissingFieldTransformer insert(String name, ConfigNode parent, ConfigNode prev) {
    return new MissingFieldTransformer(name, parent, prev);
  }

  /**
   * Creates a MissingFieldTransformer that appends a FieldConfigNode to the given parent if one
   * with the given name could not be found in the parent.
   *
   * <p>Example: <code>
   * // parent contains nodeA and nodeB
   * MissingFieldTransformer.append("nodeC", parent).generate();
   * // parent contains nodeA, nodeB, and nodeC
   * </code>
   */
  public static MissingFieldTransformer append(String name, ConfigNode parent) {
    return new MissingFieldTransformer(name, parent, NodeFinder.getLastChild(parent));
  }

  private MissingFieldTransformer(String name, ConfigNode parent, ConfigNode prev) {
    this.name = name;
    this.parent = parent;
    this.prev = prev;
  }

  public FieldConfigNode generate() {
    ConfigNode node = NodeFinder.findByValue(parent, name);
    if (node instanceof FieldConfigNode) {
      return (FieldConfigNode) node;
    }

    if (prev == null) {
      ConfigNode next = parent.getChild();
      int startLine = next.isPresent() ? next.getStartLine() : parent.getStartLine() + 1;
      node = new FieldConfigNode(startLine, name);
      parent.setChild(node.insertNext(next));
    } else {
      ConfigNode next = node.getNext();
      node = new FieldConfigNode(NodeFinder.getNextLine(prev), name);
      prev.insertNext(node.insertNext(next));
    }

    return ((FieldConfigNode) node);
  }
}
