/* Copyright 2017 Google LLC
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

import com.google.api.codegen.discovery.Document;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Utility class that provides data from "service config", as defined in a service yaml file.
 *
 * <p>The scope of this configuration is at the product level, and covers multiple API interfaces.
 */
public class DiscoApiModel implements ApiModel {
  private final Document document;
  private ImmutableList<DiscoInterfaceModel> interfaceModels;

  @Override
  public String getServiceName() {
    return document.canonicalName();
  }

  @Override
  public String getDocumentationSummary() {
    return document.description();
  }

  @Override
  public boolean hasMultipleServices(GapicProductConfig productConfig) {
    return interfaceModels.size() > 1;
  }

  @Override
  public Iterable<DiscoInterfaceModel> getInterfaces(GapicProductConfig productConfig) {
    if (interfaceModels != null) {
      return interfaceModels;
    }
    if (productConfig == null) {
      throw new NullPointerException("GapicProductConfig has not been set in DiscoApiModel.");
    }

    ImmutableList.Builder<DiscoInterfaceModel> intfModels = ImmutableList.builder();
    for (String interfaceName : productConfig.getInterfaceConfigMap().keySet()) {
      intfModels.add(new DiscoInterfaceModel(interfaceName, document));
    }
    interfaceModels = intfModels.build();
    return interfaceModels;
  }

  @Override
  public InterfaceModel getInterface(String interfaceName) {
    for (InterfaceModel interfaceModel : interfaceModels) {
      if (interfaceModel.getSimpleName().equals(interfaceName)
          || interfaceModel.getFullName().equals(interfaceName)) {
        return interfaceModel;
      }
    }
    return null;
  }

  public DiscoApiModel(Document document) {
    this.document = document;
  }

  public Document getDocument() {
    return document;
  }

  @Override
  public ApiSource getApiSource() {
    return ApiSource.DISCOVERY;
  }

  @Override
  public String getServiceAddress() {
    // TODO(andrealin): Implement.
    return document.baseUrl();
  }

  /** Return the service port. */
  @Override
  public Integer getServicePort() {
    return 443;
  }

  @Override
  public String getTitle() {
    return document.title();
  }

  @Override
  public List<String> getAuthScopes() {
    return document.authScopes();
  }

  @Override
  public boolean equals(Object o) {
    return o != null && o instanceof DiscoApiModel && ((DiscoApiModel) o).document.equals(document);
  }
}
