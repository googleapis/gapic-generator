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

import com.google.api.codegen.config.FieldConfig;

public class FeatureConfig {

  /** Returns true if generated types are supported for resource name fields. */
  public boolean resourceNameTypesEnabled() {
    return false;
  }

  /**
   * Returns true if resourceNameTypesEnabled() is true, and the field config provided has a
   * resource name format option, and is configured to use it.
   */
  public boolean useResourceNameFormatOption(FieldConfig fieldConfig) {
    return resourceNameTypesEnabled() && fieldConfig != null && fieldConfig.useResourceNameType();
  }

  /** Returns true if mixin APIs are supported. */
  public boolean enableMixins() {
    return false;
  }

  /** Returns true if streaming APIs are supported. */
  public boolean enableGrpcStreaming() {
    return false;
  }

  /** Returns true if string format functions are supported. */
  public boolean enableStringFormatFunctions() {
    return true;
  }
}
