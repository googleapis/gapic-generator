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

import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.util.ResourceNameUtil;
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

/** FieldConfig represents a configuration for a Field, derived from the GAPIC config. */
@AutoValue
public abstract class FieldConfig {
  public abstract Field getField();

  public abstract ResourceNameTreatment getResourceNameTreatment();

  @Nullable
  public abstract String getEntityName();

  public static FieldConfig createFieldConfig(
      Field field, ResourceNameTreatment resourceNameTreatment, String entityName) {
    if (resourceNameTreatment != ResourceNameTreatment.NONE && entityName == null) {
      throw new IllegalArgumentException(
          "resourceName may only be null if resourceNameTreatment is NONE");
    }
    return new AutoValue_FieldConfig(field, resourceNameTreatment, entityName);
  }

  /** Creates a FieldConfig for the given Field with ResourceNameTreatment set to None. */
  public static FieldConfig createDefaultFieldConfig(Field field) {
    return FieldConfig.createFieldConfig(field, ResourceNameTreatment.NONE, null);
  }

  @Nullable
  public static FieldConfig createFlattenedFieldConfig(
      DiagCollector diagCollector,
      MethodConfigProto methodConfigProto,
      Method method,
      String parameter) {

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

    ResourceNameTreatment treatment = ResourceNameTreatment.NONE;
    String entityName = methodConfigProto.getFieldNamePatterns().get(parameter);

    if (ResourceNameUtil.hasResourceName(parameterField)) {
      treatment = ResourceNameTreatment.STATIC_TYPES;
      entityName = ResourceNameUtil.getResourceName(parameterField);
    } else if (entityName != null) {
      treatment = ResourceNameTreatment.VALIDATE;
    }

    return createFieldConfig(parameterField, treatment, entityName);
  }

  public boolean hasEntityName() {
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
