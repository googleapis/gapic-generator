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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.DiscoApiModel;
import com.google.api.codegen.config.DiscoGapicInterfaceConfig;
import com.google.api.codegen.config.DiscoGapicMethodConfig;
import com.google.api.codegen.config.DiscoInterfaceModel;
import com.google.api.codegen.config.DiscoveryMethodModel;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Schema;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The context for transforming a Discovery Doc API into a view model to use for client library
 * generation.
 */
@AutoValue
public abstract class DiscoGapicInterfaceContext implements InterfaceContext {
  private ImmutableList<DiscoveryMethodModel> interfaceMethods;

  public static DiscoGapicInterfaceContext createWithoutInterface(
      DiscoApiModel model,
      GapicProductConfig productConfig,
      SchemaTypeTable typeTable,
      SurfaceNamer namer,
      FeatureConfig featureConfig) {
    return new AutoValue_DiscoGapicInterfaceContext(
        productConfig,
        typeTable,
        new DiscoGapicNamer(),
        new DiscoInterfaceModel("", model),
        namer,
        featureConfig);
  }

  private static DiscoGapicInterfaceContext createWithInterface(
      DiscoApiModel model,
      String interfaceName,
      GapicProductConfig productConfig,
      SchemaTypeTable typeTable,
      SurfaceNamer namer,
      FeatureConfig featureConfig) {
    ImmutableList.Builder<MethodModel> interfaceMethods = new ImmutableList.Builder<>();

    for (MethodConfig method : productConfig.getInterfaceConfig(interfaceName).getMethodConfigs()) {
      interfaceMethods.add(method.getMethodModel());
    }

    return new AutoValue_DiscoGapicInterfaceContext(
        productConfig,
        typeTable,
        new DiscoGapicNamer(),
        new DiscoInterfaceModel(interfaceName, model),
        namer,
        featureConfig);
  }

  public static DiscoGapicInterfaceContext createWithInterface(
      InterfaceModel interfaceModel,
      GapicProductConfig productConfig,
      ImportTypeTable typeTable,
      SurfaceNamer namer,
      FeatureConfig featureConfig) {
    Preconditions.checkArgument(typeTable instanceof SchemaTypeTable);
    ImmutableList.Builder<MethodModel> interfaceMethods = new ImmutableList.Builder<>();

    for (MethodConfig method :
        productConfig.getInterfaceConfig(interfaceModel.getFullName()).getMethodConfigs()) {
      interfaceMethods.add(method.getMethodModel());
    }

    return new AutoValue_DiscoGapicInterfaceContext(
        productConfig,
        (SchemaTypeTable) typeTable,
        new DiscoGapicNamer(),
        (DiscoInterfaceModel) interfaceModel,
        namer,
        featureConfig);
  }

  public Document getDocument() {
    return getApiModel().getDocument();
  }

  @Override
  public DiscoApiModel getApiModel() {
    return getInterfaceModel().getApiModel();
  }

  @Override
  public abstract GapicProductConfig getProductConfig();

  public abstract SchemaTypeTable getSchemaTypeTable();

  public abstract DiscoGapicNamer getDiscoGapicNamer();

  /** Returns a list of methods for this interface. Memoize the result. */
  @Override
  public List<DiscoveryMethodModel> getInterfaceConfigMethods() {
    if (interfaceMethods != null) {
      return interfaceMethods;
    }

    ImmutableList.Builder<DiscoveryMethodModel> methodBuilder = ImmutableList.builder();
    for (DiscoGapicMethodConfig methodConfig : getInterfaceConfig().getMethodConfigs()) {
      DiscoveryMethodModel method = methodConfig.getMethodModel();
      if (isSupported(method)) {
        methodBuilder.add(method);
      }
    }
    interfaceMethods = methodBuilder.build();
    return interfaceMethods;
  }

  /** Returns a list of methods for this interface. Memoize the result. */
  @Override
  public List<DiscoveryMethodModel> getInterfaceMethods() {
    return getInterfaceConfigMethods();
  }

  @Override
  public String getInterfaceDescription() {
    return getDocument().description();
  }

  public String getInterfaceName() {
    return getInterfaceModel().getFullName();
  }

  @Override
  public abstract DiscoInterfaceModel getInterfaceModel();

  @Override
  public abstract SurfaceNamer getNamer();

  @Override
  public abstract FeatureConfig getFeatureConfig();

  @Override
  public DiscoGapicInterfaceContext withNewTypeTable() {
    return createWithInterface(
        getApiModel(),
        getInterfaceName(),
        getProductConfig(),
        (SchemaTypeTable) getImportTypeTable().cloneEmpty(),
        getNamer(),
        getFeatureConfig());
  }

  @Override
  public DiscoGapicInterfaceContext withNewTypeTable(String packageName) {
    return createWithInterface(
        getApiModel(),
        getInterfaceName(),
        getProductConfig().withPackageName(packageName),
        getSchemaTypeTable().cloneEmpty(packageName),
        getNamer().cloneWithPackageName(packageName),
        getFeatureConfig());
  }

