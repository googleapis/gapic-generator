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

import com.google.api.codegen.LongRunningConfigProto;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.model.TypeRef;
import com.google.auto.value.AutoValue;
import com.google.longrunning.OperationInfo;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.threeten.bp.Duration;

/** LongRunningConfig represents the long-running operation configuration for a method. */
@AutoValue
public abstract class LongRunningConfig {

  // Default values for LongRunningConfig fields.
  static final Duration LRO_INITIAL_POLL_DELAY_MILLIS = Duration.ofMillis(500);
  static final double LRO_POLL_DELAY_MULTIPLIER = 1.5;
  static final Duration LRO_MAX_POLL_DELAY_MILLIS = Duration.ofMillis(5000);
  static final Duration LRO_TOTAL_POLL_TIMEOUT_MILLS = Duration.ofMillis(300000);

  /** Returns the message type returned from a completed operation. */
  public abstract TypeModel getReturnType();

  /** Returns the message type for the metadata field of an operation. */
  public abstract TypeModel getMetadataType();

  /** Returns initial delay after which first poll request will be made. */
  public abstract Duration getInitialPollDelay();

  /**
   * Returns multiplier used to gradually increase delay between subsequent polls until it reaches
   * maximum poll delay.
   */
  public abstract double getPollDelayMultiplier();

  /** Returns maximum time between two subsequent poll requests. */
  public abstract Duration getMaxPollDelay();

  /** Returns total polling timeout. */
  public abstract Duration getTotalPollTimeout();

  private static String qualifyLroTypeName(
      String typeName, Method method, ProtoParser protoParser) {
    if (!typeName.contains(".")) {
      typeName = String.format("%s.%s", protoParser.getProtoPackage(method), typeName);
    }
    return typeName;
  }

