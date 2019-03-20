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

import static com.google.api.codegen.common.TargetLanguage.JAVA;
import static com.google.api.codegen.common.TargetLanguage.NODEJS;
import static com.google.api.codegen.common.TargetLanguage.PHP;
import static com.google.api.codegen.common.TargetLanguage.PYTHON;
import static com.google.api.codegen.common.TargetLanguage.RUBY;

import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.GrpcStreamingConfig;
import com.google.api.codegen.config.MethodContext;
import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.util.List;

/**
 * The different calling forms we wish to illustrate in samples. Not every method type will have
 * every calling form, and a calling form for a given method type need not be applicable to every
 * language.
 */
public enum CallingForm {

  // By convention, the names of these enum values should be
  // concatenations of the following descriptive parts (some of which may
  // be empty), in order:
  //
  // [Method signature type][Request pattern][Response pattern][Idiomatic pattern]

  Request, // used by:  java nodejs php py ruby
  RequestAsync, // used by: 
  RequestAsyncPaged, // used by: nodejs
  RequestAsyncPagedAll, // used by: nodejs
  RequestPaged, // used by:  java php py ruby
  RequestPagedAll, // used by:  php py ruby
  RequestStreamingBidi, // used by:  nodejs php py ruby
  RequestStreamingBidiAsync, // used by: php
  RequestStreamingClient, // used by:  nodejs php py ruby
  RequestStreamingClientAsync, // used by: php
  RequestStreamingServer, // used by:  nodejs php py ruby

  Flattened, // used by:  java
  FlattenedPaged, // used by:  java
  FlattenedAsync,
  FlattenedAsyncPaged,

  Callable, // used by: java
  CallableList, // used by: java
  CallablePaged, // used by: java
  CallableStreamingBidi, // used by: java
  CallableStreamingClient, // used by: java
  CallableStreamingServer, // used by: java

  LongRunningCallable, // used by: java
  LongRunningEventEmitter, // used by: nodejs
  LongRunningFlattened, // used by: 
  LongRunningFlattenedAsync, // used by: , java
  LongRunningPromise, // used by: nodejs py
  LongRunningPromiseAwait, // used by: nodejs
  LongRunningRequest, // used by:  php
  LongRunningRequestAsync, // used by:  java php ruby

  // Used only if code does not yet support deciding on one of the other ones. The goal is to have
  // this value never set.
  Generic;

  private static enum RpcType {
    UNARY,
    LRO,
    CLIENT_STREAMING,
    SERVER_STREAMING,
    BIDI_STREAMING,
    PAGED_STREAMING;

    static RpcType fromMethodContext(MethodContext context) {
      if (context.getMethodConfig().isPageStreaming()) {
        return PAGED_STREAMING;
      }
      if (context.isLongRunningMethodContext()) {
        return LRO;
      }
      if (context.getMethodConfig().isGrpcStreaming()) {
        GrpcStreamingConfig.GrpcStreamingType streamingType =
            context.getMethodConfig().getGrpcStreamingType();
        switch (streamingType) {
          case BidiStreaming:
            return BIDI_STREAMING;
          case ClientStreaming:
            return CLIENT_STREAMING;
          case ServerStreaming:
            return SERVER_STREAMING;
          case NonStreaming:
            return UNARY;
          default:
            throw new IllegalArgumentException(
                "Illegal MethodContext: unhandled streaming type: " + streamingType);
        }
      }
      return UNARY;
    }
  }

