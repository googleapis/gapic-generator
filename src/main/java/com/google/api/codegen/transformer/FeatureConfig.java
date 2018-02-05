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

public interface FeatureConfig {

  /** Returns true if generated types are supported for resource name fields for the given API. */
  boolean resourceNameTypesEnabled();

  /** Returns true if the proto-generated types for the given API have resource name accessors. */
  boolean resourceNameProtoAccessorsEnabled();

  /**
   * Returns true if resourceNameTypesEnabled() is true, and the field config provided has a
   * resource name format option, and is configured to use it.
   */
  boolean useResourceNameFormatOption(FieldConfig fieldConfig);

  /**
   * Returns true if useResourceNameFormatOption() and resourceNameProtoAccessorsEnabled() are true.
   */
  boolean useResourceNameProtoAccessor(FieldConfig fieldConfig);

  /**
   * Returns true if useResourceNameFormatOption() is true but resourceNameProtoAccessorsEnabled()
   * is false.
   */
  boolean useResourceNameConverters(FieldConfig fieldConfig);

  boolean useInheritanceForOneofs();

  /** Returns true if mixin APIs are supported. */
  boolean enableMixins();

  /** Returns true if streaming APIs are supported. */
  boolean enableGrpcStreaming();

  /** Returns true if string format functions are supported. */
  boolean enableStringFormatFunctions();

  /** Returns true if a raw operation call settings method should be generated. */
  boolean enableRawOperationCallSettings();
}
