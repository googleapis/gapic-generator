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
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import javax.annotation.Nullable;

@AutoValue
public abstract class FieldConfig {
  public abstract Field getField();

  public abstract ResourceNameTreatment getResourceNameTreatment();

  @Nullable
  public abstract String getEntityName();

  @Nullable
  public static FieldConfig createFieldConfig(
      Field field, ResourceNameTreatment resourceNameTreatment, String resourceName) {
    if (resourceNameTreatment != ResourceNameTreatment.NONE && resourceName == null) {
      throw new IllegalArgumentException(
          "resourceName may only be null if resourceNameTreatment is NONE");
    }
    return new AutoValue_FieldConfig(field, resourceNameTreatment, resourceName);
  }

  public static FieldConfig createMessageFieldConfig(Field field) {
    return FieldConfig.createFieldConfig(field, ResourceNameTreatment.NONE, null);
  }

  @Nullable
  public static FieldConfig createFlattenedFieldConfig(
      DiagCollector diagCollector,
      MethodConfigProto methodConfigProto,
      FlatteningGroupProto flatteningGroup,
      Method method,
      String parameter) {
    boolean failed = false;

    Field parameterField = method.getInputMessage().lookupField(parameter);
    ResourceNameTreatment treatment =
        flatteningGroup.getParameterResourceNameTreatment().get(parameter);
    String entityName = methodConfigProto.getFieldNamePatterns().get(parameter);

    if (parameterField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Field missing for flattening: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getInputMessage().getFullName(),
              parameter));
      failed = true;
    }

    if (treatment == null) {
      if (entityName == null) {
        treatment = ResourceNameTreatment.NONE;
      } else {
        treatment = methodConfigProto.getResourceNameTreatment();
      }
    } else if (entityName == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Parameter %s has resource name treatment %s, but no field name pattern.",
              parameter,
              treatment));
      failed = true;
    }

    if (failed) {
      return null;
    }
    return createFieldConfig(parameterField, treatment, entityName);
  }

  public boolean hasResourceName() {
    return getEntityName() != null;
  }

  public boolean useResourceNameType() {
    return getResourceNameTreatment() == ResourceNameTreatment.STATIC_TYPES;
  }

  public boolean useValidation() {
    return getResourceNameTreatment() == ResourceNameTreatment.VALIDATE;
  }

  public static Function<FieldConfig, Field> selectFieldFunction() {
    return new Function<FieldConfig, Field>() {
      @Override
      public Field apply(FieldConfig fieldConfig) {
        return fieldConfig.getField();
      }
    };
  }

  public static Function<FieldConfig, String> selectFieldLongNameFunction() {
    return new Function<FieldConfig, String>() {
      @Override
      public String apply(FieldConfig fieldConfig) {
        return fieldConfig.getField().getFullName();
      }
    };
  }

  public static Iterable<Field> transformToFields(Iterable<FieldConfig> fieldConfigs) {
    return Iterables.transform(fieldConfigs, selectFieldFunction());
  }

  public static ImmutableMap<String, FieldConfig> transformToMap(
      Iterable<FieldConfig> fieldConfigs) {
    return Maps.uniqueIndex(fieldConfigs, selectFieldLongNameFunction());
  }
}
