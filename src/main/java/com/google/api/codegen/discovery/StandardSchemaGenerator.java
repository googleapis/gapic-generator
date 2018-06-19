package com.google.api.codegen.discovery;

import com.google.api.codegen.config.DiscoveryField;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import javax.print.DocFlavor;

import java.util.HashMap;
import java.util.Map;

import static com.google.api.codegen.discovery.Schema.Format.BYTE;

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

  public static Schema createListSchema(Schema items, String id, SurfaceNamer.Cardinality cardinality) {
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
