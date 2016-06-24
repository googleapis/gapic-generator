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

package com.google.api.codegen.java.surface;

import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.surface.SurfaceSnippetSetRunner;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.common.collect.ImmutableMap;

/**
 * A JavaProvider provides general Java code generation logic that is agnostic to the use case
 * (e.g. Gapic vs Discovery). Behavior that is specific to a use case is provided through a
 * subclass of JavaContext.
 */
public class JavaSurfaceSnippetSetRunner implements SurfaceSnippetSetRunner<JavaXApi> {

  /**
   * The path to the root of snippet resources.
   */
  private static final String SNIPPET_RESOURCE_ROOT =
      JavaSurfaceSnippetSetRunner.class.getPackage().getName().replace('.', '/');

  @Override
  public GeneratedResult generate(JavaXApi xapi, String snippetFileName) {
    JavaViewModelSnippetSet snippets =
        SnippetSet.createSnippetInterface(
            JavaViewModelSnippetSet.class,
            SNIPPET_RESOURCE_ROOT,
            snippetFileName,
            ImmutableMap.<String, Object>of());

    String outputFilename = xapi.getFileName();
    Doc result = snippets.generateClass(xapi);
    return GeneratedResult.create(result, outputFilename);
  }

  private interface JavaViewModelSnippetSet {
    Doc generateClass(JavaXApi xapi);
  }
}
