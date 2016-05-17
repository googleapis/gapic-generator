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

import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.SnippetDescriptor;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A RubyProvider provides general Ruby code generation logic.
 */
public class RubyProvider {

  /**
   * The path to the root of snippet resources.
   */
  static final String SNIPPET_RESOURCE_ROOT =
      RubyGapicProvider.class.getPackage().getName().replace('.', '/');

  public <Element> void output(
      String packageRoot, String outputPath, Multimap<Element, GeneratedResult> elements)
      throws IOException {
    Map<String, Doc> files = new LinkedHashMap<>();
    for (Map.Entry<Element, GeneratedResult> entry : elements.entries()) {
      Element element = entry.getKey();
      GeneratedResult generatedResult = entry.getValue();
      String root;
      if (element instanceof Method) {
        root = ((Method) element).getFile().getFullName().replace('.', '/');
      } else {
        root = packageRoot;
      }
      files.put(root + "/" + generatedResult.getFilename(), generatedResult.getDoc());
    }
    ToolUtil.writeFiles(files, outputPath);
  }

  @SuppressWarnings("unchecked")
  public <Element> GeneratedResult generate(
      Element element, SnippetDescriptor snippetDescriptor, RubyContext context) {
    ImmutableMap<String, Object> globalMap =
        ImmutableMap.<String, Object>builder().put("context", context).build();
    RubySnippetSet<Element> snippets =
        SnippetSet.createSnippetInterface(
            RubySnippetSet.class,
            SNIPPET_RESOURCE_ROOT,
            snippetDescriptor.getSnippetInputName(),
            globalMap);

    Doc filenameDoc = snippets.generateFilename(element);
    String outputFilename = filenameDoc.prettyPrint();
    Doc result = snippets.generateClass(element);
    return GeneratedResult.create(result, outputFilename);
  }
}
