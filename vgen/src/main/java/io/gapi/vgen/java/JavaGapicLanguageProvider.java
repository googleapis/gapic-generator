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
package io.gapi.vgen.java;

import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.collect.Multimap;

import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.GapicLanguageProvider;
import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.SnippetDescriptor;

import java.io.IOException;

/**
 * The LanguageProvider which runs Gapic code generation for Java.
 */
public class JavaGapicLanguageProvider implements GapicLanguageProvider {

  private final JavaGapicContext context;
  private final JavaLanguageProvider provider;

  public JavaGapicLanguageProvider(Model model, ApiConfig apiConfig) {
    this.context = new JavaGapicContext(model, apiConfig);
    this.provider = new JavaLanguageProvider();
  }

  @Override
  public Model getModel() {
    return context.getModel();
  }

  @Override
  public <Element> void output(String outputPath, Multimap<Element, GeneratedResult> elements)
      throws IOException {
    String root = context.getApiConfig().getPackageName().replace('.', '/');
    provider.output(root, outputPath, elements);
  }

  @Override
  public GeneratedResult generateCode(Interface service, SnippetDescriptor snippetDescriptor) {
    return provider.generate(
        service, snippetDescriptor, context, context.getApiConfig().getPackageName() + ".");
  }

  @Override
  public GeneratedResult generateFragments(Method method, SnippetDescriptor snippetDescriptor) {
    return provider.generate(method, snippetDescriptor, context);
  }

  @Override
  public GeneratedResult generateDoc(ProtoFile file, SnippetDescriptor descriptor) {
    return null;
  }
}
