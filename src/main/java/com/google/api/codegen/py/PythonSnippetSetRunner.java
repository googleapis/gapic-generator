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

import com.google.api.codegen.CodegenContext;
import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.common.collect.ImmutableMap;
import java.util.List;

/** A PythonProvider provides general Python code generation logic. */
public class PythonSnippetSetRunner<ElementT> implements SnippetSetRunner.Generator<ElementT> {

  private PythonSnippetSetInputInitializer<ElementT> initializer;
  private final String resourceRoot;

  public PythonSnippetSetRunner(
      PythonSnippetSetInputInitializer<ElementT> initializer, String resourceRoot) {
    this.initializer = initializer;
    this.resourceRoot = resourceRoot;
  }

  @Override
  @SuppressWarnings("unchecked")
  public GeneratedResult generate(
      ElementT element, String snippetFileName, CodegenContext context) {
    PythonImportHandler importHandler = initializer.getImportHandler(element);
    ImmutableMap<String, Object> globalMap = initializer.getGlobalMap(element);
    globalMap =
        ImmutableMap.<String, Object>builder()
            .putAll(globalMap)
            .put("context", context)
            .put("importHandler", importHandler)
            .put("util", new CommonRenderingUtil())
            .build();

    PythonSnippetSet<ElementT> snippets =
        SnippetSet.createSnippetInterface(
            PythonSnippetSet.class, resourceRoot, snippetFileName, globalMap);

    Doc body = snippets.generateBody(element);
    List<String> importList = importHandler.calculateImports();
    Doc result = snippets.generateModule(element, body, importList);

    Doc filenameDoc = snippets.generateFilename(element);
    String outputFilename = filenameDoc.prettyPrint();
    return GeneratedResult.create(result, outputFilename);
  }
}
