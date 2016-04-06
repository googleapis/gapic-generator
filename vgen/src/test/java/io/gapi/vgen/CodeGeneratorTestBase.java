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
package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.common.truth.Truth;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Base class for code generator baseline tests.
 */
@RunWith(Parameterized.class)
public abstract class CodeGeneratorTestBase extends GeneratorTestBase {

  public CodeGeneratorTestBase(String name, String[] veneerConfigFileNames, String snippetName) {
    super(name, veneerConfigFileNames, snippetName);
  }

  public CodeGeneratorTestBase(String name, String[] veneerConfigFileNames) {
    super(name, veneerConfigFileNames);
  }

  protected GeneratedResult generateForSnippet(int index) {
    Map<?, GeneratedResult> result = generateForSnippet(
        config.getSnippetFilesList(), index, false);
    Truth.assertThat(result.size()).isEqualTo(1);
    return result.values().iterator().next();
  }

  protected List<GeneratedResult> generateForDocSnippet(int index) {
    TreeMap<String, GeneratedResult> result = new TreeMap(
        (Map<String, GeneratedResult>) generateForSnippet(
            config.getDocSnippetFilesList(), index, true));
    return new ArrayList(result.values());
  }

  private Map<?, GeneratedResult> generateForSnippet(List<String> snippetInputNames, int index,
      boolean doc) {
    if (index >= snippetInputNames.size()) {
      return null;
    }
    String snippetInputName = snippetInputNames.get(index);
    SnippetDescriptor resourceDescriptor =
          new SnippetDescriptor(snippetInputName);
    Map<?, GeneratedResult> result = null;
    if (doc) {
      result = CodeGenerator.create(config, model).generateDocs(resourceDescriptor);
    } else {
      result = CodeGenerator.create(config, model).generate(resourceDescriptor);
    }
    if (result == null) {
      // Report diagnosis to baseline file.
      for (Diag diag : model.getDiags()) {
        testOutput().println(diag.toString());
      }
      return null;
    }
    return result;
  }

  @Override
  protected Object run() {
    GeneratedResult result = generateForSnippet(0);
    Truth.assertThat(result).isNotNull();
    return result.getDoc();
  }
}
