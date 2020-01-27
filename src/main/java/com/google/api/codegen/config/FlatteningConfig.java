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

import com.google.api.codegen.FlatteningGroupProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.configgen.transformer.DiscoveryMethodTransformer;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Oneof;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** FlatteningConfig represents a specific flattening configuration for a method. */
@AutoValue
public abstract class FlatteningConfig {

  // Maps the name of the parameter in this flattening to its FieldConfig.
  public abstract ImmutableMap<String, FieldConfig> getFlattenedFieldConfigs();

  /**
   * Appends to a map of a string representing a list of the fields in a flattening, to the
   * flattening config created from a method in the gapic config.
   */
  private static void insertFlatteningsFromGapicConfig(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      MethodConfigProto methodConfigProto,
      MethodModel methodModel,
      ImmutableMap.Builder<String, List<FlatteningConfig>> flatteningConfigs) {

    for (FlatteningGroupProto flatteningGroup : methodConfigProto.getFlattening().getGroupsList()) {
      FlatteningConfig groupConfig =
          FlatteningConfig.createFlatteningFromConfigProto(
              diagCollector,
              messageConfigs,
              resourceNameConfigs,
              methodConfigProto,
              flatteningGroup,
              methodModel);
      if (groupConfig != null) {
        if (hasAnyResourceNameParameter(groupConfig)) {
          flatteningConfigs.put(
              flatteningConfigToString(groupConfig),
              ImmutableList.of(groupConfig, groupConfig.withResourceNamesInSamplesOnly()));
        } else {
          flatteningConfigs.put(
              flatteningConfigToString(groupConfig), ImmutableList.of(groupConfig));
        }
      }
    }
  }

  static ImmutableList<FlatteningConfig> createFlatteningConfigs(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      MethodConfigProto methodConfigProto,
      MethodModel methodModel) {
    ImmutableMap.Builder<String, List<FlatteningConfig>> flatteningConfigs = ImmutableMap.builder();
    insertFlatteningsFromGapicConfig(
        diagCollector,
        messageConfigs,
        resourceNameConfigs,
        methodConfigProto,
        methodModel,
        flatteningConfigs);
    if (diagCollector.hasErrors()) {
      return null;
    }
    return flatteningConfigs
        .build()
        .values()
        .stream()
        .flatMap(List::stream)
        .collect(ImmutableList.toImmutableList());
  }

