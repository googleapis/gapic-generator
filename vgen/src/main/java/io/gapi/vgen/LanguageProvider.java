package io.gapi.vgen;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;

import io.gapi.fx.model.Interface;
import io.gapi.fx.model.Model;

import java.io.IOException;

/**
 * Base class for language providers.
 */
public abstract class LanguageProvider {

  private final Model model;
  private final Config config;

  private final ServiceMessages serviceMessages;
  private final Resources resources;

  /**
   * Constructs the abstract instance of the language provider..
   */
  protected LanguageProvider(Model model, Config config) {
    this.model = Preconditions.checkNotNull(model);
    this.config = Preconditions.checkNotNull(config);
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
      Multimap<Interface, GeneratedResult> services) throws IOException;

  /**
   * Returns the associated model.
   */
  public Model getModel() {
    return model;
  }

  /**
   * Returns the associated config.
   */
  public Config getConfig() {
    return config;
  }

  public ServiceMessages messages() {
    return serviceMessages;
  }

  public Resources resources() {
    return resources;
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
