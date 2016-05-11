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

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.GapicLanguageProvider;
import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.InputElementView;
import com.google.api.codegen.SnippetDescriptor;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The LanguageProvider which runs Gapic code generation for Python.
 */
public class PythonGapicLanguageProvider<InputElementT extends ProtoElement>
    implements GapicLanguageProvider<InputElementT> {

  private final PythonGapicContext context;
  private final PythonLanguageProvider provider;
  private final PythonInputElementView<InputElementT> view;

  @SuppressWarnings("unchecked")
  public PythonGapicLanguageProvider(
      Model model, ApiConfig apiConfig, InputElementView<InputElementT> view) {
    this.context = new PythonGapicContext(model, apiConfig);
    this.provider = new PythonLanguageProvider();
    // This cast will fail if the view specified in the configuration file is of the wrong type
    this.view = (PythonInputElementView<InputElementT>) view;
  }

  @Override
  public Model getModel() {
    return context.getModel();
  }

  @Override
  public <Element> void output(String outputPath, Multimap<Element, GeneratedResult> elements)
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
    ToolUtil.writeFiles(files, outputPath);
  }

  @Override
  public GeneratedResult generate(InputElementT element, SnippetDescriptor snippetDescriptor) {
    return provider.generate(
        element,
        snippetDescriptor,
        context,
        view.getImportHandler(element, context),
        view.getGlobalMap(element),
        view.getPathPrefix(context));
  }

  @Override
  public InputElementView<InputElementT> getView() {
    return view;
  }
}
