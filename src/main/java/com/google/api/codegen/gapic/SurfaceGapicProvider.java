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
import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.InputElementView;
import com.google.api.codegen.surface.Surface;
import com.google.api.codegen.surface.SurfaceSnippetSetRunner;
import com.google.api.codegen.surface.SurfaceXApi;
import com.google.api.codegen.transformer.ModelToJavaSurfaceTransformer;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.snippet.Doc;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

public class SurfaceGapicProvider implements GapicProvider<Interface> {
  private final Model model;
  private final InputElementView<Interface> view;
  private final ApiConfig apiConfig;
  private final SurfaceSnippetSetRunner<SurfaceXApi> snippetSetRunner;
  private final List<String> snippetFileNames;
  private final GapicCodePathMapper pathMapper;

  private SurfaceGapicProvider(
      Model model,
      InputElementView<Interface> view,
      ApiConfig apiConfig,
      SurfaceSnippetSetRunner<SurfaceXApi> snippetSetRunner,
      List<String> snippetFileNames,
      GapicCodePathMapper pathMapper) {
    this.model = model;
    this.view = view;
    this.apiConfig = apiConfig;
    this.snippetSetRunner = snippetSetRunner;
    this.snippetFileNames = snippetFileNames;
    this.pathMapper = pathMapper;
  }

  @Override
  public List<String> getSnippetFileNames() {
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

  @Nullable
  @Override
  public Map<String, Doc> generate(String snippetFileName) {
    Map<String, Doc> docs = new TreeMap<>();
    List<GeneratedResult> generatedOutput = generateSnip(snippetFileName);
    if (generatedOutput == null) {
      return docs;
    }
    for (GeneratedResult result : generatedOutput) {
      docs.put(result.getFilename(), result.getDoc());
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
    for (Interface interfaze : view.getElementIterable(model)) {
      Surface surface = ModelToJavaSurfaceTransformer.defaultTransform(interfaze, apiConfig);
      GeneratedResult result = snippetSetRunner.generate(surface.xapiClass, snippetFileName);

      String subPath = pathMapper.getOutputPath(interfaze, apiConfig);

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

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Model model;
    private InputElementView<Interface> view;
    private ApiConfig apiConfig;
    private SurfaceSnippetSetRunner<SurfaceXApi> snippetSetRunner;
    private List<String> snippetFileNames;
    private GapicCodePathMapper pathMapper;

    private Builder() {}

    public Builder setModel(Model model) {
      this.model = model;
      return this;
    }

    public Builder setView(InputElementView<Interface> view) {
      this.view = view;
      return this;
    }

    public Builder setApiConfig(ApiConfig apiConfig) {
      this.apiConfig = apiConfig;
      return this;
    }

    public Builder setSnippetSetRunner(SurfaceSnippetSetRunner<SurfaceXApi> snippetSetRunner) {
      this.snippetSetRunner = snippetSetRunner;
      return this;
    }

    public Builder setSnippetFileNames(List<String> snippetFileNames) {
      this.snippetFileNames = snippetFileNames;
      return this;
    }

    public Builder setCodePathMapper(GapicCodePathMapper pathMapper) {
      this.pathMapper = pathMapper;
      return this;
    }

    public SurfaceGapicProvider build() {
      return new SurfaceGapicProvider(
          model, view, apiConfig, snippetSetRunner, snippetFileNames, pathMapper);
    }
  }
}
