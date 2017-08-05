/* Copyright 2017 Google Inc
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

import com.google.api.codegen.BatchingConfigProto;
import com.google.api.codegen.FlatteningConfigProto;
import com.google.api.codegen.FlatteningGroupProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.PageStreamingConfigProto;
import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.SurfaceTreatmentProto;
import com.google.api.codegen.VisibilityProto;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.discovery.Schema;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.joda.time.Duration;

/**
 * GapicMethodConfig represents the code-gen config for a Discovery doc method, and includes the
 * specification of features like page streaming and parameter flattening.
 */
@AutoValue
public abstract class DiscoGapicMethodConfig extends MethodConfig {
  public Method getMethod() {
    return ((DiscoveryMethodModel) getMethodModel()).getDiscoveryMethod();
  }

  @Override
  public boolean isGrpcStreaming() {
    return false;
  }

  @Override
  public String getRerouteToGrpcInterface() {
    return null;
  }

  @Override
  /* Returns the grpc streaming configuration of the method. */
  public GrpcStreamingType getGrpcStreamingType() {
    return GrpcStreamingType.NonStreaming;
  }

  @Nullable
  @Override
  public GrpcStreamingConfig getGrpcStreaming() {
    return null;
  }

  /**
   * Creates an instance of DiscoGapicMethodConfig based on MethodConfigProto, linking it up with
   * the provided method. On errors, null will be returned, and diagnostics are reported to the diag
   * collector.
   */
  @Nullable
  static DiscoGapicMethodConfig createDiscoGapicMethodConfig(
      DiagCollector diagCollector,
      String language,
      MethodConfigProto methodConfigProto,
      Method method,
      ImmutableSet<String> retryCodesConfigNames,
      ImmutableSet<String> retryParamsConfigNames) {

    boolean error = false;

    PageStreamingConfig pageStreaming = null;
    if (!PageStreamingConfigProto.getDefaultInstance()
        .equals(methodConfigProto.getPageStreaming())) {
      pageStreaming = PageStreamingConfig.createPageStreaming(diagCollector, method);
      if (pageStreaming == null) {
        error = true;
      }
    }

    ImmutableList<FlatteningConfig> flattening = null;
    if (!FlatteningConfigProto.getDefaultInstance().equals(methodConfigProto.getFlattening())) {
      flattening = createFlattening(diagCollector, methodConfigProto, method);
      if (flattening == null) {
        error = true;
      }
    }

    BatchingConfig batching = null;
    if (!BatchingConfigProto.getDefaultInstance().equals(methodConfigProto.getBatching())) {
      batching =
          BatchingConfig.createBatching(
              diagCollector, methodConfigProto.getBatching(), new DiscoveryMethodModel(method));
      if (batching == null) {
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
              method.id()));
      error = true;
    }

