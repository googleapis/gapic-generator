package com.google.api.codegen.viewmodel;

import com.google.api.codegen.SnippetSetRunner;
import com.google.auto.value.AutoValue;

/**
 * ViewModel representing a the file containing a Discovery doc schema.
 */
@AutoValue
public abstract class StaticLangApiMessageFileView implements ViewModel {
  public abstract StaticLangApiMessageView schema();

  public abstract FileHeaderView fileHeader();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  public static StaticLangApiMessageFileView.Builder newBuilder() {
    return new AutoValue_StaticLangApiMessageFileView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract StaticLangApiMessageFileView.Builder schema(StaticLangApiMessageView val);

    public abstract StaticLangApiMessageFileView.Builder templateFileName(String val);

    public abstract StaticLangApiMessageFileView.Builder outputPath(String val);

    public abstract StaticLangApiMessageFileView.Builder fileHeader(FileHeaderView val);

    public abstract StaticLangApiMessageFileView build();
  }
}