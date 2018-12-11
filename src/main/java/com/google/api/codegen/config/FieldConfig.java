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
package com.google.api.codegen.config;

import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** FieldConfig represents a configuration for a Field, derived from the GAPIC config. */
@AutoValue
public abstract class FieldConfig {
  public abstract FieldModel getField();

  @Nullable
  public abstract ResourceNameTreatment getResourceNameTreatment();

  @Nullable
  public abstract ResourceNameConfig getResourceNameConfig();

  @Nullable
  public abstract ResourceNameConfig getMessageResourceNameConfig();

  public ResourceNameType getResourceNameType() {
    if (getResourceNameConfig() == null) {
      return null;
    }
    return getResourceNameConfig().getResourceNameType();
  }

  private static FieldConfig createFieldConfig(
      FieldModel field,
      ResourceNameTreatment resourceNameTreatment,
      ResourceNameConfig resourceNameConfig,
      ResourceNameConfig messageResourceNameConfig) {
    if (resourceNameTreatment != ResourceNameTreatment.NONE && resourceNameConfig == null) {
      throw new IllegalArgumentException(
          "resourceName may only be null if resourceNameTreatment is NONE");
    }
    if (resourceNameConfig != null
        && resourceNameConfig.getResourceNameType() == ResourceNameType.FIXED) {
      throw new IllegalArgumentException(
          "FieldConfig may not contain a ResourceNameConfig of type " + ResourceNameType.FIXED);
    }
    return new AutoValue_FieldConfig(
        field, resourceNameTreatment, resourceNameConfig, messageResourceNameConfig);
  }

  /** Creates a FieldConfig for the given Field with ResourceNameTreatment set to None. */
  public static FieldConfig createDefaultFieldConfig(FieldModel field) {
    return FieldConfig.createFieldConfig(field, ResourceNameTreatment.NONE, null, null);
  }

  static FieldConfig createMessageFieldConfig(
      ResourceNameMessageConfigs messageConfigs,
      Map<String, ResourceNameConfig> resourceNameConfigs,
      FieldModel field,
      ResourceNameTreatment defaultResourceNameTreatment) {
    return createFieldConfig(
        null,
        messageConfigs,
        null,
        resourceNameConfigs,
        field,
        ResourceNameTreatment.UNSET_TREATMENT,
        defaultResourceNameTreatment);
  }

  /** Package-private since this is not used outside the config package. */
  static FieldConfig createFieldConfig(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      Map<String, String> fieldNamePatterns,
      Map<String, ResourceNameConfig> resourceNameConfigs,
      FieldModel field,
      @Nullable ResourceNameTreatment treatment,
      ResourceNameTreatment defaultResourceNameTreatment) {
    String messageFieldEntityName = null;
    String flattenedFieldEntityName = null;
    if (messageConfigs != null && messageConfigs.fieldHasResourceName(field)) {
      messageFieldEntityName = messageConfigs.getFieldResourceName(field);
    }
    if (fieldNamePatterns != null) {
      flattenedFieldEntityName = fieldNamePatterns.get(field.getNameAsParameter());
    }
    if (flattenedFieldEntityName == null) {
      flattenedFieldEntityName = messageFieldEntityName;
    }

    if (treatment == null || treatment.equals(ResourceNameTreatment.UNSET_TREATMENT)) {
      // No specific resource name treatment is specified, so we infer the correct treatment from
      // the method-level default and the specified entities.
      if (flattenedFieldEntityName == null) {
        treatment = ResourceNameTreatment.NONE;
      } else {
        treatment = defaultResourceNameTreatment;
      }
    }

    ResourceNameConfig messageFieldResourceNameConfig =
        getResourceNameConfig(diagCollector, resourceNameConfigs, messageFieldEntityName);
    ResourceNameConfig flattenedFieldResourceNameConfig =
        getResourceNameConfig(diagCollector, resourceNameConfigs, flattenedFieldEntityName);

    if (messageFieldResourceNameConfig != null
        && !messageFieldResourceNameConfig.equals(flattenedFieldResourceNameConfig)) {
      // We support the case of the flattenedField using a specific resource name type when the
      // messageField uses a oneof containing that type, or when the messageField accepts any
      // resource name, or for Discovery fields.
      ResourceNameType resourceTypeName = messageFieldResourceNameConfig.getResourceNameType();
      boolean ok = resourceTypeName == ResourceNameType.ANY;
      if (resourceTypeName == ResourceNameType.ONEOF) {
        ResourceNameOneofConfig oneofConfig =
            (ResourceNameOneofConfig) messageFieldResourceNameConfig;
        ok = oneofConfig.getResourceNameConfigs().contains(flattenedFieldResourceNameConfig);
      }
      if (!ok) {
        // Prefer using entity name from flattening config, which is derived from GAPIC config,
        // than the entity name from method config, which might be defined in proto annotations.
        Diag warning =
            Diag.warning(
                SimpleLocation.TOPLEVEL,
                "Multiple entity names specified for field: "
                    + field.getFullName()
                    + ": ["
                    + flattenedFieldEntityName
                    + ", "
                    + messageFieldEntityName
                    + "], using flattening config instead of message config.");
        diagCollector.addDiag(warning);
        messageFieldResourceNameConfig = flattenedFieldResourceNameConfig;
      }
    }

    validate(messageConfigs, field, treatment, flattenedFieldResourceNameConfig);

    return createFieldConfig(
        field, treatment, flattenedFieldResourceNameConfig, messageFieldResourceNameConfig);
  }

