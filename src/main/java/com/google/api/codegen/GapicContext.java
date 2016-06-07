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
package com.google.api.codegen;

import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.common.base.Preconditions;

/**
 * A CodegenContext that provides helpers specific to the use case of GAPIC (code-generation of
 * client libraries built on gRPC, or code fragments for those client libraries).
 */
public class GapicContext extends CodegenContext {

  private final Model model;
  private final ApiConfig apiConfig;

  private final ServiceMessages serviceMessages;
  private final ServiceConfig serviceConfig;

  /**
   * Constructs the abstract instance.
   */
  protected GapicContext(Model model, ApiConfig apiConfig) {
    this.model = Preconditions.checkNotNull(model);
    this.apiConfig = Preconditions.checkNotNull(apiConfig);
    this.serviceMessages = new ServiceMessages();
    this.serviceConfig = new ServiceConfig();
  }

  /**
   * Returns the associated model.
   */
  public Model getModel() {
    return model;
  }

  /**
   * Returns the associated config.
   */
  public ApiConfig getApiConfig() {
    return apiConfig;
  }

  public ServiceMessages messages() {
    return serviceMessages;
  }

  public ServiceConfig getServiceConfig() {
    return serviceConfig;
  }

  public boolean isIdempotent(Method method) {
    return Resources.isIdempotent(method);
  }

  /**
   * Return the name of the class which is the GAPIC wrapper for this service interface.
   */
  public String getApiWrapperName(Interface service) {
    return service.getSimpleName() + "Api";
  }

  public String getOutputSubPath(@SuppressWarnings("unused") ProtoElement element) {
    return "";
  }

  /**
   * Returns the description of the proto element, in markdown format.
   */
  public String getDescription(ProtoElement element) {
    return DocumentationUtil.getDescription(element);
  }

  /**
   * Get collection configuration for a method.
   */
  public CollectionConfig getCollectionConfig(Interface service, String entityName) {
    CollectionConfig result =
        getApiConfig().getInterfaceConfig(service).getCollectionConfig(entityName);
    if (result == null) {
      throw new IllegalStateException(
          "A collection config was not present for entity name " + entityName);
    }
    return result;
  }
}
