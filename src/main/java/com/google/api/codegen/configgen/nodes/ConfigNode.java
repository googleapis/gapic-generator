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

/**
 * Represents an element of sytax in a gapic config.
 *
 * <p>Linked to the node that follows it to preserving ordering when inserting between nodes.
 *
 * <p>Some implementations have a child node.
 */
public interface ConfigNode {
  /** Returns the line this node starts on. */
  int getStartLine();

  /**
   * Returns the text value of this node.
   *
   * <p>Implementations may be blank.
   */
  String getText();

  /** Returns the next linked node. */
  ConfigNode getNext();

  /** Returns the child or NullConfigNode if the node does not have a child. */
  ConfigNode getChild();

  /**
   * Sets the child node. Should return itself for chaining.
   *
   * <p>Implementation may be empty.
   */
  ConfigNode setChild(ConfigNode child);

  /** Inserts next as the node following this one. Should return itself for chaining. */
  ConfigNode insertNext(ConfigNode next);

  /** Returns true if the node is not NullConfigNode. */
  boolean isPresent();
}
