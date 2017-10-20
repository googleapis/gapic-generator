/* Copyright 2017 Google Inc
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

import com.google.api.codegen.configgen.ListTransformer;
import com.google.api.codegen.configgen.StringPairTransformer;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.metadata.DefaultComment;
import com.google.api.codegen.configgen.nodes.metadata.FixmeComment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute;
import com.google.api.tools.framework.aspects.http.model.MethodKind;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableList;
import io.grpc.Status;
import java.util.List;

public class RetryMerger {
  private static final String RETRY_CODES_IDEMPOTENT_NAME = "idempotent";
  private static final String RETRY_CODES_NON_IDEMPOTENT_NAME = "non_idempotent";
  private static final String RETRY_PARAMS_DEFAULT_NAME = "default";

  public ConfigNode generateRetryDefinitionsNode(ConfigNode prevNode) {
    FieldConfigNode retryCodesDefNode =
        new FieldConfigNode("retry_codes_def")
            .setComment(new DefaultComment("Definition for retryable codes."));
    prevNode.insertNext(retryCodesDefNode);
    generateRetryCodesDefValueNode(retryCodesDefNode);
    FieldConfigNode retryParamsDefNode =
        new FieldConfigNode("retry_params_def")
            .setComment(new DefaultComment("Definition for retry/backoff parameters."));
    retryCodesDefNode.insertNext(retryParamsDefNode);
    generateRetryParamsDefValueNode(retryParamsDefNode);
    return retryParamsDefNode;
  }

  private void generateRetryCodesDefValueNode(ConfigNode parentNode) {
    ConfigNode idempotentNode =
        generateRetryCodeDefNode(
            RETRY_CODES_IDEMPOTENT_NAME,
            ImmutableList.of(Status.Code.UNAVAILABLE.name(), Status.Code.DEADLINE_EXCEEDED.name()));
    parentNode.setChild(idempotentNode);
    ConfigNode nonIdempotentNode =
        generateRetryCodeDefNode(RETRY_CODES_NON_IDEMPOTENT_NAME, ImmutableList.<String>of());
    idempotentNode.insertNext(nonIdempotentNode);
  }

  private ConfigNode generateRetryCodeDefNode(String name, List<String> codes) {
    ConfigNode retryCodeDefNode = new ListItemConfigNode();
    ConfigNode nameNode = StringPairTransformer.generateStringPair("name", name);
    retryCodeDefNode.setChild(nameNode);
    ConfigNode retryCodesNode = new FieldConfigNode("retry_codes");
    nameNode.insertNext(retryCodesNode);
    ListTransformer.generateStringList(codes, retryCodesNode);
    return retryCodeDefNode;
  }

  private void generateRetryParamsDefValueNode(ConfigNode parentNode) {
    ConfigNode defaultNode = generateRetryParamDefNode(RETRY_PARAMS_DEFAULT_NAME);
    parentNode.setChild(defaultNode);
  }

  private ConfigNode generateRetryParamDefNode(String name) {
    ConfigNode retryParamDefNode = new ListItemConfigNode();
    ConfigNode nameNode = StringPairTransformer.generateStringPair("name", name);
    retryParamDefNode.setChild(nameNode);
    ConfigNode initialRetryDelayMillisNode =
        StringPairTransformer.generateStringPair("initial_retry_delay_millis", "100");
    nameNode.insertNext(initialRetryDelayMillisNode);
    ConfigNode retryDelayMultiplierNode =
        StringPairTransformer.generateStringPair("retry_delay_multiplier", "1.3");
    initialRetryDelayMillisNode.insertNext(retryDelayMultiplierNode);
    ConfigNode maxRetryDelayMillisNode =
        StringPairTransformer.generateStringPair("max_retry_delay_millis", "60000");
    retryDelayMultiplierNode.insertNext(maxRetryDelayMillisNode);
    ConfigNode initialRpcTimeoutMillisNode =
        StringPairTransformer.generateStringPair("initial_rpc_timeout_millis", "20000");
    maxRetryDelayMillisNode.insertNext(initialRpcTimeoutMillisNode);
    ConfigNode rpcTimeoutMultiplierNode =
        StringPairTransformer.generateStringPair("rpc_timeout_multiplier", "1");
    initialRpcTimeoutMillisNode.insertNext(rpcTimeoutMultiplierNode);
    ConfigNode maxRpcTimeoutMillisNode =
        StringPairTransformer.generateStringPair("max_rpc_timeout_millis", "20000");
    rpcTimeoutMultiplierNode.insertNext(maxRpcTimeoutMillisNode);
    ConfigNode totalTimeoutMillisNode =
        StringPairTransformer.generateStringPair("total_timeout_millis", "600000");
    maxRpcTimeoutMillisNode.insertNext(totalTimeoutMillisNode);
    return retryParamDefNode;
  }

  public ConfigNode generateRetryNamesNode(ConfigNode prevNode, Method method) {
    String retryCodesName =
        isIdempotent(method) ? RETRY_CODES_IDEMPOTENT_NAME : RETRY_CODES_NON_IDEMPOTENT_NAME;
    ConfigNode retryCodesNameNode =
        StringPairTransformer.generateStringPair("retry_codes_name", retryCodesName)
            .setComment(new FixmeComment("Configure the retryable codes for this method."));
    prevNode.insertNext(retryCodesNameNode);
    ConfigNode retryParamsNameNode =
        StringPairTransformer.generateStringPair("retry_params_name", RETRY_PARAMS_DEFAULT_NAME)
            .setComment(new FixmeComment("Configure the retryable params for this method."));
    retryCodesNameNode.insertNext(retryParamsNameNode);
    return retryParamsNameNode;
  }

  /**
   * Returns true if the method is idempotent according to the http method kind (GET, PUT, DELETE).
   */
  private boolean isIdempotent(Method method) {
    HttpAttribute httpAttr = method.getAttribute(HttpAttribute.KEY);
    if (httpAttr == null) {
      return false;
    }
    MethodKind methodKind = httpAttr.getMethodKind();
    return methodKind.isIdempotent();
  }
}
