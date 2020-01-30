/* Copyright 2019 Google LLC
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
import com.google.api.tools.framework.model.DiagCollector;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldConfigFactory {

  private FieldConfigFactory() {}

  /*
   * Create a FieldConfig for a field in a message. If the field is associated
   * with multiple resource names through child_type resource reference,
   * the created FieldConfig will pick one for its messageResourceNameConfig.
   */
  static FieldConfig createMessageFieldConfig(
      ResourceNameMessageConfigs messageConfigs,
      Map<String, ResourceNameConfig> resourceNameConfigs,
      FieldModel field,
      ResourceNameTreatment defaultResourceNameTreatment) {
    List<FieldConfig> configs =
        createFlattenedFieldConfigs(
            null,
            messageConfigs,
            null,
            resourceNameConfigs,
            field,
            ResourceNameTreatment.UNSET_TREATMENT,
            defaultResourceNameTreatment);
    if (configs.size() >= 1) {
      return configs.get(0);
    }
    return null;
  }

  static List<FieldConfig> createFlattenedFieldConfigs(
      ResourceNameMessageConfigs messageConfigs,
      Map<String, ResourceNameConfig> resourceNameConfigs,
      FieldModel field,
      ResourceNameTreatment defaultResourceNameTreatment) {
    return createFlattenedFieldConfigs(
        null,
        messageConfigs,
        null,
        resourceNameConfigs,
        field,
        ResourceNameTreatment.UNSET_TREATMENT,
        defaultResourceNameTreatment);
  }

  /**
   * Create a list of FieldConfigs for a flattened field in the API surface. If the field is
   * associated with multiple resource names through child_type resource reference, each created
   * FieldConfig will have one of these resource name configs.
   *
   * <p>For example, consider the following case: <code>
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
   * and the created FieldConfigs for the field `parent` has the following mapping:
   *
   * <p>
   *
   * <ul>
   *   <li>method_signature -> (resourceNameConfig, messageResourceNameConfig,
   *       resourceNameTreatment)
   *   <li>ListFoos(ProjectName parent) -> ("Project", "Project", STATIC_TYPE);
   *   <li>ListFoos(LocationName parent) -> ("Location", "Location", STATIC_TYPE);
   *   <li>ListFoos(String parent) -> ("Project", "Project", SAMPLE_ONLY);
   * </ul>
   */
  static List<FieldConfig> createFlattenedFieldConfigs(
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
      return createFieldConfigForMultiResourceField(
          diagCollector,
          messageFieldEntityNames,
          flattenedFieldEntityNames,
          messageConfigs,
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
        FieldConfig.createFieldConfig(
            diagCollector,
            messageFieldEntityName,
            flattenedFieldEntityName,
            messageConfigs,
            resourceNameConfigs,
            field,
            treatment,
            defaultResourceNameTreatment));
  }

  /**
   * Create a FieldConfig for a flattened field in a GAPIC config backed API. Because GAPIC YAML
   * does not support configuring multiple resource names to fields, one field will always have only
   * one FieldConfig.
   */
  static FieldConfig createFlattenedFieldConfigFromGapicYaml(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableListMultimap<String, String> fieldNamePatterns,
      Map<String, ResourceNameConfig> resourceNameConfigs,
      FieldModel field,
      ResourceNameTreatment treatment,
      ResourceNameTreatment defaultResourceNameTreatment) {
    List<FieldConfig> fieldConfigs =
        createFlattenedFieldConfigs(
            diagCollector,
            messageConfigs,
            fieldNamePatterns,
            resourceNameConfigs,
            field,
            treatment,
            defaultResourceNameTreatment);
    if (fieldConfigs == null && fieldConfigs.isEmpty()) {
      return null;
    }
    Preconditions.checkState(
        fieldConfigs.size() == 1,
        "GAPICs generated from GAPIC YAML can only have one FieldConfig per field, got [%s]",
        fieldConfigs);
    return fieldConfigs.get(0);
  }

  /**
   * Returns a list of FieldConfigs for a field associated with multiple resource names. Each
   * created FieldConfig will have one of these resource name configs.
   *
   * <p>Note only GAPICs generated from proto annoatations name may have fields associated with
   * multiple resource names. GAPICs generated from GAPIC config cannot.
   */
  private static List<FieldConfig> createFieldConfigForMultiResourceField(
      DiagCollector diagCollector,
      List<String> messageFieldEntityNames,
      List<String> flattenedFieldEntityNames,
      ResourceNameMessageConfigs messageConfigs,
      Map<String, ResourceNameConfig> resourceNameConfigs,
      FieldModel field,
      ResourceNameTreatment treatment,
      ResourceNameTreatment defaultResourceNameTreatment) {
    Preconditions.checkState(
        new HashSet<String>(messageFieldEntityNames)
            .equals(new HashSet<String>(flattenedFieldEntityNames)),
        "fields with multiple resource name configs must have exactly the same set of "
            + "resource name configs as a message field and a flattened field, but got: "
            + "messageFieldEntityNames: %s and flattenedFieldEntityNames: %s",
        messageFieldEntityNames.stream().collect(Collectors.joining(",")),
        flattenedFieldEntityNames.stream().collect(Collectors.joining(",")));

    ImmutableList.Builder<FieldConfig> fieldConfigs = ImmutableList.builder();
    for (String entityName : messageFieldEntityNames) {
      fieldConfigs.add(
          FieldConfig.createFieldConfig(
              diagCollector,
              entityName,
              entityName,
              messageConfigs,
              resourceNameConfigs,
              field,
              treatment,
              defaultResourceNameTreatment));
    }
    return fieldConfigs.build();
  }
}
