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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.api.codegen.configgen.nodes.metadata.Comment;
import com.google.api.codegen.configgen.nodes.metadata.NullComment;

/** Represents a key-value pair in a gapic config. */
public class FieldConfigNode extends BaseConfigNode {
  private ConfigNode child;
  private Comment comment;

  public static FieldConfigNode createStringPair(int startLine, String key, String value) {
    return new FieldConfigNode(startLine, key).setChild(new ScalarConfigNode(startLine, value));
  }

  public FieldConfigNode(int startLine, String text) {
    super(startLine, text);
  }

  @Override
  public ConfigNode getChild() {
    return child == null ? new NullConfigNode() : child;
  }

  public Comment getComment() {
    return comment == null ? new NullComment() : comment;
  }

  @Override
  public FieldConfigNode setChild(ConfigNode child) {
    checkArgument(this != child, "Cannot set node to be its own child");
    this.child = child;
    return this;
  }

  public FieldConfigNode setComment(Comment comment) {
    this.comment = comment;
    return this;
  }
}
