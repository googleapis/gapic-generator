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

import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_MAX_RETRY_DELAY;

import com.google.api.codegen.BatchingConfigProto;
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
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
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
  private static GapicMethodConfig.Builder createCommonMethodConfig(
      DiagCollector diagCollector,
      TargetLanguage language,
      @Nonnull MethodConfigProto methodConfigProto,
      Method method,
      ProtoMethodModel methodModel,
      RetryCodesConfig retryCodesConfig,
      ImmutableSet<String> retryParamsConfigNames,
      GrpcGapicRetryMapping retryMapping,
      String gapicInterfaceName) {

    GrpcStreamingConfig grpcStreaming = null;
    if (isGrpcStreamingMethod(methodModel)) {
      if (PageStreamingConfigProto.getDefaultInstance()
          .equals(methodConfigProto.getGrpcStreaming())) {
        grpcStreaming = GrpcStreamingConfig.createGrpcStreaming(diagCollector, method);
      } else {
        grpcStreaming =
            GrpcStreamingConfig.createGrpcStreaming(
                diagCollector, methodConfigProto.getGrpcStreaming(), method);
      }
    }

    BatchingConfig batching = null;
    if (!BatchingConfigProto.getDefaultInstance().equals(methodConfigProto.getBatching())) {
      batching =
          BatchingConfig.createBatching(
              diagCollector, methodConfigProto.getBatching(), methodModel);
    }

    String retryCodesName;
    String retryParamsName;
    long defaultTimeout;
    if (retryMapping != null) {
      // use the gRPC ServiceConfig retry as the source
      retryCodesName = retryCodesConfig.getMethodRetryNames().get(methodModel.getSimpleName());

      // The retry_params are mapped to full-qualified names. If the GAPIC config uses the
      // reroute_to_grpc_interface option, we should use the GAPIC interface name to look up
      // the retry_params, because the retry config should name them using the same GAPIC interface
      // name.
      String fullName = methodModel.getFullName();
      if (!Strings.isNullOrEmpty(methodConfigProto.getRerouteToGrpcInterface())
          && !Strings.isNullOrEmpty(gapicInterfaceName)) {
        fullName = gapicInterfaceName + "." + methodModel.getSimpleName();
      }
      retryParamsName = retryMapping.methodParamsMap().get(fullName);

      // unknown/unspecified methods get no retry codes or params
      if (Strings.isNullOrEmpty(retryCodesName)) {
        retryCodesName = "no_retry_codes";
      }
      if (Strings.isNullOrEmpty(retryParamsName)) {
        retryParamsName = "no_retry_params";
      }

      // use the totalTimeoutMillis set by the gRPC ServiceConfig MethodConfig timeout field
      defaultTimeout = retryMapping.paramsDefMap().get(retryParamsName).getTotalTimeoutMillis();
    } else {
      retryCodesName = retryCodesConfig.getMethodRetryNames().get(method.getSimpleName());

      retryParamsName =
          RetryDefinitionsTransformer.getRetryParamsName(
              methodConfigProto, diagCollector, retryParamsConfigNames);

      defaultTimeout = methodConfigProto.getTimeoutMillis();
    }

    if (defaultTimeout <= 0) {
      defaultTimeout = DEFAULT_MAX_RETRY_DELAY;
    }
    long timeoutMillis = ProtoMethodTransformer.getTimeoutMillis(methodModel, defaultTimeout);

    Duration timeout = Duration.ofMillis(timeoutMillis);
    if (timeout.toMillis() <= 0) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Default timeout not found or has invalid value (in method %s)",
              methodModel.getFullName()));
    }

    List<String> sampleCodeInitFields =
        new ArrayList<>(methodConfigProto.getSampleCodeInitFieldsList());
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

    List<String> headerRequestParams = findHeaderRequestParams(method);

    return new AutoValue_GapicMethodConfig.Builder()
        .setMethodModel(methodModel)
        .setGrpcStreaming(grpcStreaming)
        .setRetryCodesConfigName(retryCodesName)
        .setRetrySettingsConfigName(retryParamsName)
        .setTimeout(timeout)
        .setBatching(batching)
        .setSampleCodeInitFields(sampleCodeInitFields)
        .setSampleSpec(sampleSpec)
        .setRerouteToGrpcInterface(rerouteToGrpcInterface)
        .setVisibility(visibility)
        .setReleaseLevel(releaseLevel)
        .setHeaderRequestParams(headerRequestParams);
  }

  @Nullable
  static GapicMethodConfig createGapicMethodConfigFromProto(
      DiagCollector diagCollector,
      TargetLanguage language,
      TransportProtocol transportProtocol,
      String defaultPackageName,
      @Nonnull MethodConfigProto methodConfigProto,
      Method method,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      RetryCodesConfig retryCodesConfig,
      ImmutableSet<String> retryParamsConfigNames,
      ProtoParser protoParser,
      GrpcGapicRetryMapping retryMapping,
      String gapicInterfaceName) {
    int previousErrors = diagCollector.getErrorCount();

    ProtoMethodModel methodModel = new ProtoMethodModel(method);
    ImmutableListMultimap<String, String> fieldNamePatterns =
        getFieldNamePatterns(method, messageConfigs);
    List<String> requiredFields = protoParser.getRequiredFields(method);
    ResourceNameTreatment defaultResourceNameTreatment = ResourceNameTreatment.UNSET_TREATMENT;
    GapicMethodConfig.Builder builder =
        createCommonMethodConfig(
                diagCollector,
                language,
                methodConfigProto,
                method,
                methodModel,
                retryCodesConfig,
                retryParamsConfigNames,
                retryMapping,
                gapicInterfaceName)
            .setPageStreaming(
                PageStreamingConfig.createPageStreamingConfig(
                    language,
                    transportProtocol,
                    diagCollector,
                    defaultPackageName,
                    methodModel,
                    methodConfigProto,
                    messageConfigs,
                    resourceNameConfigs,
                    protoParser))
            .setFlatteningConfigs(
                FlatteningConfig.createFlatteningConfigs(
                    diagCollector,
                    messageConfigs,
                    resourceNameConfigs,
                    methodConfigProto,
                    methodModel,
                    protoParser))
            .setFieldNamePatterns(fieldNamePatterns)
            .setRequiredFieldConfigs(
                createFieldNameConfigs(
                    diagCollector,
                    messageConfigs,
                    defaultResourceNameTreatment,
                    fieldNamePatterns,
                    resourceNameConfigs,
                    getRequiredFields(diagCollector, methodModel, requiredFields)))
            .setOptionalFieldConfigs(
                createFieldNameConfigs(
                    diagCollector,
                    messageConfigs,
                    defaultResourceNameTreatment,
                    fieldNamePatterns,
                    resourceNameConfigs,
                    getOptionalFields(methodModel, requiredFields)))
            .setLroConfig(
                LongRunningConfig.createLongRunningConfig(
                    method, diagCollector, methodConfigProto.getLongRunning(), protoParser));
    if (diagCollector.getErrorCount() - previousErrors > 0) {
      return null;
    } else {
      return builder.build();
    }
  }

  @Nullable
  static GapicMethodConfig createGapicMethodConfigFromGapicYaml(
      DiagCollector diagCollector,
      TargetLanguage language,
      @Nonnull MethodConfigProto methodConfigProto,
      Method method,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      RetryCodesConfig retryCodesConfig,
      ImmutableSet<String> retryParamsConfigNames) {
    int previousErrors = diagCollector.getErrorCount();

    ProtoMethodModel methodModel = new ProtoMethodModel(method);
    List<String> requiredFields = methodConfigProto.getRequiredFieldsList();
    ImmutableListMultimap<String, String> fieldNamePatterns =
        ImmutableListMultimap.copyOf(methodConfigProto.getFieldNamePatterns().entrySet());
    ResourceNameTreatment defaultResourceNameTreatment =
        methodConfigProto.getResourceNameTreatment();

    GapicMethodConfig.Builder builder =
        createCommonMethodConfig(
                diagCollector,
                language,
                methodConfigProto,
                method,
                methodModel,
                retryCodesConfig,
                retryParamsConfigNames,
                null,
                null)
            .setPageStreaming(
                PageStreamingConfig.createPageStreamingConfig(
                    diagCollector,
                    methodModel,
                    methodConfigProto,
                    messageConfigs,
                    resourceNameConfigs))
            .setFlatteningConfigs(
                FlatteningConfig.createFlatteningConfigs(
                    diagCollector,
                    messageConfigs,
                    resourceNameConfigs,
                    methodConfigProto,
                    methodModel))
            .setFieldNamePatterns(fieldNamePatterns)
            .setRequiredFieldConfigs(
                createFieldNameConfigs(
                    diagCollector,
                    messageConfigs,
                    defaultResourceNameTreatment,
                    fieldNamePatterns,
                    resourceNameConfigs,
                    getRequiredFields(diagCollector, methodModel, requiredFields)))
            .setOptionalFieldConfigs(
                createFieldNameConfigs(
                    diagCollector,
                    messageConfigs,
                    defaultResourceNameTreatment,
                    fieldNamePatterns,
                    resourceNameConfigs,
                    getOptionalFields(methodModel, requiredFields)))
            .setLroConfig(
                LongRunningConfig.createLongRunningConfigFromGapicConfigOnly(
                    method.getModel(), diagCollector, methodConfigProto.getLongRunning()));

    if (diagCollector.getErrorCount() - previousErrors > 0) {
      return null;
    } else {
      return builder.build();
    }
  }

  private static List<String> findHeaderRequestParams(Method method) {
    // Always parse header request params only from proto annotations, even if GAPIC config is
    // given.
    ProtoParser protoParser = new ProtoParser(true);
    return protoParser.getHeaderParams(method).asList();
  }

  @VisibleForTesting
  static ResourceNameTreatment defaultResourceNameTreatmentFromProto(
      Method method, ProtoParser protoParser, String defaultPackageName) {
    if (method.getInputMessage().getFields().stream().anyMatch(protoParser::hasResourceReference)) {
      String methodInputPackageName =
          protoParser.getProtoPackage(((ProtoFile) method.getInputMessage().getParent()));
      if (defaultPackageName.equals(methodInputPackageName)) {
        return ResourceNameTreatment.STATIC_TYPES;
      } else {
        return ResourceNameTreatment.VALIDATE;
      }
    } else {
      return ResourceNameTreatment.UNSET_TREATMENT;
    }
  }

  public static ImmutableListMultimap<String, String> getFieldNamePatterns(
      Method method, ResourceNameMessageConfigs messageConfigs) {
    ImmutableListMultimap.Builder<String, String> resultCollector = ImmutableListMultimap.builder();
    // Only look two levels deep in the request object, so fields of fields of the request object.
    getFieldNamePatterns(messageConfigs, method.getInputMessage(), resultCollector, "", 2);
    return resultCollector.build();
  }

  /**
   * Recursively populates the given map builder with field name patterns, up to a given depth.
   *
   * <p>A field name pattern entry maps a field name String, which can be a dot-separated nested
   * field such as "shelf.name", to the String name of the resource entity that is represented by
   * that field.
   *
   * <p>Note: this method does not check for circular references.
   *
   * @param messageConfigs ResourceNameMessageConfigs object
   * @param messageType the starting messageType from which to parse fields for resource names
   * @param resultCollector collects the resulting field name patterns
   * @param fieldNamePrefix a nested field is prefixed by the parents' names, dot-separated
   * @param depth number of levels deep in which to parse the messageType; must be positive int
   */
  private static void getFieldNamePatterns(
      ResourceNameMessageConfigs messageConfigs,
      MessageType messageType,
      ImmutableListMultimap.Builder<String, String> resultCollector,
      String fieldNamePrefix,
      int depth) {
    if (depth < 1) throw new IllegalStateException("depth must be positive");
    for (Field field : messageType.getFields()) {
      String fieldNameKey = fieldNamePrefix + field.getSimpleName();

      if (field.getType().isMessage() && depth > 1) {
        getFieldNamePatterns(
            messageConfigs,
            field.getType().getMessageType(),
            resultCollector,
            fieldNameKey + ".",
            depth - 1);
      }

      if (messageConfigs.fieldHasResourceName(messageType.getFullName(), field.getSimpleName())) {
        resultCollector.putAll(
            fieldNameKey,
            messageConfigs.getFieldResourceNames(messageType.getFullName(), field.getSimpleName()));
      }
    }
  }

  /** Return the list of "one of" instances associated with the fields. */
  @Override
  public ImmutableList<ImmutableList<String>> getOneofNames(SurfaceNamer namer) {
    return ProtoField.getOneofFieldsNames(getOptionalFields(), namer);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setHeaderRequestParams(Iterable<String> val);

    public abstract Builder setMethodModel(MethodModel val);

    public abstract Builder setPageStreaming(@Nullable PageStreamingConfig val);

    public abstract Builder setGrpcStreaming(@Nullable GrpcStreamingConfig val);

    public abstract Builder setFlatteningConfigs(@Nullable ImmutableList<FlatteningConfig> val);

    public abstract Builder setRetryCodesConfigName(String val);

    public abstract Builder setRetrySettingsConfigName(String val);

    public abstract Builder setTimeout(Duration val);

    public abstract Builder setRequiredFieldConfigs(ImmutableList<FieldConfig> val);

    public abstract Builder setOptionalFieldConfigs(ImmutableList<FieldConfig> val);

    public abstract Builder setBatching(@Nullable BatchingConfig val);

    public abstract Builder setFieldNamePatterns(ImmutableListMultimap<String, String> val);

    public abstract Builder setSampleCodeInitFields(List<String> val);

    public abstract Builder setSampleSpec(SampleSpec val);

    public abstract Builder setRerouteToGrpcInterface(@Nullable String val);

    public abstract Builder setVisibility(VisibilityConfig val);

    public abstract Builder setReleaseLevel(ReleaseLevel val);

    public abstract Builder setLroConfig(@Nullable LongRunningConfig val);

    public abstract GapicMethodConfig build();
  }
}
