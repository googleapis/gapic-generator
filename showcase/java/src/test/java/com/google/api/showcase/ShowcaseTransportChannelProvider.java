/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.api.showcase;

import com.google.api.gax.grpc.GrpcHeaderInterceptor;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.api.gax.rpc.TransportChannel;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.ManagedChannelBuilder;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

/** Creates the transport for the showcase test suite */
public class ShowcaseTransportChannelProvider implements TransportChannelProvider {

  private final String host;
  private final int port;
  private final HeaderProvider headerProvider;

  public ShowcaseTransportChannelProvider(String host, int port, HeaderProvider headerProvider) {
    this.host = host;
    this.port = port;
    this.headerProvider = headerProvider;
  }

  @Override
  public boolean shouldAutoClose() {
    return false;
  }

  @Override
  public boolean needsExecutor() {
    return false;
  }

  @Override
  public TransportChannelProvider withExecutor(ScheduledExecutorService executor) {
    return this;
  }

  @Override
  public boolean needsHeaders() {
    return false;
  }

  @Override
  public TransportChannelProvider withHeaders(Map<String, String> headers) {
    return this;
  }

  @Override
  public boolean needsEndpoint() {
    return false;
  }

  @Override
  public TransportChannelProvider withEndpoint(String endpoint) {
    return this;
  }

  @Override
  public boolean acceptsPoolSize() {
    return false;
  }

  @Override
  public TransportChannelProvider withPoolSize(int size) {
    return this;
  }

  @Override
  public TransportChannel getTransportChannel() {
    GrpcHeaderInterceptor headerInterceptor =
        new GrpcHeaderInterceptor(headerProvider.getHeaders());

    ManagedChannelBuilder builder =
        ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .intercept(headerInterceptor)
            .userAgent(headerInterceptor.getUserAgentHeader())
            .executor(MoreExecutors.directExecutor());

    return GrpcTransportChannel.create(builder.build());
  }

  @Override
  public String getTransportName() {
    return "grpc";
  }
}