    String retryParamsName = methodConfigProto.getRetryParamsName();
    if (!retryParamsConfigNames.isEmpty() && !retryParamsConfigNames.contains(retryParamsName)) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Retry parameters config used but not defined: %s (in method %s)",
              retryParamsName,
              method.id()));
      error = true;
    }

    Duration timeout = Duration.millis(methodConfigProto.getTimeoutMillis());
    if (timeout.getMillis() <= 0) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Default timeout not found or has invalid value (in method %s)",
              method.id()));
      error = true;
    }

    boolean hasRequestObjectMethod = methodConfigProto.getRequestObjectMethod();

    ImmutableMap<String, String> fieldNamePatterns =
        ImmutableMap.copyOf(methodConfigProto.getFieldNamePatterns());

    ResourceNameTreatment defaultResourceNameTreatment =
        methodConfigProto.getResourceNameTreatment();
    if (defaultResourceNameTreatment == null
        || defaultResourceNameTreatment.equals(ResourceNameTreatment.UNSET_TREATMENT)) {
      defaultResourceNameTreatment = ResourceNameTreatment.NONE;
    }

    Iterable<FieldConfig> requiredFieldConfigs =
        DiscoGapicMethodConfig.createFieldNameConfigs(
            DiscoGapicMethodConfig.getRequiredFields(
                diagCollector, method, methodConfigProto.getRequiredFieldsList()));

    Iterable<FieldConfig> optionalFieldConfigs =
        DiscoGapicMethodConfig.createFieldNameConfigs(
            DiscoGapicMethodConfig.getOptionalFields(
                method, methodConfigProto.getRequiredFieldsList()));

    List<String> sampleCodeInitFields = new ArrayList<>();
    sampleCodeInitFields.addAll(methodConfigProto.getRequiredFieldsList());
    sampleCodeInitFields.addAll(methodConfigProto.getSampleCodeInitFieldsList());

    VisibilityConfig visibility = VisibilityConfig.PUBLIC;
    ReleaseLevel releaseLevel = ReleaseLevel.ALPHA;
    for (SurfaceTreatmentProto treatment : methodConfigProto.getSurfaceTreatmentsList()) {
      if (!treatment.getIncludeLanguagesList().contains(language)) {
        continue;
      }
      if (treatment.getVisibility() != VisibilityProto.UNSET_VISIBILITY) {
        visibility = VisibilityConfig.fromProto(treatment.getVisibility());
      }
      if (treatment.getReleaseLevel() != ReleaseLevel.UNSET_RELEASE_LEVEL) {
        releaseLevel = treatment.getReleaseLevel();
      }
    }

    LongRunningConfig longRunningConfig = null;

    if (error) {
      return null;
    } else {
      return new AutoValue_DiscoGapicMethodConfig(
          new DiscoveryMethodModel(method),
          pageStreaming,
          flattening,
          retryCodesName,
          retryParamsName,
          timeout,
          requiredFieldConfigs,
          optionalFieldConfigs,
          defaultResourceNameTreatment,
          batching,
          hasRequestObjectMethod,
          fieldNamePatterns,
          sampleCodeInitFields,
          visibility,
          releaseLevel,
          longRunningConfig);
    }
  }

  @Nullable
  private static ImmutableList<FlatteningConfig> createFlattening(
      DiagCollector diagCollector, MethodConfigProto methodConfigProto, Method method) {
    boolean missing = false;
    ImmutableList.Builder<FlatteningConfig> flatteningGroupsBuilder = ImmutableList.builder();
    for (FlatteningGroupProto flatteningGroup : methodConfigProto.getFlattening().getGroupsList()) {
      FlatteningConfig groupConfig =
          FlatteningConfig.createFlattening(diagCollector, flatteningGroup, method);
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

  private static Iterable<Schema> getRequiredFields(
      DiagCollector diagCollector,
      com.google.api.codegen.discovery.Method method,
      List<String> requiredFieldNames) {
    ImmutableList.Builder<Schema> fieldsBuilder = ImmutableList.builder();
    for (String fieldName : requiredFieldNames) {
      Schema requiredField = method.parameters().get(fieldName);
      if (requiredField == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Required field '%s' not found (in method %s)",
                fieldName,
                method.id()));
        return null;
      }
      fieldsBuilder.add(requiredField);
    }
    return fieldsBuilder.build();
  }

  private static Iterable<Schema> getOptionalFields(
      com.google.api.codegen.discovery.Method method, List<String> requiredFieldNames) {
    ImmutableList.Builder<Schema> fieldsBuilder = ImmutableList.builder();
    for (Schema field : method.parameters().values()) {
      if (requiredFieldNames.contains(field.getIdentifier())) {
        continue;
      }
      fieldsBuilder.add(field);
    }
    return fieldsBuilder.build();
  }

  private static Iterable<FieldConfig> createFieldNameConfigs(Iterable<Schema> fields) {
    ImmutableList.Builder<FieldConfig> fieldConfigsBuilder = ImmutableList.builder();
    for (Schema field : fields) {
      fieldConfigsBuilder.add(FieldConfig.createFieldConfig(field));
    }
    return fieldConfigsBuilder.build();
  }

  @Override
  /* Return the list of "one of" instances associated with the fields. */
  public Iterable<Iterable<String>> getOneofsNames() {
    return ImmutableList.of();
  }
}
