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
package com.google.api.codegen.configgen;

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.DiscoApiModel;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.NullConfigNode;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Method;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** InterfaceTransformer implementation for DiscoveryInterfaceModels. */
public class DiscoInterfaceTransformer implements InterfaceTransformer {
  @Override
  public Map<String, String> getResourceToEntityNameMap(InterfaceModel apiInterface) {
    DiscoApiModel model = (DiscoApiModel) apiInterface.getApiModel();
    Document document = model.getDocument();
    SetMultimap<String, String> collectionNamePatternMap = getCollectionNamePatternMap(document);
    Map<String, String> collectionNameMap = new TreeMap<>();
    for (Method method : document.methods()) {
      collectionNameMap.put(
          DiscoGapicNamer.getCanonicalPath(method),
          getQualifiedCollectionName(
              method, apiInterface.getSimpleName(), collectionNamePatternMap));
    }
    return collectionNameMap;
  }

  @Override
  public void generateResourceNameGenerations(ConfigNode parentNode, ApiModel model) {
    Document document = ((DiscoApiModel) model).getDocument();
    SetMultimap<String, String> collectionNamePatternMap = getCollectionNamePatternMap(document);
    FieldConfigNode resourceNameGenerationsNode =
        MissingFieldTransformer.append("resource_name_generation", parentNode).generate();
    if (NodeFinder.hasContent(resourceNameGenerationsNode.getChild())) {
      return;
    }

    ConfigNode elementNode = new NullConfigNode();
    ConfigNode prevNode = null;
    for (Map.Entry<String, List<Method>> resource : document.resources().entrySet()) {
      for (Method method : resource.getValue()) {
        if (Strings.isNullOrEmpty(method.path())) {
          continue;
        }

        int startLine = NodeFinder.getNextLine(prevNode == null ? parentNode : prevNode);
        ConfigNode node = new ListItemConfigNode(startLine);
        String messageName = DiscoGapicNamer.getRequestName(method).toUpperCamel();
        ConfigNode messageNameNode =
            FieldConfigNode.createStringPair(startLine, "message_name", messageName);
        String parameterName =
            DiscoGapicNamer.getResourceIdentifier(method.flatPath()).toLowerCamel();
        String collectionName =
            getQualifiedCollectionName(method, resource.getKey(), collectionNamePatternMap);
        ConfigNode fieldEntityMapNode =
            new FieldConfigNode(NodeFinder.getNextLine(messageNameNode), "field_entity_map");
        ConfigNode fieldEntityEntryNode =
            FieldConfigNode.createStringPair(
                NodeFinder.getNextLine(fieldEntityMapNode), parameterName, collectionName);
        fieldEntityMapNode.setChild(fieldEntityEntryNode);
        node.setChild(messageNameNode.insertNext(fieldEntityMapNode));

        if (prevNode == null) {
          resourceNameGenerationsNode.setChild(node);
        } else {
          prevNode.insertNext(node);
        }

        if (!elementNode.isPresent()) {
          elementNode = node;
        }

        prevNode = node;
      }
    }
  }

  /** Creates a map from a collection name of a resource to the name patterns of its methods. */
  private SetMultimap<String, String> getCollectionNamePatternMap(Document document) {
    ImmutableSetMultimap.Builder<String, String> collectionNamePatternMap =
        ImmutableSetMultimap.builder();
    for (Method method : document.methods()) {
      String namePattern = DiscoGapicNamer.getCanonicalPath(method);
      String collectionName =
          DiscoGapicNamer.getResourceIdentifier(method.flatPath()).toLowerCamel();
      collectionNamePatternMap.put(collectionName, namePattern);
    }

    return collectionNamePatternMap.build();
  }

  /** Gets the qualified collection name for a method. */
  private String getQualifiedCollectionName(
      Method method, String parentName, SetMultimap<String, String> collectionNamePatternMap) {
    String collectionName = DiscoGapicNamer.getResourceIdentifier(method.flatPath()).toLowerCamel();
    if (collectionNamePatternMap.get(collectionName).size() == 1) {
      return collectionName;
    }

    // Qualify collection name to avoid naming clashes with other methods with same name pattern.
    return DiscoGapicNamer.getQualifiedResourceIdentifier(method, parentName).toLowerCamel();
  }
}
