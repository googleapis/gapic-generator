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
package com.google.api.codegen.clientconfig;

import com.google.api.codegen.CodegenContext;
import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.common.collect.ImmutableMap;

public class ClientConfigSnippetSetRunner<ElementT>
    implements SnippetSetRunner.Generator<ElementT> {

  private final String resourceRoot;

  public ClientConfigSnippetSetRunner(String resourceRoot) {
    this.resourceRoot = resourceRoot;
  }

  @Override
  @SuppressWarnings("unchecked")
  public GeneratedResult generate(
      ElementT element, String snippetFileName, CodegenContext context) {
    ClientConfigSnippetSet<ElementT> snippets =
        SnippetSet.createSnippetInterface(
            ClientConfigSnippetSet.class,
            resourceRoot,
            snippetFileName,
            ImmutableMap.<String, Object>of("context", context));

    String outputFilename = snippets.generateFilename(element).prettyPrint();

    Doc body = snippets.generateBody(element);

    return GeneratedResult.create(body, outputFilename);
  }
}