  @Override
  /* Returns a list of public methods, configured by FeatureConfig. Memoize the result. */
  public List<DiscoveryMethodModel> getPublicMethods() {
    return getInterfaceConfigMethods()
        .stream()
        .filter(m -> isSupported(m))
        .collect(Collectors.toList());
  }

  @Override
  /* Returns a list of supported methods, configured by FeatureConfig. Memoize the result. */
  public List<DiscoveryMethodModel> getSupportedMethods() {
    return getPublicMethods();
  }

  private boolean isSupported(MethodModel method) {
    return getInterfaceConfig().getMethodConfig(method).getVisibility()
        != VisibilityConfig.DISABLED;
  }

  @Override
  /* Returns the DiscoGapicMethodConfig for the given method. */
  public DiscoGapicMethodConfig getMethodConfig(MethodModel method) {
    String methodName = method.getFullName();
    for (InterfaceConfig config : getProductConfig().getInterfaceConfigMap().values()) {
      for (MethodConfig methodConfig : config.getMethodConfigs()) {
        if (methodConfig.getMethodModel().getFullName().equals(methodName)) {
          return (DiscoGapicMethodConfig) methodConfig;
        }
      }
    }

    throw new IllegalArgumentException("Interface config does not exist for method: " + methodName);
  }

  @Override
  public List<MethodModel> getPageStreamingMethods() {
    List<MethodModel> methods = new ArrayList<>();
    for (MethodModel method : getSupportedMethods()) {
      if (getMethodConfig(method).isPageStreaming()) {
        methods.add(method);
      }
    }
    return methods;
  }

  @Override
  public List<MethodModel> getBatchingMethods() {
    List<MethodModel> methods = new ArrayList<>();
    for (MethodModel method : getSupportedMethods()) {
      if (getMethodConfig(method).isBatching()) {
        methods.add(method);
      }
    }
    return methods;
  }

  @Override
  public List<MethodModel> getLongRunningMethods() {
    return getSupportedMethods()
        .stream()
        .filter(m -> getMethodConfig(m).isLongRunningOperation())
        .collect(Collectors.toList());
  }

  @Override
  public DiscoGapicMethodContext asFlattenedMethodContext(
      MethodModel method, FlatteningConfig flatteningConfig) {
    return DiscoGapicMethodContext.create(
        this,
        getInterfaceName(),
        getProductConfig(),
        getSchemaTypeTable(),
        getNamer(),
        (DiscoveryMethodModel) method,
        getMethodConfig(method),
        flatteningConfig,
        getFeatureConfig());
  }

  @Override
  public DiscoGapicMethodContext asRequestMethodContext(MethodModel method) {
    return DiscoGapicMethodContext.create(
        this,
        getInterfaceName(),
        getProductConfig(),
        getSchemaTypeTable(),
        getNamer(),
        (DiscoveryMethodModel) method,
        getMethodConfig(method),
        null,
        getFeatureConfig());
  }

  @Override
  public DiscoGapicMethodContext asDynamicMethodContext(MethodModel method) {
    return DiscoGapicMethodContext.create(
        this,
        getInterfaceName(),
        getProductConfig(),
        getSchemaTypeTable(),
        getNamer(),
        (DiscoveryMethodModel) method,
        getMethodConfig(method),
        null,
        getFeatureConfig());
  }

  @Override
  public DiscoGapicInterfaceConfig getInterfaceConfig() {
    return (DiscoGapicInterfaceConfig) getProductConfig().getInterfaceConfig(getInterfaceName());
  }

  @Override
  public ImportTypeTable getImportTypeTable() {
    return getSchemaTypeTable();
  }

  public DiscoGapicMethodContext asFlattenedMethodContext(
      MethodModel method, FlatteningConfig flatteningConfig, String interfaceName) {
    return DiscoGapicMethodContext.create(
        this,
        interfaceName,
        getProductConfig(),
        getSchemaTypeTable(),
        getNamer(),
        (DiscoveryMethodModel) method,
        getMethodConfig(method),
        flatteningConfig,
        getFeatureConfig());
  }

  @Override
  public String serviceTitle() {
    return getDocument().name();
  }

  @Override
  public String getServiceAddress() {
    return getDocument().baseUrl();
  }

  // TODO(andrealin): Parameterize this in Config instead of hard-coding it.
  @Override
  public String getOperationServiceName() {
    return getOperationsScopeName() + "Operation";
  }

  private String getOperationsScopeName() {
    // Hard-code Compute API's GlobalOperationClient in for now.
    // We can expose this configuration in the GAPIC config later.
    if (getLongRunningMethods().isEmpty()) {
      return "$ Failed to find methods to be used by long running client.";
    }
    DiscoveryMethodModel methodModel = (DiscoveryMethodModel) getLongRunningMethods().get(0);
    Map<String, Schema> pathParms = methodModel.getDiscoMethod().pathParams();
    if (pathParms.containsKey("region")) {
      return "Region";
    } else if (pathParms.containsKey("zone")) {
      return "Zone";
    } else {
      return "Global";
    }
  }
}
