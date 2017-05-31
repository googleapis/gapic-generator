package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/**
 * This ViewModel defines the structure of a Discovery doc's "schemas", "properties",
 * "additionalProperties", and "items".
 *
 * <p>This contains a subset of properties in the JSON Schema
 * https://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.7.
 */
@AutoValue
public abstract class SimplePropertyView {

  // The possibly-transformed ID of the schema from the Discovery Doc
  public abstract String name();

  // The escaped type name for this Schema.
  public abstract String typeName();

  // For static languages, ex "Field" for use in the getter method "getField()".
  @Nullable
  public abstract String capitalizedName();

  @Nullable
  public abstract Boolean repeated();

  public static SimplePropertyView.Builder newBuilder() {
    return new AutoValue_SimplePropertyView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract SimplePropertyView.Builder name(String val);

    public abstract SimplePropertyView.Builder typeName(String val);

    public abstract SimplePropertyView.Builder capitalizedName(String val);

    public abstract SimplePropertyView.Builder repeated(Boolean val);

    public abstract SimplePropertyView build();
  }
}
