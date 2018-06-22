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

import com.google.api.codegen.config.DiscoveryField;
import com.google.api.codegen.transformer.SurfaceNamer;
import java.util.HashMap;

public class StandardSchemaGenerator {


  public static Schema createStringSchema(String name, SurfaceNamer.Cardinality cardinality) {
    return new AutoValue_Schema(
        null,
        "",
        "",
        BYTE,
        name,
        false,
        null,
        name,
        "",
        "",
        new HashMap<>(),
        "",
        cardinality == SurfaceNamer.Cardinality.IS_REPEATED,
        true,
        false,
        Schema.Type.STRING);
  }

  public static Schema createListSchema(Schema items, String id) {
    return new AutoValue_Schema(
        null,
        "",
        "",
        Schema.Format.EMPTY,
        id,
        false,
        items,
        id,
        "",
        "",
        new HashMap<>(),
        "",
        false,
        true,
        false,
        Schema.Type.ARRAY);
  }

  public static Schema createOptionalSchema(Schema schema) {
    if (schema.type().equals(Schema.Type.ARRAY)) {
      return new AutoValue_Schema(
          schema.additionalProperties(),
          schema.defaultValue(),
          schema.description(),
          schema.format(),
          schema.id(),
          schema.isEnum(),
          schema.items(),
          schema.id(),
          schema.location(),
          schema.pattern(),
          schema.properties(),
          null,
          schema.repeated(),
          schema.required(),
          schema.isMap(),
          Schema.Type.EMPTY);
    }
    else return schema;
  }

  public static DiscoveryField createFieldMaskField() {
    return DiscoveryField.create(
        StandardSchemaGenerator.createListSchema(
            StandardSchemaGenerator.createStringSchema("", SurfaceNamer.Cardinality.NOT_REPEATED),
            "fieldMask"),
        null);
  }
}
