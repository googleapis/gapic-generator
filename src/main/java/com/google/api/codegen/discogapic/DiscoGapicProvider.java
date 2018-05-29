/* Copyright 2017 Google LLC
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
package com.google.api.codegen.discogapic;

import com.google.api.codegen.common.CodeGenerator;
import com.google.api.codegen.common.GeneratedResult;
import com.google.api.codegen.config.DiscoApiModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.discogapic.transformer.DocumentToViewTransformer;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.snippet.Doc;
import com.google.common.collect.ImmutableList;
import com.google.gson.internal.LinkedTreeMap;
import java.util.List;
import java.util.Map;

public class DiscoGapicProvider implements CodeGenerator<Doc> {
  private final DiscoApiModel model;
  private final GapicProductConfig productConfig;
  private final CommonSnippetSetRunner snippetSetRunner;
  private final List<DocumentToViewTransformer> transformers;

  private final List<String> snippetFileNames;

  private DiscoGapicProvider(
      DiscoApiModel model,
      GapicProductConfig productConfig,
      CommonSnippetSetRunner snippetSetRunner,
      List<DocumentToViewTransformer> transformers) {
    this.model = model;
    this.productConfig = productConfig;
    this.snippetSetRunner = snippetSetRunner;
    this.transformers = transformers;

    ImmutableList.Builder<String> snippetFileNames = ImmutableList.builder();
    for (DocumentToViewTransformer transformer : transformers) {
      snippetFileNames.addAll(transformer.getTemplateFileNames());
    }
    this.snippetFileNames = snippetFileNames.build();
  }

  @Override
  public List<String> getInputFileNames() {
    return snippetFileNames;
  }

  @Override
  public Map<String, GeneratedResult<Doc>> generate() {
    Map<String, GeneratedResult<Doc>> results = new LinkedTreeMap<>();

    for (DocumentToViewTransformer transformer : transformers) {
      List<ViewModel> surfaceDocs = transformer.transform(model, productConfig);

      for (ViewModel surfaceDoc : surfaceDocs) {
        results.putAll(snippetSetRunner.generate(surfaceDoc));
      }
    }

    return results;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private DiscoApiModel model;
    private GapicProductConfig productConfig;
    private CommonSnippetSetRunner snippetSetRunner;
    private List<DocumentToViewTransformer> transformers;

    private Builder() {}

    public Builder setDiscoApiModel(DiscoApiModel model) {
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

    public Builder setDocumentToViewTransformers(List<DocumentToViewTransformer> transformers) {
      this.transformers = transformers;
      return this;
    }

    public DiscoGapicProvider build() {
      return new DiscoGapicProvider(model, productConfig, snippetSetRunner, transformers);
    }
  }
}
