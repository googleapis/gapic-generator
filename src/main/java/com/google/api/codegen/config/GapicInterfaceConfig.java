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
import com.google.api.codegen.IamResourceProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.RetryParamsDefinitionProto;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.configgen.ProtoMethodTransformer;
import com.google.api.codegen.transformer.RetryDefinitionsTransformer;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
      ImmutableSet.<String>of(SERVICE_ADDRESS_PARAM, SCOPES_PARAM);

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
  public abstract ImmutableMap<String, List<String>> getRetryCodesDefinition();

  @Override
  public abstract ImmutableMap<String, RetryParamsDefinitionProto> getRetrySettingsDefinition();

  @Override
  public abstract ImmutableList<FieldModel> getIamResources();

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
      @Nullable InterfaceConfigProto interfaceConfigProto,
      Interface apiInterface,
      String interfaceNameOverride,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      ProtoMethodTransformer configUtils) {

    ImmutableMap.Builder<String, String> methodNameToRetryNameMap = ImmutableMap.builder();
    ImmutableMap<String, List<String>> retryCodesDefinition =
        RetryDefinitionsTransformer.createRetryCodesDefinition(
            diagCollector, interfaceConfigProto, apiInterface, methodNameToRetryNameMap);
    ImmutableMap<String, RetryParamsDefinitionProto> retrySettingsDefinition =
        RetryDefinitionsTransformer.createRetrySettingsDefinition(interfaceConfigProto);

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
              methodNameToRetryNameMap.build(),
              retrySettingsDefinition.keySet(),
              configUtils);
      methodConfigs = createMethodConfigs(methodConfigMap, apiInterface, interfaceConfigProto);
    }

    SmokeTestConfig smokeTestConfig =
        createSmokeTestConfig(diagCollector, apiInterface, interfaceConfigProto);

    ImmutableList<FieldModel> iamResources =
        createIamResources(apiInterface.getModel(), interfaceConfigProto);

    ImmutableList<String> requiredConstructorParams;
    if (interfaceConfigProto != null) {
      requiredConstructorParams =
          ImmutableList.copyOf(interfaceConfigProto.getRequiredConstructorParamsList());
      for (String param : interfaceConfigProto.getRequiredConstructorParamsList()) {
        if (!CONSTRUCTOR_PARAMS.contains(param)) {
          diagCollector.addDiag(
              Diag.error(SimpleLocation.TOPLEVEL, "Unsupported constructor param: %s", param));
        }
      }
    } else {
      requiredConstructorParams = ImmutableList.of();
    }

    String manualDoc = "";

    ImmutableList.Builder<SingleResourceNameConfig> resourcesBuilder = ImmutableList.builder();
    if (interfaceConfigProto != null) {
      for (CollectionConfigProto collectionConfigProto :
          interfaceConfigProto.getCollectionsList()) {
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
      manualDoc =
          Strings.nullToEmpty(
                  interfaceConfigProto.getLangDoc().get(language.toString().toLowerCase()))
              .trim();
    }
    ImmutableList<SingleResourceNameConfig> singleResourceNames = resourcesBuilder.build();

    if (diagCollector.hasErrors()) {
      return null;
    } else {
      return new AutoValue_GapicInterfaceConfig(
          interfaceNameOverride,
          new ProtoInterfaceModel(apiInterface),
          methodConfigs,
          smokeTestConfig,
          methodConfigMap,
          retryCodesDefinition,
          retrySettingsDefinition,
          iamResources,
          requiredConstructorParams,
          singleResourceNames,
          manualDoc);
    }
  }

  private static SmokeTestConfig createSmokeTestConfig(
      DiagCollector diagCollector,
      Interface apiInterface,
      @Nullable InterfaceConfigProto interfaceConfigProto) {
    if (interfaceConfigProto != null && interfaceConfigProto.hasSmokeTest()) {
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
      @Nullable InterfaceConfigProto interfaceConfigProto,
      Interface apiInterface,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      Map<String, String> retryCodesConfigNames,
      ImmutableSet<String> retryParamsConfigNames,
      ProtoMethodTransformer configUtils) {
    Map<String, GapicMethodConfig> methodConfigMapBuilder = new LinkedHashMap<>();

    // Keep track of the MethodConfigProtos encountered.
    Map<String, MethodConfigProto> methodConfigProtoMap = new HashMap<>();

    if (interfaceConfigProto != null) {
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
                retryParamsConfigNames,
                configUtils);
        if (methodConfig == null) {
          continue;
        }
        methodConfigProtoMap.put(methodConfigProto.getName(), methodConfigProto);
        methodConfigMapBuilder.put(methodConfigProto.getName(), methodConfig);
      }
    }
    for (Method method : apiInterface.getMethods()) {
      // TODO(andrealin): Reroute to grpc interface.
      MethodConfigProto methodConfigProto = methodConfigProtoMap.get(method.getSimpleName());
      GapicMethodConfig methodConfig =
          GapicMethodConfig.createMethodConfig(
              diagCollector,
              language,
              methodConfigProto,
              method,
              messageConfigs,
              resourceNameConfigs,
              retryCodesConfigNames,
              retryParamsConfigNames,
              configUtils);
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

  static <T> List<T> createMethodConfigs(
      ImmutableMap<String, T> methodConfigMap,
      Interface apiInterface,
      @Nullable InterfaceConfigProto interfaceConfigProto) {
    Map<String, T> methodConfigs = new LinkedHashMap<>();
    // Add in methods that aren't defined in the source protos but are defined in the GAPIC config.
    if (interfaceConfigProto != null) {
      for (MethodConfigProto methodConfigProto : interfaceConfigProto.getMethodsList()) {
        methodConfigs.put(
            methodConfigProto.getName(), methodConfigMap.get(methodConfigProto.getName()));
      }
    }
    // Add in methods that aren't defined in the GAPIC config but are defined in the source protos.
    for (Method method : apiInterface.getMethods()) {
      methodConfigs.put(method.getSimpleName(), methodConfigMap.get(method.getSimpleName()));
    }
    return new LinkedList<>(methodConfigs.values());
  }

  static <T> List<T> createMethodConfigs(
      ImmutableMap<String, T> methodConfigMap, InterfaceConfigProto interfaceConfigProto) {
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

  /** Creates a list of fields that can be turned into IAM resources */
  private static ImmutableList<FieldModel> createIamResources(
      Model model, InterfaceConfigProto interfaceConfigProto) {
    ImmutableList.Builder<FieldModel> fields = ImmutableList.builder();
    if (interfaceConfigProto != null) {
      List<IamResourceProto> resources =
          interfaceConfigProto.getExperimentalFeatures().getIamResourcesList();
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
        fields.add(new ProtoField(field));
      }
    }
    return fields.build();
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
      if (methodConfig.isLongRunningOperation()) {
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
