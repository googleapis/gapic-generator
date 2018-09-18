/* Copyright 2016 Google LLC
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
package com.google.api.codegen.transformer;

import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_INITIAL_RETRY_DELAY;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_MAX_RETRY_DELAY;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_MAX_RPC_TIMEOUT_MILLIS;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_RETRY_DELAY_MULTIPLIER;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_RPC_TIMEOUT_MULTIPLIER;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_TOTAL_TIMEOUT_MILLIS;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.RETRY_PARAMS_DEFAULT_NAME;

import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.RetryParamsDefinitionProto;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/** RetryDefinitionsTransformer generates retry definitions from a service model. */
public class RetryDefinitionsTransformer {

  public List<RetryCodesDefinitionView> generateRetryCodesDefinitions(InterfaceContext context) {
    List<RetryCodesDefinitionView> definitions = new ArrayList<>();

    final SurfaceNamer namer = context.getNamer();
    for (Entry<String, ImmutableList<String>> retryCodesDef :
        context.getInterfaceConfig().getRetryCodesConfig().getRetryCodesDefinition().entrySet()) {
      List<String> codeNames = new ArrayList<>();
      for (String code : retryCodesDef.getValue()) {
        codeNames.add(namer.getStatusCodeName(code));
      }
      Collections.sort(codeNames);
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

  public static ImmutableMap<String, RetryParamsDefinitionProto> createRetrySettingsDefinition(
      InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, RetryParamsDefinitionProto> builder = ImmutableMap.builder();
    if (interfaceConfigProto.getRetryParamsDefCount() > 0) {
      for (RetryParamsDefinitionProto retryDef : interfaceConfigProto.getRetryParamsDefList()) {
        builder.put(retryDef.getName(), retryDef);
      }
    } else {
      // Use default values.
      RetryParamsDefinitionProto defaultRetryParams =
          RetryParamsDefinitionProto.getDefaultInstance()
              .toBuilder()
              .setInitialRetryDelayMillis(DEFAULT_INITIAL_RETRY_DELAY)
              .setRetryDelayMultiplier(DEFAULT_RETRY_DELAY_MULTIPLIER)
              .setMaxRetryDelayMillis(DEFAULT_MAX_RETRY_DELAY)
              .setInitialRpcTimeoutMillis(DEFAULT_MAX_RPC_TIMEOUT_MILLIS)
              .setRpcTimeoutMultiplier(DEFAULT_RPC_TIMEOUT_MULTIPLIER)
              .setMaxRpcTimeoutMillis(DEFAULT_MAX_RPC_TIMEOUT_MILLIS)
              .setTotalTimeoutMillis(DEFAULT_TOTAL_TIMEOUT_MILLIS)
              .build();
      builder.put(RETRY_PARAMS_DEFAULT_NAME, defaultRetryParams);
    }
    return builder.build();
  }

  public static String getRetryParamsName(
      MethodConfigProto methodConfigProto,
      DiagCollector diagCollector,
      Set<String> retryParamsConfigNames) {
    String retryParamsName = methodConfigProto.getRetryParamsName();
    if (!retryParamsConfigNames.isEmpty() && !retryParamsConfigNames.contains(retryParamsName)) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Retry parameters config used but not defined: %s (in method %s)",
              retryParamsName,
              methodConfigProto.getName()));
      return null;
    }
    return retryParamsName;
  }

  public List<RetryParamsDefinitionView> generateRetryParamsDefinitions(InterfaceContext context) {
    List<RetryParamsDefinitionView> definitions = new ArrayList<>();

    SurfaceNamer namer = context.getNamer();
    for (Entry<String, RetryParamsDefinitionProto> retryCodesDef :
        context.getInterfaceConfig().getRetrySettingsDefinition().entrySet()) {
      RetryParamsDefinitionView.Builder view = RetryParamsDefinitionView.newBuilder();
      view.key(retryCodesDef.getKey());
      view.name(namer.publicMethodName(Name.from(retryCodesDef.getKey())));
      view.retryBackoffMethodName(namer.retryBackoffMethodName(retryCodesDef.getKey()));
      view.timeoutBackoffMethodName(namer.timeoutBackoffMethodName(retryCodesDef.getKey()));
      view.params(retryCodesDef.getValue());
      definitions.add(view.build());
    }

    return definitions;
  }
}
