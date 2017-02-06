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

import com.google.api.codegen.LongRunningConfigProto;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.model.TypeRef;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;
import org.joda.time.Duration;

/** LongRunningConfig represents the long-running operation configuration for a method. */
@AutoValue
public abstract class LongRunningConfig {

  /** Returns the message type returned from a completed operation. */
  public abstract TypeRef getReturnType();

  /** Returns the message type for the metadata field of an operation. */
  public abstract TypeRef getMetadataType();

  /** Reports whether or not the service implements delete. */
  public abstract boolean implementsDelete();

  /** Reports whether or not the service implements cancel. */
  public abstract boolean implementsCancel();

  /** Returns the polling interval of the operation. */
  @Nullable
  public abstract Duration getPollingInterval();

  /** Creates an instance of LongRunningConfig based on LongRunningConfigProto. */
  @Nullable
  public static LongRunningConfig createLongRunningConfig(
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
              longRunningConfigProto.getReturnType()));
      error = true;
    } else if (!metadataType.isMessage()) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Metadata type for long running config is not a message: '%s'",
              longRunningConfigProto.getReturnType()));
      error = true;
    }

    Duration pollingInterval = null;
    if (longRunningConfigProto.getPollingIntervalMillis() > 0) {
      pollingInterval = Duration.millis(longRunningConfigProto.getPollingIntervalMillis());
    }

    if (error) {
      return null;
    } else {
      return new AutoValue_LongRunningConfig(
          returnType,
          metadataType,
          longRunningConfigProto.getImplementsDelete(),
          longRunningConfigProto.getImplementsCancel(),
          pollingInterval);
    }
  }
}
