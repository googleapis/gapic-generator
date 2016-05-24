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
package com.google.api.codegen;

import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import java.io.IOException;

import javax.annotation.Nullable;

/**
 * Code generator.
 */
public class CodeGenerator {

  private final GapicProvider<Object> provider;

  public CodeGenerator(GapicProvider<Object> provider) {
    this.provider = Preconditions.checkNotNull(provider);
  }

  public static CodeGenerator create(ConfigProto configProto, TemplateProto template, Model model) {
    InputElementView<Object> view = GeneratorBuilderUtil.createView(template, model);
    return new CodeGenerator(
        GeneratorBuilderUtil.createCodegenProvider(configProto, template, model, view));
  }

  /**
   * Generates code for the model. Returns a map from service interface to code for the service.
   * Returns null if generation failed.
   */
  @Nullable
  public ImmutableMap<Object, GeneratedResult> generate(SnippetDescriptor snippetDescriptor) {
    Iterable<Object> elements = provider.getView().getElementIterable(provider.getModel());

    // Establish required stage for generation.
    provider.getModel().establishStage(Merged.KEY);
    if (provider.getModel().getErrorCount() > 0) {
      return null;
    }

    // Run the generator for each service.
    ImmutableMap.Builder<Object, GeneratedResult> generated = ImmutableMap.builder();
    for (Object element : elements) {
      GeneratedResult result = provider.generate(element, snippetDescriptor);
      generated.put(element, result);
    }

    // Return result.
    if (provider.getModel().getErrorCount() > 0) {
      return null;
    }
    return generated.build();
  }

  /**
   * Delegates creating code to codegen provider. Takes the result list from
   * {@link GapicProvider#output(String, Multimap)} and stores it in a language-specific
   * way.
   */
  public <Element> void output(String outputFile, Multimap<Element, GeneratedResult> results)
      throws IOException {
    provider.output(outputFile, results);
  }
}
