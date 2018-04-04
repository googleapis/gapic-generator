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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nullable;

/** Common GapicProvider which runs code generation. */
public class CommonGapicProvider<ElementT> implements GapicProvider {
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
  public Map<String, Doc> generate() {
    Map<String, Doc> docs = new TreeMap<>();

    for (String snippetFileName : snippetFileNames) {
      Map<String, Doc> snippetDocs = generate(snippetFileName);
      docs.putAll(snippetDocs);
    }

    return docs;
  }

  @Override
  public Set<String> getOutputExecutableNames() {
    return Collections.emptySet();
  }

  private Map<String, Doc> generate(String snippetFileName) {
    Map<String, Doc> docs = new TreeMap<>();
    List<GeneratedResult> generatedOutput = generateSnip(snippetFileName);
    if (generatedOutput == null) {
      return docs;
    }
    for (GeneratedResult result : generatedOutput) {
      if (!result.getDoc().isWhitespace()) {
        docs.put(result.getFilename(), result.getDoc());
      }
    }
    return docs;
  }

  @Nullable
  private List<GeneratedResult> generateSnip(String snippetFileName) {
    // Establish required stage for generation.
    model.establishStage(Merged.KEY);
    if (model.getDiagCollector().getErrorCount() > 0) {
      return null;
    }

    // Run the generator for each service.
    List<GeneratedResult> generated = new ArrayList<>();
    for (ElementT element : view.getElementIterable(model)) {
      GeneratedResult result = generator.generate(element, snippetFileName, context);

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

      if (!Strings.isNullOrEmpty(subPath)) {
        subPath = subPath + "/" + result.getFilename();
      } else {
        subPath = result.getFilename();
      }

      GeneratedResult outputResult = GeneratedResult.create(result.getDoc(), subPath);
      generated.add(outputResult);
    }

    // Return result.
    if (model.getDiagCollector().getErrorCount() > 0) {
      return null;
    }

    return generated;
  }

  public static <ElementT> Builder<ElementT> newBuilder() {
    return new Builder<ElementT>();
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
