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

import com.google.api.codegen.configgen.ConfigHelper;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.NullConfigNode;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;

/** Merges page streaming properties from a Model into a ConfigNode. */
public class PageStreamingMerger {
  private static final String PARAMETER_PAGE_TOKEN = "page_token";
  private static final String PARAMETER_PAGE_SIZE = "page_size";
  private static final String PARAMETER_NEXT_PAGE_TOKEN = "next_page_token";

  public ConfigNode generatePageStreamingNode(
      ConfigNode prevNode, Method method, ConfigHelper helper) {
    ConfigNode pageStreamingNode = new FieldConfigNode("page_streaming");
    ConfigNode requestNode = generatePageStreamingRequestNode(pageStreamingNode, method);
    if (requestNode == null) {
      return prevNode;
    }

    ConfigNode responseNode = generatePageStreamingResponseNode(requestNode, method, helper);
    if (responseNode == null) {
      return prevNode;
    }

    prevNode.insertNext(pageStreamingNode);
    return pageStreamingNode;
  }

  private ConfigNode generatePageStreamingRequestNode(ConfigNode parentNode, Method method) {
    ConfigNode requestNode = new FieldConfigNode("request");
    parentNode.setChild(requestNode);
    ConfigNode requestValueNode = generatePageStreamingRequestValueNode(requestNode, method);
    return requestValueNode.isPresent() ? requestNode : null;
  }

  private ConfigNode generatePageStreamingRequestValueNode(ConfigNode parentNode, Method method) {
    boolean hasTokenField = false;
    boolean hasPageSizeField = false;
    for (Field field : method.getInputMessage().getReachableFields()) {
      String fieldName = field.getSimpleName();
      if (fieldName.equals(PARAMETER_PAGE_TOKEN)) {
        hasTokenField = true;
      } else if (fieldName.equals(PARAMETER_PAGE_SIZE)) {
        hasPageSizeField = true;
      }
    }

    ConfigNode requestValueNode = null;
    if (hasPageSizeField) {
      requestValueNode = FieldConfigNode.createStringPair("page_size_field", PARAMETER_PAGE_SIZE);
      if (hasTokenField) {
        ConfigNode tokenFieldNode =
            FieldConfigNode.createStringPair("token_field", PARAMETER_PAGE_TOKEN);
        requestValueNode.insertNext(tokenFieldNode);
      }
    } else if (hasTokenField) {
      requestValueNode = FieldConfigNode.createStringPair("token_field", PARAMETER_PAGE_TOKEN);
    } else {
      return new NullConfigNode();
    }

    parentNode.setChild(requestValueNode);
    return requestValueNode;
  }

  private ConfigNode generatePageStreamingResponseNode(
      ConfigNode prevNode, Method method, ConfigHelper helper) {
    ConfigNode responseNode = new FieldConfigNode("response");
    ConfigNode responseValueNode =
        generatePageStreamingResponseValueNode(responseNode, method, helper);
    if (!responseValueNode.isPresent()) {
      return null;
    }

    prevNode.insertNext(responseNode);
    return responseNode;
  }

  private ConfigNode generatePageStreamingResponseValueNode(
      ConfigNode parentNode, Method method, ConfigHelper helper) {
    if (!hasResponseTokenField(method)) {
      return new NullConfigNode();
    }

    String resourcesFieldName = getResourcesFieldName(method, helper);
    if (resourcesFieldName == null) {
      return new NullConfigNode();
    }

    ConfigNode tokenFieldNode =
        FieldConfigNode.createStringPair("token_field", PARAMETER_NEXT_PAGE_TOKEN);
    parentNode.setChild(tokenFieldNode);
    ConfigNode resourcesFieldNode =
        FieldConfigNode.createStringPair("resources_field", resourcesFieldName);
    return tokenFieldNode.insertNext(resourcesFieldNode);
  }

  private boolean hasResponseTokenField(Method method) {
    Field tokenField = method.getOutputMessage().lookupField(PARAMETER_NEXT_PAGE_TOKEN);
    return tokenField != null;
  }

  private String getResourcesFieldName(Method method, ConfigHelper helper) {
    String resourcesField = null;
    for (Field field : method.getOutputMessage().getReachableFields()) {
      if (!field.getType().isRepeated()) {
        continue;
      }

      if (resourcesField != null) {
        helper.error(
            method.getLocation(),
            "Page streaming resources field could not be heuristically determined for "
                + "method '%s'%n",
            method.getSimpleName());
        return null;
      }

      resourcesField = field.getSimpleName();
    }
    return resourcesField;
  }
}
