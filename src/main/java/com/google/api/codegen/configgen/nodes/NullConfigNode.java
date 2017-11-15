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

/** Implements the Null Object Pattern for ConfigNode. */
public class NullConfigNode implements ConfigNode {
  @Override
  public String getText() {
    return "";
  }

  @Override
  public ConfigNode getNext() {
    return this;
  }

  @Override
  public ConfigNode getChild() {
    return this;
  }

  @Override
  public ConfigNode setChild(ConfigNode child) {
    return this;
  }

  @Override
  public ConfigNode insertNext(ConfigNode next) {
    return next;
  }

  @Override
  public boolean isPresent() {
    return false;
  }
}
