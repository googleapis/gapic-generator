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
package com.google.api.codegen;

import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.protobuf.Empty;
import java.util.List;

/** Utility class with methods to work with service methods. */
public class ServiceMessages {

  private static final String LRO_TYPE = "google.longrunning.Operation";

  /** Returns true if the message is the empty message. */
  public boolean isEmptyType(TypeRef type) {
    return s_isEmptyType(type);
  }

  public static boolean s_isEmptyType(TypeRef type) {
    return type.isMessage()
        && type.getMessageType().getFullName().equals(Empty.getDescriptor().getFullName());
  }

  public boolean isLongRunningOperationType(TypeRef type) {
    return type.isMessage() && type.getMessageType().getFullName().equals(LRO_TYPE);
  }

  /** Inputs a list of methods and returns only those which are page streaming. */
  public Iterable<Method> filterPageStreamingMethods(
      final GapicInterfaceConfig config, List<Method> methods) {
    Predicate<Method> isPageStreaming =
        new Predicate<Method>() {
          @Override
          public boolean apply(Method method) {
            return config.getMethodConfig(method).isPageStreaming();
          }
        };

    return Iterables.filter(methods, isPageStreaming);
  }

  /** Inputs a list of methods and returns only those which are batching. */
  public Iterable<Method> filterBatchingMethods(
      final GapicInterfaceConfig config, List<Method> methods) {
    Predicate<Method> isBatching =
        new Predicate<Method>() {
          @Override
          public boolean apply(Method method) {
            return config.getMethodConfig(method).isBatching();
          }
        };

    return Iterables.filter(methods, isBatching);
  }

  public Iterable<Method> filterLongrunningMethods(
      final GapicInterfaceConfig config, List<Method> methods) {
    Predicate<Method> isLongRunning =
        new Predicate<Method>() {
          @Override
          public boolean apply(Method method) {
            return config.getMethodConfig(method).isLongRunningOperation();
          }
        };

    return Iterables.filter(methods, isLongRunning);
  }
}
