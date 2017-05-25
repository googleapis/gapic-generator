package com.google.api.codegen.viewmodel;

import com.google.api.codegen.discovery.Schema;
import com.google.auto.value.AutoValue;
import java.util.List;

/**
 * ViewModel representing a Discovery doc schema (minus any file headers/footers).
 */
@AutoValue
public abstract class StaticLangApiSchemaClassView implements ViewModel {

  // The possibly-transformed ID of the schema from the Discovery Doc
  public abstract String typeName();

  // The escaped class name for this Schema.
  public abstract String name();

  // The escaped type name for this Schema, e.g. String, object
  public abstract Schema.Type type();

  public abstract List<StaticLangApiPropertyView> fields();


  public static StaticLangApiSchemaClassView.Builder newBuilder() {
    return new AutoValue_StaticLangApiSchemaClassView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract StaticLangApiSchemaClassView.Builder typeName(String val);

    public abstract StaticLangApiSchemaClassView.Builder name(String val);

    public abstract StaticLangApiSchemaClassView.Builder type(String val);

    public abstract StaticLangApiSchemaClassView build();
  }

}
