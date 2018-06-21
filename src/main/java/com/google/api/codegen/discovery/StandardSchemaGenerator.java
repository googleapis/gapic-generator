package com.google.api.codegen.discovery;

import static com.google.api.codegen.discovery.Schema.Format.BYTE;

import com.google.api.codegen.transformer.SurfaceNamer;
import java.util.HashMap;

public class StandardSchemaGenerator {
  public static final String PARENT_PACKAGE = "com.google.api.codegen.discovery";

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

  public static Schema createListSchema(
      Schema items, String id, SurfaceNamer.Cardinality cardinality) {
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
        cardinality == SurfaceNamer.Cardinality.IS_REPEATED,
        true,
        false,
        Schema.Type.ARRAY);
  }
}
