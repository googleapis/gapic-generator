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
package com.google.api.codegen.discovery;

import static com.google.api.codegen.discovery.Schema.Format.BYTE;
import static com.google.api.codegen.discovery.Schema.Format.EMPTY;

import com.google.api.codegen.transformer.SurfaceNamer;

public class StandardSchemaGenerator {

  public static Schema createStringSchema(
      String name, SurfaceNamer.Cardinality cardinality, boolean isRequired) {
    return Schema.newBuilder()
        .setFormat(BYTE)
        .setId(name)
        .setKey(name)
        .setRepeated(cardinality == SurfaceNamer.Cardinality.IS_REPEATED)
        .setRequired(isRequired)
        .setType(Schema.Type.STRING)
        .setIsEnum(false)
        .setIsMap(false)
        .build();
  }

  public static Schema createListSchema(
      Schema items, String id, boolean isRequired, String description) {
    return Schema.newBuilder()
        .setFormat(EMPTY)
        .setId(id)
        .setDescription(description)
        .setKey(id)
        .setRepeated(false)
        .setRequired(isRequired)
        .setType(Schema.Type.ARRAY)
        .setItems(items)
        .setIsEnum(false)
        .setIsMap(false)
        .build();
  }
}
