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

import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_INITIAL_RETRY_DELAY;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_INITIAL_RPC_TIMEOUT_MULTIPLIER;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_MAX_RETRY_DELAY;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_MAX_RPC_TIMEOUT_MILLIS;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_RETRY_DELAY_MULTIPLIER;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_RPC_TIMEOUT_MULTIPLIER;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_TOTAL_TIMEOUT_MILLIS;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.INITIAL_RETRY_DELAY_NAME;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.INITIAL_RPC_TIMEOUT_NAME;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.MAX_RETRY_DELAY_NAME;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.MAX_RPC_TIMEOUT_NAME;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.RETRY_CODES_IDEMPOTENT_NAME;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.RETRY_CODES_NON_IDEMPOTENT_NAME;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.RETRY_DELAY_MULTIPLIER_NAME;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.RETRY_PARAMS_DEFAULT_NAME;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.RPC_TIMEOUT_MULTIPLIER_NAME;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.TOTAL_TIMEOUT_NAME;

import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.configgen.ListTransformer;
import com.google.api.codegen.configgen.NodeFinder;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.metadata.DefaultComment;
import com.google.api.codegen.configgen.nodes.metadata.FixmeComment;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import io.grpc.Status;
import java.util.List;
import java.util.Map;

/** Merges retry properties from an API interface into a ConfigNode. */
public class RetryMerger {

  public static final Map<String, List<String>> DEFAULT_RETRY_CODES =
      ImmutableSortedMap.of(
          RETRY_CODES_IDEMPOTENT_NAME,
          ImmutableList.of(Status.Code.DEADLINE_EXCEEDED.name(), Status.Code.UNAVAILABLE.name()),
          RETRY_CODES_NON_IDEMPOTENT_NAME,
          ImmutableList.of());

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
            DEFAULT_RETRY_CODES.get(RETRY_CODES_IDEMPOTENT_NAME));
    parentNode.setChild(idempotentNode);
    ConfigNode nonIdempotentNode =
        generateRetryCodeDefNode(
            NodeFinder.getNextLine(idempotentNode),
            RETRY_CODES_NON_IDEMPOTENT_NAME,
            DEFAULT_RETRY_CODES.get(RETRY_CODES_NON_IDEMPOTENT_NAME));
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
            NodeFinder.getNextLine(nameNode),
            INITIAL_RETRY_DELAY_NAME,
            String.valueOf(DEFAULT_INITIAL_RETRY_DELAY));
    nameNode.insertNext(initialRetryDelayMillisNode);
    ConfigNode retryDelayMultiplierNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(initialRetryDelayMillisNode),
            RETRY_DELAY_MULTIPLIER_NAME,
            String.valueOf(DEFAULT_RETRY_DELAY_MULTIPLIER));
    initialRetryDelayMillisNode.insertNext(retryDelayMultiplierNode);
    ConfigNode maxRetryDelayMillisNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(retryDelayMultiplierNode),
            MAX_RETRY_DELAY_NAME,
            String.valueOf(DEFAULT_MAX_RETRY_DELAY));
    retryDelayMultiplierNode.insertNext(maxRetryDelayMillisNode);
    ConfigNode initialRpcTimeoutMillisNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(maxRetryDelayMillisNode),
            INITIAL_RPC_TIMEOUT_NAME,
            String.valueOf(DEFAULT_INITIAL_RPC_TIMEOUT_MULTIPLIER));
    maxRetryDelayMillisNode.insertNext(initialRpcTimeoutMillisNode);
    ConfigNode rpcTimeoutMultiplierNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(initialRpcTimeoutMillisNode),
            RPC_TIMEOUT_MULTIPLIER_NAME,
            String.valueOf(DEFAULT_RPC_TIMEOUT_MULTIPLIER));
    initialRpcTimeoutMillisNode.insertNext(rpcTimeoutMultiplierNode);
    ConfigNode maxRpcTimeoutMillisNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(rpcTimeoutMultiplierNode),
            MAX_RPC_TIMEOUT_NAME,
            String.valueOf(DEFAULT_MAX_RPC_TIMEOUT_MILLIS));
    rpcTimeoutMultiplierNode.insertNext(maxRpcTimeoutMillisNode);
    ConfigNode totalTimeoutMillisNode =
        FieldConfigNode.createStringPair(
            NodeFinder.getNextLine(maxRpcTimeoutMillisNode),
            TOTAL_TIMEOUT_NAME,
            String.valueOf(DEFAULT_TOTAL_TIMEOUT_MILLIS));
    maxRpcTimeoutMillisNode.insertNext(totalTimeoutMillisNode);
    return retryParamDefNode;
  }

  ConfigNode generateRetryNamesNode(ConfigNode prevNode, MethodModel method) {
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
