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

import com.google.api.OperationData;
import com.google.api.codegen.LongRunningConfigProto;
import com.google.api.codegen.discogapic.EmptyTypeModel;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.model.TypeRef;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import javax.annotation.Nullable;
import org.threeten.bp.Duration;

/** LongRunningConfig represents the long-running operation configuration for a method. */
@AutoValue
public abstract class LongRunningConfig {

  // Assume that a method with this return type is an LRO method.
  private static final String DISCOVERY_LRO_TYPE = "Operation";

  // Default values for LongRunningConfig fields.
  static final boolean LRO_IMPLEMENTS_CANCEL = true;
  static final boolean LRO_IMPLEMENTS_DELETE = true;
  static final int LRO_INITIAL_POLL_DELAY_MILLIS = 500;
  static final double LRO_POLL_DELAY_MULTIPLIER = 1.5;
  static final int LRO_MAX_POLL_DELAY_MILLIS = 5000;
  static final int LRO_TOTAL_POLL_TIMEOUT_MILLS = 300000;

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
        createLongRunningConfigFromProtoFile(
            method, diagCollector, longRunningConfigProto, protoParser);
    if (longRunningConfig != null) {
      return longRunningConfig;
    }

    if (!LongRunningConfigProto.getDefaultInstance().equals(longRunningConfigProto)) {
      return LongRunningConfig.createLongRunningConfigFromGapicConfig(
          method.getModel(), diagCollector, longRunningConfigProto);
    }
    return null;
  }

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
  private static LongRunningConfig createLongRunningConfigFromProtoFile(
      Method method,
      DiagCollector diagCollector,
      LongRunningConfigProto longRunningConfigProto,
      ProtoParser protoParser) {
    int preexistingErrors = diagCollector.getErrorCount();

    Model model = method.getModel();
    OperationData operationTypes = protoParser.getLongRunningOperation(method);
    if (operationTypes == null
        || operationTypes.equals(operationTypes.getDefaultInstanceForType())) {
      return null;
    }

    String responseTypeName =
        qualifyLroTypeName(operationTypes.getResponseType(), method, protoParser);
    String metadataTypeName =
        qualifyLroTypeName(operationTypes.getMetadataType(), method, protoParser);

    if (responseTypeName.equals(longRunningConfigProto.getReturnType())
        && metadataTypeName.equals(longRunningConfigProto.getMetadataType())) {
      // GAPIC config refers to the same Long running config; so use its retry settings.
      return LongRunningConfig.createLongRunningConfigFromGapicConfig(
          method.getModel(), diagCollector, longRunningConfigProto);
    }

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

    // Use default retry settings.
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

  /** Creates an instance of LongRunningConfig from a Discovery method. */
  @Nullable
  static LongRunningConfig createLongRunningConfig(DiscoveryMethodModel methodModel) {

    com.google.api.codegen.discovery.Method method = methodModel.getDiscoMethod();

    for (String part : method.id().split("\\.")) {
      if (DISCOVERY_LRO_TYPE.toLowerCase().equals(part.toLowerCase())) {
        // This is a method from the Operation Client itself; it isn't a method that *uses* LRO.
        return null;
      }
    }
    if (method.id().toLowerCase().contains(DISCOVERY_LRO_TYPE.toLowerCase())) {
      return null;
    }

    // LRO methods will have Operation as the return type.
    try {
      if (method.response() == null
          || Strings.isNullOrEmpty(method.response().reference())
          || !DISCOVERY_LRO_TYPE.equals(method.response().reference())) {
        return null;
      }
    } catch (NullPointerException e) {
      System.exit(1);
    }

    // Use default retry settings.
    Duration initialPollDelay = Duration.ofMillis(LRO_INITIAL_POLL_DELAY_MILLIS);
    Duration maxPollDelay = Duration.ofMillis(LRO_MAX_POLL_DELAY_MILLIS);
    Duration totalPollTimeout = Duration.ofMillis(LRO_TOTAL_POLL_TIMEOUT_MILLS);

    // Canceling is not available in Compute currently.
    boolean implementsCancel = false;
    boolean implementsDelete = true;

    TypeModel returnType = EmptyTypeModel.getInstance();
    // Let the return Operation type be the metadata type instead.
    TypeModel metadataType = methodModel.getOutputType();

    return new AutoValue_LongRunningConfig(
        returnType,
        metadataType,
        implementsCancel,
        implementsDelete,
        initialPollDelay,
        LRO_POLL_DELAY_MULTIPLIER,
        maxPollDelay,
        totalPollTimeout);
  }

  /** Creates an instance of LongRunningConfig based on LongRunningConfigProto. */
  @Nullable
  private static LongRunningConfig createLongRunningConfigFromGapicConfig(
      Model model, DiagCollector diagCollector, LongRunningConfigProto longRunningConfigProto) {

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
    if (pollDelayMultiplier <= 1.0) {
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

    if (diagCollector.getErrorCount() - preexistingErrors > 0) {
      return null;
    }

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
