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
import com.google.api.codegen.InterfaceView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility class that provides data from "service config", as defined in a service yaml file.
 *
 * <p>The scope of this configuration is at the product level, and covers multiple API interfaces.
 */
public class ProtoApiModel implements ApiModel {
  private final Model protoModel;
  private ImmutableList<ProtoInterfaceModel> interfaceModels;

  public ProtoApiModel(Model protoModel) {
    this.protoModel = protoModel;
  }

  @Override
  public ApiSource getApiSource() {
    return ApiSource.PROTO;
  }

  @Override
  public String getServiceAddress() {
    return protoModel.getServiceConfig().getName();
  }

  /** Return the service port. TODO(cbao): Read the port from config. */
  @Override
  public Integer getServicePort() {
    return 443;
  }

  @Override
  public String getTitle() {
    return protoModel.getServiceConfig().getTitle();
  }

  @Override
  public List<String> getAuthScopes() {
    Set<String> result = new TreeSet<>();
    Service config = protoModel.getServiceConfig();
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

  @Override
  public ProtoInterfaceModel getInterface(String interfaceName) {
    return new ProtoInterfaceModel(protoModel.getSymbolTable().lookupInterface(interfaceName));
  }

  @Override
  public boolean hasMultipleServices(GapicProductConfig productConfig) {
    return Iterables.size(getInterfaces(productConfig)) > 1;
  }

  @Override
  public String getServiceName() {
    return protoModel.getServiceConfig().getName();
  }

  @Override
  public String getDocumentationSummary() {
    return protoModel.getServiceConfig().getDocumentation().getSummary();
  }

  @Override
  public Iterable<ProtoInterfaceModel> getInterfaces(GapicProductConfig productConfig) {
    if (interfaceModels != null) {
      return interfaceModels;
    }
    Iterable<Interface> interfaces = new InterfaceView().getElementIterable(protoModel);
    ImmutableList.Builder<ProtoInterfaceModel> intfModels = ImmutableList.builder();
    for (Interface intf : interfaces) {
      intfModels.add(new ProtoInterfaceModel(intf));
    }
    interfaceModels = intfModels.build();
    return interfaceModels;
  }

  @Override
  public boolean equals(Object o) {
    return o != null
        && o instanceof ProtoApiModel
        && ((ProtoApiModel) o).protoModel.equals(protoModel);
  }
}
