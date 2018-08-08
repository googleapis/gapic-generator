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
package com.google.api.codegen.viewmodel;

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
public abstract class StaticLangApiMessageView implements Comparable<StaticLangApiMessageView> {
  @Nullable
  // TODO(andrealin) Populate and render this field.
  public abstract String description();

  @Nullable
  // TODO(andrealin) Populate and render this field.
  public abstract String defaultValue();

  // The possibly-transformed ID of the schema from the Discovery Doc
  public abstract String name();

  // The type name for this Schema when rendered as a field in its parent Schema, e.g.
  // "List<Operation>".
  public abstract String typeName();

  // The type name for this Schema when rendered as a class name, e.g. "Operation".
  public abstract String innerTypeName();

  // For static languages, name for getter function.
  @Nullable
  public abstract String fieldGetFunction();

  // For static languages, name for setter function.
  @Nullable
  public abstract String fieldSetFunction();

  // For static languages, function name for adding an element to this repeated message type.
  @Nullable
  public abstract String fieldAddFunction();

  public abstract boolean isRequired();

  public abstract boolean canRepeat();

  @Nullable
  public abstract StaticLangApiMessageView requestBodyType();

  public boolean hasRequestBody() {
    return requestBodyType() != null;
  }

  // There can be arbitrarily nested fields inside of this field.
  public abstract List<StaticLangApiMessageView> properties();

  public abstract boolean hasRequiredProperties();

  public abstract boolean hasFieldMask();

  // If this field should be part of the parent message's serialization.
  public abstract boolean isSerializable();

  public static StaticLangApiMessageView.Builder newBuilder() {
    return new AutoValue_StaticLangApiMessageView.Builder()
        .hasRequiredProperties(false)
        .hasFieldMask(false)
        .isSerializable(true);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder typeName(String val);

    public abstract Builder name(String val);

    public abstract Builder description(String val);

    public abstract Builder defaultValue(String val);

    public abstract Builder innerTypeName(String val);

    public abstract Builder fieldGetFunction(String val);

    public abstract Builder fieldSetFunction(String val);

    public abstract Builder fieldAddFunction(String val);

    public abstract Builder properties(List<StaticLangApiMessageView> val);

    public abstract Builder hasRequiredProperties(boolean val);

    public abstract Builder isRequired(boolean val);

    public abstract Builder canRepeat(boolean val);

    public abstract Builder hasFieldMask(boolean val);

    public abstract Builder isSerializable(boolean val);

    public abstract Builder requestBodyType(StaticLangApiMessageView val);

    public abstract StaticLangApiMessageView build();
  }

  @Override
  public int compareTo(StaticLangApiMessageView o) {
    return this.name().compareTo(o.name());
  }
}
