package com.google.api.codegen.config;

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
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.rpc.Code;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.google.api.codegen.configgen.mergers.RetryMerger.DEFAULT_RETRY_CODES;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.RETRY_CODES_IDEMPOTENT_NAME;

public class RetryCodesConfig {

  static ImmutableSet<String> RETRY_CODES_FOR_HTTP_GET =
      DEFAULT_RETRY_CODES.get(RETRY_CODES_IDEMPOTENT_NAME);
  static final String HTTP_RETRY_CODE_DEF_NAME = "http_get";
  static final String NO_RETRY_CODE_DEF_NAME = "no_retry";

  private Map<String, ImmutableSet<String>> retryCodesDefinition = new HashMap();
  private Map<String, String> methodRetryNames = new HashMap<>();
  private boolean error = false;

  @Nullable
  private ProtoParser protoParser;

  /** A map of retry config names to the list of code to retry on, e.g. { "idempotent" : ["UNAVAILABLE"] }. */
  public ImmutableMap<String, ImmutableSet<String>> getRetryCodesDefinition() {
    return ImmutableMap.copyOf(retryCodesDefinition);
  }

  /** A map of method names to the method's retry config name, e.g. { "ListShelves" : "idempotent" }. */
  public ImmutableMap<String, String> getMethodRetryNames() {
    return ImmutableMap.copyOf(methodRetryNames);
  }

  private RetryCodesConfig(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto) {
    this.retryCodesDefinition =
        createRetryCodesDefinitionFromConfigProto(diagCollector, interfaceConfigProto);
    this.methodRetryNames = createMethodRetryNamesFromConfigProto(interfaceConfigProto);
  }

  private RetryCodesConfig(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto, Interface apiInterface,
      ProtoParser protoParser) {
    this.protoParser = protoParser;
  }

  public static RetryCodesConfig create(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto) {
    RetryCodesConfig retryCodesConfig = new RetryCodesConfig(diagCollector, interfaceConfigProto);
    if (retryCodesConfig.error) {
      return null;
    }
    return retryCodesConfig;
  }

  public static RetryCodesConfig create(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto, Interface apiInterface,
      ProtoParser protoParser) {
    RetryCodesConfig retryCodesConfig = new RetryCodesConfig(diagCollector, interfaceConfigProto, apiInterface, protoParser);
    if (retryCodesConfig.error) {
      return null;
    }
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

  private static ImmutableMap<String, ImmutableSet<String>> createRetryCodesDefinitionFromConfigProto(
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
   * settings name.
   */
  private void populateRetryCodesDefinition(
      DiagCollector diagCollector,
      InterfaceConfigProto interfaceConfigProto,
      Interface apiInterface,
      ProtoParser protoParser) {

    ImmutableMap<String, ImmutableSet<String>> retryCodesDefFromConfigProto = createRetryCodesDefinitionFromConfigProto(diagCollector, interfaceConfigProto);
    if (error) {
      return;
    }

    Map<String, String> methodRetryNamesFromConfigProto = createMethodRetryNamesFromConfigProto(interfaceConfigProto);
    if (error) {
      return;
    }
    retryCodesDefinition.putAll(retryCodesDefFromConfigProto);
    methodRetryNames.putAll(methodRetryNamesFromConfigProto);

    populateRetryCodesDefinitionWithProtoFile(
        diagCollector,
        apiInterface);
  }

  private static Map<String, String> getMethodConfigProtoRetryCodeNames(InterfaceConfigProto interfaceConfigProto) {
    Map<String, String> methodRetryCodes = new HashMap<>();
    for (MethodConfigProto method : interfaceConfigProto.getMethodsList()) {
      if (!Strings.isNullOrEmpty(method.getRetryCodesName())) {
        methodRetryCodes.put(method.getName(), method.getRetryCodesName());
      }
    }
    return methodRetryCodes;
  }


  /**
   * Returns a mapping of a retryCodeDef name to the list of retry codes it contains. Also populates
   * the @param methodNameToRetryCodeNames with a mapping of a Method name to its retry code
   * settings name.
   */
  private boolean populateRetryCodesDefinitionWithProtoFile(
      DiagCollector diagCollector,
      Interface apiInterface) {

    ImmutableMap.Builder<String, ImmutableSet<String>> builder =
        ImmutableMap.<String, ImmutableSet<String>>builder();
    SymbolTable symbolTable = new SymbolTable();

    for (String retryCodesName : retryCodesDefinition.keySet()) {
      // Record all the preexisting retryCodeNames from configProto.
      symbolTable.getNewSymbol(retryCodesName);
    }

    // Unite all HTTP GET methods that have no additional retry codes under one retry code name to
    // reduce duplication.
    String httpGetRetryName = symbolTable.getNewSymbol(HTTP_RETRY_CODE_DEF_NAME);
    builder.put(httpGetRetryName, ImmutableSet.copyOf(RETRY_CODES_FOR_HTTP_GET));
    // Unite all methods that have no retry codes under one retry code name to reduce duplication.
    String noRetryName = symbolTable.getNewSymbol(NO_RETRY_CODE_DEF_NAME);
    builder.put(noRetryName, ImmutableSet.of());

    // Check proto annotations for retry settings.
    for (Method method : apiInterface.getMethods()) {
      if (methodRetryNames.containsKey(method.getSimpleName())) {
        // https://github.com/googleapis/gapic-generator/issues/2311.
        // For now, let GAPIC config take precedent over proto annotations, for retry code definitions.
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
        } else {
          // It is a common case to have a method with no codes to retry on,
          // so let's put these methods all under the same retry code name.
          retryCodesName = noRetryName;
        }

        methodRetryNames.put(method.getSimpleName(), retryCodesName);
      } else {
        // Add all retry codes defined in the Retry proto annotation.
        retryCodes.addAll(
            retry.getCodesList().stream().map(Code::name).collect(Collectors.toList()));

        // Create a retryCode config internally.
        String retryCodesName = symbolTable.getNewSymbol(getRetryCodesName(method));
        builder.put(
            retryCodesName, (new ImmutableSet.Builder<String>()).addAll(retryCodes).build());
        methodRetryNames.put(method.getSimpleName(), retryCodesName);
      }
    }

    return diagCollector.getErrorCount() == 0;
  }

  private static String getRetryCodesName(Method method) {
    return String.format("%s_retry_code", method.getSimpleName().toLowerCase());
  }


}
