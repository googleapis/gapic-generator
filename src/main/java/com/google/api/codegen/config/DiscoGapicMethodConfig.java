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

import com.google.api.codegen.BatchingConfigProto;
import com.google.api.codegen.FlatteningConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.PageStreamingConfigProto;
import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.SurfaceTreatmentProto;
import com.google.api.codegen.VisibilityProto;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.configgen.transformer.DiscoveryMethodTransformer;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.transformer.RetryDefinitionsTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.threeten.bp.Duration;

/**
 * GapicMethodConfig represents the code-gen config for a Discovery doc method, and includes the
 * specification of features like page streaming and parameter flattening.
 */
@AutoValue
public abstract class DiscoGapicMethodConfig extends MethodConfig {

  @Override
  public abstract DiscoveryMethodModel getMethodModel();

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
      DiscoApiModel apiModel,
      TargetLanguage language,
      MethodConfigProto methodConfigProto,
      Method method,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      RetryCodesConfig retryCodesConfig,
      ImmutableSet<String> retryParamsConfigNames) {

    boolean error = false;
    DiscoveryMethodModel methodModel = new DiscoveryMethodModel(method, apiModel);
    DiagCollector diagCollector = apiModel.getDiagCollector();

    PageStreamingConfig pageStreaming = null;
    if (!PageStreamingConfigProto.getDefaultInstance()
        .equals(methodConfigProto.getPageStreaming())) {
      pageStreaming =
          PageStreamingConfig.createPageStreamingFromGapicConfig(
              diagCollector, messageConfigs, resourceNameConfigs, methodConfigProto, methodModel);
      if (pageStreaming == null) {
        error = true;
      }
    }

    ImmutableList<FlatteningConfig> flattening = null;
    if (!FlatteningConfigProto.getDefaultInstance().equals(methodConfigProto.getFlattening())) {
      flattening =
          FlatteningConfig.createFlatteningConfigs(
              diagCollector,
              messageConfigs,
              resourceNameConfigs,
              methodConfigProto,
              methodModel,
              language);
      if (flattening == null) {
        error = true;
      }
    }

    BatchingConfig batching = null;
    if (!BatchingConfigProto.getDefaultInstance().equals(methodConfigProto.getBatching())) {
      batching =
          BatchingConfig.createBatching(
              diagCollector, methodConfigProto.getBatching(), methodModel);
      if (batching == null) {
        error = true;
      }
    }

    String retryCodesName = retryCodesConfig.getMethodRetryNames().get(methodConfigProto.getName());

    String retryParamsName =
        RetryDefinitionsTransformer.getRetryParamsName(
            methodConfigProto, diagCollector, retryParamsConfigNames);
    error |= (retryParamsName == null);

    Duration timeout = Duration.ofMillis(methodConfigProto.getTimeoutMillis());
    if (timeout.toMillis() <= 0) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Default timeout not found or has invalid value (in method %s)",
              methodModel.getFullName()));
      error = true;
    }

    ImmutableMap<String, String> fieldNamePatterns =
        ImmutableMap.copyOf(methodConfigProto.getFieldNamePatterns());

    ResourceNameTreatment defaultResourceNameTreatment =
        methodConfigProto.getResourceNameTreatment();
    if (defaultResourceNameTreatment == null
        || defaultResourceNameTreatment.equals(ResourceNameTreatment.UNSET_TREATMENT)) {
      defaultResourceNameTreatment = ResourceNameTreatment.NONE;
    }

    List<String> requiredFieldsList = Lists.newArrayList(methodConfigProto.getRequiredFieldsList());
    if (methodModel.hasExtraFieldMask()) {
      requiredFieldsList.add(DiscoveryMethodTransformer.FIELDMASK_STRING);
    }
    ImmutableList<FieldConfig> requiredFieldConfigs =
        createFieldNameConfigs(
            diagCollector,
            messageConfigs,
            defaultResourceNameTreatment,
            fieldNamePatterns,
            resourceNameConfigs,
            getRequiredFields(diagCollector, methodModel, requiredFieldsList));

    ImmutableList<FieldConfig> optionalFieldConfigs =
        createFieldNameConfigs(
            diagCollector,
            messageConfigs,
            defaultResourceNameTreatment,
            fieldNamePatterns,
            resourceNameConfigs,
            getOptionalFields(methodModel, requiredFieldsList));

    List<String> sampleCodeInitFields = new ArrayList<>();
    sampleCodeInitFields.addAll(methodConfigProto.getSampleCodeInitFieldsList());
    SampleSpec sampleSpec = new SampleSpec(methodConfigProto);

    VisibilityConfig visibility = VisibilityConfig.PUBLIC;
    ReleaseLevel releaseLevel = ReleaseLevel.ALPHA;
    for (SurfaceTreatmentProto treatment : methodConfigProto.getSurfaceTreatmentsList()) {
      if (!treatment.getIncludeLanguagesList().contains(language.toString().toLowerCase())) {
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
          pageStreaming,
          flattening,
          retryCodesName,
          retryParamsName,
          timeout,
          requiredFieldConfigs,
          optionalFieldConfigs,
          defaultResourceNameTreatment,
          batching,
          fieldNamePatterns,
          sampleCodeInitFields,
          sampleSpec,
          visibility,
          releaseLevel,
          longRunningConfig,
          methodModel);
    }
  }

  /* Return the list of "one of" instances associated with the fields. */
  @Override
  public ImmutableList<ImmutableList<String>> getOneofNames(SurfaceNamer namer) {
    return ImmutableList.of();
  }
}
