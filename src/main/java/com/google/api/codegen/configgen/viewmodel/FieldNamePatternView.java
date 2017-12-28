/* Copyright 2017 Google LLC
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
package com.google.api.codegen.configgen.viewmodel;

import com.google.auto.value.AutoValue;

/** Groups a field name of the request types with an entity name of the collections. */
@AutoValue
public abstract class FieldNamePatternView {
  /** The template representing the structure of the resource name encoded in the field. */
  public abstract String pathTemplate();

  /** The name to be used as a basis for generated methods and classes. */
  public abstract String entityName();

  public static Builder newBuilder() {
    return new AutoValue_FieldNamePatternView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder pathTemplate(String val);

    public abstract Builder entityName(String val);

    public abstract FieldNamePatternView build();
  }
}
