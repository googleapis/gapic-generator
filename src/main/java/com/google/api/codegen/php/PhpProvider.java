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
package com.google.api.codegen.php;

import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.SnippetDescriptor;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A PhpProvider provides general PHP code generation logic that is agnostic to the use
 * case (e.g. Gapic vs Discovery). Behavior that is specific to a use case is provided through a
 * PHP context class (PhpGapicContext vs PhpDiscoveryContext).
 */
public class PhpProvider {

  /**
   * The path to the root of snippet resources.
   */
  private static final String SNIPPET_RESOURCE_ROOT =
      PhpProvider.class.getPackage().getName().replace('.', '/');

  @SuppressWarnings("unchecked")
  public <Element> GeneratedResult generate(
      Element element,
      SnippetDescriptor snippetDescriptor,
      PhpContext context,
      String defaultPackagePrefix) {
    PhpSnippetSet<Element> snippets =
        SnippetSet.createSnippetInterface(
            PhpSnippetSet.class,
            SNIPPET_RESOURCE_ROOT,
            snippetDescriptor.getSnippetInputName(),
            ImmutableMap.<String, Object>of("context", context));

    String outputFilename = snippets.generateFilename(element).prettyPrint();
    PhpContextCommon phpContextCommon = new PhpContextCommon();
    context.resetState(snippets, phpContextCommon);

    Doc body = snippets.generateBody(element);

    List<String> cleanedImports = phpContextCommon.getImports();

    Doc result = snippets.generateClass(element, body, cleanedImports);
    return GeneratedResult.create(result, outputFilename);
  }

  public <Element> GeneratedResult generate(
      Element element, SnippetDescriptor snippetDescriptor, PhpContext context) {
    return generate(element, snippetDescriptor, context, null);
  }
}
