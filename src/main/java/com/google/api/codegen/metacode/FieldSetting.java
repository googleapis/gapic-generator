/* Copyright 2016 Google Inc
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
package com.google.api.codegen.metacode;

import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.model.TypeRef;
import com.google.auto.value.AutoValue;

/**
 * Represents the notion of a field on a structure being set with the value of a previously-declared
 * identifier.
 */
@AutoValue
public abstract class FieldSetting {

  public static FieldSetting create(
      TypeRef type, Name fieldName, Name identifier, InitValueConfig initValueConfig) {
    return new AutoValue_FieldSetting(type, fieldName, identifier, initValueConfig);
  }

  /** Returns the type of the field being set. */
  public abstract TypeRef getType();

  /** Returns the name of the field in the containing structure. */
  public abstract Name getFieldName();

  /** Returns the name of the identifier being set on the field. */
  public abstract Name getIdentifier();

  /** Returns the InitValueConfig for the original identifier. */
  public abstract InitValueConfig getInitValueConfig();
}
