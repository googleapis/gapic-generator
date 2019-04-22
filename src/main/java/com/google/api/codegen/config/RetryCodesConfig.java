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
import com.google.api.tools.framework.model.Method;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;

public class RetryCodesConfig {

  public static ImmutableList<String> RETRY_CODES_FOR_HTTP_GET =
      DEFAULT_RETRY_CODES.get(RETRY_CODES_IDEMPOTENT_NAME);
  public static ImmutableList<String> RETRY_CODES_FOR_HTTP_NON_GET =
      DEFAULT_RETRY_CODES.get(RETRY_CODES_NON_IDEMPOTENT_NAME);

  private Map<String, ImmutableList<String>> retryCodesDefinition = new HashMap<>();
  private Map<String, String> methodRetryNames = new HashMap<>();

  private ImmutableSet<String> retryCodeDefsFromGapicConfig;
  private ImmutableMap<String, ImmutableList<String>> finalRetryCodesDefinition;
  private ImmutableMap<String, String> finalMethodRetryNames;

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

  /** The retry code def names from the GAPIC config. */
  public Set<String> getRetryCodeDefsFromGapicConfig() {
    return retryCodeDefsFromGapicConfig;
  }

  private RetryCodesConfig() {}

  public static RetryCodesConfig create(InterfaceConfigProto interfaceConfigProto) {
    RetryCodesConfig retryCodesConfig = new RetryCodesConfig();

    retryCodesConfig.populateRetryCodesDefinitionFromConfigProto(interfaceConfigProto);
    retryCodesConfig.setFinalRetryProperties();
    return retryCodesConfig;
  }

  public static RetryCodesConfig create(
      InterfaceConfigProto interfaceConfigProto,
      List<Method> methodsToGenerate,
      ProtoParser protoParser) {
    RetryCodesConfig retryCodesConfig = new RetryCodesConfig();
    retryCodesConfig.populateRetryCodesDefinition(
        interfaceConfigProto, methodsToGenerate, protoParser);
    retryCodesConfig.setFinalRetryProperties();
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
      createRetryCodesDefinitionFromConfigProto(InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, ImmutableList<String>> builder = ImmutableMap.builder();
    for (RetryCodesDefinitionProto retryDef : interfaceConfigProto.getRetryCodesDefList()) {
      // Enforce ordering on set for baseline test consistency.
      Set<String> codes = new TreeSet<>(retryDef.getRetryCodesList());
      builder.put(retryDef.getName(), ImmutableList.copyOf(codes));
    }
    return builder.build();
  }

  private void setFinalRetryProperties() {
    finalMethodRetryNames = ImmutableMap.copyOf(methodRetryNames);
    finalRetryCodesDefinition = ImmutableMap.copyOf(retryCodesDefinition);
  }

  /**
   * Returns a mapping of a retryCodeDef name to the list of retry codes it contains. Also populates
   * the @param methodNameToRetryCodeNames with a mapping of a Method name to its retry code
   * settings name.
   */
  private void populateRetryCodesDefinition(
      InterfaceConfigProto interfaceConfigProto,
      Collection<Method> methodsToGenerate,
      ProtoParser protoParser) {
    // First create the retry codes definitions from the GAPIC config.
    populateRetryCodesDefinitionFromConfigProto(interfaceConfigProto);

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
      InterfaceConfigProto interfaceConfigProto) {

    ImmutableMap<String, ImmutableList<String>> retryCodesDefFromConfigProto =
        createRetryCodesDefinitionFromConfigProto(interfaceConfigProto);
    if (retryCodesDefFromConfigProto == null) {
      return;
    }

    retryCodeDefsFromGapicConfig = retryCodesDefFromConfigProto.keySet();

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

    // Add retry codes for default cases (idempotent and non_idempotent) if they have not
    // already be added previously (in the GAPIC config).
    retryCodesDefinition.putIfAbsent(RETRY_CODES_IDEMPOTENT_NAME, RETRY_CODES_FOR_HTTP_GET);
    retryCodesDefinition.putIfAbsent(RETRY_CODES_NON_IDEMPOTENT_NAME, RETRY_CODES_FOR_HTTP_NON_GET);

    for (String retryCodesName : retryCodesDefinition.keySet()) {
      // Record all the preexisting retryCodeNames from configProto.
      symbolTable.getNewSymbol(retryCodesName);
    }

    // Unite all HTTP GET methods that have no additional retry codes under one retry code name to
    // reduce duplication.
    String httpGetRetryName;
    if (Sets.newHashSet(RETRY_CODES_FOR_HTTP_GET)
        .containsAll(retryCodesDefinition.get(RETRY_CODES_IDEMPOTENT_NAME))) {
      // The GAPIC config defined RETRY_CODES_IDEMPOTENT_NAME to have the same retry codes as
      // this method would have,
      // so we can just reuse RETRY_CODES_IDEMPOTENT_NAME.
      httpGetRetryName = RETRY_CODES_IDEMPOTENT_NAME;
    } else {
      httpGetRetryName = symbolTable.getNewSymbol(RETRY_CODES_IDEMPOTENT_NAME);
    }

    // Unite all methods that have no retry codes under one retry code name to reduce duplication.
    String noRetryName;
    if (Sets.newHashSet(RETRY_CODES_FOR_HTTP_NON_GET)
        .containsAll(retryCodesDefinition.get(RETRY_CODES_NON_IDEMPOTENT_NAME))) {
      noRetryName = RETRY_CODES_NON_IDEMPOTENT_NAME;
    } else {
      noRetryName = symbolTable.getNewSymbol(RETRY_CODES_NON_IDEMPOTENT_NAME);
    }

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
        // It is a common case to have an HTTP GET method with no extra codes to retry on,
        // so let's put them all under the same retry code name.
        retryCodesName = httpGetRetryName;
        retryCodesList = RETRY_CODES_FOR_HTTP_GET;
      } else {
        retryCodesName = noRetryName;
        retryCodesList = ImmutableList.of();
      }

      retryCodesDefinition.put(retryCodesName, retryCodesList);
      methodRetryNames.put(method.getSimpleName(), retryCodesName);
    }
  }
}
