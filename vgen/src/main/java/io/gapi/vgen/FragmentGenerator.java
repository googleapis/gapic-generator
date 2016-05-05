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

import com.google.api.tools.framework.model.Method;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;

import java.io.IOException;

/**
 * Fragment generator.
 */
public class FragmentGenerator {

  private final GapicLanguageProvider<Method> provider;

  public FragmentGenerator(GapicLanguageProvider<Method> provider) {
    this.provider = Preconditions.checkNotNull(provider);
  }

  public static FragmentGenerator create(GapicLanguageProvider<Method> provider) {
    return new FragmentGenerator(provider);
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
