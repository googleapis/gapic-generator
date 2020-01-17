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

  public List<FieldConfig> flatResourceNameConfigs() {}

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
    List<FieldConfig> configs =
        createMessageFieldConfigs(
            messageConfigs, resourceNameConfigs, field, defaultResourceNameTreatment);
    if (configs.size() == 1) {
      return configs.get(0);
    }
    throw new IllegalArgumentException(
        String.format(
            "Field %s has multiple resource name configs: [%s], can't create a single FieldConfig object.",
            configs
                .stream()
                .map(FieldConfig::getResourceNameConfig)
                .map(ResourceNameConfig::getEntityName)
                .collect(Collectors.joining(","))));
  }

  static List<FieldConfig> createMessageFieldConfigs(
      ResourceNameMessageConfigs messageConfigs,
      Map<String, ResourceNameConfig> resourceNameConfigs,
      FieldModel field,
      ResourceNameTreatment defaultResourceNameTreatment) {
    return createFieldConfigs(
        null,
        messageConfigs,
        null,
        resourceNameConfigs,
        field,
        ResourceNameTreatment.UNSET_TREATMENT,
        defaultResourceNameTreatment);
  }

  static List<FieldConfig> createFieldConfigs(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableListMultimap<String, String> fieldNamePatterns,
      Map<String, ResourceNameConfig> resourceNameConfigs,
      FieldModel field,
      ResourceNameTreatment treatment,
      ResourceNameTreatment defaultResourceNameTreatment) {
    List<String> messageFieldEntityNames = Collections.emptyList();
    List<String> flattenedFieldEntityNames = Collections.emptyList();
    if (messageConfigs != null && messageConfigs.fieldHasResourceName(field)) {
      messageFieldEntityNames = messageConfigs.getFieldResourceNames(field);
    }
    if (fieldNamePatterns != null) {
      flattenedFieldEntityNames = fieldNamePatterns.get(field.getNameAsParameter());
    }
    if (flattenedFieldEntityNames.isEmpty()) {
      flattenedFieldEntityNames = messageFieldEntityNames;
    }

    if (messageFieldEntityNames.size() > 1 || flattenedFieldEntityNames.size() > 1) {
      return createFieldConfigsWithMultipleResourceNames(
          diagCollector,
          messageFieldEntityNames,
          flattenedFieldEntityNames,
          resourceNameConfigs,
          field,
          treatment,
          defaultResourceNameTreatment);
    }

    String messageFieldEntityName =
        messageFieldEntityNames.isEmpty() ? null : messageFieldEntityNames.get(0);
    String flattenedFieldEntityName =
        flattenedFieldEntityNames.isEmpty() ? null : flattenedFieldEntityNames.get(0);
    return Collections.singletonList(
        createFieldConfig(
            diagCollector,
            messageFieldEntityNames.get(0),
            flattenedFieldEntityNames.get(0),
            resourceNameConfigs,
            field,
            treatment,
            defaultResourceNameTreatment));
  }

  private static List<FieldConfig> createFieldConfigsWithMultipleResourceNames(
      DiagCollector diagCollector,
      List<String> messageFieldEntityNames,
      List<String> flattenedFieldEntityNames,
      Map<String, ResourceNameConfig> resourceNameConfigs,
      FieldModel field,
      ResourceNameTreatment treatment,
      ResourceNameTreatment defaultResourceNameTreatment) {
    Preconditions.checkState(
        new HashSet<String>(messageFieldEntityNames)
            .equals(new HashSet<String>(flattenedFieldEntityName)),
        "fields with multiple resource name configs must have exactly the same set of "
            + "resource name configs as a message field and a flattened field, but got: "
            + "messageFieldEntityNames: %s and flattenedFieldEntityNames: %s",
        messageFieldEntityNames.stream().collect(Collectors.joining(",")),
        flattenedFieldEntityNames.stream().collect(Collectors.joining(",")));

    ImmutableList.Builder<FieldConfig> fieldConfigs = ImmutableList.builder();
    for (String entityName : messageFieldEntityNames) {
      fieldConfigs.add(
          createFieldConfig(
              diagCollector,
              entityName,
              entityName,
              resourceNameConfigs,
              field,
              treatment,
              defaultResourceNameTreatment));
    }
    return fieldConfig.build();
  }

  /** Package-private since this is not used outside the config package. */
  static FieldConfig createFieldConfig(
      DiagCollector diagCollector,
      @Nullable String messageFieldEntityName,
      @Nullable String flattenedFieldEntityName,
      Map<String, ResourceNameConfig> resourceNameConfigs,
      FieldModel field,
      ResourceNameTreatment treatment,
      ResourceNameTreatment defaultResourceNameTreatment) {

    if (treatment == ResourceNameTreatment.UNSET_TREATMENT) {
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

    if (treatment == ResourceNameTreatment.UNSET_TREATMENT) {
      if (messageFieldResourceNameConfig != null && messageConfigs.fieldHasResourceName(field)) {
        treatment = ResourceNameTreatment.STATIC_TYPES;
      } else if (flattenedFieldResourceNameConfig != null) {
        treatment = ResourceNameTreatment.VALIDATE;
      }
    }

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

    return newBuilder()
        .setField(field)
        .setResourceNameTreatment(resourceNameTreatment)
        .setResourceNameConfig(flattenedFieldResourceNameConfig)
        .setMessageResourceNameConfig(messageFieldResourceNameConfig)
        .build();
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
                  "No resourceNameConfig with entity_name \"%s\", names: [%s]",
                  entityName,
                  String.join(", ", resourceNameConfigs.keySet()));
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

  @AutoValue.Builder
  public static class Builder {

    public abstract Builder setField(FieldModel val);

    public abstract Builder setResourceNameTreatment(ResourceNameTreatment val);

    public abstract Builder setResourceNameConfig(ResourceNameConfig val);

    public abstract Builder setMessageResourceNameConfig(ResourceNameConfig val);

    public abstract FieldConfig build();
  }

  public static Builder newBuilder() {
    return new AutoValue_Builder().setResourceNameConfigs(ImmutableList.of());
  }
}
