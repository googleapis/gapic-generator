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
package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/**
 * This ViewModel defines the structure of a Discovery doc's "schemas", "properties",
 * "additionalProperties", and "items".
 *
 * <p>This contains a subset of properties in the JSON Schema
 * https://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.7.
 */
@AutoValue
public abstract class SimplePropertyView {

  // The possibly-transformed ID of the schema from the Discovery Doc
  public abstract String name();

  // The escaped type name for this Schema.
  public abstract String typeName();

  // For static languages, name for getter function.
  @Nullable
  public abstract String fieldGetFunction();

  // For static languages, name for setter function.
  @Nullable
  public abstract String fieldSetFunction();

  public static SimplePropertyView.Builder newBuilder() {
    return new AutoValue_SimplePropertyView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract SimplePropertyView.Builder name(String val);

    public abstract SimplePropertyView.Builder typeName(String val);

    public abstract SimplePropertyView.Builder fieldGetFunction(String val);

    public abstract SimplePropertyView.Builder fieldSetFunction(String val);

    public abstract SimplePropertyView build();
  }
}
