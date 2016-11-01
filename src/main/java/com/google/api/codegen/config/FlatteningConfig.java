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
package com.google.api.codegen.config;

import com.google.api.codegen.FlatteningGroupProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;

/** FlatteningConfig represents a specific flattening configuration for a method. */
@AutoValue
public abstract class FlatteningConfig {
  public abstract ImmutableMap<String, FieldConfig> getFlattenedFieldConfigs();

  @Nullable
  public abstract String getFlatteningName();
  /**
   * Creates an instance of FlatteningConfig based on a FlatteningGroupProto, linking it up with the
   * provided method.
   */
  @Nullable
  public static FlatteningConfig createFlattening(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      MethodConfigProto methodConfigProto,
      FlatteningGroupProto flatteningGroup,
      Method method) {

    boolean missing = false;
    ImmutableMap.Builder<String, FieldConfig> flattenedFieldConfigBuilder = ImmutableMap.builder();
    for (String parameter : flatteningGroup.getParametersList()) {
      FieldConfig fieldConfig =
          FieldConfig.createFlattenedFieldConfig(
              diagCollector, messageConfigs, methodConfigProto, method, flatteningGroup, parameter);
      if (fieldConfig == null) {
        missing = true;
      } else {
        flattenedFieldConfigBuilder.put(parameter, fieldConfig);
      }
    }
    if (missing) {
      return null;
    }

    return new AutoValue_FlatteningConfig(
        flattenedFieldConfigBuilder.build(), flatteningGroup.getFlatteningGroupName());
  }

  public FieldConfig getFieldConfig(String fieldSimpleName) {
    return getFlattenedFieldConfigs().get(fieldSimpleName);
  }

  public Iterable<Field> getFlattenedFields() {
    return FieldConfig.toFieldIterable(getFlattenedFieldConfigs().values());
  }

  public Iterable<String> getParameterList() {
    return getFlattenedFieldConfigs().keySet();
  }
}
