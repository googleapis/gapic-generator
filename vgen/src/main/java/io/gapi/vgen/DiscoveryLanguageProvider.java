package io.gapi.vgen;

import com.google.api.Service;
import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Base class for language providers.
 */
public abstract class DiscoveryLanguageProvider {

  private final Service service;
  private final ApiaryConfig apiaryConfig;

  private final ServiceMessages serviceMessages;
  private final ServiceConfig serviceConfig;

  /**
   * Constructs the abstract instance of the language provider..
   */
  protected DiscoveryLanguageProvider(Service service, ApiaryConfig apiaryConfig) {
    this.service = Preconditions.checkNotNull(service);
    this.apiaryConfig = Preconditions.checkNotNull(apiaryConfig);
    this.serviceMessages = new ServiceMessages();
    this.serviceConfig = new ServiceConfig();
  }

  /**
   * Generates fragment for the given method.
   */
  public abstract GeneratedResult generateFragment(Method method,
      SnippetDescriptor snippetDescriptor);

  /**
   * Outputs fragments based on a per-method map.
   */
  public abstract void outputFragments(String outputArchiveFile,
      Multimap<Method, GeneratedResult> methods,
      boolean archive) throws IOException;

  /**
   * Returns the associated service.
   */
  public Service getService() {
    return service;
  }

  /**
   * Returns the associated config.
   */
  public ApiaryConfig getApiaryConfig() {
    return apiaryConfig;
  }

  public ServiceMessages messages() {
    return serviceMessages;
  }

  public ServiceConfig getServiceConfig() {
    return serviceConfig;
  }

  // Helpers for Subclasses and Snippets
  // ===================================

  // Note the below methods are instance-based, even if they don't depend on instance state,
  // so they can be accessed by templates.

  public <T> T getLast(List<T> list) {
    return list.get(list.size() - 1);
  }

  public String getSimpleName(String name) {
    return name.substring(name.lastIndexOf('.') + 1);
  }

  public String upperCamelToUpperUnderscore(String name) {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
  }

  public String upperCamelToLowerCamel(String name) {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name);
  }

  public String upperCamelToLowerUnderscore(String name) {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
  }

  public String lowerUnderscoreToUpperUnderscore(String name) {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_UNDERSCORE, name);
  }

  public String lowerUnderscoreToUpperCamel(String name) {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
  }

  public String lowerUnderscoreToLowerCamel(String name) {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
  }

  public String lowerCamelToUpperCamel(String name) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name);
  }

  @Nullable
  public Field getFirstRepeatedField(Type type) {
    for (Field field : type.getFieldsList()) {
      if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
        return field;
      }
    }
    return null;
  }

  public boolean isPageStreaming(Method method) {
    for (Field field : apiaryConfig.getType(method.getResponseTypeUrl()).getFieldsList()) {
      if (field.getName() == "nextPageToken") {
        return true;
      }
    }
    return false;
  }
}
