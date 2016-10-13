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
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.CollectionConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The context for transforming a model into a view model for a surface. */
@AutoValue
public abstract class SurfaceTransformerContext {
  public static SurfaceTransformerContext create(
      Interface service,
      ApiConfig apiConfig,
      ModelTypeTable typeTable,
      SurfaceNamer namer,
      FeatureConfig featureConfig) {
    return new AutoValue_SurfaceTransformerContext(
        service,
        apiConfig,
        typeTable,
        namer,
        featureConfig,
        createGrpcRerouteMap(service.getModel(), apiConfig));
  }

  private static Map<Interface, Interface> createGrpcRerouteMap(Model model, ApiConfig apiConfig) {
    HashMap<Interface, Interface> grpcRerouteMap = new HashMap<>();
    for (Interface interfaze : new InterfaceView().getElementIterable(model)) {
      if (!interfaze.isReachable()) {
        continue;
      }
      InterfaceConfig interfaceConfig = apiConfig.getInterfaceConfig(interfaze);
      for (MethodConfig methodConfig : interfaceConfig.getMethodConfigs()) {
        String reroute = methodConfig.getRerouteToGrpcInterface();
        if (!Strings.isNullOrEmpty(reroute)) {
          Interface targetInterface = model.getSymbolTable().lookupInterface(reroute);
          grpcRerouteMap.put(targetInterface, interfaze);
        }
      }
    }
    return grpcRerouteMap;
  }

  public Model getModel() {
    return getInterface().getModel();
  }

  public abstract Interface getInterface();

  public abstract ApiConfig getApiConfig();

  public abstract ModelTypeTable getTypeTable();

  public abstract SurfaceNamer getNamer();

  public abstract FeatureConfig getFeatureConfig();

  /** A map which maps the reroute grpc interface to its original interface */
  public abstract Map<Interface, Interface> getGrpcRerouteMap();

  public SurfaceTransformerContext withNewTypeTable() {
    return create(
        getInterface(),
        getApiConfig(),
        getTypeTable().cloneEmpty(),
        getNamer(),
        getFeatureConfig());
  }

  public InterfaceConfig getInterfaceConfig() {
    return getApiConfig().getInterfaceConfig(getInterface());
  }

  /**
   * Returns the MethodConfig object of the given gRPC method.
   *
   * If the method is a gRPC re-route method, returns the MethodConfig of the original method.
   */
  public MethodConfig getMethodConfig(Method method) {
    Interface originalInterface = getInterface();
    if (getGrpcRerouteMap().containsKey(originalInterface)) {
      originalInterface = getGrpcRerouteMap().get(originalInterface);
    }
    InterfaceConfig originalInterfaceConfig = getApiConfig().getInterfaceConfig(originalInterface);
    if (originalInterfaceConfig != null) {
      return originalInterfaceConfig.getMethodConfig(method);
    } else {
      throw new IllegalArgumentException(
          "Interface config does not exist for method: " + method.getSimpleName());
    }
  }

  public Collection<CollectionConfig> getCollectionConfigs() {
    return getInterfaceConfig().getCollectionConfigs();
  }

  public CollectionConfig getCollectionConfig(String entityName) {
    return getInterfaceConfig().getCollectionConfig(entityName);
  }

  public MethodTransformerContext asMethodContext(Method method) {
    return MethodTransformerContext.create(
        this,
        getInterface(),
        getApiConfig(),
        getTypeTable(),
        getNamer(),
        method,
        getMethodConfig(method),
        getFeatureConfig());
  }

  /** Returns a list of simple RPC methods. */
  public List<Method> getSupportedMethods() {
    List<Method> methods = new ArrayList<>(getInterfaceConfig().getMethodConfigs().size());
    for (MethodConfig methodConfig : getInterfaceConfig().getMethodConfigs()) {
      Method method = methodConfig.getMethod();
      if (isSupported(method)) {
        methods.add(method);
      }
    }
    return methods;
  }

  private boolean isSupported(Method method) {
    if (getFeatureConfig().enableGrpcStreaming()) {
      return true;
    }
    return !MethodConfig.isGrpcStreamingMethod(method);
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
}