  /**
   * Creates an instance of LongRunningConfig based on protofile annotations. If there is matching
   * long running config from GAPIC config, use the GAPIC config's timeout values.
   */
  @Nullable
  static LongRunningConfig createLongRunningConfig(
      Method method,
      DiagCollector diagCollector,
      @Nonnull LongRunningConfigProto longRunningConfigProto,
      ProtoParser protoParser) {
    int preexistingErrors = diagCollector.getErrorCount();

    Model model = method.getModel();
    OperationInfo operationTypes = protoParser.getLongRunningOperation(method);
    if (operationTypes == null
        || operationTypes.equals(operationTypes.getDefaultInstanceForType())) {
      return null;
    }

    String responseTypeName =
        qualifyLroTypeName(operationTypes.getResponseType(), method, protoParser);
    String metadataTypeName =
        qualifyLroTypeName(operationTypes.getMetadataType(), method, protoParser);

    TypeRef returnType = model.getSymbolTable().lookupType(responseTypeName);
    TypeRef metadataType = model.getSymbolTable().lookupType(metadataTypeName);

    if (returnType == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Type not found for long running config: '%s'",
              responseTypeName));
    } else if (!returnType.isMessage()) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Type for long running config is not a message: '%s'",
              responseTypeName));
    }

    if (metadataType == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Metadata type not found for long running config: '%s'",
              metadataTypeName));
    } else if (!metadataType.isMessage()) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Metadata type for long running config is not a message: '%s'",
              metadataTypeName));
    }

    if (diagCollector.getErrorCount() - preexistingErrors > 0) {
      return null;
    }

    LongRunningConfig.Builder builder =
        getGapicConfigLroRetrySettingsOrDefault(diagCollector, longRunningConfigProto);

    return builder
        .setReturnType(ProtoTypeRef.create(returnType))
        .setMetadataType(ProtoTypeRef.create(metadataType))
        .build();
  }

  /**
   * Creates an instance of LongRunningConfig.Builder based on a method's GAPIC config's LRO polling
   * settings.
   */
  private static LongRunningConfig.Builder getGapicConfigLroRetrySettingsOrDefault(
      DiagCollector diagCollector, LongRunningConfigProto longRunningConfigProto) {
    if (longRunningConfigProto.equals(LongRunningConfigProto.getDefaultInstance())) {
      return newBuilder()
          .setInitialPollDelay(LRO_INITIAL_POLL_DELAY_MILLIS)
          .setPollDelayMultiplier(LRO_POLL_DELAY_MULTIPLIER)
          .setMaxPollDelay(LRO_MAX_POLL_DELAY_MILLIS)
          .setTotalPollTimeout(LRO_TOTAL_POLL_TIMEOUT_MILLS);
    }

    Duration initialPollDelay =
        Duration.ofMillis(longRunningConfigProto.getInitialPollDelayMillis());
    if (initialPollDelay.compareTo(Duration.ZERO) < 0) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Initial poll delay must be provided and set to a positive number: '%s'",
              longRunningConfigProto.getInitialPollDelayMillis()));
    }

    double pollDelayMultiplier = longRunningConfigProto.getPollDelayMultiplier();
    if (pollDelayMultiplier < 1.0) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Poll delay multiplier must be provided and be greater or equal than 1.0: '%s'",
              longRunningConfigProto.getPollDelayMultiplier()));
    }

    Duration maxPollDelay = Duration.ofMillis(longRunningConfigProto.getMaxPollDelayMillis());
    if (maxPollDelay.compareTo(initialPollDelay) < 0) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Max poll delay must be provided and set be equal or greater than initial poll delay: '%s'",
              longRunningConfigProto.getMaxPollDelayMillis()));
    }

    Duration totalPollTimeout =
        Duration.ofMillis(longRunningConfigProto.getTotalPollTimeoutMillis());
    if (totalPollTimeout.compareTo(maxPollDelay) < 0) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Total poll timeout must be provided and be be equal or greater than max poll delay: '%s'",
              longRunningConfigProto.getTotalPollTimeoutMillis()));
    }
    return newBuilder()
        .setInitialPollDelay(initialPollDelay)
        .setPollDelayMultiplier(pollDelayMultiplier)
        .setMaxPollDelay(maxPollDelay)
        .setTotalPollTimeout(totalPollTimeout);
  }

  /** Creates an instance of LongRunningConfig based on LongRunningConfigProto. */
  @Nullable
  static LongRunningConfig createLongRunningConfigFromGapicConfigOnly(
      Model model, DiagCollector diagCollector, LongRunningConfigProto longRunningConfigProto) {
    if (LongRunningConfigProto.getDefaultInstance().equals(longRunningConfigProto)) {
      return null;
    }

    int preexistingErrors = diagCollector.getErrorCount();

    TypeRef returnType = model.getSymbolTable().lookupType(longRunningConfigProto.getReturnType());
    TypeRef metadataType =
        model.getSymbolTable().lookupType(longRunningConfigProto.getMetadataType());

    if (returnType == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Type not found for long running config: '%s'",
              longRunningConfigProto.getReturnType()));
    } else if (!returnType.isMessage()) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Type for long running config is not a message: '%s'",
              longRunningConfigProto.getReturnType()));
    }

    if (metadataType == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Metadata type not found for long running config: '%s'",
              longRunningConfigProto.getMetadataType()));
    } else if (!metadataType.isMessage()) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Metadata type for long running config is not a message: '%s'",
              longRunningConfigProto.getMetadataType()));
    }

    if (diagCollector.getErrorCount() - preexistingErrors > 0) {
      return null;
    }

    LongRunningConfig.Builder builder =
        getGapicConfigLroRetrySettingsOrDefault(diagCollector, longRunningConfigProto);

    return builder
        .setReturnType(ProtoTypeRef.create(returnType))
        .setMetadataType(ProtoTypeRef.create(metadataType))
        .build();
  }

  private static Builder newBuilder() {
    return new AutoValue_LongRunningConfig.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    public abstract Builder setReturnType(TypeModel val);

    public abstract Builder setMetadataType(TypeModel val);

    public abstract Builder setInitialPollDelay(Duration val);

    public abstract Builder setPollDelayMultiplier(double val);

    public abstract Builder setMaxPollDelay(Duration val);

    public abstract Builder setTotalPollTimeout(Duration val);

    public abstract LongRunningConfig build();
  }
}
