/* Copyright 2017 Google LLC
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
package com.google.api.codegen.configgen.mergers;

import com.google.api.codegen.configgen.CollectionPattern;
import com.google.api.codegen.configgen.ConfigHelper;
import com.google.api.codegen.configgen.ListTransformer;
import com.google.api.codegen.configgen.MissingFieldTransformer;
import com.google.api.codegen.configgen.NodeFinder;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.metadata.DefaultComment;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Api;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Merges the interfaces property from a Model into a ConfigNode. */
public class InterfaceMerger {
  private final CollectionMerger collectionMerger = new CollectionMerger();
  private final RetryMerger retryMerger = new RetryMerger();
  private final MethodMerger methodMerger = new MethodMerger();

  public void mergeInterfaces(final Model model, ConfigNode configNode, final ConfigHelper helper) {
    FieldConfigNode interfacesNode =
        MissingFieldTransformer.append("interfaces", configNode).generate();
    if (NodeFinder.hasChild(interfacesNode)) {
      return;
    }

    ConfigNode interfacesValueNode =
        ListTransformer.generateList(
            model.getServiceConfig().getApisList(),
            interfacesNode,
            new ListTransformer.ElementTransformer<Api>() {
              @Override
              public ConfigNode generateElement(Api api) {
                return generateInterfaceNode(model, api.getName(), helper);
              }
            });
    interfacesNode
        .setChild(interfacesValueNode)
        .setComment(new DefaultComment("A list of API interface configurations."));
  }

  private ListItemConfigNode generateInterfaceNode(
      Model model, String apiName, ConfigHelper helper) {
    Interface apiInterface = model.getSymbolTable().lookupInterface(apiName);
    Map<String, String> collectionNameMap =
        getResourceToEntityNameMap(apiInterface.getReachableMethods());
    ListItemConfigNode interfaceNode = new ListItemConfigNode();
    FieldConfigNode nameNode =
        FieldConfigNode.createStringPair("name", apiName)
            .setComment(new DefaultComment("The fully qualified name of the API interface."));
    interfaceNode.setChild(nameNode);
    ConfigNode collectionsNode =
        collectionMerger.generateCollectionsNode(nameNode, collectionNameMap);
    ConfigNode retryParamsDefNode = retryMerger.generateRetryDefinitionsNode(collectionsNode);
    methodMerger.generateMethodsNode(interfaceNode, apiInterface, collectionNameMap, helper);
    return interfaceNode;
  }

  /**
   * Examines all of the resource paths used by the methods, and returns a map from each unique
   * resource paths to a short name used by the collection configuration.
   */
  private static Map<String, String> getResourceToEntityNameMap(Iterable<Method> methods) {
    // Using a map with the string representation of the resource path to avoid duplication
    // of equivalent paths.
    // Using a TreeMap in particular so that the ordering is deterministic
    // (useful for testability).
    Map<String, CollectionPattern> specs = new TreeMap<>();
    for (Method method : methods) {
      for (CollectionPattern collectionPattern :
          CollectionPattern.getCollectionPatternsFromMethod(method)) {
        String resourcePath = collectionPattern.getTemplatizedResourcePath();
        // If there are multiple field segments with the same resource path, the last
        // one will be used, making the output deterministic. Also, the first field path
        // encountered tends to be simply "name" because it is the corresponding create
        // API method for the type.
        specs.put(resourcePath, collectionPattern);
      }
    }

    Set<String> usedNameSet = new HashSet<>();
    ImmutableMap.Builder<String, String> nameMapBuilder = ImmutableMap.builder();
    for (CollectionPattern collectionPattern : specs.values()) {
      String resourceNameString = collectionPattern.getTemplatizedResourcePath();
      String entityNameString = collectionPattern.getUniqueName(usedNameSet);
      usedNameSet.add(entityNameString);
      nameMapBuilder.put(resourceNameString, entityNameString);
    }
    return nameMapBuilder.build();
  }
}
