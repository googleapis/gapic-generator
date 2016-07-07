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
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.common.collect.ImmutableMap;

/**
 * A SurfaceSnippetSetRunner takes the element, snippet file, and context as input and then uses the
 * Snippet Set templating engine to generate an output document.
 */
public class SurfaceSnippetSetRunner {

  private String resourceRoot;
  private Object utilObject;

  public SurfaceSnippetSetRunner(String resourceRoot, Object utilObject) {
    this.resourceRoot = resourceRoot;
    this.utilObject = utilObject;
  }

  public GeneratedResult generate(SurfaceGenInput input, String snippetFileName) {
    SurfaceSnippetSet snippets =
        SnippetSet.createSnippetInterface(
            SurfaceSnippetSet.class,
            resourceRoot,
            snippetFileName,
            ImmutableMap.<String, Object>of("util", utilObject));

    String outputFilename = input.getFileName();
    Doc result = snippets.generateClass(input);
    return GeneratedResult.create(result, outputFilename);
  }

  private interface SurfaceSnippetSet {
    Doc generateClass(SurfaceGenInput input);
  }
}
