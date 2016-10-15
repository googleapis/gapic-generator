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

import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.CollectionConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.ServiceConfig;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;

/**
 * A CodegenContext that provides helpers specific to the use case of GAPIC (code-generation of
 * client libraries built on gRPC, or code fragments for those client libraries).
 */
public class GapicContext extends CodegenContext {

  private final Model model;
  private final ApiConfig apiConfig;

  private final ServiceMessages serviceMessages;
  private final ServiceConfig serviceConfig;

  /** Constructs the abstract instance. */
  protected GapicContext(Model model, ApiConfig apiConfig) {
    this.model = Preconditions.checkNotNull(model);
    this.apiConfig = Preconditions.checkNotNull(apiConfig);
    this.serviceMessages = new ServiceMessages();
    this.serviceConfig = new ServiceConfig();
  }

  /** Returns the associated model. */
  public Model getModel() {
    return model;
  }

  /** Returns the associated config. */
  public ApiConfig getApiConfig() {
    return apiConfig;
  }

  public ServiceMessages messages() {
    return serviceMessages;
  }

  public ServiceConfig getServiceConfig() {
    return serviceConfig;
  }

  /** Return the name of the class which is the GAPIC wrapper for this service interface. */
  public String getApiWrapperName(Interface service) {
    return service.getSimpleName() + "Api";
  }

  /** Returns the description of the proto element, in markdown format. */
  public String getDescription(ProtoElement element) {
    return DocumentationUtil.getDescription(element);
  }

  /** Get collection configuration for a method. */
  public CollectionConfig getCollectionConfig(Interface service, String entityName) {
    CollectionConfig result =
        getApiConfig().getInterfaceConfig(service).getCollectionConfig(entityName);
    if (result == null) {
      throw new IllegalStateException(
          "A collection config was not present for entity name " + entityName);
    }
    return result;
  }

  /**
   * Returns the list of optional fields from the given MethodConfig, excluding the Page Token field
   */
  public List<Field> removePageTokenFromFields(Iterable<Field> fields, MethodConfig methodConfig) {
    List<Field> newFields = new ArrayList<>();
    for (Field field : fields) {
      if (methodConfig.isPageStreaming()
          && field.equals(methodConfig.getPageStreaming().getRequestTokenField())) {
        continue;
      }
      newFields.add(field);
    }
    return newFields;
  }

  /**
   * Returns true when the method is supported by the current codegen context. By default, only non
   * stremaing methods are supported unless subclass explicitly allows. TODO: remove this method
   * when all languages support gRPC streaming.
   */
  protected boolean isSupported(Method method) {
    return !method.getRequestStreaming() && !method.getResponseStreaming();
  }

  /** Returns a list of RPC methods supported by the context. */
  public List<Method> getSupportedMethods(Interface service) {
    List<Method> simples = new ArrayList<>(service.getMethods().size());
    for (Method method : service.getMethods()) {
      if (isSupported(method)) {
        simples.add(method);
      }
    }
    return simples;
  }

  /**
   * Returns a list of RPC methods supported by the context, taking into account GRPC interface
   * rerouting. TODO replace getSupportedMethods with this when all languages are migrated.
   */
  public List<Method> getSupportedMethodsV2(Interface service) {
    InterfaceConfig interfaceConfig = getApiConfig().getInterfaceConfig(service);
    if (interfaceConfig == null) {
      throw new IllegalStateException(
          "Service not configured in GAPIC config: " + service.getFullName());
    }
    List<Method> methods = new ArrayList<>(interfaceConfig.getMethodConfigs().size());
    for (MethodConfig methodConfig : interfaceConfig.getMethodConfigs()) {
      Method method = methodConfig.getMethod();
      if (isSupported(method)) {
        methods.add(method);
      }
    }
    return methods;
  }

  public boolean isOneof(Field field) {
    return field.getOneof() != null;
  }
}
