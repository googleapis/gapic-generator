/* Copyright 2016 Google LLC
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
package com.google.api.codegen.gapic;

import com.google.api.tools.framework.snippet.Doc;
import java.util.List;
import java.util.Map;

/**
 * A GapicProvider performs code or fragment generation using on a proto-based Model for a
 * particular language.
 */
public interface GapicProvider {

  /** Returns the snippet files that this provider will use for code generation. */
  List<String> getSnippetFileNames();

  /** Runs code generation and returns a map from relative file paths to generated Doc. */
  Map<String, Doc> generate();

  /**
   * Runs code generation for a single snippet and returns a map from relative file paths to
   * generated Doc.
   */
  Map<String, Doc> generate(String snippetFileName);
}
