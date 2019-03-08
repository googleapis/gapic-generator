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

import com.google.common.base.CaseFormat;

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

  Request, // used by: java nodejs php py ruby
  RequestAsync,
  RequestAsyncPaged, // used by: nodejs
  RequestAsyncPagedAll, // used by: nodejs
  RequestPaged, // used by: java php py
  RequestPagedAll, // used by: php py
  RequestStreamingBidi, // used by: nodejs php py ruby
  RequestStreamingBidiAsync, // used by: php
  RequestStreamingClient, // used by: nodejs php py ruby
  RequestStreamingClientAsync, // used by: php
  RequestStreamingServer, // used by: nodejs php py ruby

  Flattened, // used by: java
  FlattenedPaged, // used by: java
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
  LongRunningFlattened,
  LongRunningFlattenedAsync, // used by: java
  LongRunningPromise, // used by: nodejs py
  LongRunningRequest, // used by: php
  LongRunningRequestAsync, // used by: java php ruby

  // Used only if code does not yet support deciding on one of the other ones. The goal is to have
  // this value never set.
  Generic;

  /**
   * Returns the {@code String} representation of this enum, but in lower camelcase.
   *
   * @return the lower camelcase name of this enum value
   */
  public String toLowerCamel() {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, toString());
  }
}
