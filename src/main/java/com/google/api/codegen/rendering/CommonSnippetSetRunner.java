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
package com.google.api.codegen.rendering;

import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * CommonSnippetSetRunner takes the view model as input and then uses the Snippet Set templating
 * engine to generate an output document.
 */
public class CommonSnippetSetRunner {

  private Object utilObject;
  private boolean allowEmptyDocs;

  public CommonSnippetSetRunner(Object utilObject) {
    this(utilObject, true);
  }

  public CommonSnippetSetRunner(Object utilObject, boolean allowEmptyDocs) {
    this.utilObject = utilObject;
    this.allowEmptyDocs = allowEmptyDocs;
  }

  public Map<String, GeneratedResult<Doc>> generate(ViewModel input) {
    SurfaceSnippetSet snippets =
        SnippetSet.createSnippetInterface(
            SurfaceSnippetSet.class,
            input.resourceRoot(),
            input.templateFileName(),
            ImmutableMap.of("util", utilObject));

    Doc doc = snippets.generate(input);
    return doc == null || doc.isWhitespace() && !allowEmptyDocs
        ? ImmutableMap.of()
        : ImmutableMap.of(input.outputPath(), GeneratedResult.create(doc, false));
  }

  private interface SurfaceSnippetSet {
    Doc generate(ViewModel input);
  }
}
