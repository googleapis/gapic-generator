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
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.PageStreamingConfigProto;
import com.google.api.codegen.SurfaceTreatmentProto;
import com.google.api.codegen.VisibilityProto;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
  public abstract FlatteningConfig getFlattening();

  public abstract String getRetryCodesConfigName();

  public abstract String getRetrySettingsConfigName();

  public abstract Duration getTimeout();

  public abstract Iterable<Field> getRequiredFields();

  public abstract Iterable<Field> getOptionalFields();

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
      final MethodConfigProto methodConfigProto,
      Method method,
      ImmutableSet<String> retryCodesConfigNames,
      ImmutableSet<String> retryParamsConfigNames) {

    boolean error = false;

    PageStreamingConfig pageStreaming = null;
    if (!PageStreamingConfigProto.getDefaultInstance()
        .equals(methodConfigProto.getPageStreaming())) {
      pageStreaming =
          PageStreamingConfig.createPageStreaming(
              diagCollector, methodConfigProto.getPageStreaming(), method);
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

    FlatteningConfig flattening = null;
    if (!FlatteningConfigProto.getDefaultInstance().equals(methodConfigProto.getFlattening())) {
      flattening =
          FlatteningConfig.createFlattening(
              diagCollector, methodConfigProto.getFlattening(), method);
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

    List<String> requiredFieldNames = methodConfigProto.getRequiredFieldsList();
    ImmutableSet.Builder<Field> builder = ImmutableSet.builder();
    for (String fieldName : requiredFieldNames) {
      Field requiredField = method.getInputMessage().lookupField(fieldName);
      if (requiredField != null) {
        builder.add(requiredField);
      } else {
        Diag.error(
            SimpleLocation.TOPLEVEL,
            "Required field '%s' not found (in method %s)",
            fieldName,
            method.getFullName());
        error = true;
      }
    }
    Set<Field> requiredFields = builder.build();

    Iterable<Field> optionalFields =
        Iterables.filter(
            method.getInputType().getMessageType().getFields(),
            new Predicate<Field>() {
              @Override
              public boolean apply(Field input) {
                return !(methodConfigProto.getRequiredFieldsList().contains(input.getSimpleName()));
              }
            });

    ImmutableMap<String, String> fieldNamePatterns =
        ImmutableMap.copyOf(methodConfigProto.getFieldNamePatterns());

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
          requiredFields,
          optionalFields,
          bundling,
          hasRequestObjectMethod,
          fieldNamePatterns,
          sampleCodeInitFields,
          rerouteToGrpcInterface,
          visibility);
    }
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
    return getFlattening() != null;
  }

  /** Returns true if this method has bundling configured. */
  public boolean isBundling() {
    return getBundling() != null;
  }
}
