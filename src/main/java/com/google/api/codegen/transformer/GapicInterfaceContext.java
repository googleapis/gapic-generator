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
package com.google.api.codegen.transformer;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoInterfaceModel;
import com.google.api.codegen.config.ProtoMethodModel;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * The context for transforming an API interface, as contained in a Model, into a view model to use
 * for client library generation.
 */
@AutoValue
public abstract class GapicInterfaceContext implements InterfaceContext {
  private ImmutableList<MethodModel> interfaceMethods;

  public static GapicInterfaceContext create(
      Interface apiInterface,
      GapicProductConfig productConfig,
      ModelTypeTable typeTable,
      SurfaceNamer namer,
      FeatureConfig featureConfig) {
    return create(
        new ProtoInterfaceModel(apiInterface), productConfig, typeTable, namer, featureConfig);
  }

  public static GapicInterfaceContext create(
      InterfaceModel apiInterface,
      GapicProductConfig productConfig,
      ModelTypeTable typeTable,
      SurfaceNamer namer,
      FeatureConfig featureConfig) {
    ProtoInterfaceModel protoInterface = (ProtoInterfaceModel) apiInterface;
    return new AutoValue_GapicInterfaceContext(
        protoInterface,
        productConfig,
        typeTable,
        namer,
        featureConfig,
        createGrpcRerouteMap(protoInterface.getInterface().getModel(), productConfig));
  }

