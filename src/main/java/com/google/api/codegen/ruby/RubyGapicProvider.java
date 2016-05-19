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

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.GapicProvider;
import com.google.api.codegen.CodeGeneratorUtil;
import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.InputElementView;
import com.google.api.codegen.SnippetDescriptor;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.ArrayList;

/**
 * The GapicProvider which runs Gapic code generation for Ruby.
 */
public class RubyGapicProvider<InputElementT extends ProtoElement>
    implements GapicProvider<InputElementT> {

  private final RubyGapicContext context;
  private final RubySnippetSetRunner snippetSetRunner;
  private InputElementView<InputElementT> view;

  public RubyGapicProvider(
      Model model, ApiConfig apiConfig, InputElementView<InputElementT> view) {
    this.context = new RubyGapicContext(model, apiConfig);
    this.snippetSetRunner = new RubySnippetSetRunner();
    this.view = view;
  }

  @Override
  public Model getModel() {
    return context.getModel();
  }

  private String getPackageRoot() {
    ArrayList<String> dirs = new ArrayList<>();
    for (String moduleName : context.getApiConfig().getPackageName().split("::")) {
      dirs.add(moduleName.toLowerCase());
    }
    return Joiner.on("/").join(dirs);
  }

  @Override
  public <Element> void output(
      String outputPath, Multimap<Element, GeneratedResult> elements)
      throws IOException {
    String fullOutputPath = outputPath + "/lib/" + getPackageRoot();
    CodeGeneratorUtil.writeGeneratedOutput(fullOutputPath, elements);
  }

  @Override
  public GeneratedResult generate(InputElementT element, SnippetDescriptor snippetDescriptor) {
    return snippetSetRunner.generate(element, snippetDescriptor, context);
  }

  @Override
  public InputElementView<InputElementT> getView() {
    return view;
  }
}
