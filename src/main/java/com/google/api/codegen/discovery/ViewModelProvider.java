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
package com.google.api.codegen.discovery;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.discovery.transformer.MethodToViewTransformer;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.snippet.Doc;
import com.google.protobuf.Method;

/*
 * Calls the MethodToViewTransformer on a method with the provided ApiaryConfig.
 */
public class ViewModelProvider implements DiscoveryProvider {

  private final ApiaryConfig apiaryConfig;
  private final CommonSnippetSetRunner snippetSetRunner;
  private final MethodToViewTransformer methodToViewTransformer;
  private final JsonNode sampleConfigOverrides;
  private final String outputRoot;

  private ViewModelProvider(
      ApiaryConfig apiaryConfig,
      CommonSnippetSetRunner snippetSetRunner,
      MethodToViewTransformer methodToViewTransformer,
      JsonNode sampleConfigOverrides,
      String outputRoot) {
    this.apiaryConfig = apiaryConfig;
    this.snippetSetRunner = snippetSetRunner;
    this.methodToViewTransformer = methodToViewTransformer;
    this.sampleConfigOverrides = sampleConfigOverrides;
    this.outputRoot = outputRoot;
  }

  @Override
  public Map<String, Doc> generate(Method method) {
    // TODO(saicheems): Explain what's going on here!
    SampleConfig sampleConfig = ApiaryConfigToSampleConfigConverter.convert(method, apiaryConfig);
    if (sampleConfigOverrides != null) {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode tree = mapper.valueToTree(sampleConfig);
      merge((ObjectNode) tree, (ObjectNode) sampleConfigOverrides);
      try {
        sampleConfig = mapper.treeToValue(tree, SampleConfig.class);
      } catch (Exception e) {
        throw new RuntimeException("failed to parse config to node, this should never happen");
      }
    }
    ViewModel surfaceDoc = methodToViewTransformer.transform(method, sampleConfig);
    Doc doc = snippetSetRunner.generate(surfaceDoc);
    Map<String, Doc> docs = new TreeMap<>();
    if (doc == null) {
      return docs;
    }
    docs.put(outputRoot + "/" + surfaceDoc.outputPath(), doc);
    return docs;
  }

  /**
   * Overwrites the fields of tree that intersect with those of overrideTree.
   *
   * Values of overrideTree are not modified.
   * The merge process loops through the fields of tree and replaces non-object
   * values with the corresponding value from overrideTree if present. Object
   * values are traversed recursively to replace sub-properties.
   *
   * @param tree the original JsonNode to modify.
   * @param overrideTree the JsonNode with values to overwrite tree with.
   */
  private static void merge(ObjectNode tree, ObjectNode overrideTree) {
    Iterator<String> fieldNames = overrideTree.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      JsonNode primaryValue = tree.get(fieldName);
      if (primaryValue == null) {
        continue;
      } else if (primaryValue.isObject()) {
        JsonNode backupValue = overrideTree.get(fieldName);
        if (backupValue.isObject()) {
          merge((ObjectNode) primaryValue, (ObjectNode) backupValue.deepCopy());
        }
      } else {
        tree.set(fieldName, overrideTree.get(fieldName));
      }
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private ApiaryConfig apiaryConfig;
    private CommonSnippetSetRunner snippetSetRunner;
    private MethodToViewTransformer methodToViewTransformer;
    private JsonNode sampleConfigOverrides;
    private String outputRoot;

    private Builder() {}

    public Builder setApiaryConfig(ApiaryConfig apiaryConfig) {
      this.apiaryConfig = apiaryConfig;
      return this;
    }

    public Builder setSnippetSetRunner(CommonSnippetSetRunner snippetSetRunner) {
      this.snippetSetRunner = snippetSetRunner;
      return this;
    }

    public Builder setMethodToViewTransformer(MethodToViewTransformer methodToViewTransformer) {
      this.methodToViewTransformer = methodToViewTransformer;
      return this;
    }

    public Builder setOverrides(JsonNode overrides) {
      this.sampleConfigOverrides = overrides;
      return this;
    }

    public Builder setOutputRoot(String outputRoot) {
      this.outputRoot = outputRoot;
      return this;
    }

    public ViewModelProvider build() {
      return new ViewModelProvider(
          apiaryConfig,
          snippetSetRunner,
          methodToViewTransformer,
          sampleConfigOverrides,
          outputRoot);
    }
  }
}
