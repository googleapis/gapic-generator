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

import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.api.Retry;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.RetryCodesDefinitionProto;
import com.google.api.codegen.RetryParamsDefinitionProto;
import com.google.api.codegen.util.Name;
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
import com.google.rpc.Code;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
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

  /**
   * Returns a mapping of a retryCodeDef name to the list of retry codes it contains. Also populates
   * the @param methodNameToRetryCodeNames with a mapping of
   */
  public static ImmutableMap<String, List<String>> createRetryCodesDefinition(
      DiagCollector diagCollector,
      @Nullable InterfaceConfigProto interfaceConfigProto,
      Interface apiInterface,
      ImmutableMap.Builder<String, String> methodNameToRetryCodeNames) {
    // Use a symbol table to get unique names for retry codes.
    SymbolTable retryCodeNameSymbolTable = new SymbolTable();
    Map<String, String> methodNamesToRetryNamesFromConfig = new TreeMap<>();

    ImmutableMap.Builder<String, List<String>> builder = ImmutableMap.builder();
    if (interfaceConfigProto != null) {
      for (RetryCodesDefinitionProto retryDef : interfaceConfigProto.getRetryCodesDefList()) {
        // Enforce ordering on set for baseline test consistency.
        List<String> codes = new ArrayList<>();
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
        Collections.sort(codes);
        builder.put(
            retryDef.getName(), (new ImmutableList.Builder<String>()).addAll(codes).build());
        retryCodeNameSymbolTable.getNewSymbol(retryDef.getName());
      }

      // Map each methodConfig name to its retry codes name.
      interfaceConfigProto
          .getMethodsList()
          .stream()
          .filter(m -> !Strings.isNullOrEmpty(m.getRetryCodesName()))
          .forEach(m -> methodNamesToRetryNamesFromConfig.put(m.getName(), m.getRetryCodesName()));
    }
    //    else {
    //      // Use default values for retry settings.
    //      builder.putAll(DEFAULT_RETRY_CODES);
    //      for (Entry<String, List<String>> entry : DEFAULT_RETRY_CODES.entrySet()) {
    //        retryCodeNameSymbolTable.getNewSymbol(entry.getKey());
    //      }
    //    }

    // Check proto annotations for retry settings.
    if (apiInterface != null) {
      for (Method method : apiInterface.getMethods()) {

        TreeSet<String> retryCodes = null;
        Retry retry = method.getDescriptor().getMethodAnnotation(AnnotationsProto.retry);
        HttpRule httpRule = method.getDescriptor().getMethodAnnotation(AnnotationsProto.http);
        String retryCodesName = null;

        // Use GAPIC config if defines retry codes, and Retry proto annotation does not exist
        if (methodNamesToRetryNamesFromConfig.containsKey(method.getSimpleName())
            && retry.getCodesCount() == 0) {
          String configRetryCodesName =
              interfaceConfigProto
                  .getMethodsList()
                  .stream()
                  .filter(mcp -> mcp.getName().equals(method.getSimpleName()))
                  .map(MethodConfigProto::getRetryCodesName)
                  .findFirst()
                  .orElse(null);
          if (!Strings.isNullOrEmpty(configRetryCodesName)) {
            Optional<RetryCodesDefinitionProto> maybeRetryCodes =
                interfaceConfigProto
                    .getRetryCodesDefList()
                    .stream()
                    .filter(retryCodeDef -> configRetryCodesName.equals(retryCodeDef.getName()))
                    .findFirst();
            if (!maybeRetryCodes.isPresent()) {
              diagCollector.addDiag(
                  Diag.error(
                      SimpleLocation.TOPLEVEL,
                      "Retry codes config used but not defined: '%s' (in method %s)",
                      configRetryCodesName,
                      method.getFullName()));
              return null;
            }
            retryCodes = new TreeSet<>(maybeRetryCodes.get().getRetryCodesList());
            retryCodesName = maybeRetryCodes.get().getName();
          }
        }

        if (retryCodes == null) {
          retryCodes = new TreeSet<>();
          if (!Strings.isNullOrEmpty(httpRule.getGet())) {
            // If this is analogous to HTTP GET, then automatically retry on `INTERNAL` and
            // `UNAVAILABLE`.
            retryCodes.addAll(DEFAULT_RETRY_CODES.get(RETRY_CODES_IDEMPOTENT_NAME));
          }
          // Add all retry codes defined in the Retry proto annotation.
          retryCodes.addAll(
              retry.getCodesList().stream().map(Code::name).collect(Collectors.toList()));

          if (retryCodes.isEmpty()) {
            retryCodesName = "";
          } else {
            // Create a retryCode config internally.
            retryCodesName = retryCodeNameSymbolTable.getNewSymbol(getRetryCodesName(method));
            methodNamesToRetryNamesFromConfig.put(method.getSimpleName(), retryCodesName);
            builder.put(
                retryCodesName, (new ImmutableList.Builder<String>()).addAll(retryCodes).build());
          }
        }

        methodNamesToRetryNamesFromConfig.put(method.getSimpleName(), retryCodesName);
      }
    }

    methodNameToRetryCodeNames.putAll(methodNamesToRetryNamesFromConfig);

    if (diagCollector.getErrorCount() > 0) {
      return null;
    }
    return builder.build();
  }

  private static String getRetryCodesName(Method method) {
    return String.format("%s_retry_code", method.getSimpleName().toLowerCase());
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
