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
import static com.google.api.codegen.configgen.transformer.RetryTransformer.RETRY_CODES_NON_IDEMPOTENT_NAME;

import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.RetryCodesDefinitionProto;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;

public class RetryCodesConfig {

  public static ImmutableList<String> RETRY_CODES_FOR_HTTP_GET =
      DEFAULT_RETRY_CODES.get(RETRY_CODES_IDEMPOTENT_NAME);

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
      List<Method> methodsToGenerate,
      ProtoParser protoParser) {
    RetryCodesConfig retryCodesConfig = new RetryCodesConfig();
    retryCodesConfig.populateRetryCodesDefinition(
        diagCollector, interfaceConfigProto, methodsToGenerate, protoParser);
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

  @Nullable
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
      Collection<Method> methodsToGenerate,
      ProtoParser protoParser) {
    // First create the retry codes definitions from the GAPIC config.
    populateRetryCodesDefinitionFromConfigProto(diagCollector, interfaceConfigProto);
    if (error) {
      return;
    }

    // Then create the retry codes defs from the proto annotations, but don't overwrite
    // existing retry codes defs from the GAPIC config.
    populateRetryCodesDefinitionWithProtoFile(methodsToGenerate, protoParser);
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
    if (retryCodesDefFromConfigProto == null) {
      return;
    }

    Map<String, String> methodRetryNamesFromConfigProto =
        createMethodRetryNamesFromConfigProto(interfaceConfigProto);

    retryCodesDefinition.putAll(retryCodesDefFromConfigProto);
    methodRetryNames.putAll(methodRetryNamesFromConfigProto);
  }

  /**
   * Returns a mapping of a retryCodeDef name to the list of retry codes it contains. Also populates
   * the @param methodNameToRetryCodeNames with a mapping of a Method name to its retry code
   * settings name.
   *
   * <p>If this object already contains a retry entry for a given Method, don't overwrite the
   * existing retry entry.
   */
  private void populateRetryCodesDefinitionWithProtoFile(
      Collection<Method> methodsToCreateRetriesFor, ProtoParser protoParser) {

    SymbolTable symbolTable = new SymbolTable();

    for (String retryCodesName : retryCodesDefinition.keySet()) {
      // Record all the preexisting retryCodeNames from configProto.
      symbolTable.getNewSymbol(retryCodesName);
    }

    // Unite all HTTP GET methods that have no additional retry codes under one retry code name to
    // reduce duplication.
    String httpGetRetryName = symbolTable.getNewSymbol(RETRY_CODES_IDEMPOTENT_NAME);

    // Unite all methods that have no retry codes under one retry code name to reduce duplication.
    String noRetryName = symbolTable.getNewSymbol(RETRY_CODES_NON_IDEMPOTENT_NAME);

    // Check proto annotations for retry settings.
    for (Method method : methodsToCreateRetriesFor) {
      if (methodRetryNames.containsKey(method.getSimpleName())) {
        // https://github.com/googleapis/gapic-generator/issues/2311.
        // For now, let GAPIC config take precedent over proto annotations, for retry code
        // definitions.
        continue;
      }

      String retryCodesName;
      ImmutableList<String> retryCodesList;
      if (protoParser.isHttpGetMethod(method)) {
        retryCodesList = RETRY_CODES_FOR_HTTP_GET;
        // It is a common case to have an HTTP GET method with no extra codes to retry on,
        // so let's put them all under the same retry code name.
        if (!new HashSet<>(RETRY_CODES_FOR_HTTP_GET)
            .equals(
                new HashSet<>(
                    retryCodesDefinition.getOrDefault(
                        RETRY_CODES_IDEMPOTENT_NAME, ImmutableList.of())))) {
          retryCodesName = httpGetRetryName;
        } else {
          // The GAPIC config defined RETRY_CODES_IDEMPOTENT_NAME to have the same retry codes as
          // this method would have,
          // so we can just reuse RETRY_CODES_IDEMPOTENT_NAME.
          retryCodesName = RETRY_CODES_IDEMPOTENT_NAME;
        }

      } else {
        retryCodesList = ImmutableList.of();
        if (retryCodesDefinition
                .getOrDefault(RETRY_CODES_NON_IDEMPOTENT_NAME, ImmutableList.of())
                .size()
            == 0) {
          retryCodesName = RETRY_CODES_NON_IDEMPOTENT_NAME;
        } else {
          // It is a common case to have a method with no codes to retry on,
          // so let's put these methods all under the same retry code name.
          retryCodesName = noRetryName;
        }
      }

      retryCodesDefinition.put(retryCodesName, retryCodesList);
      methodRetryNames.put(method.getSimpleName(), retryCodesName);
    }
  }
}
