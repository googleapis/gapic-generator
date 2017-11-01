/* Copyright 2017 Google LLC
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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.ApiSource;
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
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;

/**
 * The context for transforming a Discovery Doc API into a view model to use for client library
 * generation.
 */
@AutoValue
public abstract class DiscoGapicInterfaceContext implements InterfaceContext {
  private ImmutableList<MethodModel> interfaceMethods;

  public static DiscoGapicInterfaceContext createWithoutInterface(
      Document document,
      GapicProductConfig productConfig,
      SchemaTypeTable typeTable,
      DiscoGapicNamer discoGapicNamer,
      FeatureConfig featureConfig) {
    return new AutoValue_DiscoGapicInterfaceContext(
        productConfig,
        typeTable,
        discoGapicNamer,
        new DiscoInterfaceModel("", document),
        featureConfig);
  }

  public static DiscoGapicInterfaceContext createWithInterface(
      Document document,
      String interfaceName,
      GapicProductConfig productConfig,
      SchemaTypeTable typeTable,
      DiscoGapicNamer discoGapicNamer,
      FeatureConfig featureConfig) {
    ImmutableList.Builder<MethodModel> interfaceMethods = new ImmutableList.Builder<>();

    for (MethodConfig method : productConfig.getInterfaceConfig(interfaceName).getMethodConfigs()) {
      interfaceMethods.add(method.getMethodModel());
    }

    return new AutoValue_DiscoGapicInterfaceContext(
        productConfig,
        typeTable,
        discoGapicNamer,
        new DiscoInterfaceModel(interfaceName, document),
        featureConfig);
  }

