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
package io.gapi.vgen;

import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Fragment generator.
 */
public class FragmentGenerator {

  private final GapicLanguageProvider provider;

  public FragmentGenerator(GapicLanguageProvider provider) {
    this.provider = Preconditions.checkNotNull(provider);
  }

  public static FragmentGenerator create(GapicLanguageProvider provider) {
    return new FragmentGenerator(provider);
  }

  /**
   * Generates fragments for the model. Returns a map from each method to a fragment for the method.
   * Returns null if generation failed.
   */
  @Nullable
  public Map<Method, GeneratedResult> generateFragments(SnippetDescriptor snippetDescriptor) {
    // Establish required stage for generation.
    provider.getModel().establishStage(Merged.KEY);
    if (provider.getModel().getErrorCount() > 0) {
      return null;
    }

    // Run the generator for each method of each service.
    ImmutableMap.Builder<Method, GeneratedResult> generated = ImmutableMap.builder();
    for (Interface iface : provider.getModel().getSymbolTable().getInterfaces()) {
      if (!iface.isReachable()) {
        continue;
      }
      for (Method method : iface.getMethods()) {
        GeneratedResult result = provider.generateFragments(method, snippetDescriptor);
        generated.put(method, result);
      }
    }

    // Return result.
    if (provider.getModel().getErrorCount() > 0) {
      return null;
    }
    return generated.build();
  }

  /**
   * Delegates creating fragments to language provider. Takes the result map from
   * {@link GapicLanguageProvider#output} and stores it in a language-specific way.
   */
  public void outputFragments(String outputFile, Multimap<Method, GeneratedResult> methods)
      throws IOException {
    provider.output(outputFile, methods);
  }
}
