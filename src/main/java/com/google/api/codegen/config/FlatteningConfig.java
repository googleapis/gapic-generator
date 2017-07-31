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
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
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
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      MethodConfigProto methodConfigProto,
      FlatteningGroupProto flatteningGroup,
      Method method) {

    boolean missing = false;
    ImmutableMap.Builder<String, FieldConfig> flattenedFieldConfigBuilder = ImmutableMap.builder();
    for (String parameter : flatteningGroup.getParametersList()) {

      Field parameterField = method.getInputMessage().lookupField(parameter);
      if (parameterField == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Field missing for flattening: method = %s, message type = %s, field = %s",
                method.getFullName(),
                method.getInputMessage().getFullName(),
                parameter));
        return null;
      }

      ResourceNameTreatment defaultResourceNameTreatment =
          methodConfigProto.getResourceNameTreatment();
      if (defaultResourceNameTreatment == null
          || defaultResourceNameTreatment.equals(ResourceNameTreatment.UNSET_TREATMENT)) {
        defaultResourceNameTreatment = ResourceNameTreatment.VALIDATE;
      }

      FieldConfig fieldConfig =
          FieldConfig.createFieldConfig(
              diagCollector,
              messageConfigs,
              methodConfigProto.getFieldNamePatterns(),
              resourceNameConfigs,
              new ProtoField(parameterField),
              flatteningGroup.getParameterResourceNameTreatment().get(parameter),
              defaultResourceNameTreatment);
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

  public Iterable<FieldType> getFlattenedFields() {
    return FieldConfig.toFieldTypeIterable(getFlattenedFieldConfigs().values());
  }
}
