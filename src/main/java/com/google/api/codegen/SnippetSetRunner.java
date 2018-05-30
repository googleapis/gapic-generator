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
package com.google.api.codegen;

import com.google.api.codegen.common.CodegenContext;
import com.google.api.codegen.common.GeneratedResult;
import com.google.api.tools.framework.snippet.Doc;
import java.util.Map;

/**
 * A SnippetSetRunner takes the element, snippet file, and context as input and then uses the
 * Snippet Set templating engine to generate an output document.
 */
public interface SnippetSetRunner<Element> {

  /** The path to the root of snippet resources. */
  String SNIPPET_RESOURCE_ROOT = SnippetSetRunner.class.getPackage().getName().replace('.', '/');

  /** Runs the code generation. */
  Map<String, GeneratedResult<Doc>> generate(
      Element element, String snippetFileName, CodegenContext context);
}
