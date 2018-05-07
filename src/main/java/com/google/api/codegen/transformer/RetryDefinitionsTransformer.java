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

import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.RetryCodesDefinitionProto;
import com.google.api.codegen.RetryParamsDefinitionProto;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.threeten.bp.Duration;

/** RetryDefinitionsTransformer generates retry definitions from a service model. */
public class RetryDefinitionsTransformer {

  public List<RetryCodesDefinitionView> generateRetryCodesDefinitions(InterfaceContext context) {
    List<RetryCodesDefinitionView> definitions = new ArrayList<>();

    final SurfaceNamer namer = context.getNamer();
    for (Entry<String, ImmutableSet<String>> retryCodesDef :
        context.getInterfaceConfig().getRetryCodesDefinition().entrySet()) {
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

  public static ImmutableMap<String, ImmutableSet<String>> createRetryCodesDefinition(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, ImmutableSet<String>> builder = ImmutableMap.builder();
    for (RetryCodesDefinitionProto retryDef : interfaceConfigProto.getRetryCodesDefList()) {
      // Enforce ordering on set for baseline test consistency.
      Set<String> codes = new TreeSet<>();
      for (String codeText : retryDef.getRetryCodesList()) {
        try {
          codes.add(String.valueOf(codeText));
        } catch (IllegalArgumentException e) {
          diagCollector.addDiag(
              Diag.error(
                  SimpleLocation.TOPLEVEL,
                  "status code not found: '%s' (in interface %s)",
                  codeText,
                  interfaceConfigProto.getName()));
        }
      }
      builder.put(retryDef.getName(), (new ImmutableSet.Builder<String>()).addAll(codes).build());
    }
    if (diagCollector.getErrorCount() > 0) {
      return null;
    }
    return builder.build();
  }

  public static ImmutableMap<String, RetrySettings> createRetrySettingsDefinition(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, RetrySettings> builder = ImmutableMap.builder();
    for (RetryParamsDefinitionProto retryDef : interfaceConfigProto.getRetryParamsDefList()) {
      try {
        RetrySettings settings =
            RetrySettings.newBuilder()
                .setInitialRetryDelay(Duration.ofMillis(retryDef.getInitialRetryDelayMillis()))
                .setRetryDelayMultiplier(retryDef.getRetryDelayMultiplier())
                .setMaxRetryDelay(Duration.ofMillis(retryDef.getMaxRetryDelayMillis()))
                .setInitialRpcTimeout(Duration.ofMillis(retryDef.getInitialRpcTimeoutMillis()))
                .setRpcTimeoutMultiplier(retryDef.getRpcTimeoutMultiplier())
                .setMaxRpcTimeout(Duration.ofMillis(retryDef.getMaxRpcTimeoutMillis()))
                .setTotalTimeout(Duration.ofMillis(retryDef.getTotalTimeoutMillis()))
                .build();
        builder.put(retryDef.getName(), settings);
      } catch (IllegalStateException | NullPointerException e) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "error while creating retry params: %s (in interface %s)",
                e,
                interfaceConfigProto.getName()));
      }
    }
    if (diagCollector.getErrorCount() > 0) {
      return null;
    }
    return builder.build();
  }

  public List<RetryParamsDefinitionView> generateRetryParamsDefinitions(InterfaceContext context) {
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
