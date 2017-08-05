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

import static com.google.api.codegen.config.FieldType.ApiSource.DISCOVERY;

import com.google.api.codegen.config.DiscoGapicMethodConfig;
import com.google.api.codegen.config.DiscoveryMethodModel;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldType.ApiSource;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.VisibilityConfig;
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
      DiscoGapicNamer namer,
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
        namer);
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
        getNamer(),
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
  public String getStubName() {
    return getNamer().getStubName(getInterfaceConfig());
  }

  @Override
  public String getInterfaceSimpleName() {
    return interfaceName();
  }

  @Override
  public abstract DiscoGapicNamer getNamer();

  @Override
  public String getPageStreamingDescriptorConstName() {
    return getNamer().getPageStreamingDescriptorConstName(getMethodModel());
  }

  @Override
  public String getAndSavePagedResponseTypeName(FieldConfig fieldConfig) {
    return getNamer().getAndSavePagedResponseTypeName(this, fieldConfig);
  }

  @Override
  public String getPagedListResponseFactoryConstName() {
    return getNamer().getPagedListResponseFactoryConstName(getMethodModel());
  }

  @Override
  public String getApiMethodName(VisibilityConfig visibilityConfig) {
    return getNamer().getApiMethodName(getMethodModel(), visibilityConfig);
  }

  @Override
  public String getTargetInterfaceFullName() {
    return "TargetInterfaceFullName() not yet implemented.";
  }

  @Override
  public String getAsyncApiMethodName(VisibilityConfig visibilityConfig) {
    return getNamer().getAsyncApiMethodName(getMethodModel(), visibilityConfig);
  }

  @Override
  public String getGrpcContainerTypeName() {
    return "getGrpcContainerTypeName() not implemented for Discovery-based APIs.";
  }

  @Override
  public String getGrpcMethodConstant() {
    return "getGrpcMethodConstant() not implemented for Discovery-based APIs.";
  }

  @Override
  public String getSettingsMemberName() {
    return getNamer().getSettingsMemberName(getMethodModel());
  }

  @Override
  public String getSettingsFunctionName() {
    return getNamer().getSettingsFunctionName(getMethodModel());
  }

  @Override
  public String getCallableName() {
    return getNamer().getCallableName(getMethodModel());
  }

  @Override
  public String getPagedCallableName() {
    return getNamer().getPagedCallableName(getMethodModel());
  }

  @Override
  public String getInterfaceFileName() {
    return getTargetInterfaceFullName();
  }
}