  @VisibleForTesting
  @Nullable
  static ImmutableList<FlatteningConfig> createFlatteningConfigs(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      MethodConfigProto methodConfigProto,
      ProtoMethodModel methodModel,
      ProtoParser protoParser) {

    ImmutableMap.Builder<String, List<FlatteningConfig>> flatteningConfigs = ImmutableMap.builder();

    insertFlatteningsFromGapicConfig(
        diagCollector,
        messageConfigs,
        resourceNameConfigs,
        methodConfigProto,
        methodModel,
        flatteningConfigs);
    insertFlatteningConfigsFromProtoFile(
        diagCollector,
        messageConfigs,
        resourceNameConfigs,
        methodModel,
        protoParser,
        flatteningConfigs);

    if (diagCollector.hasErrors()) {
      return null;
    }
    return flatteningConfigs
        .build()
        .values()
        .stream()
        .flatMap(List::stream)
        // .map(
        //     f -> {
        //       System.out.println(f);
        //       return f;
        //     })
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Appends to map of a string representing a list of the fields in a flattening, to the flattening
   * config created from a method from the proto file.
   */
  private static void insertFlatteningConfigsFromProtoFile(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      ProtoMethodModel methodModel,
      ProtoParser protoParser,
      ImmutableMap.Builder<String, List<FlatteningConfig>> flatteningConfigs) {
    // Get flattenings from protofile annotations, let these override flattenings from GAPIC config.
    List<List<String>> methodSignatures =
        protoParser.getMethodSignatures(methodModel.getProtoMethod());
    for (List<String> signature : methodSignatures) {
      List<FlatteningConfig> groupConfigs =
          FlatteningConfig.createFlatteningsFromProtoFile(
              diagCollector,
              messageConfigs,
              resourceNameConfigs,
              signature,
              methodModel,
              protoParser);
      if (groupConfigs != null && !groupConfigs.isEmpty()) {
        flatteningConfigs.put(flatteningConfigToString(groupConfigs.get(0)), groupConfigs);
      }
    }
  }

  /**
   * Creates an instance of FlatteningConfig based on a FlatteningGroupProto, linking it up with the
   * provided method.
   */
  @Nullable
  private static FlatteningConfig createFlatteningFromConfigProto(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      MethodConfigProto methodConfigProto,
      FlatteningGroupProto flatteningGroup,
      MethodModel method) {

    boolean missing = false;
    ImmutableMap.Builder<String, FieldConfig> flattenedFieldConfigBuilder = ImmutableMap.builder();
    Set<String> oneofNames = new HashSet<>();
    List<String> flattenedParams = Lists.newArrayList(flatteningGroup.getParametersList());
    if (method.hasExtraFieldMask()) {
      flattenedParams.add(DiscoveryMethodTransformer.FIELDMASK_STRING);
    }
    for (String parameter : flattenedParams) {

      FieldModel parameterField = method.getInputField(parameter);
      if (parameterField == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Field missing for flattening: method = %s, message type = %s, field = %s",
                method.getFullName(),
                method.getInputFullName(),
                parameter));
        return null;
      }

      Oneof oneof = parameterField.getOneof();
      if (oneof != null) {
        String oneofName = oneof.getName();
        if (oneofNames.contains(oneofName)) {
          diagCollector.addDiag(
              Diag.error(
                  SimpleLocation.TOPLEVEL,
                  "Value from oneof already specifed for flattening:%n"
                      + "method = %s, message type = %s, oneof = %s",
                  method.getFullName(),
                  method.getInputFullName(),
                  oneofName));
          return null;
        }
        oneofNames.add(oneofName);
      }

      ResourceNameTreatment defaultResourceNameTreatment =
          methodConfigProto.getResourceNameTreatment();
      if (!parameterField.mayBeInResourceName()) {
        defaultResourceNameTreatment = ResourceNameTreatment.NONE;
      }

      FieldConfig fieldConfig =
          FieldConfigFactory.createFlattenedFieldConfigFromGapicYaml(
              diagCollector,
              messageConfigs,
              ImmutableListMultimap.copyOf(methodConfigProto.getFieldNamePatternsMap().entrySet()),
              resourceNameConfigs,
              parameterField,
              flatteningGroup
                  .getParameterResourceNameTreatmentMap()
                  .getOrDefault(parameter, ResourceNameTreatment.UNSET_TREATMENT),
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

    return new AutoValue_FlatteningConfig(flattenedFieldConfigBuilder.build());
  }

  /**
   * Creates instances of FlatteningConfig based on the method_signature and resource name related
   * proto annotations, linking it up with the provided method.
   */
  @Nullable
  private static List<FlatteningConfig> createFlatteningsFromProtoFile(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      List<String> flattenedParams,
      ProtoMethodModel method,
      ProtoParser protoParser) {
    Set<String> oneofNames = new HashSet<>();
    List<Map<String, FieldConfig>> flatteningConfigs = new ArrayList<>();

    if (flattenedParams.isEmpty()) {
      flatteningConfigs.add(Collections.emptyMap());
    }

    for (String parameter : flattenedParams) {
      List<FieldConfig> fieldConfigs =
          createFieldConfigsForParameter(
              diagCollector,
              parameter,
              messageConfigs,
              resourceNameConfigs,
              oneofNames,
              method,
              protoParser.hasResourceReference(method.getInputField(parameter).getProtoField())
                  ? ResourceNameTreatment.STATIC_TYPES
                  : ResourceNameTreatment.NONE);
      collectFieldConfigs(flatteningConfigs, fieldConfigs, parameter);
    }

    // We also generate an overload that all resource names are treated as strings
    if (hasSingularResourceNameParameters(flatteningConfigs)) {
      flatteningConfigs.add(withResourceNamesInSamplesOnly(flatteningConfigs.get(0)));
    }

    return flatteningConfigs
        .stream()
        .map(ImmutableMap::copyOf)
        .map(map -> new AutoValue_FlatteningConfig(map))
        .collect(ImmutableList.toImmutableList());
  }

  private static void collectFieldConfigs(
      List<Map<String, FieldConfig>> flatteningConfigs,
      List<FieldConfig> fieldConfigs,
      String parameter) {
    int flatteningConfigsCount = flatteningConfigs.size();
    if (flatteningConfigsCount == 0) {
      for (int j = 0; j < fieldConfigs.size(); j++) {
        LinkedHashMap<String, FieldConfig> newFlattening = new LinkedHashMap<>();
        newFlattening.put(parameter, fieldConfigs.get(j));
        flatteningConfigs.add(newFlattening);
      }
    } else {
      for (int i = 0; i < flatteningConfigsCount; i++) {
        for (int j = 0; j < fieldConfigs.size() - 1; j++) {
          LinkedHashMap<String, FieldConfig> newFlattening =
              new LinkedHashMap<>(flatteningConfigs.get(i));
          newFlattening.put(parameter, fieldConfigs.get(j));
          flatteningConfigs.add(newFlattening);
        }
        flatteningConfigs.get(i).put(parameter, fieldConfigs.get(fieldConfigs.size() - 1));
      }
    }
  }

  private static List<FieldConfig> createFieldConfigsForParameter(
      DiagCollector diagCollector,
      String parameter,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      Set<String> oneofNames,
      ProtoMethodModel method,
      ResourceNameTreatment treatment) {

    ProtoField parameterField = method.getInputField(parameter);
    if (parameterField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Field missing for flattening: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getInputFullName(),
              parameter));
      return null;
    }

