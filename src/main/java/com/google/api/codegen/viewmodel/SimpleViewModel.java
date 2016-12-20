package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SimpleViewModel implements ViewModel {

  @Override
  public abstract String resourceRoot();

  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  public static SimpleViewModel create(
      String resourceRoot, String templateFileName, String outputPath) {
    return new AutoValue_SimpleViewModel(resourceRoot, templateFileName, outputPath);
  }
}
