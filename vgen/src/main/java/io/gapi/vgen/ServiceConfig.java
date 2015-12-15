package io.gapi.vgen;

import com.google.api.Authentication;
import com.google.api.AuthenticationRule;
import com.google.api.Service;
import com.google.api.tools.framework.model.Interface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class that provides service configuration data from an Interface.
 */
public class ServiceConfig {
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
   * Return a list of scopes for authentication.
   */
  public Iterable<String> getAuthScopes(Interface service) {
    List<String> result = new ArrayList<>();
    Service config = service.getModel().getServiceConfig();
    Authentication auth = config.getAuthentication();
    for (AuthenticationRule rule : auth.getRulesList()) {
      List<String> scopes = Arrays.asList(rule.getOauth().getCanonicalScopes().split(", "));
      result.addAll(scopes);
    }
    return result;
  }
}
