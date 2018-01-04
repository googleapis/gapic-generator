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

import static com.google.api.codegen.config.ApiSource.DISCOVERY;

import com.google.api.codegen.config.ApiSource;
import com.google.api.codegen.config.DiscoGapicMethodConfig;
import com.google.api.codegen.config.DiscoInterfaceModel;
import com.google.api.codegen.config.DiscoveryMethodModel;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/** The context for transforming a method to a view model object. */
@AutoValue
public abstract class DiscoGapicMethodContext implements MethodContext {
  public static DiscoGapicMethodContext create(
      DiscoGapicInterfaceContext surfaceTransformerContext,
      String interfaceName,
      GapicProductConfig productConfig,
      SchemaTypeTable typeTable,
      DiscoGapicNamer discoNamer,
      DiscoveryMethodModel method,
      DiscoGapicMethodConfig methodConfig,
      FlatteningConfig flatteningConfig,
      FeatureConfig featureConfig) {
    Preconditions.checkArgument(method != null && method.getApiSource().equals(DISCOVERY));
    return new AutoValue_DiscoGapicMethodContext(
        productConfig,
        flatteningConfig,
        featureConfig,
        new DiscoInterfaceModel(interfaceName, surfaceTransformerContext.getDocument()),
        methodConfig,
        surfaceTransformerContext,
        typeTable,
        method,
        discoNamer);
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
        getDiscoGapicNamer(),
        getMethodModel(),
        getMethodConfig(),
        getFlatteningConfig(),
        getFeatureConfig());
  }

  @Override
  public abstract DiscoGapicInterfaceContext getSurfaceInterfaceContext();

  @Override
  public abstract SchemaTypeTable getTypeTable();

  @Override
  public abstract DiscoveryMethodModel getMethodModel();

  @Override
  public ApiSource getApiSource() {
    return DISCOVERY;
  }

  @Override
  public SurfaceNamer getNamer() {
    return getDiscoGapicNamer().getLanguageNamer();
  }

  public abstract DiscoGapicNamer getDiscoGapicNamer();

  @Override
  public String getGrpcContainerTypeName() {
    return "";
  }

  @Override
  public ImmutableMap<String, FieldConfig> getDefaultResourceNameConfigMap() {
    return ImmutableMap.of();
  }

  @Override
  public DiscoInterfaceModel getTargetInterface() {
    return new DiscoInterfaceModel(
        getInterfaceModel().getFullName(), getSurfaceInterfaceContext().getDocument());
  }
}
