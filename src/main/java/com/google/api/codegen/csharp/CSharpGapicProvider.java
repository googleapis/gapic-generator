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
package com.google.api.codegen.csharp;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.GapicProvider;
import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.InputElementView;
import com.google.api.codegen.SnippetDescriptor;
import com.google.api.tools.framework.model.Interface;
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
 * The GapicProvider which runs Gapic code generation for C#.
 */
public class CSharpGapicProvider<InputElementT extends ProtoElement>
    implements GapicProvider<InputElementT> {

  private final CSharpGapicContext context;
  private final CSharpSnippetSetRunner snippetSetRunner;
  private final InputElementView<InputElementT> view;

  public CSharpGapicProvider(
      Model model, ApiConfig apiConfig, InputElementView<InputElementT> view) {
    this.context = new CSharpGapicContext(model, apiConfig);
    this.snippetSetRunner = new CSharpSnippetSetRunner();
    // This cast will fail if the view specified in the configuration file is of the wrong type
    this.view = view;
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
    return snippetSetRunner.generate(
        element, snippetDescriptor, context, context.getNamespace(element.getFile()));
  }

  @Override
  public InputElementView<InputElementT> getView() {
    return view;
  }
}
