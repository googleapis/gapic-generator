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

import com.google.api.codegen.BundlingConfigProto;
import com.google.api.codegen.FlatteningConfigProto;
import com.google.api.codegen.FlatteningGroupProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.PageStreamingConfigProto;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.SurfaceTreatmentProto;
import com.google.api.codegen.VisibilityProto;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.joda.time.Duration;

/**
 * MethodConfig represents the code-gen config for a method, and includes the specification of
 * features like page streaming and parameter flattening.
 */
@AutoValue
public abstract class MethodConfig {
  public abstract Method getMethod();

  @Nullable
  public abstract PageStreamingConfig getPageStreaming();

  @Nullable
  public abstract GrpcStreamingConfig getGrpcStreaming();

  @Nullable
  public abstract ImmutableList<FlatteningConfig> getFlatteningConfigs();

  public abstract String getRetryCodesConfigName();

  public abstract String getRetrySettingsConfigName();

  public abstract Duration getTimeout();

  public abstract Iterable<FieldConfig> getRequiredFieldConfigs();

  public abstract Iterable<FieldConfig> getOptionalFieldConfigs();

  public abstract ResourceNameTreatment getDefaultResourceNameTreatment();

  @Nullable
  public abstract BundlingConfig getBundling();

  public abstract boolean hasRequestObjectMethod();

  public abstract ImmutableMap<String, String> getFieldNamePatterns();

  public abstract List<String> getSampleCodeInitFields();

  @Nullable
  public abstract String getRerouteToGrpcInterface();

  public abstract VisibilityConfig getVisibility();

