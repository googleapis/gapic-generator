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

import com.google.api.codegen.CodeGenerator;
import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.ProtoElementComparator;
import com.google.api.codegen.SnippetDescriptor;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.common.truth.Truth;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Base class for code generator baseline tests.
 */
@RunWith(Parameterized.class)
public abstract class CodeGeneratorTestBase extends GeneratorTestBase {

  public CodeGeneratorTestBase(
      String name,
      String[] gapicConfigFileNames,
      String gapicLanguageProviderName,
      String viewName,
      String snippetName) {
    super(name, gapicConfigFileNames, gapicLanguageProviderName, viewName, snippetName);
  }

  public CodeGeneratorTestBase(String name, String[] gapicConfigFileNames) {
    super(name, gapicConfigFileNames);
  }

  // TODO: specify indices through strings, not ints
  protected List<GeneratedResult> generateForTemplate(int templateIndex, int snippetIndex) {
    List<TemplateProto> templates = config.getTemplatesList();

    if (templateIndex >= templates.size()) {
      return null;
    }
    TemplateProto template = templates.get(templateIndex);

    if (snippetIndex >= template.getSnippetFilesCount()) {
      return null;
    }
    String snippetInputName = template.getSnippetFiles(snippetIndex);
    SnippetDescriptor resourceDescriptor = new SnippetDescriptor(snippetInputName);

    Map<ProtoElement, GeneratedResult> output =
        CodeGenerator.create(config, template, model).generate(resourceDescriptor);
    if (output == null) {
      // Report diagnosis to baseline file.
      for (Diag diag : model.getDiags()) {
        testOutput().println(diag.toString());
      }
    }

    Map<ProtoElement, GeneratedResult> result = new TreeMap<>(new ProtoElementComparator());
    result.putAll(output);

    return new ArrayList<GeneratedResult>(result.values());
  }

  @Override
  protected Object run() {
    List<GeneratedResult> result = generateForTemplate(0, 0);
    Truth.assertThat(result).isNotNull();
    return result.get(0).getDoc();
  }
}
