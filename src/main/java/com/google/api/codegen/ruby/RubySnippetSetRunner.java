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
package com.google.api.codegen.ruby;

import com.google.api.codegen.CodegenContext;
import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.common.collect.ImmutableMap;

/** A RubyProvider provides general Ruby code generation logic. */
public class RubySnippetSetRunner<ElementT> implements SnippetSetRunner.Generator<ElementT> {

  private final String resourceRoot;

  public RubySnippetSetRunner(String resourceRoot) {
    this.resourceRoot = resourceRoot;
  }

  @Override
  @SuppressWarnings("unchecked")
  public GeneratedResult generate(
      ElementT element, String snippetFileName, CodegenContext context) {
    ImmutableMap<String, Object> globalMap =
        ImmutableMap.<String, Object>builder()
            .put("context", context)
            .put("util", new CommonRenderingUtil())
            .build();
    RubySnippetSet<ElementT> snippets =
        SnippetSet.createSnippetInterface(
            RubySnippetSet.class, resourceRoot, snippetFileName, globalMap);

    Doc filenameDoc = snippets.generateFilename(element);
    String outputFilename = filenameDoc.prettyPrint();
    Doc result = snippets.generateClass(element);
    return GeneratedResult.create(result, outputFilename);
  }
}
