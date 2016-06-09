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

import com.google.api.tools.framework.model.Interface;
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

  /**
   * Return the name of the class which is the GAPIC wrapper for this service interface.
   */
  public String getApiWrapperName(Interface service) {
    return service.getSimpleName() + "Api";
  }

  public String getOutputSubPath(ProtoElement element) {
    return "";
  }
}
