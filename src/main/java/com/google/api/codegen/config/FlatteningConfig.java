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

import com.google.api.MethodSignature;
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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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

  @Nullable
  public abstract String getFlatteningName();

  @Nullable
  static ImmutableList<FlatteningConfig> createFlatteningConfigs(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      MethodConfigProto methodConfigProto,
      MethodModel methodModel) {
    return createFlatteningConfigs(
        diagCollector,
        messageConfigs,
        resourceNameConfigs,
        methodConfigProto,
        methodModel,
        new ProtoParser());
  }

  @VisibleForTesting
  @Nullable
  static ImmutableList<FlatteningConfig> createFlatteningConfigs(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      MethodConfigProto methodConfigProto,
      MethodModel methodModel,
      ProtoParser protoParser) {
    boolean missing = false;
    // Enforce unique flattening configs, in case proto annotations overlaps with configProto
    // flattening.
    Map<String, FlatteningConfig> flatteningConfigs = new LinkedHashMap<>();

    for (FlatteningGroupProto flatteningGroup : methodConfigProto.getFlattening().getGroupsList()) {
      FlatteningConfig groupConfig =
          FlatteningConfig.createFlatteningFromConfigProto(
              diagCollector,
              messageConfigs,
              resourceNameConfigs,
              methodConfigProto,
              flatteningGroup,
              methodModel);
      if (groupConfig == null) {
        missing = true;
      } else {
        flatteningConfigs.put(flatteningConfigToString(groupConfig), groupConfig);
      }
    }
    if (missing) {
      return null;
    }

    // Get flattenings from protofile annotations, let these override flattenings from GAPIC config.
    if (methodModel instanceof ProtoMethodModel) {
      ProtoMethodModel protoMethodModel = (ProtoMethodModel) methodModel;
      List<MethodSignature> methodSignatures =
          protoParser.getMethodSignatures(protoMethodModel);
      for (MethodSignature signature : methodSignatures) {
        if (signature.getFieldsCount() == 0) {
          break;
        }
        FlatteningConfig groupConfig =
            FlatteningConfig.createFlatteningFromProtoFile(
                diagCollector,
                messageConfigs,
                resourceNameConfigs,
                signature,
                protoMethodModel,
                protoParser);
        if (groupConfig == null) {
          missing = true;
        } else {
          flatteningConfigs.put(flatteningConfigToString(groupConfig), groupConfig);
        }
      }
      if (missing) {
        return null;
      }
    }

    return ImmutableList.copyOf(flatteningConfigs.values());
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
      if (defaultResourceNameTreatment == null
          || defaultResourceNameTreatment.equals(ResourceNameTreatment.UNSET_TREATMENT)) {
        defaultResourceNameTreatment = ResourceNameTreatment.VALIDATE;
      }

      FieldConfig fieldConfig =
          FieldConfig.createFieldConfig(
              diagCollector,
              messageConfigs,
              methodConfigProto.getFieldNamePatternsMap(),
              resourceNameConfigs,
              parameterField,
              flatteningGroup.getParameterResourceNameTreatmentMap().get(parameter),
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

  /**
   * Creates an instance of FlatteningConfig based on a FlatteningGroupProto, linking it up with the
   * provided method.
   */
  @Nullable
  private static FlatteningConfig createFlatteningFromProtoFile(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      MethodSignature methodSignature,
      ProtoMethodModel method,
      ProtoParser protoParser) {

    ImmutableMap.Builder<String, FieldConfig> flattenedFieldConfigBuilder = ImmutableMap.builder();
    Set<String> oneofNames = new HashSet<>();
    if (methodSignature == null || methodSignature.getFieldsCount() == 0) return null;

    List<String> flattenedParams = Lists.newArrayList(methodSignature.getFieldsList());
    for (String parameter : flattenedParams) {

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

      ResourceNameTreatment resourceNameTreatment = ResourceNameTreatment.NONE;
      String resourceNameType =
          protoParser.getResourceType(parameterField.getProtoField());
      if (!Strings.isNullOrEmpty(resourceNameType)) {
        resourceNameTreatment = ResourceNameTreatment.STATIC_TYPES;
      }
      FieldConfig fieldConfig =
          FieldConfig.createMessageFieldConfig(
              messageConfigs, resourceNameConfigs, parameterField, resourceNameTreatment);
      flattenedFieldConfigBuilder.put(parameter, fieldConfig);
    }
    return new AutoValue_FlatteningConfig(
        flattenedFieldConfigBuilder.build(), methodSignature.getFunctionName());
  }

  public Iterable<FieldModel> getFlattenedFields() {
    return FieldConfig.toFieldTypeIterable(getFlattenedFieldConfigs().values());
  }

  public FlatteningConfig withResourceNamesInSamplesOnly() {
    ImmutableMap<String, FieldConfig> newFlattenedFieldConfigs =
        getFlattenedFieldConfigs()
            .entrySet()
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Map.Entry::getKey, e -> e.getValue().withResourceNameInSampleOnly()));
    return new AutoValue_FlatteningConfig(newFlattenedFieldConfigs, getFlatteningName());
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
}
