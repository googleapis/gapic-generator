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
package com.google.api.codegen.configgen;

import com.google.api.codegen.config.DiscoveryField;
import com.google.api.codegen.config.DiscoveryMethodModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.NullConfigNode;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.util.Name;

/** PageStreamingTransformer implementation for DiscoveryMethodModel. */
public class DiscoPageStreamingTransformer implements PageStreamingTransformer {
  private static final PagingParameters PAGING_PARAMETERS = new HttpPagingParameters();

  @Override
  public String getNameForPageToken() {
    return PAGING_PARAMETERS.getNameForPageToken();
  }

  @Override
  public String getNameForPageSize() {
    return PAGING_PARAMETERS.getNameForPageSize();
  }

  @Override
  public ConfigNode generateResponseValueNode(
      ConfigNode parentNode, MethodModel method, ConfigHelper helper) {
    String resourcesFieldName = getResourcesFieldName((DiscoveryMethodModel) method);

    if (resourcesFieldName == null) {
      return new NullConfigNode();
    }

    ConfigNode tokenFieldNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(parentNode),
            "token_field",
            PAGING_PARAMETERS.getNameForNextPageToken());
    parentNode.setChild(tokenFieldNode);
    ConfigNode resourcesFieldNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(tokenFieldNode), "resources_field", resourcesFieldName);
    return tokenFieldNode.insertNext(resourcesFieldNode);
  }

  private String getResourcesFieldName(DiscoveryMethodModel method) {
    for (DiscoveryField field : method.getOutputFields()) {
      String fieldName = field.getSimpleName();
      if (!fieldName.equals(PAGING_PARAMETERS.getNameForNextPageToken())) {
        for (Schema property : field.getDiscoveryField().properties().values()) {
          if (property.getIdentifier().equals(PAGING_PARAMETERS.getNameForNextPageToken())) {
            return Name.anyCamel(fieldName).toUpperCamel();
          }
        }
      }
    }
    return null;
  }
}
