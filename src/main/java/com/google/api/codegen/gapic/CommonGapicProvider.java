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
package com.google.api.codegen.gapic;

import com.google.api.codegen.GapicContext;
import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.InputElementView;
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Common GapicProvider which runs code generation.
 */
public class CommonGapicProvider<ElementT> implements GapicProvider<ElementT> {
  private final Model model;
  private final InputElementView<ElementT> view;
  private final GapicContext context;
  private final SnippetSetRunner<ElementT> snippetSetRunner;
  private final List<String> snippetFileNames;
  private final String outputSubPath;

  private CommonGapicProvider(
      Model model,
      InputElementView<ElementT> view,
      GapicContext context,
      SnippetSetRunner<ElementT> snippetSetRunner,
      List<String> snippetFileNames,
      String outputSubPath) {
    this.model = model;
    this.view = view;
    this.context = context;
    this.snippetSetRunner = snippetSetRunner;
    this.snippetFileNames = snippetFileNames;
    this.outputSubPath = outputSubPath;
  }

  @Override
  public List<String> getSnippetFileNames() {
    return snippetFileNames;
  }

  @Override
  public void generate(String outputPath) throws Exception {
    Map<String, Doc> docs = new HashMap<>();
    for (String snippetInputName : snippetFileNames) {
      List<GeneratedResult> generatedOutput = generateSnip(snippetInputName);
      if (generatedOutput == null) {
        continue;
      }
      for (GeneratedResult result : generatedOutput) {
        docs.put(result.getFilename(), result.getDoc());
      }
    }

    ToolUtil.writeFiles(docs, outputPath);
  }

  @Nullable
  @Override
  public List<GeneratedResult> generateSnip(String snippetFileName) {
    // Establish required stage for generation.
    model.establishStage(Merged.KEY);
    if (model.getErrorCount() > 0) {
      return null;
    }

    // Run the generator for each service.
    List<GeneratedResult> generated = new ArrayList<>();
    for (ElementT element : view.getElementIterable(model)) {
      GeneratedResult result = snippetSetRunner.generate(element, snippetFileName, context);

      String subPath = outputSubPath;
      if (subPath == null) {
        // Note on usage of instanceof: there is one case (as of this writing)
        // where the element is an Iterable<> instead of a ProtoElement.
        if (element instanceof ProtoElement) {
          subPath = context.getOutputSubPath((ProtoElement) element);
        } else {
          subPath = context.getOutputSubPath(null);
        }
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
    if (model.getErrorCount() > 0) {
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
    private SnippetSetRunner<ElementT> snippetSetRunner;
    private List<String> snippetFileNames;
    private String outputSubPath;

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

    public Builder<ElementT> setSnippetSetRunner(SnippetSetRunner<ElementT> snippetSetRunner) {
      this.snippetSetRunner = snippetSetRunner;
      return this;
    }

    public Builder<ElementT> setSnippetFileNames(List<String> snippetFileNames) {
      this.snippetFileNames = snippetFileNames;
      return this;
    }

    /**
     * Optional. Overrides the path normally acquired from the GapicContext.
     */
    public Builder<ElementT> setOutputSubPath(String outputSubPath) {
      this.outputSubPath = outputSubPath;
      return this;
    }

    public CommonGapicProvider<ElementT> build() {
      return new CommonGapicProvider<ElementT>(
          model, view, context, snippetSetRunner, snippetFileNames, outputSubPath);
    }
  }
}
