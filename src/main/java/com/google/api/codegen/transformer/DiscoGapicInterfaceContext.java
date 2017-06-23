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

import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.util.TypeTable;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * The context for transforming a Discovery Doc API into a view model to use for client library
 * generation.
 */
@AutoValue
public abstract class DiscoGapicInterfaceContext implements InterfaceContext {
  public static DiscoGapicInterfaceContext create(
      Document document,
      GapicProductConfig productConfig,
      SchemaTypeTable typeTable,
      DiscoGapicNamer discoGapicNamer,
      SurfaceNamer surfaceNamer,
      FeatureConfig featureConfig) {
    return new AutoValue_DiscoGapicInterfaceContext(
        document, productConfig, typeTable, discoGapicNamer, null, surfaceNamer, featureConfig);
  }

  public abstract Document getDocument();

  @Override
  public abstract GapicProductConfig getProductConfig();

  public abstract SchemaTypeTable getSchemaTypeTable();

  public abstract DiscoGapicNamer getDiscoGapicNamer();

  @Nullable
  public abstract Interface getInterface();

  @Override
  public abstract SurfaceNamer getNamer();

  @Override
  public TypeTable getTypeTable() {
    return getSchemaTypeTable().getTypeTable();
  }

  public abstract FeatureConfig getFeatureConfig();

  /** Returns a list of supported methods, configured by FeatureConfig. */
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
    return getInterfaceConfig().getMethodConfig(method).getVisibility()
        != VisibilityConfig.DISABLED;
  }

  /** Returns the GapicMethodConfig for the given method. */
  public MethodConfig getMethodConfig(Method method) {
    Interface originalInterface = getInterface();
    GapicInterfaceConfig originalGapicInterfaceConfig =
        getProductConfig().getInterfaceConfig(originalInterface);
    if (originalGapicInterfaceConfig != null) {
      return originalGapicInterfaceConfig.getMethodConfig(method);
    } else {
      throw new IllegalArgumentException(
          "Interface config does not exist for method: " + method.getSimpleName());
    }
  }

  public DiscoGapicInterfaceContext withNewTypeTable() {
    return create(
        getDocument(),
        getProductConfig(),
        getSchemaTypeTable().cloneEmpty(),
        getDiscoGapicNamer(),
        getNamer(),
        getFeatureConfig());
  }

  public GapicMethodContext asRequestMethodContext(Method method) {
    return GapicMethodContext.create(
        this,
        getInterface(),
        getProductConfig(),
        null,
        getNamer(),
        method,
        getMethodConfig(method),
        null,
        getFeatureConfig());
  }

  public InterfaceConfig getInterfaceConfig() {
    return getProductConfig().getInterfaceConfig(getDocument().name());
  }

  @Nullable
  public ModelTypeTable getModelTypeTable() {
    return null;
  }
}
