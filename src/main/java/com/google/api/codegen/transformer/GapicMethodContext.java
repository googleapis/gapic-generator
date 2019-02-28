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

import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoInterfaceModel;
import com.google.api.codegen.config.ProtoMethodModel;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.auto.value.AutoValue;

/** The context for transforming a method to a view model object. */
@AutoValue
public abstract class GapicMethodContext implements MethodContext {

  public static GapicMethodContext create(
      GapicInterfaceContext surfaceTransformerContext,
      Interface apiInterface,
      GapicProductConfig productConfig,
      ModelTypeTable typeTable,
      SurfaceNamer namer,
      GapicMethodConfig methodConfig,
      FlatteningConfig flatteningConfig,
      FeatureConfig featureConfig) {
    return new AutoValue_GapicMethodContext(
        productConfig,
        namer,
        flatteningConfig,
        methodConfig.getLroConfig(),
        featureConfig,
        methodConfig,
        surfaceTransformerContext,
        typeTable,
        new ProtoInterfaceModel(apiInterface));
  }

  @Override
  public MethodModel getMethodModel() {
    return getMethodConfig().getMethodModel();
  }

  /** The Method for which this object is a transformation context. */
  public Method getMethod() {
    return ((ProtoMethodModel) getMethodModel()).getProtoMethod();
  }

  public Interface getInterface() {
    return getInterfaceModel().getInterface();
  }

  @Override
  public abstract GapicMethodConfig getMethodConfig();

  @Override
  public abstract GapicInterfaceContext getSurfaceInterfaceContext();

  @Override
  public abstract ModelTypeTable getTypeTable();

  @Override
  public abstract ProtoInterfaceModel getInterfaceModel();

  @Override
  public boolean isFlattenedMethodContext() {
    return getFlatteningConfig() != null;
  }

  @Override
  public ProtoInterfaceModel getTargetInterface() {
    return new ProtoInterfaceModel(
        GapicInterfaceConfig.getTargetInterface(
            getInterface(), getMethodConfig().getRerouteToGrpcInterface()));
  }

  @Override
  public GapicInterfaceConfig getInterfaceConfig() {
    return getProductConfig().getInterfaceConfig(getInterfaceModel().getInterface());
  }

  @Override
  public SingleResourceNameConfig getSingleResourceNameConfig(String entityName) {
    return getProductConfig().getSingleResourceNameConfig(entityName);
  }

  @Override
  public GapicMethodContext cloneWithEmptyTypeTable() {
    return create(
        getSurfaceInterfaceContext(),
        getInterfaceModel().getInterface(),
        getProductConfig(),
        getTypeTable().cloneEmpty(),
        getNamer(),
        getMethodConfig(),
        getFlatteningConfig(),
        getFeatureConfig());
  }

  @Override
  public String getGrpcContainerTypeName() {
    return getNamer().getGrpcContainerTypeName(getTargetInterface());
  }

  @Override
  public MethodContext withResourceNamesInSamplesOnly() {
    return create(
        getSurfaceInterfaceContext(),
        getInterfaceModel().getInterface(),
        getProductConfig(),
        getTypeTable(),
        getNamer(),
        getMethodConfig(),
        getFlatteningConfig() == null
            ? null
            : getFlatteningConfig().withResourceNamesInSamplesOnly(),
        getFeatureConfig());
  }

  @Override
  public boolean isLongRunningMethodContext() {
    return getLongRunningConfig() != null;
  }
}
