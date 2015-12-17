package io.gapi.vgen;

import com.google.api.Authentication;
import com.google.api.AuthenticationRule;
import com.google.api.Service;
import com.google.api.tools.framework.model.Interface;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    Set<String> result = new HashSet<>();
    Service config = service.getModel().getServiceConfig();
    Authentication auth = config.getAuthentication();
    for (AuthenticationRule rule : auth.getRulesList()) {
      // Scopes form a union and the union is used for down-scoping, so adding more scopes that
      // are subsets of the others already in the union essentially has no effect.
      // We are doing this for implementation simplicity so we don't have to compute which scopes
      // are subsets of the others.
      String scopesString = rule.getOauth().getCanonicalScopes();
      List<String> scopes = Arrays.asList(scopesString.split(","));
      for (String scope : scopes) {
        result.add(scope.trim());
      }
    }
    return result;
  }
}
