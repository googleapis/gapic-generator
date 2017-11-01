/* Copyright 2016 Google LLC
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
package com.google.api.codegen.viewmodel;

import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;

/** The concrete callable class type */
public enum ApiCallableImplType {
  SimpleApiCallable(ServiceMethodType.UnaryMethod),
  PagedApiCallable(ServiceMethodType.UnaryMethod),
  BatchingApiCallable(ServiceMethodType.UnaryMethod),
  BidiStreamingApiCallable(ServiceMethodType.GrpcBidiStreamingMethod),
  ServerStreamingApiCallable(ServiceMethodType.GrpcServerStreamingMethod),
  ClientStreamingApiCallable(ServiceMethodType.GrpcClientStreamingMethod),
  OperationApiCallable(ServiceMethodType.LongRunningMethod);

  private ServiceMethodType serviceMethodType;

  ApiCallableImplType(ServiceMethodType serviceMethodType) {
    this.serviceMethodType = serviceMethodType;
  }

  public ServiceMethodType serviceMethodType() {
    return serviceMethodType;
  }

  public static ApiCallableImplType of(GrpcStreamingType streamingType) {
    switch (streamingType) {
      case BidiStreaming:
        return BidiStreamingApiCallable;
      case ServerStreaming:
        return ServerStreamingApiCallable;
      case ClientStreaming:
        return ClientStreamingApiCallable;
      default:
        throw new IllegalArgumentException("Invalid gRPC streaming type: " + streamingType);
    }
  }
}
