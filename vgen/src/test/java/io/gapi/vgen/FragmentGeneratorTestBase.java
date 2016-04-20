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

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Method;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Map;

/**
 * Fragment generator baseline tests.
 */
@RunWith(Parameterized.class)
public abstract class FragmentGeneratorTestBase extends GeneratorTestBase {

  public FragmentGeneratorTestBase(String name, String[] veneerConfigFileNames) {
    super(name, veneerConfigFileNames);
  }

  @Override
  protected Object run() {
    GapicLanguageProvider languageProvider = GeneratorBuilderUtil.createLanguageProvider(config, model);
    String snippetInputName = config.getFragmentFilesList().get(0);
    SnippetDescriptor resourceDescriptor =
          new SnippetDescriptor(snippetInputName);
    Map<Method, GeneratedResult> result =
        FragmentGenerator.create(languageProvider).generateFragments(resourceDescriptor);
    if (result == null) {
      // Report diagnosis to baseline file.
      for (Diag diag : model.getDiags()) {
        testOutput().println(diag.toString());
      }
      return null;
    }
    return result.values().iterator().next().getDoc();
  }
}
