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
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** FieldConfig represents a configuration for a Field. */
@AutoValue
public abstract class FieldConfig {
  public abstract FieldModel getField();

  @Nullable
  public abstract ResourceNameTreatment getResourceNameTreatment();

  /**
   * The resource name config used in API surfaces. For resource name fields in a message,
   * getResourceNameConfig() always returns null. For flattened resource name fields,
   * getResourceNameConfigs() returns the resource name config in the API surface.
   *
   * <p>Note a field can have multiple resource names the field has child_type resource reference.
   * For example, consider the following case: <code>
   *
   * option (google.api.resource_defintion) = {
   *  type: "library.googleapis.com/Book",
   *  pattern: "projects/{project}/books/{book}",
   *  pattern: "projects/{project}/locations/{location}/books/{book}"
   * };
   *
   * rpc ListFoos(ListFoosRequest) returns (ListFoosResponse) {
   *   option (google.api.method_signature) = "parent";
   * }
   *
   * message ListFoosRequest {
   *   string parent = 1 [
   *     (google.api.resource_reference).child_type = "library.googleapis.com/Book"]
   * }
   * </code>
   *
   * <p>The field `parent` will have two resource name configs: Project and Location. In this case,
   * we need to generate three flattening overloads for the method ListFoos. The method signatures
   * and the FieldConfig for the field `parent` has the following mapping:
   * <li>method_signature -> (resourceNameConfig, exampleResourceNameConfig, resourceNameTreatment)
   * <li>ListFoos(ProjectName parent) -> ("Project", "Project", STATIC_TYPE);
   * <li>ListFoos(LocationName parent) -> ("Location", "Location", STATIC_TYPE);
   * <li>ListFoos(String parent) -> ("Project", "Project", SAMPLE_ONLY);
   * <li>ListFoos(ListFoosRequest request); -> (null, "Project", SAMPLE_ONLY);
   */
  @Nullable
  public abstract ResourceNameConfig getResourceNameConfig();

  /**
   * The resource name config used in samples and tests. For flattened resource name fields,
   * getExampleResourceConfig() and getResourceNameConfig() always return the same config.
   */
  @Nullable
  public abstract ResourceNameConfig getExampleResourceNameConfig();

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
      ResourceNameConfig exampleResourceNameConfig) {
    if (resourceNameTreatment != ResourceNameTreatment.NONE && resourceNameConfig == null) {
      throw new IllegalArgumentException(
          "resourceName may only be null if resourceNameTreatment is NONE");
    }
    if (resourceNameConfig != null
        && resourceNameConfig.getResourceNameType() == ResourceNameType.FIXED) {
      throw new IllegalArgumentException(
          "FieldConfig may not contain a ResourceNameConfig of type " + ResourceNameType.FIXED);
    }
    return FieldConfig.newBuilder()
        .setField(field)
        .setResourceNameTreatment(resourceNameTreatment)
        .setResourceNameConfig(resourceNameConfig)
        .setExampleResourceNameConfig(exampleResourceNameConfig)
        .build();
  }

  /** Creates a FieldConfig for the given Field with ResourceNameTreatment set to None. */
  public static FieldConfig createDefaultFieldConfig(FieldModel field) {
    return FieldConfig.createFieldConfig(field, ResourceNameTreatment.NONE, null, null);
  }

  /** Package-private since this is not used outside the config package. */
  static FieldConfig createFieldConfig(
      DiagCollector diagCollector,
      @Nullable String messageFieldEntityName,
      @Nullable String flattenedFieldEntityName,
      ResourceNameMessageConfigs messageConfigs,
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

    FieldConfig config =
        newBuilder()
            .setField(field)
            .setResourceNameTreatment(treatment)
            .setResourceNameConfig(flattenedFieldResourceNameConfig)
            .setExampleResourceNameConfig(messageFieldResourceNameConfig)
            .build();
    return config;
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
        getField(), getResourceNameTreatment(), resourceNameConfig, getExampleResourceNameConfig());
  }

  public FieldConfig withResourceNameInSampleOnly() {
    ResourceNameTreatment newTreatment = ResourceNameTreatment.NONE;
    if (ResourceNameTreatment.STATIC_TYPES.equals(getResourceNameTreatment())
        || ResourceNameTreatment.SAMPLE_ONLY.equals(getResourceNameTreatment())) {
      newTreatment = ResourceNameTreatment.SAMPLE_ONLY;
    }
    return FieldConfig.createFieldConfig(
        getField(), newTreatment, getResourceNameConfig(), getExampleResourceNameConfig());
  }

  public boolean requiresParamTransformation() {
    return getResourceNameConfig() != null
        && getExampleResourceNameConfig() != null
        && !getResourceNameConfig().equals(getExampleResourceNameConfig());
  }

  public boolean requiresParamTransformationFromAny() {
    return getExampleResourceNameConfig() != null
        && getExampleResourceNameConfig().getResourceNameType() == ResourceNameType.ANY;
  }

  public FieldConfig getMessageFieldConfig() {
    return FieldConfig.createFieldConfig(
        getField(),
        getExampleResourceNameConfig() == null
            ? ResourceNameTreatment.NONE
            : getResourceNameTreatment(),
        getExampleResourceNameConfig(),
        getExampleResourceNameConfig());
  }

  public boolean isRepeatedResourceNameTypeField() {
    return getField().isRepeated() && useResourceNameType();
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
  public abstract static class Builder {

    public abstract Builder setField(FieldModel val);

    public abstract Builder setResourceNameTreatment(ResourceNameTreatment val);

    public abstract Builder setResourceNameConfig(ResourceNameConfig val);

    public abstract Builder setExampleResourceNameConfig(ResourceNameConfig val);

    public abstract FieldConfig build();
  }

  public static Builder newBuilder() {
    return new AutoValue_FieldConfig.Builder();
  }

  @Override
  public String toString() {
    String resourceNameEntityId =
        getResourceNameConfig() == null ? "null" : getResourceNameConfig().getEntityId();
    String exampleResourceNameEntityId =
        getExampleResourceNameConfig() == null
            ? "null"
            : getExampleResourceNameConfig().getEntityId();

    return MoreObjects.toStringHelper(this)
        .add("fieldName", getField().getSimpleName())
        .add("resourceNameEntityId", resourceNameEntityId)
        .add("exampleResourceNameEntityId", exampleResourceNameEntityId)
        .add("resourceTreatment", getResourceNameTreatment())
        .toString();
  }
}