  // TODO: Factor this out to a yaml file
  private static final Table<TargetLanguage, RpcType, List<CallingForm>> CALLING_FORM_TABLE =
      ImmutableTable.<TargetLanguage, RpcType, List<CallingForm>>builder()
          .put(JAVA, RpcType.UNARY, ImmutableList.of(Request, Flattened, Callable))
          .put(
              JAVA,
              RpcType.LRO,
              ImmutableList.of(
                  LongRunningRequest, LongRunningFlattenedAsync, LongRunningRequestAsync))
          .put(
              JAVA,
              RpcType.PAGED_STREAMING,
              ImmutableList.of(RequestPaged, RequestPagedAll, FlattenedPaged, CallableList))
          .put(JAVA, RpcType.CLIENT_STREAMING, ImmutableList.of(CallableStreamingClient))
          .put(JAVA, RpcType.SERVER_STREAMING, ImmutableList.of(CallableStreamingServer))
          .put(JAVA, RpcType.BIDI_STREAMING, ImmutableList.of(CallableStreamingBidi))
          .put(PYTHON, RpcType.UNARY, ImmutableList.of(Request))
          .put(PYTHON, RpcType.LRO, ImmutableList.of(LongRunningPromise))
          .put(PYTHON, RpcType.PAGED_STREAMING, ImmutableList.of(RequestPagedAll, RequestPaged))
          .put(PYTHON, RpcType.CLIENT_STREAMING, ImmutableList.of(RequestStreamingClient))
          .put(PYTHON, RpcType.SERVER_STREAMING, ImmutableList.of(RequestStreamingServer))
          .put(PYTHON, RpcType.BIDI_STREAMING, ImmutableList.of(RequestStreamingBidi))
          .put(PHP, RpcType.UNARY, ImmutableList.of(Request))
          .put(PHP, RpcType.LRO, ImmutableList.of(LongRunningRequest, LongRunningRequestAsync))
          .put(PHP, RpcType.PAGED_STREAMING, ImmutableList.of(RequestPaged, RequestPagedAll))
          .put(PHP, RpcType.CLIENT_STREAMING, ImmutableList.of(RequestStreamingClient))
          .put(PHP, RpcType.SERVER_STREAMING, ImmutableList.of(RequestStreamingServer))
          .put(PHP, RpcType.BIDI_STREAMING, ImmutableList.of(RequestStreamingBidi))
          .put(NODEJS, RpcType.UNARY, ImmutableList.of(Request))
          .put(NODEJS, RpcType.LRO, ImmutableList.of(LongRunningEventEmitter, LongRunningPromise))
          .put(NODEJS, RpcType.PAGED_STREAMING, ImmutableList.of(RequestPaged, RequestPagedAll))
          .put(NODEJS, RpcType.CLIENT_STREAMING, ImmutableList.of(RequestStreamingClient))
          .put(NODEJS, RpcType.SERVER_STREAMING, ImmutableList.of(RequestStreamingServer))
          .put(NODEJS, RpcType.BIDI_STREAMING, ImmutableList.of(RequestStreamingBidi))
          .put(RUBY, RpcType.UNARY, ImmutableList.of(Request))
          .put(RUBY, RpcType.LRO, ImmutableList.of(LongRunningRequestAsync))
          .put(RUBY, RpcType.PAGED_STREAMING, ImmutableList.of(RequestPagedAll, RequestPaged))
          .put(RUBY, RpcType.CLIENT_STREAMING, ImmutableList.of(RequestStreamingClient))
          .put(RUBY, RpcType.SERVER_STREAMING, ImmutableList.of(RequestStreamingServer))
          .put(RUBY, RpcType.BIDI_STREAMING, ImmutableList.of(RequestStreamingBidi))
          .build();

  /**
   * Returns the {@code String} representation of this enum, but in lower camelcase.
   *
   * @return the lower camelcase name of this enum value
   */
  public String toLowerCamel() {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, toString());
  }

  /** Returns the string representation of this enum, but in lower snake case. */
  public String toLowerUnderscore() {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, toString());
  }

  public static List<CallingForm> getCallingForms(
      MethodContext methodContext, TargetLanguage lang) {
    Preconditions.checkArgument(lang != TargetLanguage.CSHARP, "CSharp is not supported for now.");
    Preconditions.checkArgument(lang != TargetLanguage.GO, "Go is not supported for now.");
    return CALLING_FORM_TABLE.get(lang, RpcType.fromMethodContext(methodContext));
  }
}
