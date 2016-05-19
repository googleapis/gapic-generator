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
package com.google.api.codegen.py;

import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.SnippetDescriptor;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.common.collect.ImmutableMap;

import java.util.List;

/**
 * A PythonProvider provides general Python code generation logic.
 */
public class PythonSnippetSetRunner {

  /**
   * The path to the root of snippet resources.
   */
  static final String SNIPPET_RESOURCE_ROOT =
      PythonContextCommon.class.getPackage().getName().replace('.', '/');

  @SuppressWarnings("unchecked")
  public <Element> GeneratedResult generate(
      Element element,
      SnippetDescriptor snippetDescriptor,
      PythonContext context,
      PythonImportHandler importHandler,
      ImmutableMap<String, Object> globalMap,
      String pathPrefix) {
    globalMap =
        ImmutableMap.<String, Object>builder()
            .putAll(globalMap)
            .put("context", context)
            .put("importHandler", importHandler)
            .build();

    PythonSnippetSet<Element> snippets =
        SnippetSet.createSnippetInterface(
            PythonSnippetSet.class,
            SNIPPET_RESOURCE_ROOT,
            snippetDescriptor.getSnippetInputName(),
            globalMap);

    Doc filenameDoc = snippets.generateFilename(element);
    String outputFilename = filenameDoc.prettyPrint();
    Doc body = snippets.generateBody(element);
    List<String> importList = importHandler.calculateImports();
    context.resetState(snippets);

    // Generate result.
    Doc result = snippets.generateModule(element, body, importList);
    return GeneratedResult.create(result, pathPrefix + outputFilename);
  }
}