  private static Map<Interface, Interface> createGrpcRerouteMap(
      Model model, GapicProductConfig productConfig) {
    HashMap<Interface, Interface> grpcRerouteMap = new HashMap<>();
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      if (!apiInterface.isReachable()) {
        continue;
      }
      InterfaceConfig interfaceConfig = productConfig.getInterfaceConfig(apiInterface);
      for (MethodConfig methodConfig : interfaceConfig.getMethodConfigs()) {
        String reroute = methodConfig.getRerouteToGrpcInterface();
        if (!Strings.isNullOrEmpty(reroute)) {
          Interface targetInterface = model.getSymbolTable().lookupInterface(reroute);
          grpcRerouteMap.put(targetInterface, apiInterface);
        }
      }
    }
    return grpcRerouteMap;
  }

  public Model getModel() {
    return getInterface().getModel();
  }

  @Override
  public ApiModel getApiModel() {
    return getInterfaceModel().getApiModel();
  }

  public Interface getInterface() {
    return getInterfaceModel().getInterface();
  }

  @Override
  public abstract ProtoInterfaceModel getInterfaceModel();

  @Override
  public abstract GapicProductConfig getProductConfig();

  @Override
  public abstract ModelTypeTable getImportTypeTable();

  @Override
  public abstract SurfaceNamer getNamer();

  @Override
  public abstract FeatureConfig getFeatureConfig();

  /** A map which maps the reroute grpc interface to its original interface */
  @Nullable
  public abstract Map<Interface, Interface> getGrpcRerouteMap();

  @Override
  public GapicInterfaceContext withNewTypeTable() {
    return create(
        getInterface(),
        getProductConfig(),
        getImportTypeTable().cloneEmpty(),
        getNamer(),
        getFeatureConfig());
  }

  @Override
  public GapicInterfaceContext withNewTypeTable(String packageName) {
    return create(
        getInterface(),
        getProductConfig().withPackageName(packageName),
        getImportTypeTable().cloneEmpty(packageName),
        getNamer().cloneWithPackageName(packageName),
        getFeatureConfig());
  }

  @Override
  public GapicInterfaceConfig getInterfaceConfig() {
    return getProductConfig().getInterfaceConfig(getInterface());
  }

  /**
   * Returns the MethodConfig object of the given gRPC method.
   *
   * <p>If the method is a gRPC re-route method, returns the MethodConfig of the original method.
   */
  public GapicMethodConfig getMethodConfig(MethodModel method) {
    Interface originalInterface = getInterface();
    if (getGrpcRerouteMap().containsKey(originalInterface)) {
      originalInterface = getGrpcRerouteMap().get(originalInterface);
    }
    InterfaceConfig originalInterfaceConfig =
        getProductConfig().getInterfaceConfig(originalInterface);
    if (originalInterfaceConfig != null) {
      return (GapicMethodConfig) originalInterfaceConfig.getMethodConfig(method);
    } else {
      throw new IllegalArgumentException(
          "Interface config does not exist for method: " + method.getSimpleName());
    }
  }

  @Override
  public GapicMethodContext asFlattenedMethodContext(
      MethodModel method, FlatteningConfig flatteningConfig) {
    return GapicMethodContext.create(
        this,
        getInterface(),
        getProductConfig(),
        getImportTypeTable(),
        getNamer(),
        (ProtoMethodModel) method,
        getMethodConfig(method),
        flatteningConfig,
        getFeatureConfig());
  }

  @Override
  public GapicMethodContext asRequestMethodContext(MethodModel method) {
    return GapicMethodContext.create(
        this,
        getInterface(),
        getProductConfig(),
        getImportTypeTable(),
        getNamer(),
        (ProtoMethodModel) method,
        getMethodConfig(method),
        null,
        getFeatureConfig());
  }

  @Override
  public GapicMethodContext asDynamicMethodContext(MethodModel method) {
    return GapicMethodContext.create(
        this,
        getInterface(),
        getProductConfig(),
        getImportTypeTable(),
        getNamer(),
        (ProtoMethodModel) method,
        getMethodConfig(method),
        null,
        getFeatureConfig());
  }

  @Override
  public List<MethodModel> getInterfaceMethods() {
    ImmutableList.Builder<MethodModel> methodBuilder = ImmutableList.builder();
    for (Method method : getInterface().getMethods()) {
      methodBuilder.add(new ProtoMethodModel(method));
    }
    return methodBuilder.build();
  }

  /** Returns a list of methods for this interface that have method configs. Memoize the result. */
  @Override
  public List<MethodModel> getInterfaceConfigMethods() {
    if (interfaceMethods != null) {
      return interfaceMethods;
    }

    ImmutableList.Builder<MethodModel> methodBuilder = ImmutableList.builder();
    for (MethodConfig methodConfig : getInterfaceConfig().getMethodConfigs()) {
      methodBuilder.add(new ProtoMethodModel(((GapicMethodConfig) methodConfig).getMethod()));
    }
    interfaceMethods = methodBuilder.build();
    return interfaceMethods;
  }

  /** Returns a list of supported methods, configured by FeatureConfig. */
  @Override
  public List<MethodModel> getSupportedMethods() {
    List<MethodModel> methods = new ArrayList<>(getInterfaceConfig().getMethodConfigs().size());
    for (MethodModel methodModel : getInterfaceConfigMethods()) {
      if (isSupported(methodModel)) {
        methods.add(methodModel);
      }
    }
    return methods;
  }

  /**
   * Returns a list of methods with samples, similar to getSupportedMethods, but also filter out
   * private methods.
   */
  @Override
  public List<MethodModel> getPublicMethods() {
    List<MethodModel> methods = new ArrayList<>(getInterfaceConfig().getMethodConfigs().size());
    for (MethodModel method : getInterfaceConfigMethods()) {
      VisibilityConfig visibility = getInterfaceConfig().getMethodConfig(method).getVisibility();
      if (isSupported(method) && visibility == VisibilityConfig.PUBLIC) {
        methods.add(method);
      }
    }
    return methods;
  }

  private boolean isSupported(MethodModel method) {
    boolean supported = true;
    supported &=
        getFeatureConfig().enableGrpcStreaming() || !MethodConfig.isGrpcStreamingMethod(method);
    supported &=
        getInterfaceConfig().getMethodConfig(method).getVisibility() != VisibilityConfig.DISABLED;
    return supported;
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
    List<MethodModel> methods = new ArrayList<>();
    for (MethodModel method : getSupportedMethods()) {
      if (getMethodConfig(method).isLongRunningOperation()) {
        methods.add(method);
      }
    }
    return methods;
  }

  public Iterable<MethodModel> getGrpcStreamingMethods() {
    List<MethodModel> methods = new ArrayList<>();
    for (MethodModel method : getSupportedMethods()) {
      if (getMethodConfig(method).isGrpcStreaming()) {
        methods.add(method);
      }
    }
    return methods;
  }

  @Override
  public String getInterfaceDescription() {
    return DocumentationUtil.getScopedDescription(getInterface());
  }

  @Override
  public String serviceTitle() {
    return getApiModel().getTitle();
  }
}
