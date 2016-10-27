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
import java.util.Map;
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
      ResourceNameMessageConfigs messageConfigs,
      MethodConfigProto methodConfigProto,
      Method method,
      FlatteningGroupProto flatteningGroup,
      String parameterName) {

    Field parameterField = method.getInputMessage().lookupField(parameterName);
    if (parameterField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Field missing for flattening: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getInputMessage().getFullName(),
              parameterName));
      return null;
    }

    String entityName =
        getEntityName(parameterField, messageConfigs, methodConfigProto.getFieldNamePatterns());

    ResourceNameTreatment treatment;
    if (flatteningGroup.getParameterResourceNameTreatment().containsKey(parameterName)) {
      treatment = flatteningGroup.getParameterResourceNameTreatment().get(parameterName);
    } else {
      // No specific resource name treatment is specified, so we infer the correct treatment from
      // the method-level default and the specified entities.
      if (entityName == null) {
        treatment = ResourceNameTreatment.NONE;
      } else {
        treatment = methodConfigProto.getResourceNameTreatment();
        if (treatment == null) {
          treatment = ResourceNameTreatment.NONE;
        }
      }
    }

    if (treatment == ResourceNameTreatment.NONE) {
      entityName = null;
    }

    validate(messageConfigs, parameterField, treatment, entityName);

    return createFieldConfig(parameterField, treatment, entityName);
  }

  public static String getEntityName(
      Field field,
      ResourceNameMessageConfigs messageConfigs,
      Map<String, String> fieldNamePatterns) {
    String entityName = null;
    if (fieldNamePatterns != null) {
      entityName = fieldNamePatterns.get(field.getSimpleName());
    }
    if (messageConfigs != null && messageConfigs.fieldHasResourceName(field)) {
      String resourceEntityName = messageConfigs.getFieldResourceName(field);
      if (entityName == null) {
        entityName = resourceEntityName;
      } else if (!entityName.equals(resourceEntityName)) {
        throw new IllegalArgumentException(
            "Multiple entity names specified for field: "
                + field.getFullName()
                + ": ["
                + entityName
                + ", "
                + resourceEntityName
                + "]");
      }
    }
    return entityName;
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

  /*
   * Check that the provided resource name treatment and entityName are valid for the provided field.
   */
  public static void validate(
      ResourceNameMessageConfigs messageConfigs,
      Field field,
      ResourceNameTreatment treatment,
      String entityName) {
    switch (treatment) {
      case NONE:
        break;
      case STATIC_TYPES:
        if (messageConfigs == null || !messageConfigs.fieldHasResourceName(field)) {
          throw new IllegalArgumentException(
              "Field must have a resource type specified to support "
                  + "STATIC_TYPES resource name treatment. Field: "
                  + field.getFullName());
        }
        break;
      case VALIDATE:
        if (entityName == null) {
          throw new IllegalArgumentException(
              "Field must have a resource type or field name pattern specified to support "
                  + "VALIDATE resource name treatment. Field: "
                  + field.getFullName());
        }
        break;
      case UNRECOGNIZED:
      default:
        throw new IllegalArgumentException("Unrecognized resource name type: " + treatment);
    }
  }

  private static Function<FieldConfig, Field> selectFieldFunction() {
    return new Function<FieldConfig, Field>() {
      @Override
      public Field apply(FieldConfig fieldConfig) {
        return fieldConfig.getField();
      }
    };
  }

  private static Function<FieldConfig, String> selectFieldLongNameFunction() {
    return new Function<FieldConfig, String>() {
      @Override
      public String apply(FieldConfig fieldConfig) {
        return fieldConfig.getField().getFullName();
      }
    };
  }

  public static Iterable<Field> toFieldIterable(Iterable<FieldConfig> fieldConfigs) {
    return Iterables.transform(fieldConfigs, selectFieldFunction());
  }

  public static ImmutableMap<String, FieldConfig> toFieldConfigMap(
      Iterable<FieldConfig> fieldConfigs) {
    return Maps.uniqueIndex(fieldConfigs, selectFieldLongNameFunction());
  }
}
