package io.gapi.vgen.config;

import com.google.api.tools.framework.model.Method;

import java.util.Map;

/**
 * Interface for method config generator.
 */
public interface MethodConfigGenerator {
  /**
   * Generate the config data into a map structure. Return null if no data is generated.
   */
  public Map<String, Object> generate(Method method);
}
