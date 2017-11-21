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

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.configgen.InterfaceTransformer;
import com.google.api.codegen.configgen.ListTransformer;
import com.google.api.codegen.configgen.MissingFieldTransformer;
import com.google.api.codegen.configgen.NodeFinder;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.metadata.DefaultComment;
import java.util.Map;

/** Merges the interfaces property from an ApiModel into a ConfigNode. */
public class InterfaceMerger {
  private final CollectionMerger collectionMerger;
  private final RetryMerger retryMerger;
  private final MethodMerger methodMerger;
  private final InterfaceTransformer interfaceTransformer;

  public InterfaceMerger(
      CollectionMerger collectionMerger,
      RetryMerger retryMerger,
      MethodMerger methodMerger,
      InterfaceTransformer interfaceTransformer) {
    this.collectionMerger = collectionMerger;
    this.retryMerger = retryMerger;
    this.methodMerger = methodMerger;
    this.interfaceTransformer = interfaceTransformer;
  }

  public void mergeInterfaces(ApiModel model, ConfigNode configNode) {
    FieldConfigNode interfacesNode =
        MissingFieldTransformer.append("interfaces", configNode).generate();
    if (NodeFinder.hasChild(interfacesNode)) {
      return;
    }

    ConfigNode interfacesValueNode =
        ListTransformer.generateList(
            model.getInterfaces(), interfacesNode, this::generateInterfaceNode);
    interfacesNode
        .setChild(interfacesValueNode)
        .setComment(new DefaultComment("A list of API interface configurations."));
  }

  private ListItemConfigNode generateInterfaceNode(InterfaceModel apiInterface) {
    Map<String, String> collectionNameMap =
        interfaceTransformer.getResourceToEntityNameMap(apiInterface);
    ListItemConfigNode interfaceNode = new ListItemConfigNode();
    FieldConfigNode nameNode =
        FieldConfigNode.createStringPair("name", apiInterface.getFullName())
            .setComment(new DefaultComment("The fully qualified name of the API interface."));
    interfaceNode.setChild(nameNode);
    ConfigNode collectionsNode =
        collectionMerger.generateCollectionsNode(nameNode, collectionNameMap);
    ConfigNode retryParamsDefNode = retryMerger.generateRetryDefinitionsNode(collectionsNode);
    methodMerger.generateMethodsNode(interfaceNode, apiInterface, collectionNameMap);
    return interfaceNode;
  }
}
