/* Copyright 2018 Google LLC
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
package com.google.api.codegen.util;

public class Enums {
  public enum MapType implements Comparable<MapType> {
    IS_MAP(true),
    NOT_MAP(false);

    MapType(boolean value) {
      this.value = value;
    }

    public static MapType ofMap(boolean value) {
      return value ? IS_MAP : NOT_MAP;
    }

    private final boolean value;
  }

  public enum Cardinality implements Comparable<Cardinality> {
    IS_REPEATED(true),
    NOT_REPEATED(false);

    Cardinality(boolean value) {
      this.value = value;
    }

    public static Cardinality ofRepeated(boolean value) {
      return value ? IS_REPEATED : NOT_REPEATED;
    }

    private final boolean value;
  }
}