  public static DiscoGapicInterfaceContext createWithInterface(
      InterfaceModel interfaceModel,
      GapicProductConfig productConfig,
      ImportTypeTable typeTable,
      DiscoGapicNamer discoGapicNamer,
      FeatureConfig featureConfig) {
    Preconditions.checkArgument(interfaceModel.getApiSource().equals(ApiSource.DISCOVERY));
    Preconditions.checkArgument(typeTable instanceof SchemaTypeTable);
    ImmutableList.Builder<MethodModel> interfaceMethods = new ImmutableList.Builder<>();

    for (MethodConfig method :
        productConfig.getInterfaceConfig(interfaceModel.getFullName()).getMethodConfigs()) {
      interfaceMethods.add(method.getMethodModel());
    }

    return new AutoValue_DiscoGapicInterfaceContext(
        productConfig,
        (SchemaTypeTable) typeTable,
        discoGapicNamer,
        (DiscoInterfaceModel) interfaceModel,
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
  public List<MethodModel> getInterfaceConfigMethods() {
    if (interfaceMethods != null) {
      return interfaceMethods;
    }

    ImmutableList.Builder<MethodModel> methodBuilder = ImmutableList.builder();
    for (MethodConfig methodConfig : getInterfaceConfig().getMethodConfigs()) {
      MethodModel method = methodConfig.getMethodModel();
      if (isSupported(method)) {
        methodBuilder.add(method);
      }
    }
    interfaceMethods = methodBuilder.build();
    return interfaceMethods;
  }

  /** Returns a list of methods for this interface. Memoize the result. */
  @Override
  public List<MethodModel> getInterfaceMethods() {
    // TODO(andrealin): Should this be different from getInterfaceConfigMethods()?
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
  public SurfaceNamer getNamer() {
    return getDiscoGapicNamer().getLanguageNamer();
  }

  @Override
  public abstract FeatureConfig getFeatureConfig();

  @Override
  public DiscoGapicInterfaceContext withNewTypeTable() {
    return createWithInterface(
        getDocument(),
        getInterfaceName(),
        getProductConfig(),
        (SchemaTypeTable) getImportTypeTable().cloneEmpty(),
        getDiscoGapicNamer(),
        getFeatureConfig());
  }

  @Override
  public DiscoGapicInterfaceContext withNewTypeTable(String packageName) {
    return createWithInterface(
        getDocument(),
        getInterfaceName(),
        getProductConfig().withPackageName(packageName),
        getSchemaTypeTable().cloneEmpty(packageName),
        getDiscoGapicNamer().cloneWithPackageName(packageName),
        getFeatureConfig());
  }

  @Override
  /* Returns a list of public methods, configured by FeatureConfig. Memoize the result. */
  public Iterable<MethodModel> getPublicMethods() {
    return Iterables.filter(
        getInterfaceConfigMethods(),
        new Predicate<MethodModel>() {
          @Override
          public boolean apply(MethodModel methodModel) {
            return isSupported(methodModel);
          }
        });
  }

  @Override
  /* Returns a list of supported methods, configured by FeatureConfig. Memoize the result. */
  public Iterable<MethodModel> getSupportedMethods() {
    return Iterables.filter(
        getInterfaceConfigMethods(),
        new Predicate<MethodModel>() {
          @Override
          public boolean apply(MethodModel methodModel) {
            return isSupported(methodModel);
          }
        });
  }

  public boolean isSupported(MethodModel method) {
    return getInterfaceConfig().getMethodConfig(method).getVisibility()
        != VisibilityConfig.DISABLED;
  }

  @Override
  /* Returns the GapicMethodConfig for the given method. */
  public DiscoGapicMethodConfig getMethodConfig(MethodModel method) {
    for (InterfaceConfig config : getProductConfig().getInterfaceConfigMap().values()) {
      for (MethodConfig methodConfig : config.getMethodConfigs()) {
        if (methodConfig.getMethodModel().getFullName().equals(method.getFullName())) {
          return (DiscoGapicMethodConfig) methodConfig;
        }
      }
    }

    throw new IllegalArgumentException(
        "Interface config does not exist for method: " + method.getFullName());
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
  public Iterable<MethodModel> getLongRunningMethods() {
    return ImmutableList.of();
  }

  @Override
  public DiscoGapicMethodContext asFlattenedMethodContext(
      MethodModel method, FlatteningConfig flatteningConfig) {
    Preconditions.checkArgument(method.getApiSource().equals(ApiSource.DISCOVERY));
    return DiscoGapicMethodContext.create(
        this,
        getInterfaceName(),
        getProductConfig(),
        getSchemaTypeTable(),
        getDiscoGapicNamer(),
        (DiscoveryMethodModel) method,
        getMethodConfig(method),
        flatteningConfig,
        getFeatureConfig());
  }

  @Override
  public DiscoGapicMethodContext asRequestMethodContext(MethodModel method) {
    Preconditions.checkArgument(method.getApiSource().equals(ApiSource.DISCOVERY));
    return DiscoGapicMethodContext.create(
        this,
        getInterfaceName(),
        getProductConfig(),
        getSchemaTypeTable(),
        getDiscoGapicNamer(),
        (DiscoveryMethodModel) method,
        getMethodConfig(method),
        null,
        getFeatureConfig());
  }

  @Override
  public DiscoGapicMethodContext asDynamicMethodContext(MethodModel method) {
    Preconditions.checkArgument(method.getApiSource().equals(ApiSource.DISCOVERY));
    return DiscoGapicMethodContext.create(
        this,
        getInterfaceName(),
        getProductConfig(),
        getSchemaTypeTable(),
        getDiscoGapicNamer(),
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
    Preconditions.checkArgument(method.getApiSource().equals(ApiSource.DISCOVERY));
    return DiscoGapicMethodContext.create(
        this,
        interfaceName,
        getProductConfig(),
        getSchemaTypeTable(),
        getDiscoGapicNamer(),
        (DiscoveryMethodModel) method,
        getMethodConfig(method),
        flatteningConfig,
        getFeatureConfig());
  }

  @Override
  public String serviceTitle() {
    return getDocument().name();
  }
}
