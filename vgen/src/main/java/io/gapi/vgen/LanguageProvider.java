package io.gapi.vgen;

import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Base class for language providers.
 */
public abstract class LanguageProvider {

  private final Model model;
  private final ApiConfig apiConfig;

  private final ServiceMessages serviceMessages;
  private final ServiceConfig serviceConfig;

  /**
   * Constructs the abstract instance of the language provider..
   */
  protected LanguageProvider(Model model, ApiConfig apiConfig) {
    this.model = Preconditions.checkNotNull(model);
    this.apiConfig = Preconditions.checkNotNull(apiConfig);
    this.serviceMessages = new ServiceMessages();
    this.serviceConfig = new ServiceConfig();
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

  public ServiceConfig getServiceConfig() {
    return serviceConfig;
  }

  public boolean isIdempotent(Method method) {
    return Resources.isIdempotent(method);
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

  /*
   * This method is necessary to call m.entrySet() from snippets,
   * due to method resolution complexities.
   * See com.google.api.tools.framework.snippet.Elem::findMethod for more details.
   */
  public <K, V> Collection<Map.Entry<K, V>> entrySet(Map<K, V> m) {
    return m.entrySet();
  }
}
