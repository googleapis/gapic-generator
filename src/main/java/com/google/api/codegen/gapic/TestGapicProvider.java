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

import com.google.api.codegen.transformer.Transformer;
import com.google.api.codegen.viewmodel.SurfaceSnippetSetRunner;
import com.google.api.codegen.viewmodel.ViewModelDoc;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.snippet.Doc;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** An implementation of GapicProvider for automated tests */
public class TestGapicProvider implements GapicProvider<Interface> {
  private final Model model;
  private final SurfaceSnippetSetRunner snippetSetRunner;
  private final Transformer transformer;

  private TestGapicProvider(
      Model model, SurfaceSnippetSetRunner snippetSetRunner, Transformer transformer) {
    this.model = model;
    this.snippetSetRunner = snippetSetRunner;
    this.transformer = transformer;
  }

  @Override
  public List<String> getSnippetFileNames() {
    return transformer.getTemplateFileNames();
  }

  @Override
  public Map<String, Doc> generate() {
    return generate(null);
  }

  @Override
  public Map<String, Doc> generate(String snippetFileName) {
    // Establish required stage for generation.
    model.establishStage(Merged.KEY);
    if (model.getDiagCollector().getErrorCount() > 0) {
      return null;
    }

    List<ViewModelDoc> viewModels = transformer.transform(model);
    if (model.getDiagCollector().getErrorCount() > 0) {
      return null;
    }

    Map<String, Doc> docs = new TreeMap<>();
    for (ViewModelDoc viewModel : viewModels) {
      if (snippetFileName != null && !viewModel.getTemplateFileName().equals(snippetFileName)) {
        continue;
      }
      Doc doc = snippetSetRunner.generate(viewModel);
      if (doc == null) {
        continue;
      }
      docs.put(viewModel.getOutputPath(), doc);
    }

    return docs;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Model model;
    private SurfaceSnippetSetRunner snippetSetRunner;
    private Transformer transformer;

    private Builder() {}

    public Builder setModel(Model model) {
      this.model = model;
      return this;
    }

    public Builder setSnippetSetRunner(SurfaceSnippetSetRunner snippetSetRunner) {
      this.snippetSetRunner = snippetSetRunner;
      return this;
    }

    public Builder setModelToSurfaceTransformer(Transformer modelToSurfaceTransformer) {
      this.transformer = modelToSurfaceTransformer;
      return this;
    }

    public TestGapicProvider build() {
      return new TestGapicProvider(model, snippetSetRunner, transformer);
    }
  }
}
