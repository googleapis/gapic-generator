/* Copyright 2017 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.config;

import static com.google.api.codegen.config.DiscoGapicMethodConfig.createDiscoGapicMethodConfig;

import com.google.api.codegen.CollectionConfigProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.transformer.RetryDefinitionsTransformer;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class DiscoGapicInterfaceConfig implements InterfaceConfig {

  private static final String SERVICE_ADDRESS_PARAM = "service_address";
  private static final String SCOPES_PARAM = "scopes";
  private static final ImmutableSet<String> CONSTRUCTOR_PARAMS =
      ImmutableSet.of(SERVICE_ADDRESS_PARAM, SCOPES_PARAM);

  @Override
  public String getName() {
    return hasInterfaceNameOverride() ? getInterfaceNameOverride() : getRawName();
  }

  @Override
  public abstract DiscoInterfaceModel getInterfaceModel();

  @Override
  public String getRawName() {
    // TODO(andrealin): Return actual simple name.
    return getName();
  }

  @Nullable
  public abstract String getInterfaceNameOverride();

  @Override
  public boolean hasInterfaceNameOverride() {
    return getInterfaceNameOverride() != null;
  }

  @Override
  @Nullable
  public abstract SmokeTestConfig getSmokeTestConfig();

  @Override
  public ImmutableList<FieldModel> getIamResources() {
    return ImmutableList.of();
  }

  static DiscoGapicInterfaceConfig createInterfaceConfig(
      Document document,
      DiagCollector diagCollector,
      String language,
      InterfaceConfigProto interfaceConfigProto,
      String interfaceNameOverride,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      DiscoGapicNamer discoGapicNamer) {

    ImmutableMap<String, ImmutableSet<String>> retryCodesDefinition =
        RetryDefinitionsTransformer.createRetryCodesDefinition(diagCollector, interfaceConfigProto);
    ImmutableMap<String, RetrySettings> retrySettingsDefinition =
        RetryDefinitionsTransformer.createRetrySettingsDefinition(
            diagCollector, interfaceConfigProto);

    List<DiscoGapicMethodConfig> methodConfigs = null;
    ImmutableMap<String, DiscoGapicMethodConfig> methodConfigMap = null;
    if (retryCodesDefinition != null && retrySettingsDefinition != null) {
      methodConfigMap =
          createMethodConfigMap(
              document,
              diagCollector,
              language,
              interfaceConfigProto,
              messageConfigs,
              resourceNameConfigs,
              retryCodesDefinition.keySet(),
              retrySettingsDefinition.keySet(),
              discoGapicNamer);
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
        diagCollector.addDiag(
            Diag.error(SimpleLocation.TOPLEVEL, "Unsupported constructor param: %s", param));
      }
    }

    String manualDoc = Strings.nullToEmpty(interfaceConfigProto.getLangDoc().get(language)).trim();

    ImmutableList.Builder<SingleResourceNameConfig> resourcesBuilder = ImmutableList.builder();
    for (CollectionConfigProto collectionConfigProto : interfaceConfigProto.getCollectionsList()) {
      String entityName = collectionConfigProto.getEntityName();
      ResourceNameConfig resourceName = resourceNameConfigs.get(entityName);
      if (resourceName == null || !(resourceName instanceof SingleResourceNameConfig)) {
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

    if (diagCollector.hasErrors()) {
      return null;
    } else {
      return new AutoValue_DiscoGapicInterfaceConfig(
          methodConfigs,
          retryCodesDefinition,
          retrySettingsDefinition,
          requiredConstructorParams,
          manualDoc,
          new DiscoInterfaceModel(interfaceNameOverride, document),
          interfaceNameOverride,
          smokeTestConfig,
          methodConfigMap,
          singleResourceNames);
    }
  }

  private static Method lookupMethod(Document source, String lookupMethod) {
    for (com.google.api.codegen.discovery.Method method : source.methods()) {
      if (method.id().equals(lookupMethod)) {
        return method;
      }
    }
    return null;
  }

  private static ImmutableMap<String, DiscoGapicMethodConfig> createMethodConfigMap(
      Document document,
      DiagCollector diagCollector,
      String language,
      InterfaceConfigProto interfaceConfigProto,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      ImmutableSet<String> retryCodesConfigNames,
      ImmutableSet<String> retryParamsConfigNames,
      DiscoGapicNamer discoGapicNamer) {
    ImmutableMap.Builder<String, DiscoGapicMethodConfig> methodConfigMapBuilder =
        ImmutableMap.builder();

    for (MethodConfigProto methodConfigProto : interfaceConfigProto.getMethodsList()) {
      Method method = lookupMethod(document, methodConfigProto.getName());
      if (method == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL, "method not found: %s", methodConfigProto.getName()));
        continue;
      }
      DiscoGapicMethodConfig methodConfig =
          createDiscoGapicMethodConfig(
              diagCollector,
              language,
              methodConfigProto,
              method,
              messageConfigs,
              resourceNameConfigs,
              retryCodesConfigNames,
              retryParamsConfigNames,
              discoGapicNamer);
      if (methodConfig == null) {
        continue;
      }
      methodConfigMapBuilder.put(methodConfigProto.getName(), methodConfig);
    }

    if (diagCollector.getErrorCount() > 0) {
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
      if (methodConfig.isLongRunningOperation()) {
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
  public boolean hasDefaultInstance() {
    return getRequiredConstructorParams().size() == 0;
  }

  /** Returns the DiscoGapicMethodConfig for the given method. */
  @Override
  public DiscoGapicMethodConfig getMethodConfig(MethodModel method) {
    DiscoGapicMethodConfig methodConfig =
        (DiscoGapicMethodConfig) getMethodConfigMap().get(method.getFullName());
    if (methodConfig == null) {
      throw new IllegalArgumentException(
          "no method config for method '" + method.getFullName() + "'");
    }
    return methodConfig;
  }

  abstract ImmutableMap<String, ? extends MethodConfig> getMethodConfigMap();

  @Override
  public abstract ImmutableList<SingleResourceNameConfig> getSingleResourceNameConfigs();
}
