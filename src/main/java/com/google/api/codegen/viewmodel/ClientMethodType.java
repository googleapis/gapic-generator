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
package com.google.api.codegen.viewmodel;

/** The type of method on the client surface. */
public enum ClientMethodType {
  PagedFlattenedMethod, // used by: C# Java
  PagedRequestObjectMethod, // used by: C# Java
  PagedCallableMethod, // used by: Java
  UnpagedListCallableMethod, // used by: Java
  FlattenedMethod, // used by: C# Java
  RequestObjectMethod, // used by: C# Java
  AsyncRequestObjectMethod, // used by: C#
  CallableMethod, // used by: java
  OperationRequestObjectMethod, // used by: C#
  AsyncOperationFlattenedMethod, // used by: C#, Java
  AsyncOperationRequestObjectMethod, // used by: C#, Java
  OperationCallableMethod, // used by: C#
  OptionalArrayMethod, // used by: Go Node.js PHP Python Ruby
  PagedOptionalArrayMethod, // used by: Go Node.js PHP Python Ruby
  LongRunningOptionalArrayMethod, // used by: Go Node.js PHP Python Ruby
  FlattenedAsyncCallSettingsMethod, // used by: C#
  FlattenedAsyncCancellationTokenMethod, // used by: C#
  PagedFlattenedAsyncMethod, // used by: C#
  AsyncRequestObjectCallSettingsMethod, // used by: C#
  AsyncRequestObjectCancellationMethod, // used by: C#
  AsyncPagedRequestObjectMethod, // used by: C#
  AsyncOperationFlattenedCallSettingsMethod, // used by: C#
  AsyncOperationFlattenedCancellationMethod, // used by: C#
  OperationFlattenedMethod, // used by: C#
}
