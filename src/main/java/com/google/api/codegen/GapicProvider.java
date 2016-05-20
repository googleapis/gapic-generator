/* Copyright 2016 Google Inc
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
package com.google.api.codegen;

import com.google.api.tools.framework.model.Model;
import com.google.common.collect.Multimap;

import java.io.IOException;

/**
 * A GapicProvider performs code or fragment generation using on a proto-based Model for a
 * particular language.
 */
public interface GapicProvider<InputElementT> {

  Model getModel();

  /**
   * Gets the view of the model.
   */
  public InputElementView<InputElementT> getView();

  /**
   * Runs code generation.
   */
  GeneratedResult generate(InputElementT element, SnippetDescriptor snippetDescriptor);

  /**
   * Outputs the given elements to the given output path.
   */
  <Element> void output(String outputPath, Multimap<Element, GeneratedResult> elements)
      throws IOException;
}
