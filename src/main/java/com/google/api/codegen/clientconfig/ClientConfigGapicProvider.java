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
package com.google.api.codegen.clientconfig;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.GapicProvider;
import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.InputElementView;
import com.google.api.codegen.SnippetDescriptor;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClientConfigGapicProvider implements GapicProvider<Interface> {

  private Model model;
  private ApiConfig apiConfig;
  private InputElementView<Interface> view;

  public ClientConfigGapicProvider(
      Model model, ApiConfig apiConfig, InputElementView<Interface> view) {
    this.model = model;
    this.apiConfig = apiConfig;
    this.view = view;
  }

  @Override
  public Model getModel() {
    return model;
  }

  @Override
  public <Element> void output(String outputPath, Multimap<Element, GeneratedResult> elements)
      throws IOException {
    Map<String, Doc> files = new LinkedHashMap<>();
    for (GeneratedResult generatedResult : elements.values()) {
      files.put(generatedResult.getFilename(), generatedResult.getDoc());
    }

    ToolUtil.writeFiles(files, outputPath);
  }

  /**
   * The path to the root of snippet resources.
   */
  private static final String SNIPPET_RESOURCE_ROOT =
      ClientConfigGapicProvider.class.getPackage().getName().replace('.', '/');

  @SuppressWarnings("unchecked")
  @Override
  public GeneratedResult generate(Interface service, SnippetDescriptor snippetDescriptor) {
    ClientConfigSnippetSet<Interface> snippets =
        SnippetSet.createSnippetInterface(
            ClientConfigSnippetSet.class,
            SNIPPET_RESOURCE_ROOT,
            snippetDescriptor.getSnippetInputName(),
            ImmutableMap.<String, Object>of(
                "context", new ClientConfigGapicContext(model, apiConfig)));

    String outputFilename = snippets.generateFilename(service).prettyPrint();

    Doc body = snippets.generateBody(service);

    return GeneratedResult.create(body, outputFilename);
  }

  @Override
  public InputElementView<Interface> getView() {
    return view;
  }
}
