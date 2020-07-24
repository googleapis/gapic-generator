package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
public abstract class HttpMethodSelectorView {
  public abstract String fullyQualifiedName();

  public abstract List<String> gettersChain();

  public static HttpMethodSelectorView.Builder newBuilder() {
    return new AutoValue_HttpMethodSelectorView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder fullyQualifiedName(String val);

    public abstract Builder gettersChain(List<String> val);

    public abstract HttpMethodSelectorView build();
  }
}
