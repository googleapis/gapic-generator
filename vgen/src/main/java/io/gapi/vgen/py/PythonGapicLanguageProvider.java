/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http: *www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gapi.vgen.py;

import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.LanguageProvider;
import io.gapi.vgen.SnippetDescriptor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The LanguageProvider which runs Gapic code generation for Python.
 */
public class PythonGapicLanguageProvider implements LanguageProvider {

  private final PythonGapicContext context;
  private final PythonLanguageProvider provider;

  public PythonGapicLanguageProvider(Model model, ApiConfig apiConfig) {
    this.context = new PythonGapicContext(model, apiConfig);
    this.provider = new PythonLanguageProvider();
  }

  @Override
  public Model getModel() {
    return context.getModel();
  }

  @Override
  public <Element> void output(
      String outputPath, Multimap<Element, GeneratedResult> elements, boolean archive)
      throws IOException {
    String packageRoot = context.getApiConfig().getPackageName().replace('.', '/');
    Map<String, Doc> files = new LinkedHashMap<>();
    for (Map.Entry<Element, GeneratedResult> entry : elements.entries()) {
      Element element = entry.getKey();
      GeneratedResult generatedResult = entry.getValue();
      String root;
      if (element instanceof Method) {
        root = ((Method) element).getParent().getFile().getFullName().replace('.', '/');
      } else {
        root = packageRoot;
      }
      files.put(root + "/" + generatedResult.getFilename(), generatedResult.getDoc());
    }
    if (archive) {
      // TODO: something more appropriate for Python packaging?
      ToolUtil.writeJar(files, outputPath);
    } else {
      ToolUtil.writeFiles(files, outputPath);
    }
  }

  @Override
  public GeneratedResult generateDoc(ProtoFile file, SnippetDescriptor snippetDescriptor) {
    PythonImportHandler importHandler = new PythonImportHandler(file);
    ImmutableMap<String, Object> globalMap =
        ImmutableMap.<String, Object>builder()
            .put("context", context)
            .put("file", file)
            .put("importHandler", importHandler)
            .build();
    PythonSnippetSet snippets =
        SnippetSet.createSnippetInterface(
            PythonSnippetSet.class,
            PythonLanguageProvider.SNIPPET_RESOURCE_ROOT,
            snippetDescriptor.getSnippetInputName(),
            globalMap);
    Doc filenameDoc = snippets.generateFilename(file);
    String outputFilename = filenameDoc.prettyPrint();
    List<String> importList = importHandler.calculateImports();
    Doc result = snippets.generateClass(file, importList);
    return GeneratedResult.create(result, outputFilename);
  }

  public GeneratedResult generateCode(Interface service, SnippetDescriptor snippetDescriptor) {
    PythonImportHandler importHandler =
        new PythonImportHandler(service, context.getApiConfig().getInterfaceConfig(service));
    ImmutableMap<String, Object> globalMap =
        ImmutableMap.<String, Object>builder()
            .put("context", context)
            .put("pyproto", new PythonProtoElements())
            .put("importHandler", importHandler)
            .build();
    PythonSnippetSet snippets =
        SnippetSet.createSnippetInterface(
            PythonSnippetSet.class,
            PythonLanguageProvider.SNIPPET_RESOURCE_ROOT,
            snippetDescriptor.getSnippetInputName(),
            globalMap);
    Doc filenameDoc = snippets.generateFilename(service);
    String outputFilename = filenameDoc.prettyPrint();
    List<String> importList = importHandler.calculateImports();
    // Generate result.
    Doc result = snippets.generateClass(service, importList);
    String pathPrefix;
    if (!Strings.isNullOrEmpty(context.getApiConfig().getPackageName())) {
      pathPrefix = context.getApiConfig().getPackageName().replace('.', '/') + "/";
    } else {
      pathPrefix = "";
    }
    return GeneratedResult.create(result, pathPrefix + outputFilename);
  }

  @Override
  public GeneratedResult generateFragments(Method method, SnippetDescriptor snippetDescriptor) {
    return provider.generate(
        method,
        context.getApiConfig(),
        snippetDescriptor,
        context,
        new PythonImportHandler(method.getParent().getFile()));
  }

}
