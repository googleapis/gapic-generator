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

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class IamResourceView {
  /** Name of the function; it turns the resource into IAM handle object. */
  public abstract String resourceGetterFunctionName();

  /** Name of the function example; it turns the resource into IAM handle object. */
  public abstract String exampleName();

  /** Name of the function parameter. */
  public abstract String paramName();

  /** Type name of the resource. */
  public abstract String resourceTypeName();

  /** Constructor of the resource; used in examples */
  public abstract String resourceConstructorName();

  /** Field of the object to turn into IAM handle. */
  public abstract String fieldName();

  public static Builder builder() {
    return new AutoValue_IamResourceView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder resourceGetterFunctionName(String value);

    public abstract Builder exampleName(String value);

    public abstract Builder paramName(String value);

    public abstract Builder resourceTypeName(String value);

    public abstract Builder resourceConstructorName(String value);

    public abstract Builder fieldName(String value);

    public abstract IamResourceView build();
  }
}
