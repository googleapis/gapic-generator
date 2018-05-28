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

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.common.GeneratedResult;
import com.google.api.codegen.common.OutputProvider;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.snippet.Doc;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Common OutputProvider which runs code generation. */
public class CommonGapicProvider implements OutputProvider<Doc> {
  private final Model model;
  private final GapicContext context;
  private final SnippetSetRunner.Generator<Interface> generator;
  private final List<String> snippetFileNames;
  private final GapicCodePathMapper pathMapper;

  private CommonGapicProvider(
      Model model,
      GapicContext context,
      SnippetSetRunner.Generator<Interface> generator,
      List<String> snippetFileNames,
      GapicCodePathMapper pathMapper) {
    this.model = model;
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
  public Map<String, GeneratedResult<Doc>> generate() {
    Map<String, GeneratedResult<Doc>> docs = new TreeMap<>();

    for (String snippetFileName : snippetFileNames) {
      Map<String, GeneratedResult<Doc>> snippetDocs = generate(snippetFileName);
      docs.putAll(snippetDocs);
    }

    return docs;
  }

  private Map<String, GeneratedResult<Doc>> generate(String snippetFileName) {
    // Establish required stage for generation.
    model.establishStage(Merged.KEY);
    if (model.getDiagCollector().getErrorCount() > 0) {
      return ImmutableMap.of();
    }

    // Run the generator for each service.
    Map<String, GeneratedResult<Doc>> generated = new TreeMap<>();
    for (Interface modelInterface : ProtoModels.getInterfaces(model)) {

      String subPath;
      if (modelInterface == null) {
        subPath = pathMapper.getOutputPath(null, context.getApiConfig());
      } else {
        subPath = pathMapper.getOutputPath(modelInterface.getFullName(), context.getApiConfig());
      }

      Map<String, GeneratedResult<Doc>> result =
          generator.generate(modelInterface, snippetFileName, context);

      for (Map.Entry<String, GeneratedResult<Doc>> resEntry : result.entrySet()) {
        String resSubPath =
            Strings.isNullOrEmpty(subPath) ? resEntry.getKey() : subPath + "/" + resEntry.getKey();
        generated.put(resSubPath, resEntry.getValue());
      }
    }

    // Return result.
    if (model.getDiagCollector().getErrorCount() > 0) {
      return ImmutableMap.of();
    }

    return generated;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Model model;
    private GapicContext context;
    private SnippetSetRunner.Generator<Interface> generator;
    private List<String> snippetFileNames;
    private GapicCodePathMapper pathMapper;

    private Builder() {}

    public Builder setModel(Model model) {
      this.model = model;
      return this;
    }

    public Builder setContext(GapicContext context) {
      this.context = context;
      return this;
    }

    public Builder setSnippetSetRunner(SnippetSetRunner.Generator<Interface> generator) {
      this.generator = generator;
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

    public CommonGapicProvider build() {
      return new CommonGapicProvider(model, context, generator, snippetFileNames, pathMapper);
    }
  }
}
