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

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.OneofConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.metacode.InitFieldConfig;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.go.GoCommentReformatter;
import com.google.api.codegen.util.go.GoNameFormatter;
import com.google.api.codegen.util.go.GoTypeTable;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
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
        new GoCommentReformatter(),
        packageName,
        packageName);
    this.converter = converter;
  }

  @Override
  public SurfaceNamer cloneWithPackageName(String packageName) {
    return new GoSurfaceNamer(packageName);
  }

  @Override
  public String getPathTemplateName(
      Interface apiInterface, SingleResourceNameConfig resourceNameConfig) {
    return inittedConstantName(
        getReducedServiceName(apiInterface.getSimpleName())
            .join(resourceNameConfig.getEntityName())
            .join("path")
            .join("template"));
  }

  @Override
  public String getPathTemplateNameGetter(
      Interface apiInterface, SingleResourceNameConfig resourceNameConfig) {
    return getFormatFunctionName(apiInterface, resourceNameConfig);
  }

  @Override
  public String getFormatFunctionName(
      Interface apiInterface, SingleResourceNameConfig resourceNameConfig) {
    return publicMethodName(
        clientNamePrefix(apiInterface.getSimpleName())
            .join(resourceNameConfig.getEntityName())
            .join("path"));
  }

  @Override
  public String getStaticLangReturnTypeName(Method method, GapicMethodConfig methodConfig) {
    return converter.getTypeName(method.getOutputType()).getFullName();
  }

  @Override
  public String getLongRunningOperationTypeName(ModelTypeTable typeTable, TypeRef type) {
    return valueType(typeTable.getAndSaveNicknameFor(type));
  }

  @Override
  public List<String> getDocLines(Method method, GapicMethodConfig methodConfig) {
    return super.getDocLines(
        putDocMethodName(
            method.getSimpleName(),
            DocumentationUtil.getDescription(method),
            methodConfig.getVisibility()));
  }

  /**
   * Docs in Go usually start with the name of the method. Prepend the method name if the name isn't
   * already the first word.
   */
  @VisibleForTesting
  String putDocMethodName(String methodName, String doc, VisibilityConfig visibility) {
    doc = doc.trim();
    String firstWord = doc;
    for (int i = 0; i < firstWord.length(); i++) {
      if (Character.isWhitespace(firstWord.charAt(i))) {
        firstWord = firstWord.substring(0, i);
        break;
      }
    }
    if (firstWord.equalsIgnoreCase(methodName)) {
      doc = doc.substring(firstWord.length()).trim();
    }
    return getApiMethodName(Name.upperCamel(methodName), visibility) + " " + lowerFirstLetter(doc);
  }

  private static String lowerFirstLetter(String s) {
    if (s.length() > 0) {
      s = Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
    return s;
  }

  @Override
  public String getAndSavePagedResponseTypeName(
      Method method, ModelTypeTable typeTable, FieldConfig resourcesFieldConfig) {
    String typeName =
        converter
            .getTypeNameForElementType(resourcesFieldConfig.getField().getType())
            .getNickname();
    int dotIndex = typeName.indexOf('.');
    if (dotIndex >= 0) {
      typeName = typeName.substring(dotIndex + 1);
    }
    return publicClassName(Name.anyCamel(typeName).join("iterator"));
  }

  @Override
  public String getAndSaveOperationResponseTypeName(
      Method method, ModelTypeTable typeTable, GapicMethodConfig methodConfig) {
    return getAndSaveOperationResponseTypeName(method.getSimpleName());
  }

  @VisibleForTesting
  String getAndSaveOperationResponseTypeName(String methodName) {
    Name name = Name.upperCamel(methodName);
    if (methodName.endsWith("Operation")) {
      // Avoid creating funny names like "CreateStuffOperationOperation".
      name = name.join("handle");
    } else {
      name = name.join("operation");
    }
    return publicClassName(name);
  }

  @Override
  public String valueType(String type) {
    for (int i = 0; i < type.length(); i++) {
      if (type.charAt(i) != '*') {
        return type.substring(i);
      }
    }
    return "";
  }

  private String unqualifyTypeName(String typeName) {
    int dotIndex = typeName.indexOf('.');
    if (dotIndex >= 0) {
      typeName = typeName.substring(dotIndex + 1);
    }
    return typeName;
  }

  @Override
  public String getGrpcServerTypeName(Interface apiInterface) {
    return converter.getTypeName(apiInterface).getNickname() + "Server";
  }

  @Override
  public String getGrpcClientTypeName(Interface apiInterface) {
    return converter.getTypeName(apiInterface).getNickname() + "Client";
  }

  @Override
  public String getServerRegisterFunctionName(Interface apiInterface) {
    return converter.getTypeName(apiInterface).getNickname().replace(".", ".Register") + "Server";
  }

  @Override
  public String getCallSettingsTypeName(Interface apiInterface) {
    return publicClassName(
        clientNamePrefix(apiInterface.getSimpleName()).join("call").join("options"));
  }

  @Override
  public String getDefaultApiSettingsFunctionName(Interface apiInterface) {
    return privateMethodName(
        Name.from("default")
            .join(clientNamePrefix(apiInterface.getSimpleName()))
            .join("client")
            .join("options"));
  }

  @Override
  public String getDefaultCallSettingsFunctionName(Interface apiInterface) {
    return privateMethodName(
        Name.from("default")
            .join(clientNamePrefix(apiInterface.getSimpleName()))
            .join("call")
            .join("options"));
  }

  @Override
  public String getCallableName(Method method) {
    return publicMethodName(Name.upperCamel(method.getSimpleName()));
  }

  @Override
  public String getApiWrapperClassName(InterfaceConfig interfaceConfig) {
    // TODO support non-Gapic inputs
    GapicInterfaceConfig gapicInterfaceConfig = (GapicInterfaceConfig) interfaceConfig;
    return publicClassName(
        clientNamePrefix(gapicInterfaceConfig.getInterface().getSimpleName()).join("client"));
  }

  @Override
  public String getApiWrapperClassConstructorName(Interface apiInterface) {
    return publicMethodName(
        Name.from("new").join(clientNamePrefix(apiInterface.getSimpleName())).join("client"));
  }

  @Override
  public String getApiWrapperClassConstructorExampleName(Interface apiInterface) {
    return publicMethodName(
        Name.from("example")
            .join("new")
            .join(clientNamePrefix(apiInterface.getSimpleName()))
            .join("client"));
  }

  @Override
  public String getApiMethodExampleName(Interface apiInterface, Method method) {
    return exampleFunction(apiInterface, getApiMethodName(method, VisibilityConfig.PUBLIC));
  }

  @Override
  public String getGrpcStreamingApiMethodExampleName(Interface apiInterface, Method method) {
    return exampleFunction(apiInterface, getApiMethodName(method, VisibilityConfig.PUBLIC));
  }

  @Override
  public String getAsyncApiMethodName(Method method, VisibilityConfig visibility) {
    return getApiMethodName(method, visibility);
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
  Name clientNamePrefix(String interfaceSimpleName) {
    Name name = getReducedServiceName(interfaceSimpleName);
    // If the service name matches the package name, don't include the service name in the prefix.
    // Eg, instead of "library.NewLibraryClient", we want "library.NewClient".
    // The casing of the service name does not matter.
    // Elements of the package path are usually all lowercase, even if they are multi-worded.
    if (name.toLowerCamel().equalsIgnoreCase(getLocalPackageName())) {
      return Name.from();
    }
    return name;
  }

  @Override
  public String getStatusCodeName(Status.Code code) {
    String codeString = code.toString();
    if (code.equals(Status.Code.CANCELLED)) {
      codeString = "CANCELED";
    }
    return publicFieldName(Name.upperUnderscore(codeString));
  }

  @Override
  public String getTypeConstructor(String typeNickname) {
    if (!typeNickname.startsWith("*")) {
      return typeNickname;
    }
    return "&" + typeNickname.substring(1);
  }

  @Override
  public String getGrpcContainerTypeName(Interface apiInterface) {
    return "";
  }

  @Override
  public String getServiceFileName(GapicInterfaceConfig interfaceConfig) {
    return classFileNameBase(
        getReducedServiceName(interfaceConfig.getInterface().getSimpleName()).join("client"));
  }

  @Override
  public String getExampleFileName(Interface apiInterface) {
    return classFileNameBase(
        getReducedServiceName(apiInterface.getSimpleName())
            .join("client")
            .join("example")
            .join("test"));
  }

  @Override
  public String getStubName(Interface apiInterface) {
    return privateFieldName(clientNamePrefix(apiInterface.getSimpleName()).join("client"));
  }

  @Override
  public String getCreateStubFunctionName(Interface apiInterface) {
    return getGrpcClientTypeName(apiInterface).replace(".", ".New");
  }

  @Override
  public String getStreamingServerName(Method method) {
    // Unsafe string manipulation: The name looks like "LibraryService_StreamShelvesServer",
    // neither camel or underscore.
    return converter.getTypeName(method.getParent()).getNickname()
        + "_"
        + publicClassName(Name.upperCamel(method.getSimpleName()).join("server"));
  }

  @Override
  public String getGrpcStreamingApiReturnTypeName(Method method, ModelTypeTable typeTable) {
    // Unsafe string manipulation: The name looks like "LibraryService_StreamShelvesClient",
    // neither camel or underscore.
    return converter.getTypeName(method.getParent()).getNickname()
        + "_"
        + publicClassName(Name.upperCamel(method.getSimpleName()).join("client"));
  }

  @Override
  public String getIamResourceGetterFunctionName(Field field) {
    return Name.upperCamel(field.getParent().getSimpleName())
        .join(Name.upperCamelKeepUpperAcronyms("IAM"))
        .toUpperCamel();
  }

  @Override
  public String getIamResourceGetterFunctionExampleName(Interface apiInterface, Field field) {
    return exampleFunction(apiInterface, getIamResourceGetterFunctionName(field));
  }

  @Override
  public String getSettingsMemberName(Method method) {
    return publicFieldName(Name.upperCamel(method.getSimpleName()));
  }

  private String exampleFunction(Interface apiInterface, String functionName) {
    // We use "unsafe" string concatenation here.
    // Godoc expects the name to be in format "ExampleMyType_MyMethod";
    // it is the only place we have mixed camel and underscore names.
    return publicMethodName(
            Name.from("example")
                .join(clientNamePrefix(apiInterface.getSimpleName()))
                .join("client"))
        + "_"
        + functionName;
  }

  @Override
  public String getMockGrpcServiceImplName(Interface apiInterface) {
    return privateClassName(
        Name.from("mock").join(getReducedServiceName(apiInterface.getSimpleName())).join("server"));
  }

  @Override
  public String getMockServiceVarName(Interface apiInterface) {
    return localVarName(
        Name.from("mock").join(getReducedServiceName(apiInterface.getSimpleName())));
  }

  @Override
  public String getTestCaseName(SymbolTable symbolTable, Method method) {
    Name testCaseName =
        symbolTable.getNewSymbol(
            Name.upperCamel("Test", method.getParent().getSimpleName(), method.getSimpleName()));
    return publicMethodName(testCaseName);
  }

  @Override
  public String getExceptionTestCaseName(SymbolTable symbolTable, Method method) {
    Name testCaseName =
        symbolTable.getNewSymbol(
            Name.upperCamel(
                "Test", method.getParent().getSimpleName(), method.getSimpleName(), "Error"));
    return publicMethodName(testCaseName);
  }

  @Override
  public String getFieldGetFunctionName(TypeRef type, Name identifier) {
    return publicMethodName(identifier);
  }

  @Override
  public String getOneofVariantTypeName(OneofConfig oneof) {
    return String.format(
        "%s_%s",
        converter.getTypeName(oneof.parentType(), false).getNickname(),
        publicFieldName(Name.from(oneof.field().getSimpleName())));
  }

  @Override
  public String getSmokeTestClassName(GapicInterfaceConfig interfaceConfig) {
    // Go smoke test is a just a method; return method name instead.
    return publicMethodName(Name.upperCamel("Test", getInterfaceName(interfaceConfig), "Smoke"));
  }

  @Override
  public String injectRandomStringGeneratorCode(String randomString) {
    return randomString.replace(
        InitFieldConfig.RANDOM_TOKEN, "\" + strconv.FormatInt(time.Now().UnixNano(), 10) + \"");
  }
}
