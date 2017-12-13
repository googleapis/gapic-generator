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

/** Base class for the ConfigNode types. */
public abstract class BaseConfigNode implements ConfigNode {
  private final int startLine;
  private final String text;
  private ConfigNode next = new NullConfigNode();

  protected BaseConfigNode(int startLine, String text) {
    this.startLine = startLine;
    this.text = text;
  }

  @Override
  public int getStartLine() {
    return startLine;
  }

  @Override
  public String getText() {
    return this.text;
  }

  @Override
  public ConfigNode getNext() {
    return next;
  }

  @Override
  public ConfigNode getChild() {
    return new NullConfigNode();
  }

  @Override
  public ConfigNode setChild(ConfigNode child) {
    return this;
  }

  @Override
  public ConfigNode insertNext(ConfigNode next) {
    if (next != null) {
      this.next = next.insertNext(this.next);
    } else {
      this.next = new NullConfigNode();
    }

    return this;
  }

  @Override
  public boolean isPresent() {
    return true;
  }
}
