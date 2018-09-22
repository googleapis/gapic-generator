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

import com.google.api.codegen.BatchingConfigProto;
import com.google.api.codegen.FlatteningConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.PageStreamingConfigProto;
import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.SurfaceTreatmentProto;
import com.google.api.codegen.VisibilityProto;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.configgen.ProtoMethodTransformer;
import com.google.api.codegen.transformer.RetryDefinitionsTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
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
import org.threeten.bp.Duration;

/**
 * GapicMethodConfig represents the code-gen config for a method, and includes the specification of
 * features like page streaming and parameter flattening.
 */
@AutoValue
public abstract class GapicMethodConfig extends MethodConfig {

  public Method getMethod() {
    return ((ProtoMethodModel) getMethodModel()).getProtoMethod();
  }

  public abstract Iterable<String> getHeaderRequestParams();

  /**
   * Creates an instance of GapicMethodConfig based on MethodConfigProto, linking it up with the
   * provided method. On errors, null will be returned, and diagnostics are reported to the diag
   * collector.
   */
  @Nullable
  static GapicMethodConfig createMethodConfig(
      DiagCollector diagCollector,
      TargetLanguage language,
      MethodConfigProto methodConfigProto,
      Method method,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      RetryCodesConfig retryCodesConfig,
      ImmutableSet<String> retryParamsConfigNames) {

    boolean error = false;
    ProtoMethodModel methodModel = new ProtoMethodModel(method);

    PageStreamingConfig pageStreaming = null;
    if (!PageStreamingConfigProto.getDefaultInstance()
        .equals(methodConfigProto.getPageStreaming())) {
      pageStreaming =
          PageStreamingConfig.createPageStreaming(
              diagCollector, messageConfigs, resourceNameConfigs, methodConfigProto, methodModel);
      if (pageStreaming == null) {
        error = true;
      }
    }

    GrpcStreamingConfig grpcStreaming = null;
    if (isGrpcStreamingMethod(methodModel)) {
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
      flattening =
          createFlattening(
              diagCollector, messageConfigs, resourceNameConfigs, methodConfigProto, methodModel);
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

    String retryCodesName = retryCodesConfig.getMethodRetryNames().get(method.getSimpleName());

    String retryParamsName =
        RetryDefinitionsTransformer.getRetryParamsName(
            methodConfigProto, diagCollector, retryParamsConfigNames);
    error |= (retryParamsName == null);

    Duration timeout = Duration.ofMillis(methodConfigProto.getTimeoutMillis());
    if (timeout.toMillis() <= 0) {
      timeout = Duration.ofMillis(ProtoMethodTransformer.getTimeoutMillis(methodModel));
    }
    if (timeout.toMillis() <= 0) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Default timeout not found or has invalid value (in method %s)",
              methodModel.getFullName()));
      error = true;
    }

    boolean hasRequestObjectMethod = methodConfigProto.getRequestObjectMethod();
    if (hasRequestObjectMethod && method.getRequestStreaming()) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "request_object_method incompatible with streaming method %s",
              method.getFullName()));
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

    ImmutableList<FieldConfig> requiredFieldConfigs =
        createFieldNameConfigs(
            diagCollector,
            messageConfigs,
            defaultResourceNameTreatment,
            fieldNamePatterns,
            resourceNameConfigs,
            getRequiredFields(
                diagCollector, methodModel, methodConfigProto.getRequiredFieldsList()));

    ImmutableList<FieldConfig> optionalFieldConfigs =
        createFieldNameConfigs(
            diagCollector,
            messageConfigs,
            defaultResourceNameTreatment,
            fieldNamePatterns,
            resourceNameConfigs,
            getOptionalFields(methodModel, methodConfigProto.getRequiredFieldsList()));
    if (diagCollector.getErrorCount() > 0) {
      return null;
    }

    List<String> sampleCodeInitFields = new ArrayList<>();
    sampleCodeInitFields.addAll(methodConfigProto.getSampleCodeInitFieldsList());
    SampleSpec sampleSpec = new SampleSpec(methodConfigProto);

    String rerouteToGrpcInterface =
        Strings.emptyToNull(methodConfigProto.getRerouteToGrpcInterface());

    VisibilityConfig visibility = VisibilityConfig.PUBLIC;
    ReleaseLevel releaseLevel = ReleaseLevel.GA;
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

    LongRunningConfig longRunningConfig =
        LongRunningConfig.createLongRunningConfig(
            method,
            diagCollector,
            methodConfigProto.getLongRunning(),
            ProtoParser.getProtoParser());
    if (diagCollector.getErrorCount() > 0) {
      error = true;
    }

    List<String> headerRequestParams =
        ImmutableList.copyOf(methodConfigProto.getHeaderRequestParamsList());

    if (error) {
      return null;
    } else {
      return new AutoValue_GapicMethodConfig(
          methodModel,
          pageStreaming,
          grpcStreaming,
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
          sampleSpec,
          rerouteToGrpcInterface,
          visibility,
          releaseLevel,
          longRunningConfig,
          headerRequestParams);
    }
  }

  /** Return the list of "one of" instances associated with the fields. */
  @Override
  public ImmutableList<ImmutableList<String>> getOneofNames(SurfaceNamer namer) {
    return ProtoField.getOneofFieldsNames(getOptionalFields(), namer);
  }
}