  /**
   * Creates an instance of MethodConfig based on MethodConfigProto, linking it up with the provided
   * method. On errors, null will be returned, and diagnostics are reported to the diag collector.
   */
  @Nullable
  public static MethodConfig createMethodConfig(
      DiagCollector diagCollector,
      String language,
      MethodConfigProto methodConfigProto,
      Method method,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableSet<String> retryCodesConfigNames,
      ImmutableSet<String> retryParamsConfigNames) {

    boolean error = false;

    PageStreamingConfig pageStreaming = null;
    if (!PageStreamingConfigProto.getDefaultInstance()
        .equals(methodConfigProto.getPageStreaming())) {
      pageStreaming =
          PageStreamingConfig.createPageStreaming(
              diagCollector, messageConfigs, methodConfigProto, method);
      if (pageStreaming == null) {
        error = true;
      }
    }

    GrpcStreamingConfig grpcStreaming = null;
    if (isGrpcStreamingMethod(method)) {
      if (PageStreamingConfigProto.getDefaultInstance()
          .equals(methodConfigProto.getGrpcStreaming())) {
        grpcStreaming = GrpcStreamingConfig.createGrpcStreaming(diagCollector, method);
      } else {
        grpcStreaming =
            GrpcStreamingConfig.createGrpcStreaming(
                diagCollector, methodConfigProto.getGrpcStreaming(), method);
        if (grpcStreaming == null) {
          error = true;
        }
      }
    }

    ImmutableList<FlatteningConfig> flattening = null;
    if (!FlatteningConfigProto.getDefaultInstance().equals(methodConfigProto.getFlattening())) {
      flattening = createFlattening(diagCollector, messageConfigs, methodConfigProto, method);
      if (flattening == null) {
        error = true;
      }
    }

    BundlingConfig bundling = null;
    if (!BundlingConfigProto.getDefaultInstance().equals(methodConfigProto.getBundling())) {
      bundling =
          BundlingConfig.createBundling(diagCollector, methodConfigProto.getBundling(), method);
      if (bundling == null) {
        error = true;
      }
    }

    String retryCodesName = methodConfigProto.getRetryCodesName();
    if (!retryCodesName.isEmpty() && !retryCodesConfigNames.contains(retryCodesName)) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Retry codes config used but not defined: '%s' (in method %s)",
              retryCodesName,
              method.getFullName()));
      error = true;
    }

    String retryParamsName = methodConfigProto.getRetryParamsName();
    if (!retryParamsConfigNames.isEmpty() && !retryParamsConfigNames.contains(retryParamsName)) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Retry parameters config used but not defined: %s (in method %s)",
              retryParamsName,
              method.getFullName()));
      error = true;
    }

    Duration timeout = Duration.millis(methodConfigProto.getTimeoutMillis());
    if (timeout.getMillis() <= 0) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Default timeout not found or has invalid value (in method %s)",
              method.getFullName()));
      error = true;
    }

    boolean hasRequestObjectMethod = methodConfigProto.getRequestObjectMethod();

    ImmutableMap<String, String> fieldNamePatterns =
        ImmutableMap.copyOf(methodConfigProto.getFieldNamePatterns());

    Iterable<FieldConfig> requiredFieldConfigs =
        createRequiredFieldNameConfigs(
            method,
            messageConfigs,
            methodConfigProto.getResourceNameTreatment(),
            fieldNamePatterns,
            methodConfigProto.getRequiredFieldsList());

    Iterable<FieldConfig> optionalFieldConfigs =
        createOptionalFieldNameConfigs(
            method,
            messageConfigs,
            methodConfigProto.getResourceNameTreatment(),
            fieldNamePatterns,
            methodConfigProto.getRequiredFieldsList());

    ResourceNameTreatment defaultResourceNameTreatment =
        methodConfigProto.getResourceNameTreatment();

    List<String> sampleCodeInitFields = new ArrayList<>();
    sampleCodeInitFields.addAll(methodConfigProto.getRequiredFieldsList());
    sampleCodeInitFields.addAll(methodConfigProto.getSampleCodeInitFieldsList());

    String rerouteToGrpcInterface =
        Strings.emptyToNull(methodConfigProto.getRerouteToGrpcInterface());

    VisibilityConfig visibility = VisibilityConfig.PUBLIC;
    for (SurfaceTreatmentProto treatment : methodConfigProto.getSurfaceTreatmentsList()) {
      if (!treatment.getIncludeLanguagesList().contains(language)) {
        continue;
      }
      if (treatment.getVisibility() != VisibilityProto.UNSET) {
        visibility = VisibilityConfig.fromProto(treatment.getVisibility());
      }
    }

    if (error) {
      return null;
    } else {
      return new AutoValue_MethodConfig(
          method,
          pageStreaming,
          grpcStreaming,
          flattening,
          retryCodesName,
          retryParamsName,
          timeout,
          requiredFieldConfigs,
          optionalFieldConfigs,
          defaultResourceNameTreatment,
          bundling,
          hasRequestObjectMethod,
          fieldNamePatterns,
          sampleCodeInitFields,
          rerouteToGrpcInterface,
          visibility);
    }
  }

  @Nullable
  private static ImmutableList<FlatteningConfig> createFlattening(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      MethodConfigProto methodConfigProto,
      Method method) {
    boolean missing = false;
    ImmutableList.Builder<FlatteningConfig> flatteningGroupsBuilder = ImmutableList.builder();
    for (FlatteningGroupProto flatteningGroup : methodConfigProto.getFlattening().getGroupsList()) {
      FlatteningConfig groupConfig =
          FlatteningConfig.createFlattening(
              diagCollector, messageConfigs, methodConfigProto, flatteningGroup, method);
      if (groupConfig == null) {
        missing = true;
      } else {
        flatteningGroupsBuilder.add(groupConfig);
      }
    }
    if (missing) {
      return null;
    }

    return flatteningGroupsBuilder.build();
  }

  private static Iterable<FieldConfig> createRequiredFieldNameConfigs(
      Method method,
      ResourceNameMessageConfigs messageConfigs,
      ResourceNameTreatment defaultResourceNameTreatment,
      ImmutableMap<String, String> fieldNamePatterns,
      List<String> requiredFieldNames) {
    ImmutableList.Builder<FieldConfig> builder = ImmutableList.builder();
    for (String fieldName : requiredFieldNames) {
      Field requiredField = method.getInputMessage().lookupField(fieldName);
      if (requiredField != null) {
        builder.add(
            getFieldConfig(
                messageConfigs, fieldNamePatterns, requiredField, defaultResourceNameTreatment));
      } else {
        Diag.error(
            SimpleLocation.TOPLEVEL,
            "Required field '%s' not found (in method %s)",
            fieldName,
            method.getFullName());
        return null;
      }
    }
    return builder.build();
  }

  private static Iterable<FieldConfig> createOptionalFieldNameConfigs(
      Method method,
      ResourceNameMessageConfigs messageConfigs,
      ResourceNameTreatment defaultResourceNameTreatment,
      ImmutableMap<String, String> fieldNamePatterns,
      List<String> requiredFieldNames) {
    ImmutableList.Builder<FieldConfig> optionalFieldConfigsBuilder = ImmutableList.builder();
    for (Field field : method.getInputType().getMessageType().getFields()) {
      if (requiredFieldNames.contains(field.getSimpleName())) {
        continue;
      }
      optionalFieldConfigsBuilder.add(
          getFieldConfig(messageConfigs, fieldNamePatterns, field, defaultResourceNameTreatment));
    }
    return optionalFieldConfigsBuilder.build();
  }

  private static FieldConfig getFieldConfig(
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, String> fieldNamePatterns,
      Field field,
      ResourceNameTreatment defaultResourceNameTreatment) {

    String entityName = FieldConfig.getEntityName(field, messageConfigs, fieldNamePatterns);
    ResourceNameTreatment treatment = defaultResourceNameTreatment;

    if (entityName == null || treatment == null) {
      treatment = ResourceNameTreatment.NONE;
    }

    if (treatment == ResourceNameTreatment.NONE) {
      entityName = null;
    }

    FieldConfig.validate(messageConfigs, field, treatment, entityName);

    return FieldConfig.createFieldConfig(field, treatment, entityName);
  }

  /** Returns true if the method is a streaming method */
  public static boolean isGrpcStreamingMethod(Method method) {
    return method.getRequestStreaming() || method.getResponseStreaming();
  }

  /** Returns true if this method has page streaming configured. */
  public boolean isPageStreaming() {
    return getPageStreaming() != null;
  }

  /** Returns true if this method has grpc streaming configured. */
  public boolean isGrpcStreaming() {
    return getGrpcStreaming() != null;
  }

  /** Returns the grpc streaming configuration of the method. */
  public GrpcStreamingType getGrpcStreamingType() {
    if (isGrpcStreaming()) {
      return getGrpcStreaming().getType();
    } else {
      return GrpcStreamingType.NonStreaming;
    }
  }

  /** Returns true if this method has flattening configured. */
  public boolean isFlattening() {
    return getFlatteningConfigs() != null;
  }

  /** Returns true if this method has bundling configured. */
  public boolean isBundling() {
    return getBundling() != null;
  }

  public Iterable<Field> getRequiredFields() {
    return FieldConfig.toFieldIterable(getRequiredFieldConfigs());
  }

  public Iterable<Field> getOptionalFields() {
    return FieldConfig.toFieldIterable(getOptionalFieldConfigs());
  }
}
