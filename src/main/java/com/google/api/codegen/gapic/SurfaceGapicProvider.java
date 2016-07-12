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

import com.google.api.codegen.InputElementView;
import com.google.api.codegen.surface.SurfaceDoc;
import com.google.api.codegen.surface.SurfaceSnippetSetRunner;
import com.google.api.codegen.transformer.ModelToSurfaceTransformer;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.snippet.Doc;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SurfaceGapicProvider implements GapicProvider<Interface> {
  private final Model model;
  private final SurfaceSnippetSetRunner snippetSetRunner;
  private final ModelToSurfaceTransformer modelToSurfaceTransformer;

  private SurfaceGapicProvider(
      Model model,
      SurfaceSnippetSetRunner snippetSetRunner,
      ModelToSurfaceTransformer modelToSurfaceTransformer) {
    this.model = model;
    this.snippetSetRunner = snippetSetRunner;
    this.modelToSurfaceTransformer = modelToSurfaceTransformer;
  }

  @Override
  public List<String> getSnippetFileNames() {
    return modelToSurfaceTransformer.getTemplateFileNames();
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

    List<SurfaceDoc> surfaceDocs = modelToSurfaceTransformer.transform(model);
    if (model.getDiagCollector().getErrorCount() > 0) {
      return null;
    }

    Map<String, Doc> docs = new TreeMap<>();
    for (SurfaceDoc surfaceDoc : surfaceDocs) {
      if (snippetFileName != null && !surfaceDoc.getTemplateFileName().equals(snippetFileName)) {
        continue;
      }
      Doc doc = snippetSetRunner.generate(surfaceDoc);
      if (doc == null) {
        continue;
      }
      docs.put(surfaceDoc.getOutputPath(), doc);
    }

    return docs;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Model model;
    private SurfaceSnippetSetRunner snippetSetRunner;
    private ModelToSurfaceTransformer modelToSurfaceTransformer;

    private Builder() {}

    public Builder setModel(Model model) {
      this.model = model;
      return this;
    }

    public Builder setSnippetSetRunner(SurfaceSnippetSetRunner snippetSetRunner) {
      this.snippetSetRunner = snippetSetRunner;
      return this;
    }

    public Builder setModelToSurfaceTransformer(
        ModelToSurfaceTransformer modelToSurfaceTransformer) {
      this.modelToSurfaceTransformer = modelToSurfaceTransformer;
      return this;
    }

    public SurfaceGapicProvider build() {
      return new SurfaceGapicProvider(model, snippetSetRunner, modelToSurfaceTransformer);
    }
  }
}
