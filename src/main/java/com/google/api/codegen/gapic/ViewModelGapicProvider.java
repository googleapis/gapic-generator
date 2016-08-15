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

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.snippet.Doc;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ViewModelGapicProvider implements GapicProvider<Interface> {
  private final Model model;
  private final ApiConfig apiConfig;
  private final CommonSnippetSetRunner snippetSetRunner;
  private final ModelToViewTransformer modelToViewTransformer;

  private ViewModelGapicProvider(
      Model model,
      ApiConfig apiConfig,
      CommonSnippetSetRunner snippetSetRunner,
      ModelToViewTransformer modelToViewTransformer) {
    this.model = model;
    this.apiConfig = apiConfig;
    this.snippetSetRunner = snippetSetRunner;
    this.modelToViewTransformer = modelToViewTransformer;
  }

  @Override
  public List<String> getSnippetFileNames() {
    return modelToViewTransformer.getTemplateFileNames();
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

    List<ViewModel> surfaceDocs = modelToViewTransformer.transform(model, apiConfig);
    if (model.getDiagCollector().getErrorCount() > 0) {
      return null;
    }

    Map<String, Doc> docs = new TreeMap<>();
    for (ViewModel surfaceDoc : surfaceDocs) {
      if (snippetFileName != null && !surfaceDoc.templateFileName().equals(snippetFileName)) {
        continue;
      }
      Doc doc = snippetSetRunner.generate(surfaceDoc);
      if (doc == null) {
        // generation failed; failures are captured in the model.
        continue;
      }
      docs.put(surfaceDoc.outputPath(), doc);
    }

    return docs;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Model model;
    private ApiConfig apiConfig;
    private CommonSnippetSetRunner snippetSetRunner;
    private ModelToViewTransformer modelToViewTransformer;

    private Builder() {}

    public Builder setModel(Model model) {
      this.model = model;
      return this;
    }

    public Builder setApiConfig(ApiConfig apiConfig) {
      this.apiConfig = apiConfig;
      return this;
    }

    public Builder setSnippetSetRunner(CommonSnippetSetRunner snippetSetRunner) {
      this.snippetSetRunner = snippetSetRunner;
      return this;
    }

    public Builder setModelToViewTransformer(ModelToViewTransformer modelToViewTransformer) {
      this.modelToViewTransformer = modelToViewTransformer;
      return this;
    }

    public ViewModelGapicProvider build() {
      return new ViewModelGapicProvider(model, apiConfig, snippetSetRunner, modelToViewTransformer);
    }
  }
}
