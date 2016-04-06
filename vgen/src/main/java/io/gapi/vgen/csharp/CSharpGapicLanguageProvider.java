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
package io.gapi.vgen.csharp;

import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.collect.Multimap;

import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.LanguageProvider;
import io.gapi.vgen.SnippetDescriptor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The LanguageProvider which runs Gapic code generation for C#.
 */
public class CSharpGapicLanguageProvider implements LanguageProvider {

  private final CSharpGapicContext context;
  private final CSharpLanguageProvider provider;

  public CSharpGapicLanguageProvider(Model model, ApiConfig apiConfig) {
    this.context = new CSharpGapicContext(model, apiConfig);
    this.provider = new CSharpLanguageProvider();
  }

  @Override
  public Model getModel() {
    return context.getModel();
  }

  @Override
  public <Element> void output(String outputPath,
      Multimap<Element, GeneratedResult> elements, boolean archive)
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
    if (archive) {
      ToolUtil.writeJar(files, outputPath);
    } else {
      ToolUtil.writeFiles(files, outputPath);
    }
  }

  @Override
  public GeneratedResult generateCode(Interface service, SnippetDescriptor snippetDescriptor) {
    return provider.generate(service, snippetDescriptor, context,
        context.getNamespace(service.getFile()));
  }

  @Override
  public GeneratedResult generateFragments(Method method, SnippetDescriptor snippetDescriptor) {
    return provider.generate(method, snippetDescriptor, context,
        context.getNamespace(method.getParent().getFile()));
  }

  @Override
  public GeneratedResult generateDoc(ProtoFile file, SnippetDescriptor descriptor) {
    return null;
  }

}
