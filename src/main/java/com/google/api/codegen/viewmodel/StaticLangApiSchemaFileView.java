package com.google.api.codegen.viewmodel;

import com.google.api.codegen.SnippetSetRunner;
import com.google.auto.value.AutoValue;

/**
 * ViewModel representing a the file containing a Discovery doc schema.
 */
@AutoValue
public abstract class StaticLangApiSchemaFileView implements ViewModel {
  public abstract StaticLangApiSchemaView schema();

  public abstract FileHeaderView fileHeader();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  public static StaticLangApiSchemaFileView.Builder newBuilder() {
    return new AutoValue_StaticLangApiSchemaFileView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract StaticLangApiSchemaFileView.Builder schema(StaticLangApiSchemaView val);

    public abstract StaticLangApiSchemaFileView.Builder templateFileName(String val);

    public abstract StaticLangApiSchemaFileView.Builder outputPath(String val);

    public abstract StaticLangApiSchemaFileView.Builder fileHeader(FileHeaderView val);

    public abstract StaticLangApiSchemaFileView build();
  }
}