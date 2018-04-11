/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.gapic;

import com.google.api.codegen.GapicContext;
import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.InputElementView;
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.snippet.Doc;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Common GapicProvider which runs code generation. */
public class CommonGapicProvider<ElementT> implements GapicProvider<Doc> {
  private final Model model;
  private final InputElementView<ElementT> view;
  private final GapicContext context;
  private final SnippetSetRunner.Generator<ElementT> generator;
  private final List<String> snippetFileNames;
  private final GapicCodePathMapper pathMapper;

  private CommonGapicProvider(
      Model model,
      InputElementView<ElementT> view,
      GapicContext context,
      SnippetSetRunner.Generator<ElementT> generator,
      List<String> snippetFileNames,
      GapicCodePathMapper pathMapper) {
    this.model = model;
    this.view = view;
    this.context = context;
    this.generator = generator;
    this.snippetFileNames = snippetFileNames;
    this.pathMapper = pathMapper;
  }

  @Override
  public List<String> getInputFileNames() {
    return snippetFileNames;
  }

  @Override
  public Map<String, GeneratedResult<Doc>> generate() {
    Map<String, GeneratedResult<Doc>> docs = new TreeMap<>();

    for (String snippetFileName : snippetFileNames) {
      Map<String, GeneratedResult<Doc>> snippetDocs = generate(snippetFileName);
      docs.putAll(snippetDocs);
    }

    return docs;
  }

  private Map<String, GeneratedResult<Doc>> generate(String snippetFileName) {
    // Establish required stage for generation.
    model.establishStage(Merged.KEY);
    if (model.getDiagCollector().getErrorCount() > 0) {
      return ImmutableMap.of();
    }

    // Run the generator for each service.
    Map<String, GeneratedResult<Doc>> generated = new TreeMap<>();
    for (ElementT element : view.getElementIterable(model)) {

      String subPath;
      // Note on usage of instanceof: there is one case (as of this writing)
      // where the element is an Iterable<> instead of a ProtoElement.
      if (element instanceof ProtoElement) {
        subPath =
            pathMapper.getOutputPath(
                ((ProtoElement) element).getFullName(), context.getApiConfig());
      } else {
        subPath = pathMapper.getOutputPath(null, context.getApiConfig());
      }

      Map<String, GeneratedResult<Doc>> result =
          generator.generate(element, snippetFileName, context);

      for (Map.Entry<String, GeneratedResult<Doc>> resEntry : result.entrySet()) {
        String resSubPath =
            Strings.isNullOrEmpty(subPath) ? resEntry.getKey() : subPath + "/" + resEntry.getKey();
        generated.put(resSubPath, resEntry.getValue());
      }
    }

    // Return result.
    if (model.getDiagCollector().getErrorCount() > 0) {
      return ImmutableMap.of();
    }

    return generated;
  }

  public static <ElementT> Builder<ElementT> newBuilder() {
    return new Builder<>();
  }

  public static class Builder<ElementT> {
    private Model model;
    private InputElementView<ElementT> view;
    private GapicContext context;
    private SnippetSetRunner.Generator<ElementT> generator;
    private List<String> snippetFileNames;
    private GapicCodePathMapper pathMapper;

    private Builder() {}

    public Builder<ElementT> setModel(Model model) {
      this.model = model;
      return this;
    }

    public Builder<ElementT> setView(InputElementView<ElementT> view) {
      this.view = view;
      return this;
    }

    public Builder<ElementT> setContext(GapicContext context) {
      this.context = context;
      return this;
    }

    public Builder<ElementT> setSnippetSetRunner(SnippetSetRunner.Generator<ElementT> generator) {
      this.generator = generator;
      return this;
    }

    public Builder<ElementT> setSnippetFileNames(List<String> snippetFileNames) {
      this.snippetFileNames = snippetFileNames;
      return this;
    }

    public Builder<ElementT> setCodePathMapper(GapicCodePathMapper pathMapper) {
      this.pathMapper = pathMapper;
      return this;
    }

    public CommonGapicProvider<ElementT> build() {
      return new CommonGapicProvider<ElementT>(
          model, view, context, generator, snippetFileNames, pathMapper);
    }
  }
}
