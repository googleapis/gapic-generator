/* Copyright 2016 Google Inc
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

import com.google.api.codegen.CollectionConfigProto;
import com.google.api.codegen.IamResourceProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.RetryCodesDefinitionProto;
import com.google.api.codegen.RetryParamsDefinitionProto;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.model.TypeRef;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.grpc.Status;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import org.threeten.bp.Duration;

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
      ImmutableSet.<String>of(SERVICE_ADDRESS_PARAM, SCOPES_PARAM);

  public abstract Interface getInterface();

  public abstract List<GapicMethodConfig> getMethodConfigs();

  @Nullable
  public abstract SmokeTestConfig getSmokeTestConfig();

  abstract ImmutableMap<String, GapicMethodConfig> getMethodConfigMap();

  public abstract ImmutableMap<String, ImmutableSet<Status.Code>> getRetryCodesDefinition();

  public abstract ImmutableMap<String, RetrySettings> getRetrySettingsDefinition();

  public abstract ImmutableList<Field> getIamResources();

  public abstract ImmutableList<String> getRequiredConstructorParams();

  public abstract ImmutableList<SingleResourceNameConfig> getSingleResourceNameConfigs();

  public abstract String getManualDoc();

  @Nullable
  public abstract String getInterfaceNameOverride();

  @Override
  public String getName() {
    return hasInterfaceNameOverride() ? getInterfaceNameOverride() : getInterface().getSimpleName();
  }

  @Override
  public String getRawName() {
    return getInterface().getSimpleName();
  }

  public boolean hasInterfaceNameOverride() {
    return getInterfaceNameOverride() != null;
  }

  /**
   * Creates an instance of GapicInterfaceConfig based on ConfigProto, linking up method
   * configurations with specified methods in methodConfigMap. On errors, null will be returned, and
   * diagnostics are reported to the model.
   */
  @Nullable
  public static GapicInterfaceConfig createInterfaceConfig(
      DiagCollector diagCollector,
      String language,
      InterfaceConfigProto interfaceConfigProto,
      Interface apiInterface,
      String interfaceNameOverride,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs) {

    ImmutableMap<String, ImmutableSet<Status.Code>> retryCodesDefinition =
        createRetryCodesDefinition(diagCollector, interfaceConfigProto);
    ImmutableMap<String, RetrySettings> retrySettingsDefinition =
        createRetrySettingsDefinition(diagCollector, interfaceConfigProto);

    List<GapicMethodConfig> methodConfigs = null;
    ImmutableMap<String, GapicMethodConfig> methodConfigMap = null;
    if (retryCodesDefinition != null && retrySettingsDefinition != null) {
      methodConfigMap =
          createMethodConfigMap(
              diagCollector,
              language,
              interfaceConfigProto,
              apiInterface,
              messageConfigs,
              resourceNameConfigs,
              retryCodesDefinition.keySet(),
              retrySettingsDefinition.keySet());
      methodConfigs = createMethodConfigs(methodConfigMap, interfaceConfigProto);
    }

    SmokeTestConfig smokeTestConfig =
        createSmokeTestConfig(diagCollector, apiInterface, interfaceConfigProto);

    ImmutableList<Field> iamResources =
        createIamResources(
            apiInterface.getModel(),
            interfaceConfigProto.getExperimentalFeatures().getIamResourcesList());

    ImmutableList<String> requiredConstructorParams =
        ImmutableList.<String>copyOf(interfaceConfigProto.getRequiredConstructorParamsList());
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

    String manualDoc = Strings.nullToEmpty(interfaceConfigProto.getLangDoc().get(language)).trim();

    if (diagCollector.hasErrors()) {
      return null;
    } else {
      return new AutoValue_GapicInterfaceConfig(
          apiInterface,
          methodConfigs,
          smokeTestConfig,
          methodConfigMap,
          retryCodesDefinition,
          retrySettingsDefinition,
          iamResources,
          requiredConstructorParams,
          singleResourceNames,
          manualDoc,
          interfaceNameOverride);
    }
  }

  private static SmokeTestConfig createSmokeTestConfig(
      DiagCollector diagCollector,
      Interface apiInterface,
      InterfaceConfigProto interfaceConfigProto) {
    if (interfaceConfigProto.hasSmokeTest()) {
      return SmokeTestConfig.createSmokeTestConfig(
          apiInterface, interfaceConfigProto.getSmokeTest(), diagCollector);
    } else {
      return null;
    }
  }

  private static ImmutableMap<String, ImmutableSet<Status.Code>> createRetryCodesDefinition(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, ImmutableSet<Status.Code>> builder =
        ImmutableMap.<String, ImmutableSet<Status.Code>>builder();
    for (RetryCodesDefinitionProto retryDef : interfaceConfigProto.getRetryCodesDefList()) {
      EnumSet<Status.Code> codes = EnumSet.noneOf(Status.Code.class);
      for (String codeText : retryDef.getRetryCodesList()) {
        try {
          codes.add(Status.Code.valueOf(codeText));
        } catch (IllegalArgumentException e) {
          diagCollector.addDiag(
              Diag.error(
                  SimpleLocation.TOPLEVEL,
                  "status code not found: '%s' (in interface %s)",
                  codeText,
                  interfaceConfigProto.getName()));
        }
      }
      builder.put(retryDef.getName(), Sets.immutableEnumSet(codes));
    }
    if (diagCollector.getErrorCount() > 0) {
      return null;
    }
    return builder.build();
  }

  private static ImmutableMap<String, RetrySettings> createRetrySettingsDefinition(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, RetrySettings> builder =
        ImmutableMap.<String, RetrySettings>builder();
    for (RetryParamsDefinitionProto retryDef : interfaceConfigProto.getRetryParamsDefList()) {
      try {
        RetrySettings settings =
            RetrySettings.newBuilder()
                .setInitialRetryDelay(Duration.ofMillis(retryDef.getInitialRetryDelayMillis()))
                .setRetryDelayMultiplier(retryDef.getRetryDelayMultiplier())
                .setMaxRetryDelay(Duration.ofMillis(retryDef.getMaxRetryDelayMillis()))
                .setInitialRpcTimeout(Duration.ofMillis(retryDef.getInitialRpcTimeoutMillis()))
                .setRpcTimeoutMultiplier(retryDef.getRpcTimeoutMultiplier())
                .setMaxRpcTimeout(Duration.ofMillis(retryDef.getMaxRpcTimeoutMillis()))
                .setTotalTimeout(Duration.ofMillis(retryDef.getTotalTimeoutMillis()))
                .build();
        builder.put(retryDef.getName(), settings);
      } catch (IllegalStateException | NullPointerException e) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "error while creating retry params: %s (in interface %s)",
                e,
                interfaceConfigProto.getName()));
      }
    }
    if (diagCollector.getErrorCount() > 0) {
      return null;
    }
    return builder.build();
  }

  private static ImmutableMap<String, GapicMethodConfig> createMethodConfigMap(
      DiagCollector diagCollector,
      String language,
      InterfaceConfigProto interfaceConfigProto,
      Interface apiInterface,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      ImmutableSet<String> retryCodesConfigNames,
      ImmutableSet<String> retryParamsConfigNames) {
    ImmutableMap.Builder<String, GapicMethodConfig> methodConfigMapBuilder = ImmutableMap.builder();

    for (MethodConfigProto methodConfigProto : interfaceConfigProto.getMethodsList()) {
      Interface targetInterface =
          getTargetInterface(apiInterface, methodConfigProto.getRerouteToGrpcInterface());
      Method method = targetInterface.lookupMethod(methodConfigProto.getName());
      if (method == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL, "method not found: %s", methodConfigProto.getName()));
        continue;
      }
      GapicMethodConfig methodConfig =
          GapicMethodConfig.createMethodConfig(
              diagCollector,
              language,
              methodConfigProto,
              method,
              messageConfigs,
              resourceNameConfigs,
              retryCodesConfigNames,
              retryParamsConfigNames);
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

  private static List<GapicMethodConfig> createMethodConfigs(
      ImmutableMap<String, GapicMethodConfig> methodConfigMap,
      InterfaceConfigProto interfaceConfigProto) {
    List<GapicMethodConfig> methodConfigs = new ArrayList<>();
    for (MethodConfigProto methodConfigProto : interfaceConfigProto.getMethodsList()) {
      methodConfigs.add(methodConfigMap.get(methodConfigProto.getName()));
    }
    return methodConfigs;
  }

  /** Returns the GapicMethodConfig for the given method. */
  public GapicMethodConfig getMethodConfig(Method method) {
    GapicMethodConfig methodConfig = getMethodConfigMap().get(method.getSimpleName());
    if (methodConfig == null) {
      throw new IllegalArgumentException(
          "no method config for method '" + method.getFullName() + "'");
    }
    return methodConfig;
  }

  public boolean hasDefaultServiceAddress() {
    return !getRequiredConstructorParams().contains(SERVICE_ADDRESS_PARAM);
  }

  public boolean hasDefaultServiceScopes() {
    return !getRequiredConstructorParams().contains(SCOPES_PARAM);
  }

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

  /** Creates a list of fields that can be turned into IAM resources */
  private static ImmutableList<Field> createIamResources(
      Model model, List<IamResourceProto> resources) {
    ImmutableList.Builder<Field> fields = ImmutableList.builder();
    for (IamResourceProto resource : resources) {
      TypeRef type = model.getSymbolTable().lookupType(resource.getType());
      if (type == null) {
        throw new IllegalArgumentException("type not found: " + resource.getType());
      }
      if (!type.isMessage()) {
        throw new IllegalArgumentException("type must be a message: " + type);
      }
      Field field = type.getMessageType().lookupField(resource.getField());
      if (field == null) {
        throw new IllegalArgumentException(
            String.format(
                "type %s does not have field %s", resource.getType(), resource.getField()));
      }
      fields.add(field);
    }
    return fields.build();
  }

  public boolean hasPageStreamingMethods() {
    for (GapicMethodConfig methodConfig : getMethodConfigs()) {
      if (methodConfig.isPageStreaming()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasBatchingMethods() {
    for (GapicMethodConfig methodConfig : getMethodConfigs()) {
      if (methodConfig.isBatching()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasGrpcStreamingMethods() {
    for (GapicMethodConfig methodConfig : getMethodConfigs()) {
      if (methodConfig.isGrpcStreaming()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasGrpcStreamingMethods(GrpcStreamingConfig.GrpcStreamingType streamingType) {
    for (GapicMethodConfig methodConfig : getMethodConfigs()) {
      if (methodConfig.isGrpcStreaming() && methodConfig.getGrpcStreamingType() == streamingType) {
        return true;
      }
    }
    return false;
  }

  public boolean hasLongRunningOperations() {
    for (GapicMethodConfig methodConfig : getMethodConfigs()) {
      if (methodConfig.isLongRunningOperation()) {
        return true;
      }
    }
    return false;
  }
}
