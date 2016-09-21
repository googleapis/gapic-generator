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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.discovery.config.ApiaryConfigToSampleConfigConverter;
import com.google.api.codegen.discovery.config.SampleConfig;
import com.google.api.codegen.discovery.config.TypeNameGenerator;
import com.google.api.codegen.discovery.transformer.SampleMethodToViewTransformer;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.snippet.Doc;
import com.google.protobuf.Method;

/*
 * Calls the MethodToViewTransformer on a method with the provided ApiaryConfig.
 *
 * Responsible for generating the SampleConfig, merging with optional overrides,
 * and calling the MethodToViewTransformer.
 */
public class ViewModelProvider implements DiscoveryProvider {

  private final ApiaryConfig apiaryConfig;
  private final CommonSnippetSetRunner snippetSetRunner;
  private final SampleMethodToViewTransformer methodToViewTransformer;
  private final JsonNode sampleConfigOverrides;
  private final TypeNameGenerator typeNameGenerator;
  private final String outputRoot;

  private ViewModelProvider(
      ApiaryConfig apiaryConfig,
      CommonSnippetSetRunner snippetSetRunner,
      SampleMethodToViewTransformer methodToViewTransformer,
      JsonNode sampleConfigOverrides,
      TypeNameGenerator typeNameGenerator,
      String outputRoot) {
    this.apiaryConfig = apiaryConfig;
    this.snippetSetRunner = snippetSetRunner;
    this.methodToViewTransformer = methodToViewTransformer;
    this.sampleConfigOverrides = sampleConfigOverrides;
    this.typeNameGenerator = typeNameGenerator;
    this.outputRoot = outputRoot;
  }

  @Override
  public Map<String, Doc> generate(Method method) {
    // Before the transformer step, we generate the SampleConfig and apply overrides if available.
    SampleConfig sampleConfig =
        new ApiaryConfigToSampleConfigConverter(
                Collections.singletonList(method), apiaryConfig, typeNameGenerator)
            .convert();
    sampleConfig = override(sampleConfig, sampleConfigOverrides);
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
   * Applies sampleConfigOverrides to sampleConfig.
   *
   * If sampleConfigOverrides is null, sampleConfig is returned as is.
   */
  private static SampleConfig override(SampleConfig sampleConfig, JsonNode sampleConfigOverrides) {
    // We use JSON merging to facilitate this override mechanism:
    // 1. Convert the SampleConfig into a JSON tree.
    // 2. Convert the overrides file into a JSON tree with arbitrary schema.
    // 3. Overwrite object fields of the SampleConfig tree where field names match.
    // 4. Convert the modified SampleConfig tree back into a SampleConfig.
    if (sampleConfigOverrides != null) {
      ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());
      JsonNode tree = mapper.valueToTree(sampleConfig);
      merge((ObjectNode) tree, (ObjectNode) sampleConfigOverrides);
      try {
        return mapper.treeToValue(tree, SampleConfig.class);
      } catch (Exception e) {
        throw new RuntimeException("failed to parse config to node: " + e.getMessage());
      }
    }
    return sampleConfig;
  }

  /**
   * Overwrites the fields of tree that intersect with those of overrideTree.
   *
   * Values of tree are modified, and values of overrideTree are not modified.
   * The merge process loops through the fields of tree and replaces non-object
   * values with the corresponding value from overrideTree if present. Object
   * values are traversed recursively to replace sub-properties.
   * Null fields in overrideTree are removed from tree.
   *
   * @param tree the original JsonNode to modify.
   * @param overrideTree the JsonNode with values to overwrite tree with.
   */
  private static void merge(ObjectNode tree, ObjectNode overrideTree) {
    Iterator<String> fieldNames = overrideTree.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      JsonNode primaryValue = tree.get(fieldName);
      JsonNode backupValue = overrideTree.get(fieldName);
      if (primaryValue == null) {
        // If backupValue isn't null, then we add it to tree. This can happen if
        // extra fields/properties are specified.
        if (backupValue != null) {
          tree.set(fieldName, backupValue);
        }
        // Otherwise, skip null nodes.
      } else if (backupValue.isNull()) {
        // If a node is overridden as null, we pretend it was never specified
        // altogether. We provide this functionality so nodes from an object can
        // be deleted from both trees.
        // TODO(saicheems): Verify that this is the best approach for this issue.
        tree.remove(fieldName);
      } else if (primaryValue.isObject() && backupValue.isObject()) {
        merge((ObjectNode) primaryValue, (ObjectNode) backupValue.deepCopy());
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
    private SampleMethodToViewTransformer methodToViewTransformer;
    private JsonNode sampleConfigOverrides;
    private TypeNameGenerator typeNameGenerator;
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

    public Builder setMethodToViewTransformer(
        SampleMethodToViewTransformer methodToViewTransformer) {
      this.methodToViewTransformer = methodToViewTransformer;
      return this;
    }

    public Builder setOverrides(JsonNode overrides) {
      this.sampleConfigOverrides = overrides;
      return this;
    }

    public Builder setTypeNameGenerator(TypeNameGenerator typeNameGenerator) {
      this.typeNameGenerator = typeNameGenerator;
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
          typeNameGenerator,
          outputRoot);
    }
  }
}
