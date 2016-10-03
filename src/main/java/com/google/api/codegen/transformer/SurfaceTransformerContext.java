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

import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.CollectionConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** The context for transforming a model into a view model for a surface. */
@AutoValue
public abstract class SurfaceTransformerContext {
  public static SurfaceTransformerContext create(
      Interface interfaze,
      ApiConfig apiConfig,
      ModelTypeTable typeTable,
      SurfaceNamer namer,
      FeatureConfig featureConfig) {
    return new AutoValue_SurfaceTransformerContext(
        interfaze, apiConfig, typeTable, namer, featureConfig);
  }

  public Model getModel() {
    return getInterface().getModel();
  }

  public abstract Interface getInterface();

  public abstract ApiConfig getApiConfig();

  public abstract ModelTypeTable getTypeTable();

  public abstract SurfaceNamer getNamer();

  public abstract FeatureConfig getFeatureConfig();

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

  public MethodConfig getMethodConfig(Method method) {
    if (getInterfaceConfig() != null) {
      return getInterfaceConfig().getMethodConfig(method);
    } else {
      return null;
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

  /**
   * Returns true if the method is supported by the current context.
   * Currently no streaming methods are supported.
   * TODO: integrate with GapicContext for this method.
   */
  private boolean isSupported(Method method) {
    return !method.getResponseStreaming() && !method.getRequestStreaming();
  }

  /**
   * Returns a list of simple RPC methods.
   */
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
