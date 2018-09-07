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

import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.configgen.ListTransformer;
import com.google.api.codegen.configgen.NodeFinder;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.metadata.DefaultComment;
import com.google.api.codegen.configgen.nodes.metadata.FixmeComment;
import com.google.common.collect.ImmutableList;
import io.grpc.Status;
import java.util.List;

/** Merges retry properties from an API interface into a ConfigNode. */
public class RetryMerger {
  private static final String RETRY_CODES_IDEMPOTENT_NAME = "idempotent";
  private static final String RETRY_CODES_NON_IDEMPOTENT_NAME = "non_idempotent";
  private static final String RETRY_PARAMS_DEFAULT_NAME = "default";

  public ConfigNode generateRetryDefinitionsNode(ConfigNode prevNode) {
    FieldConfigNode retryCodesDefNode =
        new FieldConfigNode(NodeFinder.getNextLine(prevNode), "retry_codes_def")
            .setComment(new DefaultComment("Definition for retryable codes."));
    prevNode.insertNext(retryCodesDefNode);
    generateRetryCodesDefValueNode(retryCodesDefNode);
    FieldConfigNode retryParamsDefNode =
        new FieldConfigNode(NodeFinder.getNextLine(retryCodesDefNode), "retry_params_def")
            .setComment(new DefaultComment("Definition for retry/backoff parameters."));
    retryCodesDefNode.insertNext(retryParamsDefNode);
    generateRetryParamsDefValueNode(retryParamsDefNode);
    return retryParamsDefNode;
  }

  private void generateRetryCodesDefValueNode(ConfigNode parentNode) {
    ConfigNode idempotentNode =
        generateRetryCodeDefNode(
            NodeFinder.getNextLine(parentNode),
            RETRY_CODES_IDEMPOTENT_NAME,
            ImmutableList.of(Status.Code.DEADLINE_EXCEEDED.name(), Status.Code.UNAVAILABLE.name()));
    parentNode.setChild(idempotentNode);
    ConfigNode nonIdempotentNode =
        generateRetryCodeDefNode(
            NodeFinder.getNextLine(idempotentNode),
            RETRY_CODES_NON_IDEMPOTENT_NAME,
            ImmutableList.<String>of());
    idempotentNode.insertNext(nonIdempotentNode);
  }

  private ConfigNode generateRetryCodeDefNode(int startLine, String name, List<String> codes) {
    ConfigNode retryCodeDefNode = new ListItemConfigNode(startLine);
    ConfigNode nameNode = FieldConfigNode.createStringPair(startLine, "name", name);
    retryCodeDefNode.setChild(nameNode);
    ConfigNode retryCodesNode =
        new FieldConfigNode(NodeFinder.getNextLine(nameNode), "retry_codes");
    nameNode.insertNext(retryCodesNode);
    ListTransformer.generateStringList(codes, retryCodesNode);
    return retryCodeDefNode;
  }

  private void generateRetryParamsDefValueNode(ConfigNode parentNode) {
    ConfigNode defaultNode =
        generateRetryParamDefNode(NodeFinder.getNextLine(parentNode), RETRY_PARAMS_DEFAULT_NAME);
    parentNode.setChild(defaultNode);
  }

  private ConfigNode generateRetryParamDefNode(int startLine, String name) {
    ConfigNode retryParamDefNode = new ListItemConfigNode(startLine);
    ConfigNode nameNode = FieldConfigNode.createStringPair(startLine, "name", name);
    retryParamDefNode.setChild(nameNode);
    ConfigNode initialRetryDelayMillisNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(nameNode), "initial_retry_delay_millis", "100");
    nameNode.insertNext(initialRetryDelayMillisNode);
    ConfigNode retryDelayMultiplierNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(initialRetryDelayMillisNode), "retry_delay_multiplier", "1.3");
    initialRetryDelayMillisNode.insertNext(retryDelayMultiplierNode);
    ConfigNode maxRetryDelayMillisNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(retryDelayMultiplierNode), "max_retry_delay_millis", "60000");
    retryDelayMultiplierNode.insertNext(maxRetryDelayMillisNode);
    ConfigNode initialRpcTimeoutMillisNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(maxRetryDelayMillisNode), "initial_rpc_timeout_millis", "20000");
    maxRetryDelayMillisNode.insertNext(initialRpcTimeoutMillisNode);
    ConfigNode rpcTimeoutMultiplierNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(initialRpcTimeoutMillisNode), "rpc_timeout_multiplier", "1");
    initialRpcTimeoutMillisNode.insertNext(rpcTimeoutMultiplierNode);
    ConfigNode maxRpcTimeoutMillisNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(rpcTimeoutMultiplierNode), "max_rpc_timeout_millis", "20000");
    rpcTimeoutMultiplierNode.insertNext(maxRpcTimeoutMillisNode);
    ConfigNode totalTimeoutMillisNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(maxRpcTimeoutMillisNode), "total_timeout_millis", "600000");
    maxRpcTimeoutMillisNode.insertNext(totalTimeoutMillisNode);
    return retryParamDefNode;
  }

  public ConfigNode generateRetryNamesNode(ConfigNode prevNode, MethodModel method) {
    String retryCodesName =
        method.isIdempotent() ? RETRY_CODES_IDEMPOTENT_NAME : RETRY_CODES_NON_IDEMPOTENT_NAME;
    ConfigNode retryCodesNameNode =
        FieldConfigNode.createStringPair(
                NodeFinder.getNextLine(prevNode), "retry_codes_name", retryCodesName)
            .setComment(new FixmeComment("Configure the retryable codes for this method."));
    prevNode.insertNext(retryCodesNameNode);
    ConfigNode retryParamsNameNode =
        FieldConfigNode.createStringPair(
                NodeFinder.getNextLine(retryCodesNameNode),
                "retry_params_name",
                RETRY_PARAMS_DEFAULT_NAME)
            .setComment(new FixmeComment("Configure the retryable params for this method."));
    retryCodesNameNode.insertNext(retryParamsNameNode);
    return retryParamsNameNode;
  }
}
