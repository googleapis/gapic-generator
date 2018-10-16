/* Copyright 2018 Google LLC
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
package com.google.api.codegen.config;

import static com.google.api.codegen.configgen.mergers.RetryMerger.DEFAULT_RETRY_CODES;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.RETRY_CODES_IDEMPOTENT_NAME;

import com.google.api.Retry;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.RetryCodesDefinitionProto;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.rpc.Code;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class RetryCodesConfig {

  public static ImmutableList<String> RETRY_CODES_FOR_HTTP_GET =
      DEFAULT_RETRY_CODES.get(RETRY_CODES_IDEMPOTENT_NAME);
  public static final String HTTP_RETRY_CODE_DEF_NAME = "http_get";
  public static final String NO_RETRY_CODE_DEF_NAME = "no_retry";

  private Map<String, ImmutableList<String>> retryCodesDefinition = new HashMap<>();
  private Map<String, String> methodRetryNames = new HashMap<>();

  private ImmutableMap<String, ImmutableList<String>> finalRetryCodesDefinition;
  private ImmutableMap<String, String> finalMethodRetryNames;
  private boolean error = false;

  /**
   * A map of retry config names to the list of codes to retry on, e.g. { "idempotent" :
   * ["UNAVAILABLE"] }.
   */
  public ImmutableMap<String, ImmutableList<String>> getRetryCodesDefinition() {
    return finalRetryCodesDefinition;
  }

  /**
   * A map of method names to the method's retry config name, e.g. { "ListShelves" : "idempotent" }.
   */
  public ImmutableMap<String, String> getMethodRetryNames() {
    return finalMethodRetryNames;
  }

  private RetryCodesConfig() {}

  public static RetryCodesConfig create(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto) {
    RetryCodesConfig retryCodesConfig = new RetryCodesConfig();

    retryCodesConfig.populateRetryCodesDefinitionFromConfigProto(
        diagCollector, interfaceConfigProto);
    if (retryCodesConfig.error) {
      return null;
    }
    retryCodesConfig.finalMethodRetryNames = ImmutableMap.copyOf(retryCodesConfig.methodRetryNames);
    retryCodesConfig.finalRetryCodesDefinition =
        ImmutableMap.copyOf(retryCodesConfig.retryCodesDefinition);
    return retryCodesConfig;
  }

  public static RetryCodesConfig create(
      DiagCollector diagCollector,
      InterfaceConfigProto interfaceConfigProto,
      Interface apiInterface,
      ProtoParser protoParser) {
    RetryCodesConfig retryCodesConfig = new RetryCodesConfig();
    retryCodesConfig.populateRetryCodesDefinition(
        diagCollector, interfaceConfigProto, apiInterface, protoParser);
    if (retryCodesConfig.error) {
      return null;
    }
    retryCodesConfig.finalMethodRetryNames = ImmutableMap.copyOf(retryCodesConfig.methodRetryNames);
    retryCodesConfig.finalRetryCodesDefinition =
        ImmutableMap.copyOf(retryCodesConfig.retryCodesDefinition);
    return retryCodesConfig;
  }

  private static ImmutableMap<String, String> createMethodRetryNamesFromConfigProto(
      InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    // Add each method name from config proto and its non-empty retry code name.
    // Sample entry: {"ListShelves", "non_idempotent"}
    for (MethodConfigProto method : interfaceConfigProto.getMethodsList()) {
      if (!Strings.isNullOrEmpty(method.getRetryCodesName())) {
        builder.put(method.getName(), method.getRetryCodesName());
      }
    }
    return builder.build();
  }

  private static ImmutableMap<String, ImmutableList<String>>
      createRetryCodesDefinitionFromConfigProto(
          DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, ImmutableList<String>> builder = ImmutableMap.builder();
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
      builder.put(retryDef.getName(), ImmutableList.copyOf(codes));
    }
    if (diagCollector.getErrorCount() > 0) {
      return null;
    }
    return builder.build();
  }

  /**
   * Returns a mapping of a retryCodeDef name to the list of retry codes it contains. Also populates
   * the @param methodNameToRetryCodeNames with a mapping of a Method name to its retry code
   * settings name.
   */
  private void populateRetryCodesDefinition(
      DiagCollector diagCollector,
      InterfaceConfigProto interfaceConfigProto,
      Interface apiInterface,
      ProtoParser protoParser) {

    populateRetryCodesDefinitionFromConfigProto(diagCollector, interfaceConfigProto);
    if (error) {
      return;
    }

    populateRetryCodesDefinitionWithProtoFile(apiInterface, interfaceConfigProto, protoParser);
  }

  /**
   * Returns a mapping of a retryCodeDef name to the list of retry codes it contains. Also populates
   * the @param methodNameToRetryCodeNames with a mapping of a Method name to its retry code
   * settings name.
   */
  private void populateRetryCodesDefinitionFromConfigProto(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto) {

    ImmutableMap<String, ImmutableList<String>> retryCodesDefFromConfigProto =
        createRetryCodesDefinitionFromConfigProto(diagCollector, interfaceConfigProto);

    Map<String, String> methodRetryNamesFromConfigProto =
        createMethodRetryNamesFromConfigProto(interfaceConfigProto);
    if (diagCollector.getErrorCount() > 0) {
      return;
    }
    retryCodesDefinition.putAll(retryCodesDefFromConfigProto);
    methodRetryNames.putAll(methodRetryNamesFromConfigProto);
  }

  /**
   * Returns a mapping of a retryCodeDef name to the list of retry codes it contains. Also populates
   * the @param methodNameToRetryCodeNames with a mapping of a Method name to its retry code
   * settings name.
   */
  private void populateRetryCodesDefinitionWithProtoFile(
      Interface apiInterface, InterfaceConfigProto interfaceConfigProto, ProtoParser protoParser) {

    SymbolTable symbolTable = new SymbolTable();

    for (String retryCodesName : retryCodesDefinition.keySet()) {
      // Record all the preexisting retryCodeNames from configProto.
      symbolTable.getNewSymbol(retryCodesName);
    }

    // For now, only create retryCodeDef for methods that are also defined in the GAPIC config.
    Map<String, Method> methodsFromProtoFile =
        apiInterface.getMethods().stream().collect(Collectors.toMap(Method::getSimpleName, m -> m));
    List<Method> methodsFromGapicConfig =
        interfaceConfigProto
            .getMethodsList()
            .stream()
            .map(m -> methodsFromProtoFile.get(m.getName()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    // Unite all HTTP GET methods that have no additional retry codes under one retry code name to
    // reduce duplication.
    String httpGetRetryName = symbolTable.getNewSymbol(HTTP_RETRY_CODE_DEF_NAME);

    // Unite all methods that have no retry codes under one retry code name to reduce duplication.
    String noRetryName = symbolTable.getNewSymbol(NO_RETRY_CODE_DEF_NAME);

    // Check proto annotations for retry settings.
    for (Method method : methodsFromGapicConfig) {
      if (methodRetryNames.containsKey(method.getSimpleName())) {
        // https://github.com/googleapis/gapic-generator/issues/2311.
        // For now, let GAPIC config take precedent over proto annotations, for retry code
        // definitions.
        continue;
      }

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
          retryCodesDefinition.put(httpGetRetryName, RETRY_CODES_FOR_HTTP_GET);
        } else {
          // It is a common case to have a method with no codes to retry on,
          // so let's put these methods all under the same retry code name.
          retryCodesName = noRetryName;
          retryCodesDefinition.put(noRetryName, ImmutableList.of());
        }

        methodRetryNames.put(method.getSimpleName(), retryCodesName);

      } else {
        // Add all retry codes defined in the Retry proto annotation.
        retryCodes.addAll(
            retry.getCodesList().stream().map(Code::name).collect(Collectors.toList()));

        // Create a retryCode config internally.
        String retryCodesName = symbolTable.getNewSymbol(getRetryCodesName(method));
        methodRetryNames.put(method.getSimpleName(), retryCodesName);
        retryCodesDefinition.put(retryCodesName, ImmutableList.copyOf(retryCodes));
      }
    }
  }

  private static String getRetryCodesName(Method method) {
    return String.format("%s_retry_code", method.getSimpleName().toLowerCase());
  }
}
