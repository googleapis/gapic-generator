/* Copyright 2017 Google LLC
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

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import javax.annotation.Nullable;
import org.threeten.bp.Duration;

/**
 * MethodConfig represents the code-gen config for a method, and includes the specification of
 * features like page streaming and parameter flattening.
 *
 * <p>Subclasses should have a field to contain the method for which this a config.
 */
public abstract class MethodConfig {

  public abstract MethodModel getMethodModel();

  @Nullable
  public abstract PageStreamingConfig getPageStreaming();

  @Nullable
  public abstract GrpcStreamingConfig getGrpcStreaming();

  @Nullable
  public abstract ImmutableList<FlatteningConfig> getFlatteningConfigs();

  public abstract String getRetryCodesConfigName();

  public abstract String getRetrySettingsConfigName();

  public abstract Duration getTimeout();

  public abstract ImmutableList<FieldConfig> getRequiredFieldConfigs();

  public abstract ImmutableList<FieldConfig> getOptionalFieldConfigs();

  public abstract ResourceNameTreatment getDefaultResourceNameTreatment();

  @Nullable
  public abstract BatchingConfig getBatching();

  public abstract ImmutableMap<String, String> getFieldNamePatterns();

  public abstract List<String> getSampleCodeInitFields();

  public abstract SampleSpec getSampleSpec();

  @Nullable
  public abstract String getRerouteToGrpcInterface();

  public abstract VisibilityConfig getVisibility();

  public abstract ReleaseLevel getReleaseLevel();

  /**
   * package-private for internal use.
   *
   * <p>{@link MethodContext#getLongRunningConfig} wraps this call, because there are times where we
   * want to generate the same API method as an LRO method and also as a non-LRO method.
   */
  @Nullable
  abstract LongRunningConfig getLroConfig();

  /* package-private for internal use. */
  boolean hasLroConfig() {
    return getLroConfig() != null;
  }

  public abstract MethodConfig asNonLroConfig();

  /** Returns true if the method is a streaming method */
  public static boolean isGrpcStreamingMethod(MethodModel method) {
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

  /** Returns true if this method has batching configured. */
  public boolean isBatching() {
    return getBatching() != null;
  }

  public ImmutableList<FieldModel> getRequiredFields() {
    return getRequiredFieldConfigs()
        .stream()
        .map(FieldConfig::getField)
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<FieldModel> getOptionalFields() {
    return getOptionalFieldConfigs()
        .stream()
        .map(FieldConfig::getField)
        .collect(ImmutableList.toImmutableList());
  }

  /** Return the lists of the simple names of the "one of" instances associated with the fields. */
  public abstract ImmutableList<ImmutableList<String>> getOneofNames(SurfaceNamer namer);

  static Iterable<FieldModel> getOptionalFields(
      MethodModel method, List<String> requiredFieldNames) {
    ImmutableList.Builder<FieldModel> fieldsBuilder = ImmutableList.builder();
    for (FieldModel field : method.getInputFields()) {
      if (requiredFieldNames.contains(field.getSimpleName())) {
        continue;
      }
      fieldsBuilder.add(field);
    }
    return fieldsBuilder.build();
  }

  static Iterable<FieldModel> getRequiredFields(
      DiagCollector diagCollector, MethodModel method, List<String> requiredFieldNames) {
    ImmutableList.Builder<FieldModel> fieldsBuilder = ImmutableList.builder();
    for (String fieldName : requiredFieldNames) {
      FieldModel requiredField = method.getInputField(fieldName);
      if (requiredField == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Required field '%s' not found (in method %s)",
                fieldName,
                method.getFullName()));
        return null;
      } else if (requiredField.getOneof() != null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "oneof field %s cannot be required (in method %s)",
                fieldName,
                method.getFullName()));
        return null;
      }
      fieldsBuilder.add(requiredField);
    }
    return fieldsBuilder.build();
  }

  static ImmutableList<FieldConfig> createFieldNameConfigs(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ResourceNameTreatment defaultResourceNameTreatment,
      ImmutableMap<String, String> fieldNamePatterns,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      Iterable<FieldModel> fields) {
    ImmutableList.Builder<FieldConfig> fieldConfigsBuilder = ImmutableList.builder();
    for (FieldModel field : fields) {
      fieldConfigsBuilder.add(
          FieldConfig.createFieldConfig(
              diagCollector,
              messageConfigs,
              fieldNamePatterns,
              resourceNameConfigs,
              field,
              null,
              defaultResourceNameTreatment));
    }
    return fieldConfigsBuilder.build();
  }
}
