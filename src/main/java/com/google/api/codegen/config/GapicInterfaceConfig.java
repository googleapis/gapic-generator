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
package com.google.api.codegen.config;

import com.google.api.codegen.CollectionConfigProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.RetryParamsDefinitionProto;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.transformer.RetryDefinitionsTransformer;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;

/**
 * GapicInterfaceConfig represents the client code-gen config for an API interface, and includes the
 * configuration for methods and resource names.
 *
 * <p>In grpc-based Gapic clients, an API interface is defined by a "service" section in a proto
 * file.
 */
@AutoValue
public abstract class GapicInterfaceConfig implements InterfaceConfig {

  private static final String SERVICE_ADDRESS_PARAM = "service_address";
  private static final String SCOPES_PARAM = "scopes";
  private static final ImmutableSet<String> CONSTRUCTOR_PARAMS =
      ImmutableSet.of(SERVICE_ADDRESS_PARAM, SCOPES_PARAM);

  public Interface getInterface() {
    return getInterfaceModel().getInterface();
  }

  @Override
  public abstract ProtoInterfaceModel getInterfaceModel();

  @Override
  public abstract List<GapicMethodConfig> getMethodConfigs();

  @Nullable
  @Override
  public abstract SmokeTestConfig getSmokeTestConfig();

  abstract ImmutableMap<String, GapicMethodConfig> getMethodConfigMap();

  @Override
  public abstract RetryCodesConfig getRetryCodesConfig();

  @Override
  public abstract ImmutableMap<String, RetryParamsDefinitionProto> getRetrySettingsDefinition();

  @Override
  public abstract ImmutableList<String> getRequiredConstructorParams();

  @Override
  public abstract ImmutableList<SingleResourceNameConfig> getSingleResourceNameConfigs();

  @Override
  public abstract String getManualDoc();

  @Override
  public String getName() {
    return getInterfaceNameOverride() != null
        ? getInterfaceNameOverride()
        : getInterface().getSimpleName();
  }

  @Override
  public String getRawName() {
    return getInterface().getSimpleName();
  }

