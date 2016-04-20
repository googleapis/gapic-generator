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
package io.gapi.vgen.ruby;

import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.GapicLanguageProvider;
import io.gapi.vgen.SnippetDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The LanguageProvider which runs Gapic code generation for Ruby.
 */
public class RubyGapicLanguageProvider implements GapicLanguageProvider {

  private final RubyGapicContext context;

  /**
   * Entry points for the snippet set. Generation is partitioned into a first phase
   * which generates the content of the class without package and imports header,
   * and a second phase which completes the class based on the knowledge of which
   * other classes have been imported.
   */
  interface RubySnippetSet<Element> {

    /**
     * Generates the result filename for the generated document
     */
    Doc generateFilename(Element iface);

    /**
     * Generates the result class, and a set of accumulated types to be imported.
     */
    Doc generateClass(Element iface);
  }

  /**
   * The path to the root of snippet resources.
   */
  static final String SNIPPET_RESOURCE_ROOT =
      RubyGapicLanguageProvider.class.getPackage().getName().replace('.', '/');

  public RubyGapicLanguageProvider(Model model, ApiConfig apiConfig) {
    this.context = new RubyGapicContext(model, apiConfig);
  }

  @Override
  public Model getModel() {
    return context.getModel();
  }

  String getPackageRoot() {
    ArrayList<String> dirs = new ArrayList();
    for (String moduleName : context.getApiConfig().getPackageName().split("::")) {
      dirs.add(moduleName.toLowerCase());
    }
    return String.join("/", dirs);
  }

  @Override
  public <Element> void output(
      String outputPath, Multimap<Element, GeneratedResult> elements, boolean archive)
      throws IOException {
    String packageRoot = getPackageRoot();
    Map<String, Doc> files = new LinkedHashMap<>();
    for (Map.Entry<Element, GeneratedResult> entry : elements.entries()) {
      Element element = entry.getKey();
      GeneratedResult generatedResult = entry.getValue();
      String root;
      if (element instanceof Method) {
        root = ((Method) element).getParent().getFile().getFullName().replace('.', '/');
      } else {
        root = "lib/" + packageRoot;
      }
      files.put(root + "/" + generatedResult.getFilename(), generatedResult.getDoc());
    }
    if (archive) {
      // TODO: something more appropriate for Ruby packaging?
      ToolUtil.writeJar(files, outputPath);
    } else {
      ToolUtil.writeFiles(files, outputPath);
    }
  }

  @SuppressWarnings("unchecked")
  private <Element> GeneratedResult generate(
      Element element,
      SnippetDescriptor snippetDescriptor) {
    ImmutableMap<String, Object> globalMap = ImmutableMap.<String, Object>builder()
        .put("context", context)
        .build();
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

  @Override
  public GeneratedResult generateDoc(ProtoFile file, SnippetDescriptor snippetDescriptor) {
    return generate(file, snippetDescriptor);
  }

  @Override
  public GeneratedResult generateCode(Interface service, SnippetDescriptor snippetDescriptor) {
    return generate(service, snippetDescriptor);
  }

  @Override
  public GeneratedResult generateFragments(Method method, SnippetDescriptor snippetDescriptor) {
    return generate(method, snippetDescriptor);
  }
}
