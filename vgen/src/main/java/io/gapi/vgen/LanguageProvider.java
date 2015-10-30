package io.gapi.vgen;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;

import io.gapi.fx.model.Interface;
import io.gapi.fx.model.Method;
import io.gapi.fx.model.Model;

import java.io.IOException;

/**
 * Base class for language providers.
 */
public abstract class LanguageProvider {

  private final Model model;
  private final ApiConfig apiConfig;

  private final ServiceMessages serviceMessages;
  private final Resources resources;

  /**
   * Constructs the abstract instance of the language provider..
   */
  protected LanguageProvider(Model model, ApiConfig apiConfig) {
    this.model = Preconditions.checkNotNull(model);
    this.apiConfig = Preconditions.checkNotNull(apiConfig);
    this.serviceMessages = new ServiceMessages();
    this.resources = new Resources();
  }

  /**
   * Generates code for the given service interface.
   */
  public abstract GeneratedResult generate(Interface service,
      SnippetDescriptor snippetDescriptor);

  /**
   * Outputs the code based on a per-service map.
   */
  public abstract void outputCode(String outputArchiveFile,
      Multimap<Interface, GeneratedResult> services,
      boolean archive) throws IOException;

  /**
   * Returns the associated model.
   */
  public Model getModel() {
    return model;
  }

  /**
   * Returns the associated config.
   */
  public ApiConfig getApiConfig() {
    return apiConfig;
  }

  public ServiceMessages messages() {
    return serviceMessages;
  }

  public Resources resources() {
    return resources;
  }

  /**
   * Return the service address.
   */
  public String getServiceAddress(Interface service) {
    return service.getModel().getServiceConfig().getName();
  }

  /**
   * Return the service port.
   * TODO(cbao): Read the port from config.
   */
  public Integer getServicePort() {
    return 443;
  }

  /**
   * Return the name of the class which is the veneer for this service interface.
   */
  public String getVeneerName(Interface service) {
    return service.getSimpleName() + "Api";
  }

  // Helpers for Subclasses and Snippets
  // ===================================

  // Note the below methods are instance-based, even if they don't depend on instance state,
  // so they can be accessed by templates.

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
}
