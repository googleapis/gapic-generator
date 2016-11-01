/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.config;

import com.google.api.Authentication;
import com.google.api.AuthenticationRule;
import com.google.api.Service;
import com.google.api.tools.framework.model.Interface;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Utility class that provides service configuration data from an Interface. */
public class ServiceConfig {
  /** Return the service address. */
  public String getServiceAddress(Interface service) {
    return service.getModel().getServiceConfig().getName();
  }

  /** Return the service port. TODO(cbao): Read the port from config. */
  public Integer getServicePort() {
    return 443;
  }

  public String getTitle(Interface service) {
    return service.getModel().getServiceConfig().getTitle();
  }

  /** Return a list of scopes for authentication. */
  public Iterable<String> getAuthScopes(Interface service) {
    Set<String> result = new TreeSet<>();
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
