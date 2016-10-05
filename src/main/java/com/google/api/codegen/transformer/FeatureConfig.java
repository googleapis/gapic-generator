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

import com.google.api.codegen.util.ResourceNameUtil;
import com.google.api.tools.framework.model.Field;

public class FeatureConfig {

  /** Returns true if generated types are supported for resource name fields. */
  public boolean resourceNameTypesEnabled() {
    return false;
  }

  /**
   * Returns true if resourceNameTypesEnabled() is true, and the field provided has a resource name
   * format option.
   */
  public boolean useResourceNameFormatOption(Field field) {
    return resourceNameTypesEnabled() && ResourceNameUtil.hasResourceName(field);
  }

  /** Returns true if mixin APIs are supported. */
  public boolean enableMixins() {
    return false;
  }

  /** Returns true if streaming APIs are supported. */
  public boolean enableGrpcStreaming() {
    return false;
  }
}
