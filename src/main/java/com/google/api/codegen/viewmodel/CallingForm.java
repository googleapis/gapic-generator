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

  Request, // Java, NodeJS
  RequestAsync,
  RequestAsyncPaged, // NodeJS
  RequestAsyncPagedAll, // NodeJS
  RequestPaged, // Java
  RequestStreamingBidi, // NodeJS
  RequestStreamingClient, // NodeJS
  RequestStreamingServer, // NodeJS

  Flattened, // Java
  FlattenedPaged, // Java
  FlattenedAsync,
  FlattenedAsyncPaged,

  Callable, // Java
  CallableList, // Java
  CallablePaged, // Java
  CallableStreamingBidi, // Java
  CallableStreamingClient, // Java
  CallableStreamingServer, // Java

  LongRunningCallable, // Java
  LongRunningEventEmitter, // NodeJS
  LongRunningFlattened,
  LongRunningFlattenedAsync, // Java
  LongRunningPromise, // NodeJS
  LongRunningRequest,
  LongRunningRequestAsync, // Java

  // Used only if code does not yet support deciding on one of the other ones. The goal is to have
  // this value never set.
  Generic
}
