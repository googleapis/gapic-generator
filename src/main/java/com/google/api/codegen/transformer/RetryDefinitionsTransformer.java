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
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_INITIAL_RETRY_DELAY;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_MAX_RETRY_DELAY;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_MAX_RPC_TIMEOUT_MILLIS;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_RETRY_DELAY_MULTIPLIER;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_RPC_TIMEOUT_MULTIPLIER;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_TOTAL_TIMEOUT_MILLIS;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.RETRY_CODES_IDEMPOTENT_NAME;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.RETRY_PARAMS_DEFAULT_NAME;

import com.google.api.Retry;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.RetryCodesDefinitionProto;
import com.google.api.codegen.RetryParamsDefinitionProto;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.rpc.Code;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** RetryDefinitionsTransformer generates retry definitions from a service model. */
public class RetryDefinitionsTransformer {

  static ImmutableSet<String> RETRY_CODES_FOR_HTTP_GET =
      DEFAULT_RETRY_CODES.get(RETRY_CODES_IDEMPOTENT_NAME);

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

  /**
   * Returns a mapping of a retryCodeDef name to the list of retry codes it contains. Also populates
   * the @param methodNameToRetryCodeNames with a mapping of a Method name to its retry code
   * settings nmame.
   */
  private static ImmutableMap<String, ImmutableSet<String>> createRetryCodesDefinition(
      DiagCollector diagCollector,
      Interface apiInterface,
      ImmutableMap.Builder<String, String> methodNameToRetryCodeNames,
      ProtoParser protoParser,
      Map<String, ImmutableSet<String>> configProtoRetryMap) {

    ImmutableMap.Builder<String, ImmutableSet<String>> builder =
        ImmutableMap.<String, ImmutableSet<String>>builder().putAll(configProtoRetryMap);
    SymbolTable symbolTable = new SymbolTable();
    for (String retryCodesName : configProtoRetryMap.keySet()) {
      symbolTable.getNewSymbol(retryCodesName);
    }

    // Unite all HTTP GET methods that have no additional retry codes under one retry code name to
    // reduce duplication.
    String httpGetRetryName = symbolTable.getNewSymbol("http_get");
    builder.put(httpGetRetryName, ImmutableSet.copyOf(RETRY_CODES_FOR_HTTP_GET));
    // Unite all methods that have no retry codes under one retry code name to reduce duplication.
    String noRetryName = symbolTable.getNewSymbol("no_retry");
    builder.put(noRetryName, ImmutableSet.of());

    // Check proto annotations for retry settings.
    for (Method method : apiInterface.getMethods()) {
      Retry retry = protoParser.getRetry(method);
      Set<String> retryCodes = new TreeSet<>();

      if (protoParser.isHttpGetMethod(method)) {
        // If this is analogous to HTTP GET, then automatically retry on `INTERNAL` and
        // `UNAVAILABLE`.
        retryCodes.addAll(RETRY_CODES_FOR_HTTP_GET);
      }

      if (retry.getCodesCount() == 0) {
        String retryCodesName;
        if (protoParser.isHttpGetMethod(method)) {
          // It is a common case to have an HTTP GET method with no extra codes to retry on,
          // so let's put them all under the same retry code name.
          retryCodesName = httpGetRetryName;
        } else {
          // It is a common case to have a method with no codes to retry on,
          // so let's put these methods all under the same retry code name.
          retryCodesName = noRetryName;
        }

        methodNameToRetryCodeNames.put(method.getSimpleName(), retryCodesName);
      } else {
        // Add all retry codes defined in the Retry proto annotation.
        retryCodes.addAll(
            retry.getCodesList().stream().map(Code::name).collect(Collectors.toList()));

        // Create a retryCode config internally.
        String retryCodesName = symbolTable.getNewSymbol(getRetryCodesName(method));
        builder.put(
            retryCodesName, (new ImmutableSet.Builder<String>()).addAll(retryCodes).build());
        methodNameToRetryCodeNames.put(method.getSimpleName(), retryCodesName);
      }
    }

    if (diagCollector.getErrorCount() > 0) {
      return null;
    }
    return builder.build();
  }

  /**
   * Returns a mapping of a retryCodeDef name to the list of retry codes it contains. Also populates
   * the @param methodNameToRetryCodeNames with a mapping of a Method name to its retry code
   * settings nmame.
   */
  public static ImmutableMap<String, ImmutableSet<String>> createRetryCodesDefinition(
      DiagCollector diagCollector,
      InterfaceConfigProto interfaceConfigProto,
      Interface apiInterface,
      ImmutableMap.Builder<String, String> methodNameToRetryCodeNames,
      ProtoParser protoParser) {

    Map<String, ImmutableSet<String>> retryDefsFromConfig =
        createRetryCodesDefinition(diagCollector, interfaceConfigProto);
    if (retryDefsFromConfig == null) {
      return null;
    }
    Map<String, ImmutableSet<String>> retryCodesFromProto =
        createRetryCodesDefinition(
            diagCollector,
            apiInterface,
            methodNameToRetryCodeNames,
            protoParser,
            retryDefsFromConfig);
    if (retryCodesFromProto == null) {
      return null;
    }
    Map<String, ImmutableSet<String>> returnValue = new LinkedHashMap<>();
    returnValue.putAll(retryDefsFromConfig);
    returnValue.putAll(retryCodesFromProto);
    return ImmutableMap.copyOf(returnValue);
  }

  private static String getRetryCodesName(Method method) {
    return String.format("%s_retry_code", method.getSimpleName().toLowerCase());
  }

  public static ImmutableMap<String, RetryParamsDefinitionProto> createRetrySettingsDefinition(
      InterfaceConfigProto interfaceConfigProto) {
    Map<String, RetryParamsDefinitionProto> builder = new LinkedHashMap<>();
    for (RetryParamsDefinitionProto retryDef : interfaceConfigProto.getRetryParamsDefList()) {
      builder.put(retryDef.getName(), retryDef);
    }
    if (builder.isEmpty()) {
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
    } else {

    }
    return ImmutableMap.copyOf(builder);
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
    if (Strings.isNullOrEmpty(retryParamsName)) {
      return RETRY_PARAMS_DEFAULT_NAME;
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
