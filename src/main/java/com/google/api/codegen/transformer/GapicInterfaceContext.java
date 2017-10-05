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
package com.google.api.codegen.transformer;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.util.TypeTable;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
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
  public static GapicInterfaceContext create(
      Interface apiInterface,
      GapicProductConfig productConfig,
      ModelTypeTable typeTable,
      SurfaceNamer namer,
      FeatureConfig featureConfig) {
    return new AutoValue_GapicInterfaceContext(
        apiInterface,
        productConfig,
        typeTable,
        namer,
        featureConfig,
        createGrpcRerouteMap(apiInterface.getModel(), productConfig));
  }

  private static Map<Interface, Interface> createGrpcRerouteMap(
      Model model, GapicProductConfig productConfig) {
    HashMap<Interface, Interface> grpcRerouteMap = new HashMap<>();
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      if (!apiInterface.isReachable()) {
        continue;
      }
      GapicInterfaceConfig interfaceConfig = productConfig.getInterfaceConfig(apiInterface);
      for (GapicMethodConfig methodConfig : interfaceConfig.getMethodConfigs()) {
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

  public abstract Interface getInterface();

  @Override
  public abstract GapicProductConfig getProductConfig();

  public abstract ModelTypeTable getModelTypeTable();

  @Override
  public TypeTable getTypeTable() {
    return getModelTypeTable().getTypeTable();
  }

  @Override
  public abstract SurfaceNamer getNamer();

  public abstract FeatureConfig getFeatureConfig();

  /** A map which maps the reroute grpc interface to its original interface */
  @Nullable
  public abstract Map<Interface, Interface> getGrpcRerouteMap();

  public GapicInterfaceContext withNewTypeTable() {
    return create(
        getInterface(),
        getProductConfig(),
        getModelTypeTable().cloneEmpty(),
        getNamer(),
        getFeatureConfig());
  }

  public GapicInterfaceContext withNewTypeTable(String packageName) {
    return create(
        getInterface(),
        getProductConfig().withPackageName(packageName),
        getModelTypeTable().cloneEmpty(packageName),
        getNamer().cloneWithPackageName(packageName),
        getFeatureConfig());
  }

  public GapicInterfaceConfig getInterfaceConfig() {
    return getProductConfig().getInterfaceConfig(getInterface());
  }

  /**
   * Returns the GapicMethodConfig object of the given gRPC method.
   *
   * <p>If the method is a gRPC re-route method, returns the GapicMethodConfig of the original
   * method.
   */
  public GapicMethodConfig getMethodConfig(Method method) {
    Interface originalInterface = getInterface();
    if (getGrpcRerouteMap().containsKey(originalInterface)) {
      originalInterface = getGrpcRerouteMap().get(originalInterface);
    }
    GapicInterfaceConfig originalGapicInterfaceConfig =
        getProductConfig().getInterfaceConfig(originalInterface);
    if (originalGapicInterfaceConfig != null) {
      return originalGapicInterfaceConfig.getMethodConfig(method);
    } else {
      throw new IllegalArgumentException(
          "Interface config does not exist for method: " + method.getSimpleName());
    }
  }

  public GapicMethodContext asFlattenedMethodContext(
      Method method, FlatteningConfig flatteningConfig) {
    return GapicMethodContext.create(
        this,
        getInterface(),
        getProductConfig(),
        getModelTypeTable(),
        getNamer(),
        method,
        getMethodConfig(method),
        flatteningConfig,
        getFeatureConfig());
  }

  public GapicMethodContext asRequestMethodContext(Method method) {
    return GapicMethodContext.create(
        this,
        getInterface(),
        getProductConfig(),
        getModelTypeTable(),
        getNamer(),
        method,
        getMethodConfig(method),
        null,
        getFeatureConfig());
  }

  public GapicMethodContext asDynamicMethodContext(Method method) {
    return GapicMethodContext.create(
        this,
        getInterface(),
        getProductConfig(),
        getModelTypeTable(),
        getNamer(),
        method,
        getMethodConfig(method),
        null,
        getFeatureConfig());
  }

  /** Returns a list of supported methods, configured by FeatureConfig. */
  public List<Method> getSupportedMethods() {
    List<Method> methods = new ArrayList<>(getInterfaceConfig().getMethodConfigs().size());
    for (GapicMethodConfig methodConfig : getInterfaceConfig().getMethodConfigs()) {
      Method method = methodConfig.getMethod();
      if (isSupported(method)) {
        methods.add(method);
      }
    }
    return methods;
  }

  /**
   * Returns a list of methods with samples, similar to getSupportedMethods, but also filter out
   * private methods.
   */
  public List<Method> getPublicMethods() {
    List<Method> methods = new ArrayList<>(getInterfaceConfig().getMethodConfigs().size());
    for (GapicMethodConfig methodConfig : getInterfaceConfig().getMethodConfigs()) {
      Method method = methodConfig.getMethod();
      VisibilityConfig visibility = getInterfaceConfig().getMethodConfig(method).getVisibility();
      if (isSupported(method) && visibility == VisibilityConfig.PUBLIC) {
        methods.add(method);
      }
    }
    return methods;
  }

  private boolean isSupported(Method method) {
    boolean supported = true;
    supported &=
        getFeatureConfig().enableGrpcStreaming()
            || !GapicMethodConfig.isGrpcStreamingMethod(method);
    supported &=
        getInterfaceConfig().getMethodConfig(method).getVisibility() != VisibilityConfig.DISABLED;
    return supported;
  }

  public List<Method> getPageStreamingMethods() {
    List<Method> methods = new ArrayList<>();
    for (Method method : getSupportedMethods()) {
      if (getMethodConfig(method).isPageStreaming()) {
        methods.add(method);
      }
    }
    return methods;
  }

  public List<Method> getBatchingMethods() {
    List<Method> methods = new ArrayList<>();
    for (Method method : getSupportedMethods()) {
      if (getMethodConfig(method).isBatching()) {
        methods.add(method);
      }
    }
    return methods;
  }

  public Iterable<Method> getLongRunningMethods() {
    List<Method> methods = new ArrayList<>();
    for (Method method : getSupportedMethods()) {
      if (getMethodConfig(method).isLongRunningOperation()) {
        methods.add(method);
      }
    }
    return methods;
  }

  public Iterable<Method> getGrpcStreamingMethods() {
    List<Method> methods = new ArrayList<>();
    for (Method method : getSupportedMethods()) {
      if (getMethodConfig(method).isGrpcStreaming()) {
        methods.add(method);
      }
    }
    return methods;
  }
}
