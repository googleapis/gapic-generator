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

import com.google.api.codegen.config.CollectionConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.go.GoNameFormatter;
import com.google.api.codegen.util.go.GoTypeTable;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.common.annotations.VisibleForTesting;
import io.grpc.Status;
import java.util.List;

public class GoSurfaceNamer extends SurfaceNamer {

  private final GoModelTypeNameConverter converter;

  public GoSurfaceNamer(String packageName) {
    this(new GoModelTypeNameConverter(), packageName);
  }

  private GoSurfaceNamer(GoModelTypeNameConverter converter, String packageName) {
    super(
        new GoNameFormatter(),
        new ModelTypeFormatterImpl(converter),
        new GoTypeTable(),
        packageName);
    this.converter = converter;
  }

  @Override
  public String getPathTemplateName(Interface service, CollectionConfig collectionConfig) {
    return inittedConstantName(
        getReducedServiceName(service)
            .join(collectionConfig.getEntityName())
            .join("path")
            .join("template"));
  }

  @Override
  public String getPathTemplateNameGetter(Interface service, CollectionConfig collectionConfig) {
    return publicMethodName(
        getReducedServiceName(service).join(collectionConfig.getEntityName()).join("path"));
  }

  @Override
  public String getStaticLangReturnTypeName(Method method, MethodConfig methodConfig) {
    return converter.getTypeName(method.getOutputType()).getFullName();
  }

  @Override
  public List<String> getDocLines(Method method, MethodConfig methodConfig) {
    String text = DocumentationUtil.getDescription(method);
    text = lowerFirstLetter(text);
    return super.getDocLines(getApiMethodName(method, methodConfig.getVisibility()) + " " + text);
  }

  @Override
  public String getAndSavePagedResponseTypeName(
      Method method, ModelTypeTable typeTable, Field resourcesField) {
    String typeName = converter.getTypeNameForElementType(resourcesField.getType()).getNickname();
    int dotIndex = typeName.indexOf('.');
    if (dotIndex >= 0) {
      typeName = typeName.substring(dotIndex + 1);
    }
    return className(Name.anyCamel(typeName).join("iterator"));
  }

  private static String lowerFirstLetter(String s) {
    if (s.length() > 0) {
      s = Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
    return s;
  }

  @Override
  public String getGrpcClientTypeName(Interface service) {
    return converter.getTypeName(service).getNickname() + "Client";
  }

  @Override
  public String getCallSettingsTypeName(Interface service) {
    return className(clientNamePrefix(service).join("call").join("options"));
  }

  @Override
  public String getDefaultApiSettingsFunctionName(Interface service) {
    return privateMethodName(
        Name.from("default").join(clientNamePrefix(service)).join("client").join("options"));
  }

  @Override
  public String getDefaultCallSettingsFunctionName(Interface service) {
    return privateMethodName(
        Name.from("default").join(clientNamePrefix(service)).join("call").join("options"));
  }

  @Override
  public String getCallableName(Method method) {
    return publicMethodName(Name.upperCamel(method.getSimpleName()));
  }

  @Override
  public String getApiWrapperClassName(Interface service) {
    return className(clientNamePrefix(service).join("client"));
  }

  @Override
  public String getApiWrapperClassConstructorName(Interface service) {
    return publicMethodName(Name.from("new").join(clientNamePrefix(service)).join("client"));
  }

  @Override
  public String getApiWrapperClassConstructorExampleName(Interface service) {
    return publicMethodName(
        Name.from("example").join("new").join(clientNamePrefix(service)).join("client"));
  }

  @Override
  public String getApiMethodExampleName(Interface service, Method method) {
    return exampleFunction(service, getApiMethodName(method, VisibilityConfig.PUBLIC));
  }

  @Override
  public String getLocalPackageName() {
    // packagePath is in form "cloud.google.com/go/library/apiv1";
    // we want "library".
    String[] parts = getPackageName().split("/");
    return parts[parts.length - 2];
  }

  @Override
  public String getLocalExamplePackageName() {
    return getLocalPackageName() + "_test";
  }

  @VisibleForTesting
  Name clientNamePrefix(Interface service) {
    Name name = getReducedServiceName(service);
    // If the service name matches the package name, don't include the service name in the prefix.
    // Eg, instead of "library.NewLibraryClient", we want "library.NewClient".
    if (Name.from(getLocalPackageName()).equals(name)) {
      return Name.from();
    }
    return name;
  }

  @Override
  public String getStatusCodeName(Status.Code code) {
    return publicFieldName(Name.upperUnderscore(code.toString()));
  }

  @Override
  public String getTypeConstructor(String typeNickname) {
    if (!typeNickname.startsWith("*")) {
      return typeNickname;
    }
    return "&" + typeNickname.substring(1);
  }

  @Override
  public String getGrpcContainerTypeName(Interface service) {
    return "";
  }

  @Override
  public String getServiceFileName(Interface service) {
    return classFileNameBase(getReducedServiceName(service).join("client"));
  }

  @Override
  public String getExampleFileName(Interface service) {
    return classFileNameBase(
        getReducedServiceName(service).join("client").join("example").join("test"));
  }

  @Override
  public String getStubName(Interface service) {
    return privateFieldName(clientNamePrefix(service).join("client"));
  }

  @Override
  public String getCreateStubFunctionName(Interface service) {
    return getGrpcClientTypeName(service).replace(".", ".New");
  }

  @Override
  public String getStaticLangStreamingReturnTypeName(Method method, MethodConfig methodConfig) {
    // Unsafe string manipulation: The name looks like "LibraryService_StreamShelvesClient",
    // neither camel or underscore.
    return converter.getTypeName(method.getParent()).getNickname()
        + "_"
        + className(Name.upperCamel(method.getSimpleName()).join("client"));
  }

  @Override
  public String getIamResourceGetterFunctionName(Field field) {
    return Name.upperCamel(field.getParent().getSimpleName())
        .join(Name.upperCamelKeepUpperAcronyms("IAM"))
        .toUpperCamel();
  }

  @Override
  public String getIamResourceGetterFunctionExampleName(Interface service, Field field) {
    return exampleFunction(service, getIamResourceGetterFunctionName(field));
  }

  @Override
  public String getSettingsMemberName(Method method) {
    return publicFieldName(Name.upperCamel(method.getSimpleName()));
  }

  private String exampleFunction(Interface service, String functionName) {
    // We use "unsafe" string concatenation here.
    // Godoc expects the name to be in format "ExampleMyType_MyMethod";
    // it is the only place we have mixed camel and underscore names.
    return publicMethodName(Name.from("example").join(clientNamePrefix(service)).join("client"))
        + "_"
        + functionName;
  }
}
