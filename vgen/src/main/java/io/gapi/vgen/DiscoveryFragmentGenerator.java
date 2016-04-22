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

import com.google.api.Service;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Api;
import com.google.protobuf.Method;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Discovery doc code fragment generator.
 *
 * It uses the method and type data in the Discovery doc to create code snippets following the
 * commonly used patterns in the API client libraries. Exceptions to these patterns are handled in a
 * case-by-case basis.
 */
public class DiscoveryFragmentGenerator {

  private final DiscoveryLanguageProvider provider;

  public DiscoveryFragmentGenerator(DiscoveryLanguageProvider provider) {
    this.provider = Preconditions.checkNotNull(provider);
  }

  public static DiscoveryFragmentGenerator create(
      ConfigProto configProto, DiscoveryImporter discovery) {
    Preconditions.checkNotNull(configProto);
    Preconditions.checkNotNull(discovery);

    ApiaryConfig apiaryConfig = discovery.getConfig();

    DiscoveryLanguageProvider languageProvider =
        GeneratorBuilderUtil.createLanguageProvider(
            configProto.getLanguageProvider(),
            DiscoveryLanguageProvider.class,
            new Class<?>[] {Service.class, ApiaryConfig.class},
            new Object[] {discovery.getService(), apiaryConfig},
            new GeneratorBuilderUtil.ErrorReporter() {
              @Override
              public void error(String message, Object... args) {
                System.err.printf(message, args);
              }
            });
    if (languageProvider == null) {
      return null;
    }

    return new DiscoveryFragmentGenerator(languageProvider);
  }

  /**
   * Generates fragments for the model. Returns a map from each method to a fragment for the method.
   * Returns null if generation failed.
   */
  @Nullable
  public Map<Method, GeneratedResult> generateFragments(SnippetDescriptor snippetDescriptor) {
    // Run the generator for each method of each API.
    ImmutableMap.Builder<Method, GeneratedResult> generated = ImmutableMap.builder();
    for (Api api : provider.getService().getApisList()) {
      for (Method method : api.getMethodsList()) {
        GeneratedResult result = provider.generateFragments(method, snippetDescriptor);
        generated.put(method, result);
      }
    }

    return generated.build();
  }

  /**
   * Delegates creating fragments to language provider. Takes the result map from
   * {@link DiscoveryContext#output} and stores it in a language-specific way.
   */
  public void outputFragments(String outputFile, Multimap<Method, GeneratedResult> methods)
      throws IOException {
    provider.output(outputFile, methods);
  }
}
