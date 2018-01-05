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

import com.google.api.codegen.PageStreamingConfigProto;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import javax.annotation.Nullable;

/** GrpcStreamingConfig represents the gRPC streaming configuration for a method. */
public class GrpcStreamingConfig {

  /** Grpc streaming types */
  public enum GrpcStreamingType {
    NonStreaming,
    ClientStreaming,
    ServerStreaming,
    BidiStreaming
  }

  private final FieldModel resourcesField;
  private final GrpcStreamingType type;

  /**
   * Creates an instance of GrpcStreamingConfig for gRPC response streaming, based on
   * PageStreamingConfigProto, linking it up with the provided method. On errors, null will be
   * returned, and diagnostics are reported to the diag collector.
   *
   * <p>TODO: Create a separate GrpcStreamingConfigProto
   */
  @Nullable
  public static GrpcStreamingConfig createGrpcStreaming(
      DiagCollector diagCollector, PageStreamingConfigProto pageStreaming, Method method) {
    String resourcesFieldName = pageStreaming.getResponse().getResourcesField();
    Field resourcesField = method.getOutputType().getMessageType().lookupField(resourcesFieldName);
    GrpcStreamingType type = getGrpcStreamingType(diagCollector, method);
    if (type == null) {
      return null;
    }
    return new GrpcStreamingConfig(resourcesField, type);
  }

  /**
   * Creates an instance of GrpcStreamingConfig for gRPC response streaming from the given Grpc
   * method. On errors, null will be returned, and diagnostics are reported to the diag collector.
   */
  @Nullable
  public static GrpcStreamingConfig createGrpcStreaming(
      DiagCollector diagCollector, Method method) {
    GrpcStreamingType type = getGrpcStreamingType(diagCollector, method);
    if (type == null) {
      return null;
    }
    return new GrpcStreamingConfig(null, type);
  }

  private static GrpcStreamingType getGrpcStreamingType(
      DiagCollector diagCollector, Method method) {
    GrpcStreamingType type = null;
    if (method.getRequestStreaming() && method.getResponseStreaming()) {
      type = GrpcStreamingType.BidiStreaming;
    } else if (method.getResponseStreaming()) {
      type = GrpcStreamingType.ServerStreaming;
    } else if (method.getRequestStreaming()) {
      type = GrpcStreamingType.ClientStreaming;
    } else {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "The given grpc method does not support streaming: %s",
              method.getFullName()));
    }
    return type;
  }

  private GrpcStreamingConfig(Field resourcesField, GrpcStreamingType type) {
    this.resourcesField = resourcesField == null ? null : new ProtoField(resourcesField);
    this.type = type;
  }

  /** Returns true if the resource field is set. */
  public boolean hasResourceField() {
    return resourcesField != null;
  }

  /** Returns the field used in the response to hold the resource being returned. */
  public FieldModel getResourcesField() {
    return resourcesField;
  }

  /** Returns the streaming type. */
  public GrpcStreamingType getType() {
    return type;
  }
}
