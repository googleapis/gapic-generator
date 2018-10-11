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
import com.google.longrunning.OperationTypes;
import javax.annotation.Nullable;
import org.threeten.bp.Duration;

/** LongRunningConfig represents the long-running operation configuration for a method. */
@AutoValue
public abstract class LongRunningConfig {

  // Default values for LongRunningConfig fields.
  static final boolean LRO_IMPLEMENTS_CANCEL = true;
  static final boolean LRO_IMPLEMENTS_DELETE = true;
  static final int LRO_INITIAL_POLL_DELAY_MILLIS = 3000;
  static final double LRO_POLL_DELAY_MULTIPLIER = 1.3;
  static final int LRO_MAX_POLL_DELAY_MILLIS = 60000;
  static final int LRO_TOTAL_POLL_TIMEOUT_MILLS = 600000;

  /** Returns the message type returned from a completed operation. */
  public abstract TypeModel getReturnType();

  /** Returns the message type for the metadata field of an operation. */
  public abstract TypeModel getMetadataType();

  /** Reports whether or not the service implements delete. */
  public abstract boolean implementsDelete();

  /** Reports whether or not the service implements cancel. */
  public abstract boolean implementsCancel();

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

  @Nullable
  static LongRunningConfig createLongRunningConfig(
      Method method,
      DiagCollector diagCollector,
      LongRunningConfigProto longRunningConfigProto,
      ProtoParser protoParser) {
    LongRunningConfig longRunningConfig =
        createLongRunningConfigFromProtoFile(method, diagCollector, protoParser);
    if (longRunningConfig != null) {
      return longRunningConfig;
    }

    if (!LongRunningConfigProto.getDefaultInstance().equals(longRunningConfigProto)) {
      return LongRunningConfig.createLongRunningConfigFromGapicConfig(
          method.getModel(), diagCollector, longRunningConfigProto);
    }
    return null;
  }

  /** Creates an instance of LongRunningConfig based on protofile annotations. */
  @Nullable
  private static LongRunningConfig createLongRunningConfigFromProtoFile(
      Method method, DiagCollector diagCollector, ProtoParser protoParser) {

    Model model = method.getModel();
    OperationTypes operationTypes = protoParser.getLongRunningOperation(method);
    if (operationTypes == null
        || operationTypes.equals(operationTypes.getDefaultInstanceForType())) {
      return null;
    }

    String responseTypeName = operationTypes.getResponse();
    String metadataTypeName = operationTypes.getMetadata();

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

    if (diagCollector.getErrorCount() > 0) {
      return null;
    }

    Duration initialPollDelay = Duration.ofMillis(LRO_INITIAL_POLL_DELAY_MILLIS);
    Duration maxPollDelay = Duration.ofMillis(LRO_MAX_POLL_DELAY_MILLIS);
    Duration totalPollTimeout = Duration.ofMillis(LRO_TOTAL_POLL_TIMEOUT_MILLS);

    return new AutoValue_LongRunningConfig(
        ProtoTypeRef.create(returnType),
        ProtoTypeRef.create(metadataType),
        LRO_IMPLEMENTS_CANCEL,
        LRO_IMPLEMENTS_DELETE,
        initialPollDelay,
        LRO_POLL_DELAY_MULTIPLIER,
        maxPollDelay,
        totalPollTimeout);
  }

  /** Creates an instance of LongRunningConfig based on LongRunningConfigProto. */
  @Nullable
  private static LongRunningConfig createLongRunningConfigFromGapicConfig(
      Model model, DiagCollector diagCollector, LongRunningConfigProto longRunningConfigProto) {

    boolean error = false;

    TypeRef returnType = model.getSymbolTable().lookupType(longRunningConfigProto.getReturnType());
    TypeRef metadataType =
        model.getSymbolTable().lookupType(longRunningConfigProto.getMetadataType());

    if (returnType == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Type not found for long running config: '%s'",
              longRunningConfigProto.getReturnType()));
      error = true;
    } else if (!returnType.isMessage()) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Type for long running config is not a message: '%s'",
              longRunningConfigProto.getReturnType()));
      error = true;
    }

    if (metadataType == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Metadata type not found for long running config: '%s'",
              longRunningConfigProto.getMetadataType()));
      error = true;
    } else if (!metadataType.isMessage()) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Metadata type for long running config is not a message: '%s'",
              longRunningConfigProto.getMetadataType()));
      error = true;
    }

    Duration initialPollDelay =
        Duration.ofMillis(longRunningConfigProto.getInitialPollDelayMillis());
    if (initialPollDelay.compareTo(Duration.ZERO) < 0) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Initial poll delay must be provided and set to a positive number: '%s'",
              longRunningConfigProto.getInitialPollDelayMillis()));
      error = true;
    }

    double pollDelayMultiplier = longRunningConfigProto.getPollDelayMultiplier();
    if (pollDelayMultiplier <= 1.0) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Poll delay multiplier must be provided and be greater or equal than 1.0: '%s'",
              longRunningConfigProto.getPollDelayMultiplier()));
      error = true;
    }

    Duration maxPollDelay = Duration.ofMillis(longRunningConfigProto.getMaxPollDelayMillis());
    if (maxPollDelay.compareTo(initialPollDelay) < 0) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Max poll delay must be provided and set be equal or greater than initial poll delay: '%s'",
              longRunningConfigProto.getMaxPollDelayMillis()));
      error = true;
    }

    Duration totalPollTimeout =
        Duration.ofMillis(longRunningConfigProto.getTotalPollTimeoutMillis());
    if (totalPollTimeout.compareTo(maxPollDelay) < 0) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Total poll timeout must be provided and be be equal or greater than max poll delay: '%s'",
              longRunningConfigProto.getTotalPollTimeoutMillis()));
      error = true;
    }

    if (error) {
      return null;
    } else {
      return new AutoValue_LongRunningConfig(
          ProtoTypeRef.create(returnType),
          ProtoTypeRef.create(metadataType),
          longRunningConfigProto.getImplementsDelete(),
          longRunningConfigProto.getImplementsCancel(),
          initialPollDelay,
          pollDelayMultiplier,
          maxPollDelay,
          totalPollTimeout);
    }
  }
}
