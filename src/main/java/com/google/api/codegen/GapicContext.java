/* Copyright 2016 Google LLC
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

import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
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
  private final GapicProductConfig productConfig;

  private final ServiceMessages serviceMessages;

  public static final String API_WRAPPER_SUFFIX = "Client";

  /** Constructs the abstract instance. */
  protected GapicContext(Model model, GapicProductConfig productConfig) {
    this.model = Preconditions.checkNotNull(model);
    this.productConfig = Preconditions.checkNotNull(productConfig);
    this.serviceMessages = new ServiceMessages();
  }

  /** Returns the associated model. */
  public Model getModel() {
    return model;
  }

  /*
   * Returns the associated config.
   *
   * NOTE: The name here is out of date, but this whole class will be deprecated soon.
   */
  public GapicProductConfig getApiConfig() {
    return productConfig;
  }

  public ServiceMessages messages() {
    return serviceMessages;
  }

  /** Return the name of the class which is the GAPIC wrapper for this API interface. */
  public String getApiWrapperName(Interface apiInterface) {
    return apiInterface.getSimpleName() + API_WRAPPER_SUFFIX;
  }

  /** Returns the description of the proto element, in markdown format. */
  public String getDescription(ProtoElement element) {
    return DocumentationUtil.getDescription(element);
  }

  /** Get collection configuration for a method. */
  public SingleResourceNameConfig getSingleResourceNameConfig(String entityName) {
    return getApiConfig().getSingleResourceNameConfig(entityName);
  }

  /**
   * Returns the list of optional fields from the given GapicMethodConfig, excluding the Page Token
   * field
   */
  public List<FieldModel> removePageTokenFromFields(
      Iterable<FieldModel> fields, MethodConfig methodConfig) {
    List<FieldModel> newFields = new ArrayList<>();
    for (FieldModel field : fields) {
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

  /**
   * Returns true when the method supports retrying. By default, all supported methods support
   * retrying. TODO: when isSupported is removed, this should return true.
   */
  public boolean isRetryingSupported(Method method) {
    return isSupported(method);
  }

  /** Returns a list of RPC methods supported by the context. */
  public List<Method> getSupportedMethods(Interface apiInterface) {
    List<Method> simples = new ArrayList<>(apiInterface.getMethods().size());
    for (Method method : apiInterface.getMethods()) {
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
  public List<Method> getSupportedMethodsV2(Interface apiInterface) {
    GapicInterfaceConfig interfaceConfig = getApiConfig().getInterfaceConfig(apiInterface);
    if (interfaceConfig == null) {
      throw new IllegalStateException(
          "Service not configured in GAPIC config: " + apiInterface.getFullName());
    }
    List<Method> methods = new ArrayList<>(interfaceConfig.getMethodConfigs().size());
    for (MethodConfig methodConfig : interfaceConfig.getMethodConfigs()) {
      Method method = ((GapicMethodConfig) methodConfig).getMethod();
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
