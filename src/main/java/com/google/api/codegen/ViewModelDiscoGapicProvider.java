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
package com.google.api.codegen;

import com.google.api.codegen.config.DiscoApiModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.gapic.GapicProvider;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.snippet.Doc;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ViewModelDiscoGapicProvider implements GapicProvider<Document> {
  private final Document model;
  private final GapicProductConfig productConfig;
  private final CommonSnippetSetRunner snippetSetRunner;
  private final ModelToViewTransformer modelToViewTransformer;
  private final DiagCollector diagCollector;

  private ViewModelDiscoGapicProvider(
      Document model,
      GapicProductConfig productConfig,
      CommonSnippetSetRunner snippetSetRunner,
      ModelToViewTransformer modelToViewTransformer) {
    this.model = model;
    this.productConfig = productConfig;
    this.snippetSetRunner = snippetSetRunner;
    this.modelToViewTransformer = modelToViewTransformer;
    this.diagCollector = new BoundedDiagCollector();
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
    List<ViewModel> surfaceDocs =
        modelToViewTransformer.transform(new DiscoApiModel(model), productConfig);
    if (diagCollector.getErrorCount() > 0) {
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
    private Document model;
    private GapicProductConfig productConfig;
    private CommonSnippetSetRunner snippetSetRunner;
    private ModelToViewTransformer modelToViewTransformer;

    private Builder() {}

    public Builder setModel(Document model) {
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

    public ViewModelDiscoGapicProvider build() {
      return new ViewModelDiscoGapicProvider(
          model, productConfig, snippetSetRunner, modelToViewTransformer);
    }
  }
}