  /**
   * Creates an instance of GapicInterfaceConfig based on ConfigProto, linking up method
   * configurations with specified methods in methodConfigMap. On errors, null will be returned, and
   * diagnostics are reported to the model.
   */
  @Nullable
  static GapicInterfaceConfig createInterfaceConfig(
      DiagCollector diagCollector,
      TargetLanguage language,
      String defaultPackageName,
      GapicInterfaceInput interfaceInput,
      String interfaceNameOverride,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      ProtoParser protoParser) {

    Interface apiInterface = interfaceInput.getInterface();
    Map<Method, MethodConfigProto> methodsToGenerate = interfaceInput.getMethodsToGenerate();
    InterfaceConfigProto interfaceConfigProto = interfaceInput.getInterfaceConfigProto();

    RetryCodesConfig retryCodesConfig =
        RetryCodesConfig.create(
            diagCollector,
            interfaceConfigProto,
            new ArrayList<>(interfaceInput.getMethodsToGenerate().keySet()),
            protoParser);

    ImmutableMap<String, RetryParamsDefinitionProto> retrySettingsDefinition =
        RetryDefinitionsTransformer.createRetrySettingsDefinition(interfaceConfigProto);

    ImmutableMap<String, GapicMethodConfig> methodConfigsMap;
    List<GapicMethodConfig> methodConfigs;
    if (retryCodesConfig != null && retrySettingsDefinition != null) {
      methodConfigsMap =
          createMethodConfigMap(
              diagCollector,
              language,
              defaultPackageName,
              methodsToGenerate,
              messageConfigs,
              resourceNameConfigs,
              retryCodesConfig,
              retrySettingsDefinition.keySet(),
              protoParser);
      if (methodConfigsMap == null) {
        diagCollector.addDiag(
            Diag.error(SimpleLocation.TOPLEVEL, "Error constructing methodConfigMap"));
        return null;
      }
      methodConfigs = createMethodConfigs(methodConfigsMap, interfaceConfigProto);
    } else {
      methodConfigsMap = ImmutableMap.of();
      methodConfigs = ImmutableList.of();
    }

    SmokeTestConfig smokeTestConfig =
        createSmokeTestConfig(diagCollector, apiInterface, interfaceConfigProto);

    ImmutableList<String> requiredConstructorParams =
        ImmutableList.copyOf(interfaceConfigProto.getRequiredConstructorParamsList());
    for (String param : interfaceConfigProto.getRequiredConstructorParamsList()) {
      if (!CONSTRUCTOR_PARAMS.contains(param)) {
        diagCollector.addDiag(
            Diag.error(SimpleLocation.TOPLEVEL, "Unsupported constructor param: %s", param));
      }
    }

    ImmutableList.Builder<SingleResourceNameConfig> resourcesBuilder = ImmutableList.builder();
    for (CollectionConfigProto collectionConfigProto : interfaceConfigProto.getCollectionsList()) {
      String entityName = collectionConfigProto.getEntityName();
      ResourceNameConfig resourceName = resourceNameConfigs.get(entityName);
      if (!(resourceName instanceof SingleResourceNameConfig)) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Inconsistent configuration - single resource name %s specified for interface, "
                    + " but was not found in GapicProductConfig configuration.",
                entityName));
        return null;
      }
      resourcesBuilder.add((SingleResourceNameConfig) resourceName);
    }
    ImmutableList<SingleResourceNameConfig> singleResourceNames = resourcesBuilder.build();

    String manualDoc =
        Strings.nullToEmpty(
                interfaceConfigProto.getLangDoc().get(language.toString().toLowerCase()))
            .trim();

    if (diagCollector.hasErrors()) {
      return null;
    } else {
      return new AutoValue_GapicInterfaceConfig(
          interfaceNameOverride,
          new ProtoInterfaceModel(apiInterface),
          methodConfigs,
          smokeTestConfig,
          methodConfigsMap,
          retryCodesConfig,
          retrySettingsDefinition,
          requiredConstructorParams,
          singleResourceNames,
          manualDoc);
    }
  }

  private static SmokeTestConfig createSmokeTestConfig(
      DiagCollector diagCollector,
      Interface apiInterface,
      InterfaceConfigProto interfaceConfigProto) {
    if (interfaceConfigProto.hasSmokeTest()) {
      return SmokeTestConfig.createSmokeTestConfig(
          new ProtoInterfaceModel(apiInterface),
          interfaceConfigProto.getSmokeTest(),
          diagCollector);
    } else {
      return null;
    }
  }

  private static ImmutableMap<String, GapicMethodConfig> createMethodConfigMap(
      DiagCollector diagCollector,
      TargetLanguage language,
      String defaultPackageName,
      Map<Method, MethodConfigProto> methodsToGenerate,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      RetryCodesConfig retryCodesConfig,
      ImmutableSet<String> retryParamsConfigNames,
      ProtoParser protoParser) {
    Map<String, GapicMethodConfig> methodConfigMapBuilder = new LinkedHashMap<>();

    for (Entry<Method, MethodConfigProto> methodEntry : methodsToGenerate.entrySet()) {
      MethodConfigProto methodConfigProto = methodEntry.getValue();
      Method method = methodEntry.getKey();
      GapicMethodConfig methodConfig =
          GapicMethodConfig.createMethodConfig(
              diagCollector,
              language,
              defaultPackageName,
              methodConfigProto,
              method,
              messageConfigs,
              resourceNameConfigs,
              retryCodesConfig,
              retryParamsConfigNames,
              protoParser);
      if (methodConfig == null) {
        continue;
      }
      methodConfigMapBuilder.put(method.getSimpleName(), methodConfig);
    }

    if (diagCollector.getErrorCount() > 0) {
      return null;
    } else {
      return ImmutableMap.copyOf(methodConfigMapBuilder);
    }
  }

  /** Return a list of configs for method in the order given by the GAPIC interface config. */
  static <T> List<T> createMethodConfigs(
      ImmutableMap<String, T> methodConfigMap, InterfaceConfigProto interfaceConfigProto) {
    if (interfaceConfigProto.equals(InterfaceConfigProto.getDefaultInstance())) {
      // InterfaceConfigProto was not given, so just return the order in methodConfigMap.
      return methodConfigMap.values().asList();
    }
    List<T> methodConfigs = new ArrayList<>();
    for (MethodConfigProto methodConfigProto : interfaceConfigProto.getMethodsList()) {
      methodConfigs.add(methodConfigMap.get(methodConfigProto.getName()));
    }
    return methodConfigs;
  }

  /** Returns the GapicMethodConfig for the given method. */
  @Override
  public GapicMethodConfig getMethodConfig(MethodModel method) {
    return getMethodConfig(method.getSimpleName());
  }

  /** Returns the GapicMethodConfig for the given method. */
  public GapicMethodConfig getMethodConfig(Method method) {
    return getMethodConfig(method.getSimpleName());
  }

  public GapicMethodConfig getMethodConfig(String methodSimpleName) {
    return getMethodConfigMap().get(methodSimpleName);
  }

  @Override
  public boolean hasDefaultServiceAddress() {
    return !getRequiredConstructorParams().contains(SERVICE_ADDRESS_PARAM);
  }

  @Override
  public boolean hasDefaultServiceScopes() {
    return !getRequiredConstructorParams().contains(SCOPES_PARAM);
  }

  @Override
  public boolean hasDefaultInstance() {
    return getRequiredConstructorParams().size() == 0;
  }

  /**
   * If rerouteToGrpcInterface is set, then looks up that interface and returns it, otherwise
   * returns the value of defaultInterface.
   */
  public static Interface getTargetInterface(
      Interface defaultInterface, String rerouteToGrpcInterface) {
    Interface targetInterface = defaultInterface;
    if (!Strings.isNullOrEmpty(rerouteToGrpcInterface)) {
      targetInterface =
          defaultInterface.getModel().getSymbolTable().lookupInterface(rerouteToGrpcInterface);
      if (targetInterface == null) {
        throw new IllegalArgumentException(
            "reroute_to_grpc_interface not found: " + rerouteToGrpcInterface);
      }
    }
    return targetInterface;
  }

  @Override
  public boolean hasPageStreamingMethods() {
    for (MethodConfig methodConfig : getMethodConfigs()) {
      if (methodConfig.isPageStreaming()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasBatchingMethods() {
    for (MethodConfig methodConfig : getMethodConfigs()) {
      if (methodConfig.isBatching()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasGrpcStreamingMethods() {
    for (MethodConfig methodConfig : getMethodConfigs()) {
      if (methodConfig.isGrpcStreaming()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasGrpcStreamingMethods(GrpcStreamingConfig.GrpcStreamingType streamingType) {
    for (GapicMethodConfig methodConfig : getMethodConfigs()) {
      if (methodConfig.isGrpcStreaming() && methodConfig.getGrpcStreamingType() == streamingType) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasLongRunningOperations() {
    for (MethodConfig methodConfig : getMethodConfigs()) {
      if (methodConfig.hasLroConfig()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasReroutedInterfaceMethods() {
    for (MethodConfig methodConfig : getMethodConfigs()) {
      if (!Strings.isNullOrEmpty(methodConfig.getRerouteToGrpcInterface())) {
        return true;
      }
    }
    return false;
  }
}
