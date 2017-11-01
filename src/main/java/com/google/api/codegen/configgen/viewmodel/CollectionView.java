/* Copyright 2017 Google LLC
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
package com.google.api.codegen.configgen.viewmodel;

import com.google.auto.value.AutoValue;

/** Represents the resource collection configurations. */
@AutoValue
public abstract class CollectionView {
  /** Pattern to describe the names of the resources. */
  public abstract String namePattern();

  /** Name to be used as a basis for generated methods and classes. */
  public abstract String entityName();

  public static Builder newBuilder() {
    return new AutoValue_CollectionView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder namePattern(String val);

    public abstract Builder entityName(String val);

    public abstract CollectionView build();
  }
}
