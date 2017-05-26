package com.google.api.codegen.viewmodel;

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.discovery.Schema;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

/**
 * This ViewModel defines the structure of a Discovery doc's "schemas", "properties",
 * "additionalProperties", and "items".
 *
 * This contains a subset of properties in the JSON Schema
 * https://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.7.
 */
@AutoValue
public abstract class StaticLangApiSchemaView implements ViewModel {

  // The possibly-transformed ID of the schema from the Discovery Doc
  public abstract String typeName();

  // The escaped class name for this Schema.
  public abstract String className();

  // The type of this schema.
  public abstract Schema.Type type();

  @Nullable
  public abstract String description();

  @Nullable
  public abstract String defaultValue();

  @Nullable
  // Assume all Discovery doc enums are Strings.
  public abstract List<String> enumValues();

  @Nullable
  public abstract Boolean repeated();

  // There can be arbitrarily nested fields inside of this field.
  @Nullable
  public abstract List<SimplePropertyView> properties();

  @Nullable
  // The typeName of a Schema that contains the full representation of this schema.
  public abstract StaticLangApiSchemaView ref();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Nullable
  public abstract String templateFileName();

  @Nullable
  public abstract String outputPath();

  public static StaticLangApiSchemaView.Builder newBuilder() {
    return new AutoValue_StaticLangApiSchemaView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract StaticLangApiSchemaView.Builder typeName(String val);

    public abstract StaticLangApiSchemaView.Builder className(String val);

    public abstract StaticLangApiSchemaView.Builder type(Schema.Type val);

    public abstract StaticLangApiSchemaView.Builder description(String val);

    public abstract StaticLangApiSchemaView.Builder defaultValue(String val);

    public abstract StaticLangApiSchemaView.Builder enumValues(List<String> val);

    public abstract StaticLangApiSchemaView.Builder repeated(Boolean val);

    public abstract StaticLangApiSchemaView.Builder properties(List<SimplePropertyView> val);

    public abstract StaticLangApiSchemaView.Builder ref(StaticLangApiSchemaView val);

    public abstract StaticLangApiSchemaView.Builder templateFileName(String val);

    public abstract StaticLangApiSchemaView.Builder outputPath(String val);

    public abstract StaticLangApiSchemaView build();
  }
}