    Oneof oneof = parameterField.getOneof();
    if (oneof != null) {
      String oneofName = oneof.getName();
      if (oneofNames.contains(oneofName)) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Value from oneof already specifed for flattening:%n"
                    + "method = %s, message type = %s, oneof = %s",
                method.getFullName(),
                method.getInputFullName(),
                oneofName));
        return null;
      }
      oneofNames.add(oneofName);
    }

    List<FieldConfig> fieldConfigs =
        FieldConfigFactory.createFlattenedFieldConfigs(
            messageConfigs, resourceNameConfigs, parameterField, treatment);

    if (fieldConfigs.isEmpty()) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "internal: failed to create any field config for field: %s of method: %s",
              parameter,
              method.getFullName()));
    }
    return fieldConfigs;
  }

  public Iterable<FieldModel> getFlattenedFields() {
    return FieldConfig.toFieldTypeIterable(getFlattenedFieldConfigs().values());
  }

  public FlatteningConfig withResourceNamesInSamplesOnly() {
    ImmutableMap<String, FieldConfig> newFlattenedFieldConfigs =
        withResourceNamesInSamplesOnly(getFlattenedFieldConfigs());
    return new AutoValue_FlatteningConfig(newFlattenedFieldConfigs);
  }

  private static ImmutableMap<String, FieldConfig> withResourceNamesInSamplesOnly(
      Map<String, FieldConfig> flatteningGroup) {
    ImmutableMap<String, FieldConfig> newFlattenedFieldConfigs =
        flatteningGroup
            .entrySet()
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Map.Entry::getKey, e -> e.getValue().withResourceNameInSampleOnly()));
    return newFlattenedFieldConfigs;
  }

  public static boolean hasAnyRepeatedResourceNameParameter(FlatteningConfig flatteningGroup) {
    // Used in Java to prevent generating a flattened method with List<ResourceName> as a parameter
    // because that has the same type erasure as the version of the flattened method with
    // List<String> as a parameter.

    // TODO(gapic-generator issue #2137) Only use raw String type for repeated params
    // not for singular params in the same flattened method.
    return flatteningGroup
        .getFlattenedFieldConfigs()
        .values()
        .stream()
        .anyMatch(
            (FieldConfig fieldConfig) ->
                fieldConfig.getField().isRepeated() && fieldConfig.useResourceNameType());
  }

  /** Returns a string representing the ordered fields in a flattening config. */
  private static String flatteningConfigToString(FlatteningConfig flatteningConfig) {
    Iterable<FieldModel> paramList = flatteningConfig.getFlattenedFields();
    StringBuilder paramsAsString = new StringBuilder();
    paramList.forEach(p -> paramsAsString.append(p.getSimpleName()).append(", "));
    return paramsAsString.toString();
  }

  /** Return if the flattening config contains a parameter that is a resource name. */
  public static boolean hasAnyResourceNameParameter(FlatteningConfig flatteningGroup) {
    return hasAnyResourceNameParameter(flatteningGroup.getFlattenedFieldConfigs());
  }

  private static boolean hasAnyResourceNameParameter(
      List<Map<String, FieldConfig>> flatteningGroups) {
    return flatteningGroups.stream().anyMatch(FlatteningConfig::hasAnyResourceNameParameter);
  }

  private static boolean hasAnyResourceNameParameter(Map<String, FieldConfig> flatteningGroup) {
    return flatteningGroup.values().stream().anyMatch(FieldConfig::useResourceNameType);
  }

  private static boolean hasSingularResourceNameParameter(
      Map<String, FieldConfig> flatteningGroup) {
    return flatteningGroup
        .values()
        .stream()
        .anyMatch(
            f -> (!f.getField().isRepeated() && !f.getField().isMap() && f.useResourceNameType()));
  }

  private static boolean hasAnyRepeatedResourceNameParameter(
      Map<String, FieldConfig> flatteningGroup) {
    return flatteningGroup
        .values()
        .stream()
        .anyMatch(f -> f.getField().isRepeated() && f.useResourceNameType());
  }

  private static boolean hasSingularResourceNameParameters(
      List<Map<String, FieldConfig>> flatteningGroups) {
    return flatteningGroups.stream().anyMatch(FlatteningConfig::hasSingularResourceNameParameter);
  }
}
