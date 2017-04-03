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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.discovery.config.ApiaryConfigToSampleConfigConverter;
import com.google.api.codegen.discovery.config.SampleConfig;
import com.google.api.codegen.discovery.config.SampleOptions;
import com.google.api.codegen.discovery.config.TypeNameGenerator;
import com.google.api.codegen.discovery.transformer.SampleMethodToViewTransformer;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.snippet.Doc;
import com.google.auto.value.AutoValue;
import com.google.protobuf.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*
 * Calls the MethodToViewTransformer on a method with the provided ApiaryConfig.
 *
 * Responsible for generating the SampleConfig, merging with optional overrides,
 * and calling the MethodToViewTransformer.
 */
@AutoValue
public abstract class ViewModelProvider implements DiscoveryProvider {

  public abstract List<Method> methods();

  public abstract ApiaryConfig apiaryConfig();

  public abstract CommonSnippetSetRunner snippetSetRunner();

  public abstract SampleMethodToViewTransformer methodToViewTransformer();

  public abstract List<JsonNode> sampleConfigOverrides();

  public abstract SampleOptions sampleOptions();

  public abstract TypeNameGenerator typeNameGenerator();

  public abstract String outputRoot();

  @Override
  public Map<String, Doc> generate(Method method) {
    // Before the transformer step, we generate the SampleConfig and apply overrides if available.
    // TODO(saicheems): Once all MVVM refactoring is done, change
    // DiscoveryProvider to generate a single SampleConfig and provide one
    // method at a time.
    SampleConfig sampleConfig =
        new ApiaryConfigToSampleConfigConverter(methods(), apiaryConfig(), typeNameGenerator())
            .convert();
    ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());
    JsonNode tree = mapper.valueToTree(sampleConfig);
    tree = override(tree, sampleConfigOverrides());

    if (sampleOptions().noAuth()) {
      ((ObjectNode) tree).put("authType", "NONE");
    }

    try {
      sampleConfig = mapper.treeToValue(tree, SampleConfig.class);
    } catch (Exception e) {
      throw new RuntimeException("failed to parse config to node: " + e.getMessage());
    }
    ViewModel surfaceDoc = methodToViewTransformer().transform(method, sampleConfig);
    Doc doc = snippetSetRunner().generate(surfaceDoc);
    Map<String, Doc> docs = new TreeMap<>();
    if (doc == null) {
      return docs;
    }
    docs.put(outputRoot() + "/" + surfaceDoc.outputPath(), doc);
    return docs;
  }

  /**
   * Applies sampleConfigOverrides to sampleConfig.
   *
   * <p>If sampleConfigOverrides is null, sampleConfig is returned as is.
   */
  private static JsonNode override(JsonNode sampleConfig, List<JsonNode> sampleConfigOverrides) {
    if (sampleConfigOverrides.isEmpty()) {
      return sampleConfig;
    }
    // We use JSON merging to facilitate this override mechanism:
    // 1. Overwrite object fields of the SampleConfig tree where field names match.
    // 2. Convert the modified SampleConfig tree back into a SampleConfig.
    for (JsonNode override : sampleConfigOverrides) {
      merge((ObjectNode) sampleConfig, (ObjectNode) override);
    }
    return sampleConfig;
  }

  /**
   * Overwrites the fields of tree that intersect with those of overrideTree.
   *
   * <p>Values of tree are modified, and values of overrideTree are not modified. The merge process
   * loops through the fields of tree and replaces non-object values with the corresponding value
   * from overrideTree if present. Object values are traversed recursively to replace
   * sub-properties. Null fields in overrideTree are removed from tree.
   *
   * @param tree the original JsonNode to modify.
   * @param overrideTree the JsonNode with values to overwrite tree with.
   */
  private static void merge(ObjectNode tree, ObjectNode overrideTree) {
    Iterator<String> fieldNames = overrideTree.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      JsonNode defaultValue = tree.get(fieldName);
      JsonNode overrideValue = overrideTree.get(fieldName);
      if (defaultValue == null) {
        // If backupValue isn't null, then we add it to tree. This can happen if
        // extra fields/properties are specified.
        if (overrideValue != null) {
          tree.set(fieldName, overrideValue);
        }
        // Otherwise, skip null nodes.
      } else if (overrideValue.isNull()) {
        // If a node is overridden as null, we pretend it was never specified
        // altogether. We provide this functionality so nodes from an object can
        // be deleted from both trees.
        // TODO(saicheems): Verify that this is the best approach for this issue.
        tree.remove(fieldName);
      } else if (defaultValue.isObject() && overrideValue.isObject()) {
        merge((ObjectNode) defaultValue, (ObjectNode) overrideValue.deepCopy());
      } else {
        tree.set(fieldName, overrideTree.get(fieldName));
      }
    }
  }

  public static Builder newBuilder() {
    return new AutoValue_ViewModelProvider.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder methods(List<Method> val);

    public abstract Builder apiaryConfig(ApiaryConfig val);

    public abstract Builder snippetSetRunner(CommonSnippetSetRunner val);

    public abstract Builder methodToViewTransformer(SampleMethodToViewTransformer val);

    public abstract Builder sampleConfigOverrides(List<JsonNode> val);

    public abstract Builder sampleOptions(SampleOptions val);

    public abstract Builder typeNameGenerator(TypeNameGenerator val);

    public abstract Builder outputRoot(String val);

    public abstract ViewModelProvider build();
  }
}
