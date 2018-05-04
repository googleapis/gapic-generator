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
  Request,
  RequestAsync,
  RequestStreaming,
  RequestPaged,
  RequestAsyncPaging,
  Flattened,
  FlattenedPaging,
  FlattenedAsync,
  FlattenedAsyncPaging,
  Callable,
  CallableList,
  CallablePaging,

  LongRunningRequest,
  LongRunningRequestAsync,
  LongRunningFlattened,
  LongRunningFlattenedAsync,
  LongRunningCallable,

  // Used only if code does not yet support deciding on one of the other ones. The goal is to have
  // this value never set.
  Generic
}
