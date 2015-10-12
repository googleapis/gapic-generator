package io.gapi.fx.aspects.http.model;

/**
 * Rest method kind. 
 */
public enum RestKind {
  CREATE("create"),
  GET("get"),
  LIST("list"),
  UPDATE("update"),
  PATCH("patch"),
  DELETE("delete"),
  CUSTOM("") {
    @Override public String getMethodName() {
      throw new IllegalStateException("RestKind 'CUSTOM' cannot be asked for method name");
    }
  };

  private final String methodName;

  RestKind(String methodName) {
    this.methodName = methodName;
  }

  public String getMethodName() {
    return methodName;
  }
}