package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

// View of both api and settings for static language.
// ViewModel members delegate to api.
@AutoValue
public abstract class StaticLangXCommonView implements ViewModel {
  public abstract StaticLangXApiView api();
  @Nullable // TODO: Remove
  public abstract StaticLangXSettingsView settings();
  public static Builder newBuilder() {
    return new AutoValue_StaticLangXCommonView.Builder();
  }
  @Override
  public String resourceRoot() {return api().resourceRoot();}

  @Override
  public String templateFileName(){return api().templateFileName();}

  @Override
  public String outputPath(){return api().outputPath();}
  
  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder api(StaticLangXApiView val);
    public abstract Builder settings(StaticLangXSettingsView val);
    public abstract StaticLangXCommonView build();
  }
}
