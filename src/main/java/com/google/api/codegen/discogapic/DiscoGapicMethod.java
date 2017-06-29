/* Copyright 2017 Google Inc
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
package com.google.api.codegen.discogapic;

import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.tools.framework.model.Interface;
import com.google.auto.value.AutoValue;

/** Represents a method declaration. The REST accessors correspond to the primary REST binding. */
@AutoValue
public abstract class DiscoGapicMethod {

  /** @return The underlying Discovery Document model for the method. */
  public abstract Method method();

  /** @return The fully-qualified type name for the inner request object. */
  public abstract String resourceFullName();

  /** @return The fully-qualified type name for the encapsulating request object. */
  public abstract String requestFullName();

  /** @return The fully-qualified type name for the response object. */
  public abstract String responseFullName();

  /**
   * @return The type table containing the fully qualified names for the resources, requests, and
   *     responses.
   */
  public abstract SchemaTypeTable schemaTypeTable();

  /** @return ????? hopefully the yaml config. */
  public abstract Interface parent();

  @Override
  public String toString() {
    return "method " + getFullName();
  }

  public String getFullName() {
    return "method " + method().id();
  }
}
