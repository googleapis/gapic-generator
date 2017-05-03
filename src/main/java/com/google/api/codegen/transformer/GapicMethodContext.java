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

import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** The context for transforming a method to a view model object. */
@AutoValue
public abstract class GapicMethodContext {
  public static GapicMethodContext create(
      GapicInterfaceContext surfaceTransformerContext,
      Interface apiInterface,
      GapicProductConfig productConfig,
      ModelTypeTable typeTable,
      SurfaceNamer namer,
      Method method,
      GapicMethodConfig methodConfig,
      FlatteningConfig flatteningConfig,
      FeatureConfig featureConfig) {
    return new AutoValue_GapicMethodContext(
        surfaceTransformerContext,
        apiInterface,
        productConfig,
        typeTable,
        namer,
        method,
        methodConfig,
        flatteningConfig,
        featureConfig);
  }

  public abstract GapicInterfaceContext getSurfaceTransformerContext();

  public abstract Interface getInterface();

  public abstract GapicProductConfig getProductConfig();

  public abstract ModelTypeTable getTypeTable();

  public abstract SurfaceNamer getNamer();

  public abstract Method getMethod();

  public abstract GapicMethodConfig getMethodConfig();

  @Nullable
  public abstract FlatteningConfig getFlatteningConfig();

  public abstract FeatureConfig getFeatureConfig();

  public boolean isFlattenedMethodContext() {
    return getFlatteningConfig() != null;
  }

  public Interface getTargetInterface() {
    return GapicInterfaceConfig.getTargetInterface(
        getInterface(), getMethodConfig().getRerouteToGrpcInterface());
  }

  public GapicInterfaceConfig getInterfaceConfig() {
    return getProductConfig().getInterfaceConfig(getInterface());
  }

  public SingleResourceNameConfig getSingleResourceNameConfig(String entityName) {
    return getProductConfig().getSingleResourceNameConfig(entityName);
  }

  public GapicMethodContext cloneWithEmptyTypeTable() {
    return create(
        getSurfaceTransformerContext(),
        getInterface(),
        getProductConfig(),
        getTypeTable().cloneEmpty(),
        getNamer(),
        getMethod(),
        getMethodConfig(),
        getFlatteningConfig(),
        getFeatureConfig());
  }
}
