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
package com.google.api.codegen.java;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.CodeGeneratorUtil;
import com.google.api.codegen.GapicProvider;
import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.InputElementView;
import com.google.api.codegen.SnippetDescriptor;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.Multimap;

import java.io.IOException;

/**
 * The GapicProvider which runs Gapic code generation for Java.
 */
public class JavaSimpleFileProvider<T> implements GapicProvider<Iterable<T>> {

  private final JavaGapicContext context;
  private final JavaSimpleFileSnippetSetRunner simpleFileSnippetSetRunner;
  private InputElementView<Iterable<T>> view;

  public JavaSimpleFileProvider(
      Model model, ApiConfig apiConfig, InputElementView<Iterable<T>> view) {
    this.context = new JavaGapicContext(model, apiConfig);
    this.simpleFileSnippetSetRunner = new JavaSimpleFileSnippetSetRunner();
    this.view = view;
  }

  @Override
  public Model getModel() {
    return context.getModel();
  }

  @Override
  public <Element> void output(String outputPath, Multimap<Element, GeneratedResult> elements)
      throws IOException {
    String subPath = context.getApiConfig().getPackageName().replace('.', '/');
    String fullOutputPath = outputPath + "/" + subPath;
    CodeGeneratorUtil.writeGeneratedOutput(fullOutputPath, elements);
  }

  @Override
  public InputElementView<Iterable<T>> getView() {
    return view;
  }

  @Override
  public GeneratedResult generate(Iterable<T> element, SnippetDescriptor snippetDescriptor) {
    return simpleFileSnippetSetRunner.generate(element, snippetDescriptor, context);
  }
}
