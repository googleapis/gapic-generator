package com.google.api.codegen.discovery.config;

import java.util.List;

public interface TypeNameGenerator {

  public String getApiTypeName(String apiName, String apiVersion);

  public String getRequestTypeName(
      String apiName, String apiVersion, List<String> methodNameComponents);

  public String getMessageTypeName(String apiName, String apiVersion, String messageTypeName);
}
