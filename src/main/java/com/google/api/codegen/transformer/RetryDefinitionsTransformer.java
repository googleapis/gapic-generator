/* Copyright 2016 Google Inc
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
package com.google.api.codegen.transformer;

import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.gax.retrying.RetrySettings;
import com.google.common.collect.ImmutableSet;
import io.grpc.Status.Code;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/** RetryDefinitionsTransformer generates retry definitions from a service model. */
public class RetryDefinitionsTransformer {

  public List<RetryCodesDefinitionView> generateRetryCodesDefinitions(
      GapicInterfaceContext context) {
    List<RetryCodesDefinitionView> definitions = new ArrayList<>();

    final SurfaceNamer namer = context.getNamer();
    for (Entry<String, ImmutableSet<Code>> retryCodesDef :
        context.getInterfaceConfig().getRetryCodesDefinition().entrySet()) {
      List<String> codeNames = new ArrayList<>();
      for (Code code : retryCodesDef.getValue()) {
        codeNames.add(namer.getStatusCodeName(code));
      }
      definitions.add(
          RetryCodesDefinitionView.newBuilder()
              .key(retryCodesDef.getKey())
              .name(namer.getRetryDefinitionName(retryCodesDef.getKey()))
              .retryFilterMethodName(namer.retryFilterMethodName(retryCodesDef.getKey()))
              .codes(retryCodesDef.getValue())
              .codeNames(codeNames)
              .build());
    }

    return definitions;
  }

  public List<RetryParamsDefinitionView> generateRetryParamsDefinitions(
      GapicInterfaceContext context) {
    List<RetryParamsDefinitionView> definitions = new ArrayList<>();

    SurfaceNamer namer = context.getNamer();
    for (Entry<String, RetrySettings> retryCodesDef :
        context.getInterfaceConfig().getRetrySettingsDefinition().entrySet()) {
      RetrySettings settings = retryCodesDef.getValue();
      RetryParamsDefinitionView.Builder params = RetryParamsDefinitionView.newBuilder();
      params.key(retryCodesDef.getKey());
      params.name(namer.publicMethodName(Name.from(retryCodesDef.getKey())));
      params.retryBackoffMethodName(namer.retryBackoffMethodName(retryCodesDef.getKey()));
      params.timeoutBackoffMethodName(namer.timeoutBackoffMethodName(retryCodesDef.getKey()));
      params.initialRetryDelay(settings.getInitialRetryDelay());
      params.retryDelayMultiplier(settings.getRetryDelayMultiplier());
      params.maxRetryDelay(settings.getMaxRetryDelay());
      params.initialRpcTimeout(settings.getInitialRpcTimeout());
      params.rpcTimeoutMultiplier(settings.getRpcTimeoutMultiplier());
      params.maxRpcTimeout(settings.getMaxRpcTimeout());
      params.totalTimeout(settings.getTotalTimeout());
      definitions.add(params.build());
    }

    return definitions;
  }
}
