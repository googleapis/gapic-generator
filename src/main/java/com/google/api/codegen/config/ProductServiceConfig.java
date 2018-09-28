/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import com.google.api.tools.framework.model.Model;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility class that provides data from "service config", as defined in a service yaml file.
 *
 * <p>The scope of this configuration is at the product level, and covers multiple API interfaces.
 */
public class ProductServiceConfig {
  /** Return the service address. */
  public String getServiceHostname(Model model) {
    return model.getServiceConfig().getName();
  }

  /** Return the service port. TODO(cbao): Read the port from config. */
  public Integer getServicePort() {
    return 443;
  }

  public String getTitle(Model model) {
    return model.getServiceConfig().getTitle();
  }

  /** Return a list of scopes for authentication. */
  public List<String> getAuthScopes(Model model) {
    Set<String> result = new TreeSet<>();
    Service config = model.getServiceConfig();
    Authentication auth = config.getAuthentication();
    for (AuthenticationRule rule : auth.getRulesList()) {
      // Scopes form a union and the union is used for down-scoping, so adding more scopes that
      // are subsets of the others already in the union essentially has no effect.
      // We are doing this for implementation simplicity so we don't have to compute which scopes
      // are subsets of the others.
      String scopesString = rule.getOauth().getCanonicalScopes();
      for (String scope : scopesString.split(",")) {
        result.add(scope.trim());
      }
    }
    return new ArrayList<>(result);
  }
}
