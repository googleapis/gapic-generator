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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.ResourceNameMessageConfigs;
import com.google.api.codegen.transformer.DefaultFeatureConfig;
import com.google.api.codegen.transformer.MethodContext;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class JavaFeatureConfig extends DefaultFeatureConfig {

  @Override
  public abstract boolean enableStringFormatFunctions();

  @Override
  public boolean resourceNameTypesEnabled() {
    return true;
  }

  @Override
  public boolean useResourceNameFormatOptionInSample(
      MethodContext context, FieldConfig fieldConfig) {
    return resourceNameTypesEnabled()
        && fieldConfig != null
        && (fieldConfig.useResourceNameType() || fieldConfig.useResourceNameTypeInSampleOnly())
        && !(context.isFlattenedMethodContext() && fieldConfig.getField().isRepeated());
  }

  @Override
  public boolean useResourceNameConvertersInSampleOnly(
      MethodContext context, FieldConfig fieldConfig) {
    return !resourceNameProtoAccessorsEnabled()
        && useResourceNameFormatOptionInSampleOnly(fieldConfig)
        && !(context.isFlattenedMethodContext() && fieldConfig.getField().isRepeated());
  }

  @Override
  public boolean useInheritanceForOneofs() {
    return true;
  }

  @Override
  public boolean enableMixins() {
    return true;
  }

  @Override
  public boolean enableRawOperationCallSettings() {
    return true;
  }

  public static Builder newBuilder() {
    return new AutoValue_JavaFeatureConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder enableStringFormatFunctions(boolean value);

    public abstract JavaFeatureConfig build();
  }

  public static JavaFeatureConfig create(ResourceNameMessageConfigs resourceNameMessageConfigs) {
    return JavaFeatureConfig.newBuilder()
        .enableStringFormatFunctions(
            resourceNameMessageConfigs == null || resourceNameMessageConfigs.isEmpty())
        .build();
  }
}
