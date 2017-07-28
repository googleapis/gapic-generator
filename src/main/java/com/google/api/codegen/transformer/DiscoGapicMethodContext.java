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

import com.google.api.codegen.config.DiscoGapicMethodConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Method;
import com.google.api.tools.framework.model.Interface;
import com.google.auto.value.AutoValue;

/** The context for transforming a method to a view model object. */
@AutoValue
public abstract class DiscoGapicMethodContext implements MethodContext {
  public static DiscoGapicMethodContext create(
      InterfaceContext surfaceTransformerContext,
      Interface apiInterface,
      GapicProductConfig productConfig,
      SchemaTypeTable typeTable,
      SurfaceNamer namer,
      Method method,
      DiscoGapicMethodConfig methodConfig,
      FlatteningConfig flatteningConfig,
      FeatureConfig featureConfig) {
    return new AutoValue_DiscoGapicMethodContext(
        surfaceTransformerContext,
        apiInterface,
        productConfig,
        namer,
        flatteningConfig,
        featureConfig,
        method,
        new DiscoGapicNamer(namer),
        methodConfig,
        typeTable);
  }

  /** The Discovery Method for which this object is a transformation context. */
  public abstract Method getMethod();

  public abstract DiscoGapicNamer getDiscoGapicNamer();

  @Override
  public abstract DiscoGapicMethodConfig getMethodConfig();

  @Override
  public boolean isFlattenedMethodContext() {
    return getFlatteningConfig() != null;
  }

  public Interface getTargetInterface() {
    return GapicInterfaceConfig.getTargetInterface(
        getInterface(), getMethodConfig().getRerouteToGrpcInterface());
  }

  @Override
  public InterfaceConfig getInterfaceConfig() {
    return getProductConfig().getInterfaceConfig(getInterface());
  }

  @Override
  public SingleResourceNameConfig getSingleResourceNameConfig(String entityName) {
    return getProductConfig().getSingleResourceNameConfig(entityName);
  }

  @Override
  public DiscoGapicMethodContext cloneWithEmptyTypeTable() {
    return create(
        getSurfaceTransformerContext(),
        getInterface(),
        getProductConfig(),
        (SchemaTypeTable) getTypeTable().cloneEmpty(),
        getNamer(),
        getMethod(),
        getMethodConfig(),
        getFlatteningConfig(),
        getFeatureConfig());
  }

  @Override
  public abstract SchemaTypeTable getTypeTable();
}
