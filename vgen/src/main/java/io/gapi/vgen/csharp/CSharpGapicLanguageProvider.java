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
package io.gapi.vgen.csharp;

import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.collect.Multimap;

import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.GapicLanguageProvider;
import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.InputElementView;
import io.gapi.vgen.SnippetDescriptor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The LanguageProvider which runs Gapic code generation for C#.
 */
public class CSharpGapicLanguageProvider<InputElementT extends ProtoElement>
    implements GapicLanguageProvider<InputElementT> {

  private final CSharpGapicContext context;
  private final CSharpLanguageProvider provider;
  private final CSharpProtoElementView<InputElementT> view;

  public CSharpGapicLanguageProvider(
      Model model, ApiConfig apiConfig, InputElementView<InputElementT> view) {
    this.context = new CSharpGapicContext(model, apiConfig);
    this.provider = new CSharpLanguageProvider();
    // This cast will fail if the view specified in the configuration file is of the wrong type
    this.view = (CSharpProtoElementView<InputElementT>) view;
  }

  @Override
  public Model getModel() {
    return context.getModel();
  }

  @Override
  public <Element> void output(String outputPath, Multimap<Element, GeneratedResult> elements)
      throws IOException {
    Map<String, Doc> files = new LinkedHashMap<>();
    for (Map.Entry<Element, GeneratedResult> entry : elements.entries()) {
      Element element = entry.getKey();
      GeneratedResult generatedResult = entry.getValue();
      Interface service;
      if (element instanceof Method) {
        service = (Interface) ((Method) element).getParent();
      } else {
        service = (Interface) element;
      }
      String path = context.getNamespace(service.getFile()).replace('.', '/');
      files.put(path + "/" + generatedResult.getFilename(), generatedResult.getDoc());
    }
    ToolUtil.writeFiles(files, outputPath);
  }

  @Override
  public GeneratedResult generate(InputElementT element, SnippetDescriptor snippetDescriptor) {
    CSharpProtoElementView<InputElementT> view = getView();
    return provider.generate(
        element, snippetDescriptor, context, context.getNamespace(view.getNamespaceFile(element)));
  }

  @Override
  public CSharpProtoElementView<InputElementT> getView() {
    return view;
  }
}
