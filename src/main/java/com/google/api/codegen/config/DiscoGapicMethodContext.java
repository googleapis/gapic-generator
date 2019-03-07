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
package com.google.api.codegen.config;

import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.auto.value.AutoValue;

/** The context for transforming a method to a view model object. */
@AutoValue
public abstract class DiscoGapicMethodContext implements MethodContext {
  public static DiscoGapicMethodContext create(
      DiscoGapicInterfaceContext surfaceTransformerContext,
      String interfaceName,
      GapicProductConfig productConfig,
      SchemaTypeTable typeTable,
      SurfaceNamer namer,
      DiscoveryMethodModel method,
      DiscoGapicMethodConfig methodConfig,
      FlatteningConfig flatteningConfig,
      LongRunningConfig longRunningConfig,
      FeatureConfig featureConfig) {
    return new AutoValue_DiscoGapicMethodContext(
        productConfig,
        flatteningConfig,
        longRunningConfig,
        featureConfig,
        new DiscoInterfaceModel(interfaceName, surfaceTransformerContext.getApiModel()),
        methodConfig,
        surfaceTransformerContext,
        typeTable,
        method,
        namer);
  }

  public String interfaceName() {
    return getInterfaceModel().getFullName();
  }

  @Override
  public abstract InterfaceModel getInterfaceModel();

  @Override
  public abstract DiscoGapicMethodConfig getMethodConfig();

  @Override
  public boolean isFlattenedMethodContext() {
    return getFlatteningConfig() != null;
  }

  @Override
  public InterfaceConfig getInterfaceConfig() {
    return getProductConfig().getInterfaceConfig(interfaceName());
  }

  @Override
  public SingleResourceNameConfig getSingleResourceNameConfig(String entityName) {
    return getProductConfig().getSingleResourceNameConfig(entityName);
  }

  @Override
  public DiscoGapicMethodContext cloneWithEmptyTypeTable() {
    return create(
        getSurfaceInterfaceContext(),
        interfaceName(),
        getProductConfig(),
        getTypeTable().cloneEmpty(),
        getNamer(),
        getMethodModel(),
        getMethodConfig(),
        getFlatteningConfig(),
        getLongRunningConfig(),
        getFeatureConfig());
  }

  @Override
  public abstract DiscoGapicInterfaceContext getSurfaceInterfaceContext();

  @Override
  public abstract SchemaTypeTable getTypeTable();

  @Override
  public abstract DiscoveryMethodModel getMethodModel();

  @Override
  public abstract SurfaceNamer getNamer();

  @Override
  public String getGrpcContainerTypeName() {
    return "";
  }

  @Override
  public DiscoInterfaceModel getTargetInterface() {
    return new DiscoInterfaceModel(
        getInterfaceModel().getFullName(), getSurfaceInterfaceContext().getApiModel());
  }

  @Override
  public MethodContext withResourceNamesInSamplesOnly() {
    return create(
        getSurfaceInterfaceContext(),
        interfaceName(),
        getProductConfig(),
        getTypeTable(),
        getNamer(),
        getMethodModel(),
        getMethodConfig(),
        getFlatteningConfig() == null
            ? null
            : getFlatteningConfig().withResourceNamesInSamplesOnly(),
        getLongRunningConfig(),
        getFeatureConfig());
  }

  @Override
  public boolean isLongRunningMethodContext() {
    return getLongRunningConfig() != null;
  }
}
