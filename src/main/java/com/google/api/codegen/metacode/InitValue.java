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

/** A class which represents the initialized value of a field. */
public class InitValue {
  private final String value;
  private final InitValueType type;

  public enum InitValueType {
    Literal,
    Variable,
  }

  public InitValue(String value, InitValueType type) {
    this.value = value;
    this.type = type;
  }

  public String getValue() {
    return value;
  }

  public InitValueType getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this.getClass() == o.getClass()) {
      InitValue initValue = (InitValue) o;
      return initValue.getValue().equals(this.getValue()) && initValue.getType() == this.getType();
    }
    return false;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}
