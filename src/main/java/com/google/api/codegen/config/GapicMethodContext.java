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
package com.google.api.codegen.config;

import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        new ProtoInterfaceModel(apiInterface),
        Collections.emptyList());
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
    return new AutoValue_GapicMethodContext(
        getProductConfig(),
        getNamer(),
        getFlatteningConfig() == null
            ? null
            : getFlatteningConfig().withResourceNamesInSamplesOnly(),
        getMethodConfig().getLroConfig(),
        getFeatureConfig(),
        getMethodConfig(),
        getSurfaceInterfaceContext(),
        getTypeTable(),
        new ProtoInterfaceModel(getInterfaceModel().getInterface()),
        getCallingForms());
  }

  @Override
  public boolean isLongRunningMethodContext() {
    return getLongRunningConfig() != null;
  }

  @Override
  // TODO(hzyi): This is SampleGen specific. Move everything specific to samples
  // to a separate SampleContext.
  public abstract List<CallingForm> getCallingForms();

  @Override
  // TODO(hzyi): This is SampleGen specific. Move everything specific to samples
  // to a separate SampleContext.
  public MethodContext withCallingForms(List<CallingForm> callingForms) {
    return new AutoValue_GapicMethodContext(
        getProductConfig(),
        getNamer(),
        getFlatteningConfig(),
        getMethodConfig().getLroConfig(),
        getFeatureConfig(),
        getMethodConfig(),
        getSurfaceInterfaceContext(),
        getTypeTable(),
        new ProtoInterfaceModel(getInterfaceModel().getInterface()),
        callingForms);
  }

  public ImmutableMap<String, String> getFieldResourceEntityMap() {
    ImmutableMap.Builder<String, String> fieldResourceEntityMap = ImmutableMap.builder();
    if (isFlattenedMethodContext()) {
      for (Map.Entry<String, FieldConfig> entry :
          getFlatteningConfig().getFlattenedFieldConfigs().entrySet()) {
        FieldConfig fieldConfig = entry.getValue();
        ResourceNameConfig resourceConfig = fieldConfig.getResourceNameConfig();
        if (resourceConfig != null) {
          fieldResourceEntityMap.put(entry.getKey(), resourceConfig.getEntityId());
        }
      }
      return fieldResourceEntityMap.build();
    }

    // For non-flattening methods, resource name configs are only used to generate
    // samples, so we can always use the first resource name config for this purpose.
    //
    // Because YAML does not support many-to-many mapping, for GAPICs generated solely
    // from GAPIC YAML, fieldNamePatterns is in fact a normal map instead of a multimap,
    // therefore taking only the first element from fieldNamePatterns causes no changes
    // to those libraries.
    for (Map.Entry<String, Collection<String>> entry :
        getMethodConfig().getFieldNamePatterns().asMap().entrySet()) {
      fieldResourceEntityMap.put(entry.getKey(), ((List<String>) entry.getValue()).get(0));
    }
    return fieldResourceEntityMap.build();
  }
}
