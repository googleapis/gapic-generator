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

import com.google.api.codegen.common.CodeGenerator;
import com.google.api.codegen.common.GeneratedResult;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.snippet.Doc;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ViewModelGapicProvider implements CodeGenerator<Doc> {
  private final Model model;
  private final GapicProductConfig productConfig;
  private final CommonSnippetSetRunner snippetSetRunner;
  private final ModelToViewTransformer modelToViewTransformer;

  private ViewModelGapicProvider(
      Model model,
      GapicProductConfig productConfig,
      CommonSnippetSetRunner snippetSetRunner,
      ModelToViewTransformer modelToViewTransformer) {
    this.model = model;
    this.productConfig = productConfig;
    this.snippetSetRunner = snippetSetRunner;
    this.modelToViewTransformer = modelToViewTransformer;
  }

  @Override
  public Collection<String> getInputFileNames() {
    return modelToViewTransformer.getTemplateFileNames();
  }

  @Override
  public Map<String, GeneratedResult<Doc>> generate() {
    // Establish required stage for generation.
    model.establishStage(Merged.KEY);
    if (model.getDiagCollector().getErrorCount() > 0) {
      return null;
    }

    List<ViewModel> surfaceDocs =
        modelToViewTransformer.transform(new ProtoApiModel(model), productConfig);
    if (model.getDiagCollector().getErrorCount() > 0) {
      return null;
    }

    Map<String, GeneratedResult<Doc>> results = new TreeMap<>();
    for (ViewModel surfaceDoc : surfaceDocs) {
      results.putAll(snippetSetRunner.generate(surfaceDoc));
    }

    return results;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Model model;
    private GapicProductConfig productConfig;
    private CommonSnippetSetRunner snippetSetRunner;
    private ModelToViewTransformer modelToViewTransformer;

    private Builder() {}

    public Builder setModel(Model model) {
      this.model = model;
      return this;
    }

    public Builder setProductConfig(GapicProductConfig productConfig) {
      this.productConfig = productConfig;
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
      return new ViewModelGapicProvider(
          model, productConfig, snippetSetRunner, modelToViewTransformer);
    }
  }
}
