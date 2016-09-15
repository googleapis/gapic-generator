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

  private final GoModelTypeNameConverter converter;
  private final Model model;
  private final String packagePath;

  public GoSurfaceNamer(Model model, String packagePath) {
    this(new GoModelTypeNameConverter(), model, packagePath);
  }

  private GoSurfaceNamer(GoModelTypeNameConverter converter, Model model, String packagePath) {
    super(new GoNameFormatter(), new ModelTypeFormatterImpl(converter), new GoTypeTable());
    this.converter = converter;
    this.model = model;
    this.packagePath = packagePath;
  }

  @Override
  public String getPathTemplateName(Interface service, CollectionConfig collectionConfig) {
    return inittedConstantName(
        Name.from(
            getReducedServiceName(service), collectionConfig.getEntityName(), "path", "template"));
  }

  @Override
  public String getPathTemplateNameGetter(Interface service, CollectionConfig collectionConfig) {
    return methodName(
        Name.from(getReducedServiceName(service), collectionConfig.getEntityName(), "path"));
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
    int dotIndex = typeName.indexOf('.');
    if (dotIndex >= 0) {
      typeName = typeName.substring(dotIndex + 1);
    }
    return Name.anyCamel(typeName).join("iterator").toUpperCamel();
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
  public String getGrpcClientTypeName(Interface service) {
    // getNickname() thinks the client is a message type and prepends a "*".
    // substring gets rid of the star.
    return converter.getTypeName(service).getNickname().substring(1) + "Client";
  }

  @Override
  public String getGrpcClientConstructorName(Interface service) {
    return getGrpcClientTypeName(service).replace(".", ".New");
  }

  @Override
  public String getCallOptionsTypeName(Interface service) {
    return clientNamePrefix(service).join("call").join("options").toUpperCamel();
  }

  @Override
  public String getDefaultClientOptionFunctionName(Interface service) {
    return Name.lowerCamel("default")
        .join(clientNamePrefix(service))
        .join("client")
        .join("options")
        .toLowerCamel();
  }

  @Override
  public String getDefaultCallOptionFunctionName(Interface service) {
    return Name.lowerCamel("default")
        .join(clientNamePrefix(service))
        .join("call")
        .join("options")
        .toLowerCamel();
  }

  @Override
  public String getClientTypeName(Interface service) {
    return clientNamePrefix(service).join("client").toUpperCamel();
  }

  @Override
  public String getClientConstructorName(Interface service) {
    return Name.lowerCamel("new").join(clientNamePrefix(service)).join("client").toUpperCamel();
  }

  @Override
  public String getPackageName() {
    // packagePath is in form "cloud.google.com/go/library/apiv1";
    // we want "library".
    String[] parts = packagePath.split("/");
    return parts[parts.length - 2];
  }

  private Name clientNamePrefix(Interface service) {
    String name = getReducedServiceName(service);
    // If there's only one service, or the service name matches the package name, don't prefix with
    // the service name.
    if (model.getSymbolTable().getInterfaces().size() == 1 || name.equals(getPackageName())) {
      name = "";
    }
    return Name.upperCamel(name);
  }

  public static String getOutputPath(Interface service, ApiConfig apiConfig) {
    return apiConfig.getPackageName()
        + File.separator
        + getReducedServiceName(service)
        + "_client.go";
  }

  /**
   * Returns the service name with common suffixes removed.
   *
   * For example:
   *  LoggingServiceV2 => logging
   */
  public static String getReducedServiceName(Interface service) {
    String name = service.getSimpleName().replaceAll("V[0-9]+$", "");
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
