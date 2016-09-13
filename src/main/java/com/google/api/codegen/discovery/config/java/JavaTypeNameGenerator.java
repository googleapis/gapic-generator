package com.google.api.codegen.discovery.config.java;

import com.google.api.codegen.discovery.config.TypeNameGenerator;
import com.google.api.codegen.util.Name;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class JavaTypeNameGenerator implements TypeNameGenerator {

  private static final String PACKAGE_PREFIX = "com.google.api.services";

  @Override
  public String getApiTypeName(String apiName, String apiVersion) {
    return StringUtils.join(
        ".", PACKAGE_PREFIX, apiName, apiVersion, Name.lowerCamel(apiName).toUpperCamel());
  }

  @Override
  public String getRequestTypeName(
      String apiName, String apiVersion, List<String> methodNameComponents) {
    return StringUtils.join(".", getApiTypeName(apiName, apiVersion), methodNameComponents);
  }

  @Override
  public String getMessageTypeName(String apiName, String apiVersion, String messageTypeName) {
    return StringUtils.join(".", PACKAGE_PREFIX, apiName, apiVersion, "model", messageTypeName);
  }
}
