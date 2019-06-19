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

import static com.google.api.codegen.common.TargetLanguage.CSHARP;
import static com.google.api.codegen.common.TargetLanguage.JAVA;
import static com.google.api.codegen.common.TargetLanguage.NODEJS;
import static com.google.api.codegen.common.TargetLanguage.PHP;
import static com.google.api.codegen.common.TargetLanguage.PYTHON;
import static com.google.api.codegen.common.TargetLanguage.RUBY;

import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.GrpcStreamingConfig;
import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.SampleSpec;
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

  Request, // used by: csharp java nodejs php py ruby
  RequestAsync, // used by: csharp
  RequestAsyncPaged, // used by: csharp nodejs
  RequestAsyncPagedAll, // used by: csharp nodejs
  RequestAsyncPagedPageSize, // used by: csharp
  RequestPaged, // used by: csharp java php py ruby
  RequestPagedAll, // used by: csharp php py ruby
  RequestPagedPageSize, // used by: csharp
  RequestStreamingBidi, // used by: nodejs php py ruby
  RequestStreamingBidiAsync, // used by: php
  RequestStreamingClient, // used by: nodejs php py ruby
  RequestStreamingClientAsync, // used by: php
  RequestStreamingServer, // used by: nodejs php py ruby

  Flattened, // used by: csharp java
  FlattenedPaged, // used by: csharp java
  FlattenedPagedAll, // used by: csharp
  FlattenedPagedPageSize, // used by: csharp
  FlattenedAsync, // used by: csharp
  FlattenedAsyncPaged, // used by: csharp
  FlattenedAsyncPagedAll, // used by: csharp
  FlattenedAsyncPagedPageSize, // used by: csharp

  Callable, // used by: java
  CallableList, // used by: java
  CallablePaged, // used by: java
  CallableStreamingBidi, // used by: java
  CallableStreamingClient, // used by: java
  CallableStreamingServer, // used by: java

  LongRunningCallable, // used by: java
  LongRunningEventEmitter, // used by: nodejs
  LongRunningFlattened,
  LongRunningFlattenedAsync, // used by: java
  LongRunningPromise, // used by: nodejs py
  LongRunningPromiseAwait, // used by: nodejs
  LongRunningRequest, // used by: php
  LongRunningRequestAsync, // used by: java php ruby
  LongRunningStartThenCancel, // used by: java nodejs php py ruby

  // TODO: the following calling forms should be added for csharp. They are
  // currently removed to turn off generating samples in these calling forms
  // so that baseline do not explode
  // Flattened,
  // RequestStreamingBidi,
  // RequestStreamingServer,
  // FlattenedStreamingBidi,
  // FlattenedStreamingServer,
  // LongRunningFlattenedPollUntilComplete,
  // LongRunningFlattenedPollLater,
  // LongRunningFlattenedAsyncPollUntilComplete,
  // LongRunningFlattenedAsyncPollLater,
  // LongRunningRequestPollUntilComplete,
  // LongRunningRequestPollLater,
  // LongRunningRequestAsyncPollUntilComplete,
  // LongRunningRequestAsyncPollLater,

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

  // Note: in some languages we generate in-code samples based off calling forms as well.
  // But we don't always want to generate in-code samples in all calling forms that
  // we generate standalone samples in.
  private static final Table<TargetLanguage, RpcType, List<CallingForm>>
      INCODE_CALLING_FORM_OVERRIDE_TABLE =
          ImmutableTable.<TargetLanguage, RpcType, List<CallingForm>>builder()
              .put(
                  NODEJS,
                  RpcType.LRO,
                  ImmutableList.of(
                      LongRunningPromise, LongRunningEventEmitter, LongRunningPromiseAwait))
              .put(PYTHON, RpcType.LRO, ImmutableList.of(LongRunningPromise))
              .put(PHP, RpcType.LRO, ImmutableList.of(LongRunningRequest, LongRunningRequestAsync))
              .build();

  // TODO: Factor this out to a yaml file
  private static final Table<TargetLanguage, RpcType, List<CallingForm>> CALLING_FORM_TABLE =
      ImmutableTable.<TargetLanguage, RpcType, List<CallingForm>>builder()
          .put(JAVA, RpcType.UNARY, ImmutableList.of(Request, Flattened, Callable))
          .put(
              JAVA,
              RpcType.LRO,
              ImmutableList.of(
                  LongRunningFlattenedAsync, LongRunningRequestAsync, LongRunningStartThenCancel))
          .put(
              JAVA,
              RpcType.PAGED_STREAMING,
              ImmutableList.of(RequestPaged, RequestPagedAll, FlattenedPaged, CallableList))
          .put(JAVA, RpcType.CLIENT_STREAMING, ImmutableList.of(CallableStreamingClient))
          .put(JAVA, RpcType.SERVER_STREAMING, ImmutableList.of(CallableStreamingServer))
          .put(JAVA, RpcType.BIDI_STREAMING, ImmutableList.of(CallableStreamingBidi))
          .put(PYTHON, RpcType.UNARY, ImmutableList.of(Request))
          .put(
              PYTHON, RpcType.LRO, ImmutableList.of(LongRunningPromise, LongRunningStartThenCancel))
          .put(PYTHON, RpcType.PAGED_STREAMING, ImmutableList.of(RequestPagedAll, RequestPaged))
          .put(PYTHON, RpcType.CLIENT_STREAMING, ImmutableList.of(RequestStreamingClient))
          .put(PYTHON, RpcType.SERVER_STREAMING, ImmutableList.of(RequestStreamingServer))
          .put(PYTHON, RpcType.BIDI_STREAMING, ImmutableList.of(RequestStreamingBidi))
          .put(PHP, RpcType.UNARY, ImmutableList.of(Request))
          .put(
              PHP,
              RpcType.LRO,
              ImmutableList.of(
                  LongRunningRequest, LongRunningRequestAsync, LongRunningStartThenCancel))
          .put(PHP, RpcType.PAGED_STREAMING, ImmutableList.of(RequestPaged, RequestPagedAll))
          .put(PHP, RpcType.CLIENT_STREAMING, ImmutableList.of(RequestStreamingClient))
          .put(PHP, RpcType.SERVER_STREAMING, ImmutableList.of(RequestStreamingServer))
          .put(PHP, RpcType.BIDI_STREAMING, ImmutableList.of(RequestStreamingBidi))
          .put(NODEJS, RpcType.UNARY, ImmutableList.of(Request))
          .put(
              NODEJS,
              RpcType.LRO,
              ImmutableList.of(
                  LongRunningEventEmitter,
                  LongRunningPromise,
                  LongRunningStartThenCancel,
                  LongRunningPromiseAwait))
          .put(NODEJS, RpcType.PAGED_STREAMING, ImmutableList.of(RequestPaged, RequestPagedAll))
          .put(NODEJS, RpcType.CLIENT_STREAMING, ImmutableList.of(RequestStreamingClient))
          .put(NODEJS, RpcType.SERVER_STREAMING, ImmutableList.of(RequestStreamingServer))
          .put(NODEJS, RpcType.BIDI_STREAMING, ImmutableList.of(RequestStreamingBidi))
          .put(RUBY, RpcType.UNARY, ImmutableList.of(Request))
          .put(
              RUBY,
              RpcType.LRO,
              ImmutableList.of(LongRunningRequestAsync, LongRunningStartThenCancel))
          .put(RUBY, RpcType.PAGED_STREAMING, ImmutableList.of(RequestPagedAll, RequestPaged))
          .put(RUBY, RpcType.CLIENT_STREAMING, ImmutableList.of(RequestStreamingClient))
          .put(RUBY, RpcType.SERVER_STREAMING, ImmutableList.of(RequestStreamingServer))
          .put(RUBY, RpcType.BIDI_STREAMING, ImmutableList.of(RequestStreamingBidi))
          .build();

  private static final Table<TargetLanguage, RpcType, CallingForm> DEFAULT_CALLING_FORM_TABLE =
      ImmutableTable.<TargetLanguage, RpcType, CallingForm>builder()
          // TODO(hzyi): Change C# calling forms to appropriate ones after C# LRO and streaming are
          // done
          .put(CSHARP, RpcType.UNARY, Request)
          .put(CSHARP, RpcType.LRO, Generic)
          .put(CSHARP, RpcType.PAGED_STREAMING, RequestPagedAll)
          .put(CSHARP, RpcType.CLIENT_STREAMING, Generic)
          .put(CSHARP, RpcType.SERVER_STREAMING, Generic)
          .put(CSHARP, RpcType.BIDI_STREAMING, Generic)
          .put(JAVA, RpcType.UNARY, Request)
          .put(JAVA, RpcType.LRO, LongRunningRequestAsync)
          .put(JAVA, RpcType.PAGED_STREAMING, RequestPaged)
          .put(JAVA, RpcType.CLIENT_STREAMING, CallableStreamingClient)
          .put(JAVA, RpcType.SERVER_STREAMING, CallableStreamingServer)
          .put(JAVA, RpcType.BIDI_STREAMING, CallableStreamingBidi)
          .put(PYTHON, RpcType.UNARY, Request)
          .put(PYTHON, RpcType.LRO, LongRunningPromise)
          .put(PYTHON, RpcType.PAGED_STREAMING, RequestPagedAll)
          .put(PYTHON, RpcType.CLIENT_STREAMING, RequestStreamingClient)
          .put(PYTHON, RpcType.SERVER_STREAMING, RequestStreamingServer)
          .put(PYTHON, RpcType.BIDI_STREAMING, RequestStreamingBidi)
          .put(PHP, RpcType.UNARY, Request)
          .put(PHP, RpcType.LRO, LongRunningRequest)
          .put(PHP, RpcType.PAGED_STREAMING, RequestPagedAll)
          .put(PHP, RpcType.CLIENT_STREAMING, RequestStreamingClient)
          .put(PHP, RpcType.SERVER_STREAMING, RequestStreamingServer)
          .put(PHP, RpcType.BIDI_STREAMING, RequestStreamingBidi)
          .put(NODEJS, RpcType.UNARY, Request)
          .put(NODEJS, RpcType.LRO, LongRunningPromiseAwait)
          .put(NODEJS, RpcType.PAGED_STREAMING, RequestAsyncPagedAll)
          .put(NODEJS, RpcType.CLIENT_STREAMING, RequestStreamingClient)
          .put(NODEJS, RpcType.SERVER_STREAMING, RequestStreamingServer)
          .put(NODEJS, RpcType.BIDI_STREAMING, RequestStreamingBidi)
          .put(RUBY, RpcType.UNARY, Request)
          .put(RUBY, RpcType.LRO, LongRunningRequestAsync)
          .put(RUBY, RpcType.PAGED_STREAMING, RequestPagedAll)
          .put(RUBY, RpcType.CLIENT_STREAMING, RequestStreamingClient)
          .put(RUBY, RpcType.SERVER_STREAMING, RequestStreamingServer)
          .put(RUBY, RpcType.BIDI_STREAMING, RequestStreamingBidi)
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
      MethodContext methodContext, TargetLanguage lang, SampleSpec.SampleType sampleType) {
    Preconditions.checkArgument(lang != TargetLanguage.GO, "Go is not supported for now.");

    List<CallingForm> forms = null;
    if (sampleType == SampleSpec.SampleType.IN_CODE) {
      forms =
          INCODE_CALLING_FORM_OVERRIDE_TABLE.get(lang, RpcType.fromMethodContext(methodContext));
    }
    if (forms == null) {
      forms = CALLING_FORM_TABLE.get(lang, RpcType.fromMethodContext(methodContext));
    }
    return forms;
  }

  public static CallingForm getDefaultCallingForm(
      MethodContext methodContext, TargetLanguage lang) {
    Preconditions.checkArgument(lang != TargetLanguage.GO, "Go is not supported for now.");
    return DEFAULT_CALLING_FORM_TABLE.get(lang, RpcType.fromMethodContext(methodContext));
  }
}
