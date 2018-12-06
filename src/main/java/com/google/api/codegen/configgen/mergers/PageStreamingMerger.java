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
package com.google.api.codegen.configgen.mergers;

import com.google.api.codegen.config.ProtoMethodModel;
import com.google.api.codegen.configgen.ConfigHelper;
import com.google.api.codegen.configgen.NodeFinder;
import com.google.api.codegen.configgen.PageStreamingTransformer;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.NullConfigNode;

/** Merges page streaming properties from a MethodModel into a ConfigNode. */
class PageStreamingMerger {
  private final PageStreamingTransformer<ProtoMethodModel> pageStreamingTransformer;
  private final ConfigHelper helper;

  PageStreamingMerger(
      PageStreamingTransformer<ProtoMethodModel> pageStreamingTransformer, ConfigHelper helper) {
    this.pageStreamingTransformer = pageStreamingTransformer;
    this.helper = helper;
  }

  ConfigNode generatePageStreamingNode(ConfigNode prevNode, ProtoMethodModel method) {
    ConfigNode pageStreamingNode =
        new FieldConfigNode(NodeFinder.getNextLine(prevNode), "page_streaming");
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

  private ConfigNode generatePageStreamingRequestNode(
      ConfigNode parentNode, ProtoMethodModel method) {
    ConfigNode requestNode = new FieldConfigNode(NodeFinder.getNextLine(parentNode), "request");
    parentNode.setChild(requestNode);
    ConfigNode requestValueNode =
        generatePageStreamingRequestValueNode(
            requestNode, NodeFinder.getNextLine(requestNode), method);
    return requestValueNode.isPresent() ? requestNode : null;
  }

  private ConfigNode generatePageStreamingRequestValueNode(
      ConfigNode parentNode, int startLine, ProtoMethodModel method) {
    String pageTokenName = pageStreamingTransformer.getNameForPageToken();
    String pageSizeName = pageStreamingTransformer.getNameForPageSize();
    boolean hasTokenField = method.getInputField(pageTokenName) != null;
    boolean hasPageSizeField = method.getInputField(pageSizeName) != null;
    ConfigNode requestValueNode;
    if (hasPageSizeField) {
      requestValueNode =
          FieldConfigNode.createStringPair(startLine, "page_size_field", pageSizeName);
      if (hasTokenField) {
        ConfigNode tokenFieldNode =
            FieldConfigNode.createStringPair(
                NodeFinder.getNextLine(requestValueNode), "token_field", pageTokenName);
        requestValueNode.insertNext(tokenFieldNode);
      }
    } else if (hasTokenField) {
      requestValueNode = FieldConfigNode.createStringPair(startLine, "token_field", pageTokenName);
    } else {
      return new NullConfigNode();
    }

    parentNode.setChild(requestValueNode);
    return requestValueNode;
  }

  private ConfigNode generatePageStreamingResponseNode(
      ConfigNode prevNode, ProtoMethodModel method) {
    ConfigNode responseNode = new FieldConfigNode(NodeFinder.getNextLine(prevNode), "response");
    ConfigNode responseValueNode =
        pageStreamingTransformer.generateResponseValueNode(responseNode, method, helper);
    if (!responseValueNode.isPresent()) {
      return null;
    }

    prevNode.insertNext(responseNode);
    return responseNode;
  }
}
