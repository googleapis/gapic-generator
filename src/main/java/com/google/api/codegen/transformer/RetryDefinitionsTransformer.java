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

import static com.google.api.codegen.configgen.mergers.RetryMerger.DEFAULT_RETRY_CODES;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.*;

import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.RetryCodesDefinitionProto;
import com.google.api.codegen.RetryParamsDefinitionProto;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.ProtoAnnotations;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** RetryDefinitionsTransformer generates retry definitions from a service model. */
public class RetryDefinitionsTransformer {

  public List<RetryCodesDefinitionView> generateRetryCodesDefinitions(InterfaceContext context) {
    List<RetryCodesDefinitionView> definitions = new ArrayList<>();

    final SurfaceNamer namer = context.getNamer();
    for (Entry<String, List<String>> retryCodesDef :
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

  public static ImmutableMap<String, List<String>> createRetryCodesDefinition(
      DiagCollector diagCollector,
      @Nullable InterfaceConfigProto interfaceConfigProto,
      Interface apiInterface) {
    // Use a symbol table to get unique names for retry codes.
    SymbolTable retryCodeNames = new SymbolTable();
    // Keep track of which sets of retry codes (key) have been encountered, and the names (value)
    // for each set.
    Map<String, String> retryCodeSets = new HashMap<>();

    Set<String> methodsWithoutRetryConfigProto;

    ImmutableMap.Builder<String, List<String>> builder = ImmutableMap.builder();
    if (interfaceConfigProto != null) {
      methodsWithoutRetryConfigProto =
          interfaceConfigProto.getMethodsList().stream()
              .filter(m -> Strings.isNullOrEmpty(m.getRetryCodesName()))
              .map(MethodConfigProto::getName)
              .collect(Collectors.toSet());
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
        builder.put(
            retryDef.getName(), (new ImmutableList.Builder<String>()).addAll(codes).build());
        retryCodeSets.put(ProtoAnnotations.listToString(codes), retryDef.getName());
        retryCodeNames.getNewSymbol(retryDef.getName());
      }
    } else {
      methodsWithoutRetryConfigProto = new HashSet<>();
      // TODO (andrealin): Use unique names for default values
      // Use default values for retry settings.
      builder.putAll(DEFAULT_RETRY_CODES);
      for (Entry<String, List<String>> entry : DEFAULT_RETRY_CODES.entrySet()) {
        retryCodeSets.put(ProtoAnnotations.listToString(entry.getValue()), entry.getKey());
        retryCodeNames.getNewSymbol(entry.getKey());
      }
    }

    // Check proto annotations for retry settings.
    if (apiInterface != null) {
      for (Method method : apiInterface.getMethods()) {

        List<String> retryCodes = ProtoAnnotations.getRetryCodes(method);
        if (retryCodeSets.containsKey(ProtoAnnotations.listToString(retryCodes))) {
          continue; // We've already seen this set of retry codes.
        }

        String retryCodesName = retryCodeNames.getNewSymbol(getRetryCodesName(method));
        retryCodeSets.put(ProtoAnnotations.listToString(retryCodes), retryCodesName);
      }
    }

    if (diagCollector.getErrorCount() > 0) {
      return null;
    }
    return builder.build();
  }

  private static String getRetryCodesName(Method method) {
    return String.format("%s_retry_code", method.getSimpleName());
  }

  public static ImmutableMap<String, RetryParamsDefinitionProto> createRetrySettingsDefinition(
      InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, RetryParamsDefinitionProto> builder = ImmutableMap.builder();
    if (interfaceConfigProto != null) {
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
