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
 * This ViewModel defines the view model structure of a generic message.
 *
 * <p>For example, this can be used to represent a Discovery Document's "schemas", "properties",
 * "additionalProperties", and "items".
 *
 * <p>This contains a subset of properties in the JSON Schema
 * https://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.7.
 */
@AutoValue
public abstract class SimpleQueryParamView implements Comparable<SimpleQueryParamView> {
  @Nullable
  // TODO(andrealin) Populate and render this field.
  public abstract String description();

  @Nullable
  // TODO(andrealin) Populate and render this field.
  public abstract String defaultValue();

  // The possibly-transformed ID of the schema from the Discovery Doc
  public abstract String name();

  // The type of this object; most likely a String.
  public abstract String typeName();

  // For static languages, name for getter function.
  @Nullable
  public abstract String getterFunction();

  // For static languages, name for setter function.
  @Nullable
  public abstract String setterFunction();

  public abstract boolean isRequired();

  public static SimpleQueryParamView.Builder newBuilder() {
    return new AutoValue_SimpleQueryParamView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract SimpleQueryParamView.Builder typeName(String val);

    public abstract SimpleQueryParamView.Builder name(String val);

    public abstract SimpleQueryParamView.Builder defaultValue(String val);

    public abstract SimpleQueryParamView.Builder description(String val);

    public abstract SimpleQueryParamView.Builder getterFunction(String val);

    public abstract SimpleQueryParamView.Builder setterFunction(String val);

    public abstract SimpleQueryParamView.Builder isRequired(boolean val);

    public abstract SimpleQueryParamView build();
  }

  public int compareTo(SimpleQueryParamView o) {
    return this.name().compareTo(o.name());
  }
}
