/* Copyright 2017 Google LLC
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
import com.google.api.codegen.discogapic.transformer.DiscoGapicParser;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.transformer.RetryDefinitionsTransformer;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@AutoValue
public abstract class DiscoGapicInterfaceConfig implements InterfaceConfig {

  private static final String SERVICE_ADDRESS_PARAM = "service_address";
  private static final String SCOPES_PARAM = "scopes";
  private static final ImmutableSet<String> CONSTRUCTOR_PARAMS =
      ImmutableSet.of(SERVICE_ADDRESS_PARAM, SCOPES_PARAM);

  @Override
  public String getName() {
    return getInterfaceNameOverride() != null ? getInterfaceNameOverride() : getRawName();
  }

  @Override
  public abstract DiscoInterfaceModel getInterfaceModel();

  @Override
  public String getRawName() {
    return getInterfaceModel().getSimpleName();
  }

  @Override
  @Nullable
  public abstract SmokeTestConfig getSmokeTestConfig();

  // Mapping of a method to its main resource name.
  public abstract Map<MethodConfig, SingleResourceNameConfig> methodToResourceNameMap();

  static DiscoGapicInterfaceConfig createInterfaceConfig(
      DiscoApiModel model,
      TargetLanguage language,
      InterfaceConfigProto interfaceConfigProto,
      String interfaceNameOverride,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs) {

    RetryCodesConfig retryCodesConfig =
        RetryCodesConfig.create(model.getDiagCollector(), interfaceConfigProto);
    ImmutableMap<String, RetryParamsDefinitionProto> retrySettingsDefinition =
        RetryDefinitionsTransformer.createRetrySettingsDefinition(interfaceConfigProto);

    List<DiscoGapicMethodConfig> methodConfigs = null;
    ImmutableMap<String, DiscoGapicMethodConfig> methodConfigMap = null;
    if (retryCodesConfig != null && retrySettingsDefinition != null) {
      methodConfigMap =
          createMethodConfigMap(
              model,
              language,
              interfaceConfigProto,
              messageConfigs,
              resourceNameConfigs,
              retryCodesConfig,
              retrySettingsDefinition.keySet());
      methodConfigs =
          GapicInterfaceConfig.createMethodConfigs(methodConfigMap, interfaceConfigProto);
    }

    // TODO(andrealin)  Make non-null smokeTestConfig.
    SmokeTestConfig smokeTestConfig = null;

    // TODO(andrealin) IAM permissions configs.

    ImmutableList<String> requiredConstructorParams =
        ImmutableList.copyOf(interfaceConfigProto.getRequiredConstructorParamsList());
    for (String param : interfaceConfigProto.getRequiredConstructorParamsList()) {
      if (!CONSTRUCTOR_PARAMS.contains(param)) {
        model
            .getDiagCollector()
            .addDiag(
                Diag.error(SimpleLocation.TOPLEVEL, "Unsupported constructor param: %s", param));
      }
    }

    ImmutableList.Builder<SingleResourceNameConfig> resourcesBuilder = ImmutableList.builder();
    for (CollectionConfigProto collectionConfigProto : interfaceConfigProto.getCollectionsList()) {
      String entityName = collectionConfigProto.getEntityName();
      ResourceNameConfig resourceName = resourceNameConfigs.get(entityName);
      if (!(resourceName instanceof SingleResourceNameConfig)) {
        model
            .getDiagCollector()
            .addDiag(
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

    ImmutableMap.Builder<MethodConfig, SingleResourceNameConfig> methodToSingleResourceNameMap =
        ImmutableMap.builder();
    if (methodConfigs != null) {
      for (MethodConfig methodConfig : methodConfigs) {
        Method method = ((DiscoveryMethodModel) methodConfig.getMethodModel()).getDiscoMethod();
        String canonicalMethodPath = DiscoGapicParser.getCanonicalPath(method.flatPath());
        for (SingleResourceNameConfig nameConfig : singleResourceNames) {
          if (nameConfig.getNamePattern().equals(canonicalMethodPath)) {
            methodToSingleResourceNameMap.put(methodConfig, nameConfig);
          }
        }
      }
    }

    String manualDoc =
        Strings.nullToEmpty(
                interfaceConfigProto.getLangDoc().get(language.toString().toLowerCase()))
            .trim();

    String interfaceName =
        interfaceNameOverride != null
            ? interfaceNameOverride
            : DiscoGapicParser.getInterfaceName(interfaceConfigProto.getName()).toUpperCamel();

    if (model.getDiagCollector().hasErrors()) {
      return null;
    } else {
      return new AutoValue_DiscoGapicInterfaceConfig(
          retryCodesConfig,
          retrySettingsDefinition,
          requiredConstructorParams,
          manualDoc,
          interfaceNameOverride,
          new DiscoInterfaceModel(interfaceName, model),
          smokeTestConfig,
          methodToSingleResourceNameMap.build(),
          methodConfigs,
          methodConfigMap,
          singleResourceNames);
    }
  }

  @Override
  public abstract List<DiscoGapicMethodConfig> getMethodConfigs();

  private static Method lookupMethod(Document source, String lookupMethod) {
    for (com.google.api.codegen.discovery.Method method : source.methods()) {
      if (method.id().equals(lookupMethod)) {
        return method;
      }
    }
    return null;
  }

  private static ImmutableMap<String, DiscoGapicMethodConfig> createMethodConfigMap(
      DiscoApiModel model,
      TargetLanguage language,
      InterfaceConfigProto interfaceConfigProto,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      RetryCodesConfig retryCodesConfig,
      ImmutableSet<String> retryParamsConfigNames) {
    ImmutableMap.Builder<String, DiscoGapicMethodConfig> methodConfigMapBuilder =
        ImmutableMap.builder();

    for (MethodConfigProto methodConfigProto : interfaceConfigProto.getMethodsList()) {
      Method method = lookupMethod(model.getDocument(), methodConfigProto.getName());
      if (method == null) {
        model
            .getDiagCollector()
            .addDiag(
                Diag.error(
                    SimpleLocation.TOPLEVEL, "method not found: %s", methodConfigProto.getName()));
        continue;
      }
      DiscoGapicMethodConfig methodConfig =
          DiscoGapicMethodConfig.createDiscoGapicMethodConfig(
              model,
              language,
              methodConfigProto,
              method,
              messageConfigs,
              resourceNameConfigs,
              retryCodesConfig,
              retryParamsConfigNames);
      if (methodConfig == null) {
        continue;
      }
      methodConfigMapBuilder.put(methodConfigProto.getName(), methodConfig);
    }

    if (model.getDiagCollector().getErrorCount() > 0) {
      return null;
    } else {
      return methodConfigMapBuilder.build();
    }
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
  public boolean hasLongRunningOperations() {
    for (MethodConfig methodConfig : getMethodConfigs()) {
      if (methodConfig.getLroConfig() != null) {
        return true;
      }
    }
    return false;
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

  @Override
  public boolean hasDefaultInstance() {
    return getRequiredConstructorParams().size() == 0;
  }

  /** Returns the DiscoGapicMethodConfig for the given method. */
  @Override
  public DiscoGapicMethodConfig getMethodConfig(MethodModel method) {
    final String methodName = method.getFullName();
    DiscoGapicMethodConfig methodConfig =
        (DiscoGapicMethodConfig) getMethodConfigMap().get(methodName);
    if (methodConfig == null) {
      throw new IllegalArgumentException("no method config for method '" + methodName + "'");
    }
    return methodConfig;
  }

  abstract ImmutableMap<String, ? extends MethodConfig> getMethodConfigMap();

  @Override
  public abstract ImmutableList<SingleResourceNameConfig> getSingleResourceNameConfigs();
}
