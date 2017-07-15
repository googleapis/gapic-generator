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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.DiscoGapicMethodConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.util.TypeTable;
import com.google.api.tools.framework.model.Interface;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * The context for transforming a Discovery Doc API into a view model to use for client library
 * generation.
 */
@AutoValue
public abstract class DiscoGapicInterfaceContext implements InterfaceContext {
  public static DiscoGapicInterfaceContext createWithoutInterface(
      Document document,
      GapicProductConfig productConfig,
      SchemaTypeTable typeTable,
      DiscoGapicNamer discoGapicNamer,
      SurfaceNamer surfaceNamer,
      FeatureConfig featureConfig) {
    return new AutoValue_DiscoGapicInterfaceContext(
        document,
        productConfig,
        typeTable,
        discoGapicNamer,
        (new ImmutableList.Builder<Method>()).build(),
        "",
        surfaceNamer,
        featureConfig);
  }

  public static DiscoGapicInterfaceContext create(
      Document document,
      String interfaceName,
      GapicProductConfig productConfig,
      SchemaTypeTable typeTable,
      DiscoGapicNamer discoGapicNamer,
      SurfaceNamer surfaceNamer,
      FeatureConfig featureConfig) {
    ImmutableList.Builder<Method> interfaceMethods = new ImmutableList.Builder<>();

    for (MethodConfig method : productConfig.getInterfaceConfig(interfaceName).getMethodConfigs()) {
      interfaceMethods.add(((DiscoGapicMethodConfig) method).getMethod());
    }

    return new AutoValue_DiscoGapicInterfaceContext(
        document,
        productConfig,
        typeTable,
        discoGapicNamer,
        interfaceMethods.build(),
        interfaceName,
        surfaceNamer,
        featureConfig);
  }

  public abstract Document getDocument();

  @Override
  public abstract GapicProductConfig getProductConfig();

  public abstract SchemaTypeTable getSchemaTypeTable();

  public abstract DiscoGapicNamer getDiscoGapicNamer();

  public List<Method> getMethods() {
    List<Method> methods = new LinkedList<>();
    for (InterfaceConfig config : getProductConfig().getInterfaceConfigMap().values()) {
      for (MethodConfig methodConfig : config.getMethodConfigs()) {
        methods.add(((DiscoGapicMethodConfig) methodConfig).getMethod());
      }
    }
    return methods;
  }

  @Nullable
  @Override
  public Interface getInterface() {
    return null;
  }

  public abstract List<Method> getInterfaceMethods();

  public abstract String getInterfaceName();

  @Override
  public abstract SurfaceNamer getNamer();

  @Override
  public TypeTable getTypeTable() {
    return getSchemaTypeTable().getTypeTable();
  }

  public abstract FeatureConfig getFeatureConfig();

  /** Returns a list of supported methods, configured by FeatureConfig. */
  public List<Method> getSupportedMethods() {
    List<Method> allMethods = getMethods();
    List<Method> methods = new ArrayList<>();

    for (Method method : allMethods) {
      if (isSupported(method)) {
        methods.add(method);
      }
    }
    return methods;
  }

  private boolean isSupported(Method method) {
    return getInterfaceConfig().getMethodConfig(method).getVisibility()
        != VisibilityConfig.DISABLED;
  }

  /** Returns the GapicMethodConfig for the given method. */
  public DiscoGapicMethodConfig getMethodConfig(Method method) {
    for (InterfaceConfig config : getProductConfig().getInterfaceConfigMap().values()) {
      for (MethodConfig methodConfig : config.getMethodConfigs()) {
        DiscoGapicMethodConfig discoMethodConfig = (DiscoGapicMethodConfig) methodConfig;
        if (discoMethodConfig.getMethod().equals(method)) {
          return discoMethodConfig;
        }
      }
    }

    throw new IllegalArgumentException(
        "Interface config does not exist for method: " + method.id());
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

  public DiscoGapicMethodContext asRequestMethodContext(Method method) {
    return DiscoGapicMethodContext.create(
        this,
        getInterface(),
        getProductConfig(),
        getSchemaTypeTable(),
        getNamer(),
        method,
        getMethodConfig(method),
        null,
        getFeatureConfig());
  }

  public InterfaceConfig getInterfaceConfig() {
    return getProductConfig().getInterfaceConfig(getInterfaceName());
  }

  @Nullable
  public ModelTypeTable getModelTypeTable() {
    return null;
  }
}
