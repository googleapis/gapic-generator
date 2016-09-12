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
package com.google.api.codegen.transformer.go;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.go.GoNameFormatter;
import com.google.api.codegen.util.go.GoTypeTable;
import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;

import io.grpc.Status;

import java.io.File;
import java.util.List;

public class GoSurfaceNamer extends SurfaceNamer {
  private final Model model;
  private final Interface service;
  private final ApiConfig apiConfig;
  private final GoModelTypeNameConverter converter;

  public GoSurfaceNamer(Model model, Interface service, ApiConfig apiConfig) {
    this(model, service, apiConfig, new GoModelTypeNameConverter());
  }

  private GoSurfaceNamer(
      Model model, Interface service, ApiConfig apiConfig, GoModelTypeNameConverter converter) {
    super(new GoNameFormatter(), new ModelTypeFormatterImpl(converter), new GoTypeTable());
    this.model = model;
    this.service = service;
    this.apiConfig = apiConfig;
    this.converter = converter;
  }

  @Override
  public String getPathTemplateName(CollectionConfig collectionConfig) {
    return inittedConstantName(
        Name.from(getReducedServiceName(), collectionConfig.getEntityName(), "path", "template"));
  }

  @Override
  public String getPathTemplateNameGetter(CollectionConfig collectionConfig) {
    return methodName(Name.from(getReducedServiceName(), collectionConfig.getEntityName(), "path"));
  }

  @Override
  public String getStaticLangReturnTypeName(Method method, MethodConfig methodConfig) {
    return converter.getTypeName(method.getOutputType()).getFullName();
  }

  @Override
  public List<String> getDocLines(ProtoElement element) {
    if (!(element instanceof Method)) {
      return super.getDocLines(element);
    }
    Method method = (Method) element;
    String text = DocumentationUtil.getDescription(method);
    text = lowerFirstLetter(text);
    return super.getDocLines(getApiMethodName(method) + " " + text);
  }

  @Override
  public String getAndSavePagedResponseTypeName(ModelTypeTable typeTable, TypeRef resourceType) {
    String typeName = converter.getTypeNameForElementType(resourceType).getNickname();
    int p = typeName.indexOf('.');
    if (p >= 0) {
      typeName = typeName.substring(p + 1);
    }
    return Name.anyCamel(typeName).toUpperCamel() + "Iterator";
  }

  private static String lowerFirstLetter(String s) {
    if (s.length() > 0) {
      s = Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
    return s;
  }

  private static String upperFirstLetter(String s) {
    if (s.length() > 0) {
      s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    return s;
  }

  @Override
  public String getFieldGetFunctionName(TypeRef type, Name identifier) {
    return methodName(identifier);
  }

  public String getGrpcClientTypeName() {
    // getNickname() thinks the client is a message type and appends a "*".
    // substring gets rid of the star.
    return converter.getTypeName(service).getNickname().substring(1) + "Client";
  }

  public String getGrpcClientConstructorName() {
    return getGrpcClientTypeName().replace(".", ".New");
  }

  public String getCallOptionsName() {
    return clientNamePrefix() + "CallOptions";
  }

  public String getDefaultClientOptionFunc() {
    return "default" + getClientName() + "Options";
  }

  public String getDefaultCallOptionFunc() {
    return "default" + getCallOptionsName();
  }

  public String getClientName() {
    return clientNamePrefix() + "Client";
  }

  public String getClientConstructorName() {
    return "New" + getClientName();
  }

  public String getServiceName() {
    return service.getSimpleName();
  }

  /**
   * Returns the Go package name.
   *
   * Retrieved by splitting the package string on `/` and returning the second to last string if
   * possible, and otherwise the last string in the resulting array.
   *
   * TODO(saicheems): Figure out how to reliably get the intended value...
   */
  public String getPackageName() {
    String split[] = apiConfig.getPackageName().split("/");
    if (split.length - 2 >= 0) {
      return split[split.length - 2];
    }
    return split[split.length - 1];
  }

  private String clientNamePrefix() {
    String name = getReducedServiceName();
    // If there's only one service, or the service name matches the package name, don't prefix with
    // the service name.
    if (model.getSymbolTable().getInterfaces().size() == 1 || name.equals(getPackageName())) {
      name = "";
    }
    return name;
  }

  public String getOutputPath() {
    return apiConfig.getPackageName() + File.separator + getReducedServiceName() + "_client.go";
  }

  /**
   * Returns the service name with common suffixes removed.
   *
   * For example:
   *  LoggingServiceV2 => logging
   */
  private String getReducedServiceName() {
    String name = getServiceName().replaceAll("V[0-9]+$", "");
    name = name.replaceAll("Service$", "");
    return Name.upperCamel(name).toLowerUnderscore();
  }

  public String getStatusCodeName(Status.Code code) {
    return Name.upperUnderScore(code.toString()).toUpperCamel();
  }

  @Override
  public String getGrpcContainerTypeName(Interface service) {
    return "";
  }
}
