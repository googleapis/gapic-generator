/* Copyright 2017 Google LLC
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

import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Utility class that provides data from a Discovery document.
 *
 * <p>The scope of this configuration is at the product level, and covers multiple API interfaces.
 */
public class DiscoApiModel implements ApiModel {
  private final Document document;
  private final DiagCollector diagCollector;
  private ImmutableList<DiscoInterfaceModel> interfaceModels;
  private final String defaultPackageName;

  @Override
  public String getServiceName() {
    return document.canonicalName();
  }

  @Override
  public String getDocumentationSummary() {
    return document.description();
  }

  @Override
  public boolean hasMultipleServices() {
    return document.resources().size() > 1;
  }

  @Override
  public List<DiscoInterfaceModel> getInterfaces() {
    if (interfaceModels != null) {
      return interfaceModels;
    }

    ImmutableList.Builder<DiscoInterfaceModel> builder = ImmutableList.builder();
    for (String resource : document.resources().keySet()) {
      String ownerName = document.ownerDomain().split("\\.")[0];
      String resourceName = Name.anyCamel(resource).toUpperCamel();
      String interfaceName =
          String.format(
              "%s.%s.%s.%s", ownerName, document.name(), document.version(), resourceName);
      builder.add(new DiscoInterfaceModel(interfaceName, this));
    }
    interfaceModels = builder.build();
    return interfaceModels;
  }

  @Override
  public List<? extends TypeModel> getAdditionalTypes() {
    throw new UnsupportedOperationException("Discovery does not support additional types");
  }

  @Override
  public InterfaceModel getInterface(String interfaceName) {
    return null;
  }

  public DiscoApiModel(Document document, String defaultPackageName) {
    this.document = document;
    this.diagCollector = new BoundedDiagCollector();
    this.defaultPackageName = defaultPackageName;
  }

  public Document getDocument() {
    return document;
  }

  @Override
  public String getTitle() {
    return document.title();
  }

  @Override
  public List<String> getAuthScopes(GapicProductConfig productConfig) {
    return document.authScopes();
  }

  @Override
  public DiagCollector getDiagCollector() {
    return diagCollector;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof DiscoApiModel && ((DiscoApiModel) o).document.equals(document);
  }

  public String getDefaultPackageName() {
    return defaultPackageName;
  }
}
