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
package com.google.api.codegen.metacode;

import com.google.auto.value.AutoValue;

/** A class which represents the initialized value of a field. */
@AutoValue
public abstract class InitValue {
  public enum InitValueType {
    /** Literal values such as strings and numbers */
    Literal,
    /** Variable references */
    Variable,
    /** Random value which needs to be randomized at runtime */
    Random,
  }

  public static InitValue create(String value, InitValueType type) {
    return new AutoValue_InitValue(value, type);
  }

  public static InitValue createLiteral(String value) {
    return new AutoValue_InitValue(value, InitValueType.Literal);
  }

  public static InitValue createVariable(String variableName) {
    return new AutoValue_InitValue(variableName, InitValueType.Variable);
  }

  public static InitValue createRandom(String randomValueString) {
    return new AutoValue_InitValue(randomValueString, InitValueType.Random);
  }

  public abstract String getValue();

  public abstract InitValueType getType();
}
