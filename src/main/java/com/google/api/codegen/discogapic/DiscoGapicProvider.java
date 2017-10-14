/* Copyright 2017 Google Inc
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
package com.google.api.codegen.discogapic;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.discogapic.transformer.DocumentToViewTransformer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.snippet.Doc;
import com.google.gson.internal.LinkedTreeMap;
import java.util.List;
import java.util.Map;

public class DiscoGapicProvider {
  private final Document document;
  private final GapicProductConfig productConfig;
  private final CommonSnippetSetRunner snippetSetRunner;
  private final List<DocumentToViewTransformer> transformers;

  private DiscoGapicProvider(
      Document document,
      GapicProductConfig productConfig,
      CommonSnippetSetRunner snippetSetRunner,
      List<DocumentToViewTransformer> transformers) {
    this.document = document;
    this.productConfig = productConfig;
    this.snippetSetRunner = snippetSetRunner;
    this.transformers = transformers;
  }

  public Map<String, Doc> generate() {
    return generate(null);
  }

  public Map<String, Doc> generate(String snippetFileName) {
    Map<String, Doc> docs = new LinkedTreeMap<>();

    for (DocumentToViewTransformer transformer : transformers) {
      List<ViewModel> surfaceDocs = transformer.transform(document, productConfig);

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
    }

    return docs;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Document document;
    private GapicProductConfig productConfig;
    private CommonSnippetSetRunner snippetSetRunner;
    private List<DocumentToViewTransformer> transformers;

    private Builder() {}

    public Builder setDocument(Document document) {
      this.document = document;
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
      return new DiscoGapicProvider(document, productConfig, snippetSetRunner, transformers);
    }
  }
}
