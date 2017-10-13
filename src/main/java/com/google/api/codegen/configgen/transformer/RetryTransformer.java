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
package com.google.api.codegen.configgen.transformer;

import com.google.api.codegen.configgen.viewmodel.InterfaceView;
import com.google.api.codegen.configgen.viewmodel.RetryCodeView;
import com.google.api.codegen.configgen.viewmodel.RetryParamView;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** Generates view objects for the retry codes def and retry params def. */
public class RetryTransformer {
  public static final String RETRY_CODES_IDEMPOTENT_NAME = "idempotent";
  public static final String RETRY_CODES_NON_IDEMPOTENT_NAME = "non_idempotent";
  public static final String RETRY_PARAMS_DEFAULT_NAME = "default";

  public void generateRetryDefinitions(
      InterfaceView.Builder interfaceView,
      List<String> idempotentRetryCodes,
      List<String> nonIdempotentRetryCodes) {
    interfaceView.retryCodesDef(generateRetryCodes(idempotentRetryCodes, nonIdempotentRetryCodes));
    interfaceView.retryParamsDef(generateRetryParams());
  }

  private List<RetryCodeView> generateRetryCodes(
      List<String> idempotentRetryCodes, List<String> nonIdempotentRetryCodes) {
    ImmutableList.Builder<RetryCodeView> retryCodes = ImmutableList.builder();
    retryCodes.add(
        RetryCodeView.newBuilder()
            .name(RETRY_CODES_IDEMPOTENT_NAME)
            .retryCodes(ImmutableList.copyOf(idempotentRetryCodes))
            .build());
    retryCodes.add(
        RetryCodeView.newBuilder()
            .name(RETRY_CODES_NON_IDEMPOTENT_NAME)
            .retryCodes(ImmutableList.copyOf(nonIdempotentRetryCodes))
            .build());
    return retryCodes.build();
  }

  private List<RetryParamView> generateRetryParams() {
    return ImmutableList.of(
        RetryParamView.newBuilder()
            .name(RETRY_PARAMS_DEFAULT_NAME)
            .initialRetryDelayMillis("100")
            .retryDelayMultiplier("1.3")
            .maxRetryDelayMillis("60000")
            .initialRpcTimeoutMillis("20000")
            .rpcTimeoutMultiplier("1")
            .maxRpcTimeoutMillis("20000")
            .totalTimeoutMillis("600000")
            .build());
  }
}
