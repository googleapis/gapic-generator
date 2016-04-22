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
package io.gapi.vgen.nodejs;

import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.SnippetDescriptor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A NodeJSLanguageProvider provides general NodeJS code generation logic that is agnostic to the use
 * case (e.g. Gapic vs Discovery). Behavior that is specific to a use case is provided through a
 * subclass of NodeJSContext.
 */
public class NodeJSLanguageProvider {

  /**
   * The path to the root of snippet resources.
   */
  private static final String SNIPPET_RESOURCE_ROOT =
      NodeJSLanguageProvider.class.getPackage().getName().replace('.', '/');

  public <Element> void output(
      String root, String outputPath, Multimap<Element, GeneratedResult> elements, boolean archive)
      throws IOException {
    Map<String, Doc> files = new LinkedHashMap<>();
    for (GeneratedResult generatedResult : elements.values()) {
      files.put(root + "/" + generatedResult.getFilename(), generatedResult.getDoc());
    }
    if (archive) {
      throw new IllegalArgumentException("NodeJS does not support archive");
    }
    ToolUtil.writeFiles(files, outputPath);
  }

  @SuppressWarnings("unchecked")
  public <Element> GeneratedResult generate(
      Element element,
      SnippetDescriptor snippetDescriptor,
      NodeJSDiscoveryContext context,
      String defaultPackagePrefix) {
    NodeJSSnippetSet<Element> snippets =
        SnippetSet.createSnippetInterface(
            NodeJSSnippetSet.class,
            SNIPPET_RESOURCE_ROOT,
            snippetDescriptor.getSnippetInputName(),
            ImmutableMap.<String, Object>of("context", context));

    String outputFilename = snippets.generateFilename(element).prettyPrint();
    Doc body = snippets.generateBody(element);
    return GeneratedResult.create(body, outputFilename);
  }

  public <Element> GeneratedResult generate(
      Element element, SnippetDescriptor snippetDescriptor, NodeJSDiscoveryContext context) {
    return generate(element, snippetDescriptor, context, null);
  }
}
