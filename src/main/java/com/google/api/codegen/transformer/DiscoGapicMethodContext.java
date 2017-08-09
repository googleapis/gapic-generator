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

import static com.google.api.codegen.config.ApiSource.DISCOVERY;

import com.google.api.codegen.config.ApiSource;
import com.google.api.codegen.config.DiscoGapicMethodConfig;
import com.google.api.codegen.config.DiscoveryMethodModel;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

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
        interfaceName,
        methodConfig,
        surfaceTransformerContext,
        typeTable,
        method,
        discoNamer);
  }

  public abstract String interfaceName();

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
        (SchemaTypeTable) getTypeTable().cloneEmpty(),
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
  public String getInterfaceSimpleName() {
    return getDiscoGapicNamer().getSimpleInterfaceName(interfaceName());
  }

  @Override
  public SurfaceNamer getNamer() {
    return getDiscoGapicNamer().getLanguageNamer();
  }

  public abstract DiscoGapicNamer getDiscoGapicNamer();

  @Override
  public String getTargetInterfaceFullName() {
    return "TargetInterfaceFullName() not yet implemented.";
  }

  @Override
  public String getGrpcContainerTypeName() {
    return "getGrpcContainerTypeName() not implemented for Discovery-based APIs.";
  }

  @Override
  public String getInterfaceFileName() {
    return getTargetInterfaceFullName();
  }
}
