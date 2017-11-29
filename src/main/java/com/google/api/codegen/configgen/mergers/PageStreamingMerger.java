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

import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.configgen.PageStreamingTransformer;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.NullConfigNode;
import com.google.api.tools.framework.model.DiagCollector;

/** Merges page streaming properties from a MethodModel into a ConfigNode. */
public class PageStreamingMerger {
  private final PageStreamingTransformer pageStreamingTransformer;
  private final DiagCollector diagCollector;

  public PageStreamingMerger(
      PageStreamingTransformer pageStreamingTransformer, DiagCollector diagCollector) {
    this.pageStreamingTransformer = pageStreamingTransformer;
    this.diagCollector = diagCollector;
  }

  public ConfigNode generatePageStreamingNode(ConfigNode prevNode, MethodModel method) {
    ConfigNode pageStreamingNode = new FieldConfigNode("page_streaming");
    ConfigNode requestNode = generatePageStreamingRequestNode(pageStreamingNode, method);
    if (requestNode == null) {
      return prevNode;
    }

    ConfigNode responseNode = generatePageStreamingResponseNode(requestNode, method);
    if (responseNode == null) {
      return prevNode;
    }

    prevNode.insertNext(pageStreamingNode);
    return pageStreamingNode;
  }

  private ConfigNode generatePageStreamingRequestNode(ConfigNode parentNode, MethodModel method) {
    ConfigNode requestNode = new FieldConfigNode("request");
    parentNode.setChild(requestNode);
    ConfigNode requestValueNode = generatePageStreamingRequestValueNode(requestNode, method);
    return requestValueNode.isPresent() ? requestNode : null;
  }

  private ConfigNode generatePageStreamingRequestValueNode(
      ConfigNode parentNode, MethodModel method) {
    String pageTokenName = pageStreamingTransformer.getNameForPageToken();
    String pageSizeName = pageStreamingTransformer.getNameForPageSize();
    boolean hasTokenField = method.getInputField(pageTokenName) != null;
    boolean hasPageSizeField = method.getInputField(pageSizeName) != null;
    ConfigNode requestValueNode = null;
    if (hasPageSizeField) {
      requestValueNode = FieldConfigNode.createStringPair("page_size_field", pageSizeName);
      if (hasTokenField) {
        ConfigNode tokenFieldNode = FieldConfigNode.createStringPair("token_field", pageTokenName);
        requestValueNode.insertNext(tokenFieldNode);
      }
    } else if (hasTokenField) {
      requestValueNode = FieldConfigNode.createStringPair("token_field", pageTokenName);
    } else {
      return new NullConfigNode();
    }

    parentNode.setChild(requestValueNode);
    return requestValueNode;
  }

  private ConfigNode generatePageStreamingResponseNode(ConfigNode prevNode, MethodModel method) {
    ConfigNode responseNode = new FieldConfigNode("response");
    ConfigNode responseValueNode = generatePageStreamingResponseValueNode(responseNode, method);
    if (!responseValueNode.isPresent()) {
      return null;
    }

    prevNode.insertNext(responseNode);
    return responseNode;
  }

  private ConfigNode generatePageStreamingResponseValueNode(
      ConfigNode parentNode, MethodModel method) {
    ConfigNode responseValueNode =
        pageStreamingTransformer.generateResponseValueNode(method, diagCollector);
    if (responseValueNode.isPresent()) {
      parentNode.setChild(responseValueNode);
    }

    return responseValueNode;
  }
}
