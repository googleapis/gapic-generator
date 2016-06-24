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
package com.google.api.codegen.surface;

import com.google.api.codegen.GeneratedResult;

/**
 * A SurfaceSnippetSetRunner takes the element, snippet file, and context as input and then uses the
 * Snippet Set templating engine to generate an output document.
 */
public interface SurfaceSnippetSetRunner<Element> {

  /**
   * Runs the code generation.
   */
  GeneratedResult generate(Element element, String snippetFileName);
}
