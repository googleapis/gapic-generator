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

import com.google.api.codegen.config.FieldConfig;

public class DefaultFeatureConfig implements FeatureConfig {

  @Override
  public boolean resourceNameTypesEnabled() {
    return false;
  }

  @Override
  public boolean resourceNameProtoAccessorsEnabled() {
    return false;
  }

  @Override
  public boolean useResourceNameFormatOption(FieldConfig fieldConfig) {
    return resourceNameTypesEnabled() && fieldConfig != null && fieldConfig.useResourceNameType();
  }

  @Override
  public boolean useResourceNameProtoAccessor(FieldConfig fieldConfig) {
    return resourceNameProtoAccessorsEnabled() && useResourceNameFormatOption(fieldConfig);
  }

  @Override
  public boolean useResourceNameConverters(FieldConfig fieldConfig) {
    return !resourceNameProtoAccessorsEnabled() && useResourceNameFormatOption(fieldConfig);
  }

  @Override
  public boolean enableMixins() {
    return false;
  }

  @Override
  public boolean enableGrpcStreaming() {
    return true;
  }

  @Override
  public boolean enableStringFormatFunctions() {
    return true;
  }

  @Override
  public boolean enableRawOperationCallSettings() {
    return false;
  }
}
