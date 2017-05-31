package com.google.api.codegen.discovery;

import javax.annotation.Nullable;

/**
 * Represents a node in a tree of nodes.
 */
public interface Node {
  /** @return the immediate parent of this node. */
  @Nullable
  Node parent();

  /** @return the ID of this node. */
  String id();
}