  private static ResourceNameConfig getResourceNameConfig(
      DiagCollector diagCollector,
      Map<String, ResourceNameConfig> resourceNameConfigs,
      String entityName) {
    if (entityName != null) {
      if (entityName.equals(AnyResourceNameConfig.GAPIC_CONFIG_ANY_VALUE)) {
        return AnyResourceNameConfig.instance();
      } else {
        ResourceNameConfig flattenedFieldResourceNameConfig = resourceNameConfigs.get(entityName);
        if (flattenedFieldResourceNameConfig == null) {
          Diag error =
              Diag.error(
                  SimpleLocation.TOPLEVEL,
                  "No resourceNameConfig with entity_name \"%s\"",
                  entityName);
          if (diagCollector == null) {
            throw new IllegalArgumentException(error.toString());
          }
          diagCollector.addDiag(error);
          return null;
        }
        return flattenedFieldResourceNameConfig;
      }
    }
    return null;
  }

  public boolean useResourceNameType() {
    return getResourceNameTreatment() == ResourceNameTreatment.STATIC_TYPES;
  }

  public boolean useResourceNameTypeInSampleOnly() {
    return getResourceNameTreatment() == ResourceNameTreatment.SAMPLE_ONLY;
  }

  public boolean useValidation() {
    return getResourceNameTreatment() == ResourceNameTreatment.VALIDATE;
  }

  public FieldConfig withResourceNameConfig(ResourceNameConfig resourceNameConfig) {
    return FieldConfig.createFieldConfig(
        getField(), getResourceNameTreatment(), resourceNameConfig, getMessageResourceNameConfig());
  }

  public FieldConfig withResourceNameInSampleOnly() {
    ResourceNameTreatment newTreatment = ResourceNameTreatment.NONE;
    if (ResourceNameTreatment.STATIC_TYPES.equals(getResourceNameTreatment())) {
      newTreatment = ResourceNameTreatment.SAMPLE_ONLY;
    }
    return FieldConfig.createFieldConfig(
        getField(), newTreatment, getResourceNameConfig(), getMessageResourceNameConfig());
  }

  public boolean requiresParamTransformation() {
    return getResourceNameConfig() != null
        && getMessageResourceNameConfig() != null
        && !getResourceNameConfig().equals(getMessageResourceNameConfig());
  }

  public boolean requiresParamTransformationFromAny() {
    return getMessageResourceNameConfig() != null
        && getMessageResourceNameConfig().getResourceNameType() == ResourceNameType.ANY;
  }

  public FieldConfig getMessageFieldConfig() {
    return FieldConfig.createFieldConfig(
        getField(),
        getMessageResourceNameConfig() == null
            ? ResourceNameTreatment.NONE
            : getResourceNameTreatment(),
        getMessageResourceNameConfig(),
        getMessageResourceNameConfig());
  }

  /*
   * Check that the provided resource name treatment and entityName are valid for the provided field.
   */
  public static void validate(
      ResourceNameMessageConfigs messageConfigs,
      FieldModel field,
      ResourceNameTreatment treatment,
      ResourceNameConfig resourceNameConfig) {
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
        if (resourceNameConfig == null) {
          throw new IllegalArgumentException(
              "Field must have a resource type or field name pattern specified to support "
                  + "VALIDATE resource name treatment. Field: "
                  + field.getFullName());
        }
        break;
      case UNSET_TREATMENT:
      case UNRECOGNIZED:
      default:
        throw new IllegalArgumentException("Unrecognized resource name type: " + treatment);
    }
  }

  public static Collection<FieldModel> toFieldTypeIterable(Collection<FieldConfig> fieldConfigs) {
    return fieldConfigs.stream().map(FieldConfig::getField).collect(Collectors.toList());
  }

  public static Collection<FieldModel> toFieldTypeIterableFromField(
      Collection<Field> fieldConfigs) {
    return fieldConfigs.stream().map(ProtoField::new).collect(Collectors.toList());
  }

  public static ImmutableMap<String, FieldConfig> toFieldConfigMap(
      Iterable<FieldConfig> fieldConfigs) {
    return Maps.uniqueIndex(fieldConfigs, f -> f.getField().getFullName());
  }
}
