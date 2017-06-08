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

import com.google.api.codegen.discovery.Schema;
import com.google.auto.value.AutoValue;
import java.util.List;
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
public abstract class StaticLangApiMessageView {

  // The type of this schema.
  public abstract Schema.Type type();

  @Nullable
  // TODO(andrealin) Populate and render this field.
  public abstract String description();

  @Nullable
  // TODO(andrealin) Populate and render this field.
  public abstract String defaultValue();

  @Nullable
  // Assume all Discovery doc enums are Strings.
  // TODO(andrealin) Populate and render this field.
  public abstract List<String> enumValues();

  // The possibly-transformed ID of the schema from the Discovery Doc
  @Nullable
  public abstract String name();

  // The escaped type name for this Schema.
  public abstract String typeName();

  // For static languages, name for getter function.
  @Nullable
  public abstract String fieldGetFunction();

  // For static languages, name for setter function.
  @Nullable
  public abstract String fieldSetFunction();

  // There can be arbitrarily nested fields inside of this field.
  public abstract List<StaticLangApiMessageView> properties();

  public static StaticLangApiMessageView.Builder newBuilder() {
    return new AutoValue_StaticLangApiMessageView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract StaticLangApiMessageView.Builder typeName(String val);

    public abstract StaticLangApiMessageView.Builder name(String val);

    public abstract StaticLangApiMessageView.Builder type(Schema.Type val);

    public abstract StaticLangApiMessageView.Builder description(String val);

    public abstract StaticLangApiMessageView.Builder defaultValue(String val);

    public abstract StaticLangApiMessageView.Builder enumValues(List<String> val);

    public abstract StaticLangApiMessageView.Builder fieldGetFunction(String val);

    public abstract StaticLangApiMessageView.Builder fieldSetFunction(String val);

    public abstract StaticLangApiMessageView.Builder properties(List<StaticLangApiMessageView> val);

    public abstract StaticLangApiMessageView build();
  }
}
