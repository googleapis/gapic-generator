/* Copyright 2018 Google LLC
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
package com.google.api.codegen.viewmodel;

import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import java.util.EnumSet;

/* View of the GrpcStreamingConfig.GrpcStreamingType */
public class GrpcStreamingView {

  private final GrpcStreamingType grpcStreamingType;

  public GrpcStreamingView(GrpcStreamingType grpcStreamingType) {
    this.grpcStreamingType = grpcStreamingType;
  }

  public String type() {
    return grpcStreamingType.toString();
  }

  public boolean isSingularRequest() {
    return EnumSet.of(GrpcStreamingType.NonStreaming, GrpcStreamingType.ServerStreaming)
        .contains(grpcStreamingType);
  }

  public boolean isRequestStreaming() {
    return EnumSet.of(GrpcStreamingType.ClientStreaming, GrpcStreamingType.BidiStreaming)
        .contains(grpcStreamingType);
  }

  public boolean isResponseStreaming() {
    return EnumSet.of(GrpcStreamingType.ServerStreaming, GrpcStreamingType.BidiStreaming)
        .contains(grpcStreamingType);
  }

  public boolean isStreaming() {
    return EnumSet.of(
            GrpcStreamingType.ClientStreaming,
            GrpcStreamingType.ServerStreaming,
            GrpcStreamingType.BidiStreaming)
        .contains(grpcStreamingType);
  }
}
