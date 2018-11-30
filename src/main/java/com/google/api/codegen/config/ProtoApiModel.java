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
import com.google.api.codegen.gapic.ProtoModels;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Type;
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
  // private final ProtoParser protoParser;

  public ProtoApiModel(Model protoModel) {
    this.protoModel = protoModel;
  }

  public Model getProtoModel() {
    return protoModel;
  }

  @Override
  public String getTitle() {
    return protoModel.getServiceConfig().getTitle();
  }

  @Override
  public List<String> getAuthScopes(ProtoParser protoParser) {
    return getAuthScopes(protoParser, getInterfaces());
  }

  @VisibleForTesting
  List<String> getAuthScopes(ProtoParser protoParser, List<ProtoInterfaceModel> interfaces) {
    Set<String> result = new TreeSet<>();

    // Get scopes from protofile.
    interfaces.forEach(i -> result.addAll(protoParser.getAuthScopes(i.getInterface())));

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
  public boolean hasMultipleServices() {
    return protoModel.getServiceConfig().getApisCount() > 1;
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
  public List<ProtoInterfaceModel> getInterfaces() {
    if (interfaceModels == null) {
      interfaceModels =
          ProtoModels.getInterfaces(protoModel)
              .stream()
              .map(ProtoInterfaceModel::new)
              .collect(ImmutableList.toImmutableList());
    }
    return interfaceModels;
  }

  @Override
  public List<ProtoTypeRef> getAdditionalTypes() {
    return getTypes(protoModel)
        .stream()
        .map(ProtoTypeRef::create)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public DiagCollector getDiagCollector() {
    return protoModel.getDiagReporter().getDiagCollector();
  }

  /** Helper to extract the types from the underlying model. */
  private List<TypeRef> getTypes(Model model) {
    List<TypeRef> types = new ArrayList<>();
    for (Type type : model.getServiceConfig().getTypesList()) {
      types.add(model.getSymbolTable().lookupType(type.getName()));
    }
    return types;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ProtoApiModel && ((ProtoApiModel) o).protoModel.equals(protoModel);
  }
}
