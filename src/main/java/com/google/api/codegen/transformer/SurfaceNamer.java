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
package com.google.api.codegen.transformer;

import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NameFormatter;
import com.google.api.codegen.util.NameFormatterDelegator;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;

import io.grpc.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * A SurfaceNamer provides language-specific names for specific components of a view for a surface.
 *
 * Naming is composed of two steps:
 *
 * 1. Composing a Name instance with the name pieces
 * 2. Formatting the Name for the particular type of identifier needed.
 *
 * This class delegates step 2 to the provided name formatter, which generally
 * would be a language-specific namer.
 */
public class SurfaceNamer extends NameFormatterDelegator {
  private ModelTypeFormatter modelTypeFormatter;
  private TypeNameConverter typeNameConverter;

  public SurfaceNamer(
      NameFormatter languageNamer,
      ModelTypeFormatter modelTypeFormatter,
      TypeNameConverter typeNameConverter) {
    super(languageNamer);
    this.modelTypeFormatter = modelTypeFormatter;
    this.typeNameConverter = typeNameConverter;
  }

  public ModelTypeFormatter getModelTypeFormatter() {
    return modelTypeFormatter;
  }

  public TypeNameConverter getTypeNameConverter() {
    return typeNameConverter;
  }

  public String getNotImplementedString(String feature) {
    return "$ NOT IMPLEMENTED: " + feature + " $";
  }

  /** The full path to the source file  */
  public String getSourceFilePath(String path, String className) {
    return getNotImplementedString("SurfaceNamer.getSourceFilePath");
  }

  /** The name of the class that implements a particular proto interface. */
  public String getApiWrapperClassName(Interface interfaze) {
    return className(Name.upperCamel(interfaze.getSimpleName(), "Api"));
  }

  /**
   * The name of the constructor for the service client.
   * The client is VKit generated, not GRPC.
   */
  public String getApiWrapperClassConstructorName(Interface interfaze) {
    return className(Name.upperCamel(interfaze.getSimpleName(), "Api"));
  }

  /**
   * The name of example of the constructor for the service client.
   * The client is VKit generated, not GRPC.
   */
  public String getApiWrapperClassConstructorExampleName(Interface interfaze) {
    return getApiWrapperClassConstructorName(interfaze);
  }

  /**
   * Constructor name for the type with the given nickname.
   */
  public String getTypeConstructor(String typeNickname) {
    return typeNickname;
  }

  /**
   * The name of a variable that holds an instance of the class that implements
   * a particular proto interface.
   */
  public String getApiWrapperVariableName(Interface interfaze) {
    return privateVarName(Name.upperCamel(interfaze.getSimpleName(), "Api"));
  }

  /**
   * The name of a variable that holds an instance of the module that contains
   * the implementation of a particular proto interface. So far it is used by
   * just NodeJS.
   */
  public String getApiWrapperModuleName(Interface interfaze) {
    return getNotImplementedString("SurfaceNamer.getApiWrapperModuleName");
  }

  /**
   * The name of the settings class for a particular proto interface;
   * not used in most languages.
   */
  public String getApiSettingsClassName(Interface interfaze) {
    return className(Name.upperCamel(interfaze.getSimpleName(), "Settings"));
  }

  /** The function name to retrieve default client option */
  public String getDefaultApiSettingsFunctionName(Interface service) {
    return getNotImplementedString("SurfaceNamer.getDefaultClientOptionFunctionName");
  }

  /**
   * The name of a variable that holds the settings class for a particular
   * proto interface; not used in most languages.
   */
  public String getApiSettingsVariableName(Interface interfaze) {
    return privateVarName(Name.upperCamel(interfaze.getSimpleName(), "Settings"));
  }

  /**
   * The name of the builder class for the settings class for a particular
   * proto interface; not used in most languages.
   */
  public String getApiSettingsBuilderVarName(Interface interfaze) {
    return privateVarName(Name.upperCamel(interfaze.getSimpleName(), "SettingsBuilder"));
  }

  /**
   * The variable name for the given identifier. If it has formatting config
   * (specified by initValueConfig), then its name reflects that.
   */
  public String getVariableName(Name identifier, InitValueConfig initValueConfig) {
    if (initValueConfig == null || !initValueConfig.hasFormattingConfig()) {
      return privateVarName(identifier);
    } else {
      return privateVarName(Name.from("formatted").join(identifier));
    }
  }

  /** The name of the field. */
  public String getFieldName(Field field) {
    return publicVarName(Name.from(field.getSimpleName()));
  }

  /** The function name to set the given proto field. */
  public String getFieldSetFunctionName(Field field) {
    return getFieldSetFunctionName(field.getType(), Name.from(field.getSimpleName()));
  }

  /** The function name to set a field having the given type and name. */
  public String getFieldSetFunctionName(TypeRef type, Name identifier) {
    if (type.isMap()) {
      return publicMethodName(Name.from("put", "all").join(identifier));
    } else if (type.isRepeated()) {
      return publicMethodName(Name.from("add", "all").join(identifier));
    } else {
      return publicMethodName(Name.from("set").join(identifier));
    }
  }

  /** The function name to get the given proto field. */
  public String getFieldGetFunctionName(Field field) {
    return getFieldGetFunctionName(field.getType(), Name.from(field.getSimpleName()));
  }

  /** The function name to get a field having the given type and name. */
  public String getFieldGetFunctionName(TypeRef type, Name identifier) {
    if (type.isRepeated() && !type.isMap()) {
      return publicMethodName(Name.from("get").join(identifier).join("list"));
    } else {
      return publicMethodName(Name.from("get").join(identifier));
    }
  }

  /**
   * The function name to get the count of elements in the given field.
   *
   * @throws IllegalArgumentException if the field is not a repeated field.
   */
  public String getFieldCountGetFunctionName(Field field) {
    if (field.isRepeated()) {
      return publicMethodName(Name.from("get", field.getSimpleName(), "count"));
    } else {
      throw new IllegalArgumentException(
          "Non-repeated field " + field.getSimpleName() + " has no count function.");
    }
  }

  /**
   * The function name to get an element by index from the given field.
   *
   * @throws IllegalArgumentException if the field is not a repeated field.
   */
  public String getByIndexGetFunctionName(Field field) {
    if (field.isRepeated()) {
      return publicMethodName(Name.from("get", field.getSimpleName()));
    } else {
      throw new IllegalArgumentException(
          "Non-repeated field " + field.getSimpleName() + " has no get-by-index function.");
    }
  }

  /**
   * The name of the package
   */
  public String getLocalPackageName() {
    return getNotImplementedString("SurfaceNamer.getLocalPackageName");
  }

  /**
   * The name of a path template constant for the given collection,
   * to be held in an API wrapper class.
   */
  public String getPathTemplateName(Interface service, CollectionConfig collectionConfig) {
    return inittedConstantName(Name.from(collectionConfig.getEntityName(), "path", "template"));
  }

  /** The name of a getter function to get a particular path template for the given collection. */
  public String getPathTemplateNameGetter(Interface service, CollectionConfig collectionConfig) {
    return publicMethodName(Name.from("get", collectionConfig.getEntityName(), "name", "template"));
  }

  /** The name of the path template resource, in human format. */
  public String getPathTemplateResourcePhraseName(CollectionConfig collectionConfig) {
    return Name.from(collectionConfig.getEntityName()).toPhrase();
  }

  /** The function name to format the entity for the given collection. */
  public String getFormatFunctionName(CollectionConfig collectionConfig) {
    return staticFunctionName(Name.from("format", collectionConfig.getEntityName(), "name"));
  }

  /**
   * The function name to parse a variable from the string representing the entity for
   * the given collection.
   */
  public String getParseFunctionName(String var, CollectionConfig collectionConfig) {
    return staticFunctionName(
        Name.from("parse", var, "from", collectionConfig.getEntityName(), "name"));
  }

  /** The entity name for the given collection. */
  public String getEntityName(CollectionConfig collectionConfig) {
    return privateVarName(Name.from(collectionConfig.getEntityName()));
  }

  /** The parameter name for the entity for the given collection config. */
  public String getEntityNameParamName(CollectionConfig collectionConfig) {
    return privateVarName(Name.from(collectionConfig.getEntityName(), "name"));
  }

  /** The parameter name for the given lower-case field name. */
  public String getParamName(String var) {
    return privateVarName(Name.from(var));
  }

  /** The page streaming descriptor name for the given method. */
  public String getPageStreamingDescriptorName(Method method) {
    return privateVarName(Name.upperCamel(method.getSimpleName(), "PageStreamingDescriptor"));
  }

  /** The name of the constant to hold the page streaming descriptor for the given method. */
  public String getPageStreamingDescriptorConstName(Method method) {
    return inittedConstantName(Name.upperCamel(method.getSimpleName()).join("page_str_desc"));
  }

  /** The name of the constant to hold the bundling descriptor for the given method. */
  public String getBundlingDescriptorConstName(Method method) {
    return inittedConstantName(Name.upperCamel(method.getSimpleName()).join("bundling_desc"));
  }

  /** Adds the imports used in the implementation of page streaming descriptors. */
  public void addPageStreamingDescriptorImports(ModelTypeTable typeTable) {
    // do nothing
  }

  /** Adds the imports used in the implementation of bundling descriptors. */
  public void addBundlingDescriptorImports(ModelTypeTable typeTable) {
    // do nothing
  }

  /** Adds the imports used for page streaming call settings. */
  public void addPageStreamingCallSettingsImports(ModelTypeTable typeTable) {
    // do nothing
  }

  /** Adds the imports used for bundling call settings. */
  public void addBundlingCallSettingsImports(ModelTypeTable typeTable) {
    // do nothing
  }

  /** The key to use in a dictionary for the given method. */
  public String getMethodKey(Method method) {
    return keyName(Name.upperCamel(method.getSimpleName()));
  }

  /** The path to the client config for the given interface. */
  public String getClientConfigPath(Interface service) {
    return getNotImplementedString("SurfaceNamer.getClientConfigPath");
  }

  /**
   * Human-friendly name of this service
   */
  public String getServicePhraseName(Interface service) {
    return Name.upperCamel(service.getSimpleName()).toPhrase();
  }

  /**
   * The type name of the Grpc client class.
   * This needs to match what Grpc generates for the particular language.
   */
  public String getGrpcClientTypeName(Interface service) {
    NamePath namePath = typeNameConverter.getNamePath(modelTypeFormatter.getFullNameFor(service));
    String className = className(Name.upperCamel(namePath.getHead(), "Client"));
    return qualifiedName(namePath.withHead(className));
  }

  /**
   * Name of the Grpc client constructor.
   * This needs to match what Grpc generates for the particular language.
   */
  public String getGrpcClientConstructorName(Interface service) {
    return getGrpcClientTypeName(service);
  }

  /**
   * The type name of the Grpc container class.
   * This needs to match what Grpc generates for the particular language.
   */
  public String getGrpcContainerTypeName(Interface service) {
    NamePath namePath = typeNameConverter.getNamePath(modelTypeFormatter.getFullNameFor(service));
    String className = className(Name.upperCamel(namePath.getHead(), "Grpc"));
    return qualifiedName(namePath.withHead(className));
  }

  /**
   * The type name of the Grpc service class
   * This needs to match what Grpc generates for the particular language.
   */
  public String getGrpcServiceClassName(Interface service) {
    NamePath namePath = typeNameConverter.getNamePath(modelTypeFormatter.getFullNameFor(service));
    String grpcContainerName = className(Name.upperCamel(namePath.getHead(), "Grpc"));
    String serviceClassName = className(Name.upperCamel(service.getSimpleName(), "ImplBase"));
    return qualifiedName(namePath.withHead(grpcContainerName).append(serviceClassName));
  }

  /**
   * The type name of the method constant in the Grpc container class.
   * This needs to match what Grpc generates for the particular language.
   */
  public String getGrpcMethodConstant(Method method) {
    return inittedConstantName(Name.from("method").join(Name.upperCamel(method.getSimpleName())));
  }

  /** The name of the surface method which can call the given API method. */
  public String getApiMethodName(Method method) {
    return publicMethodName(Name.upperCamel(method.getSimpleName()));
  }

  /** The name of the example for the method. */
  public String getApiMethodExampleName(Interface interfaze, Method method) {
    return getApiMethodName(method);
  }

  /**
   * The name of a variable to hold a value for the given proto message field
   * (such as a flattened parameter).
   */
  public String getVariableName(Field field) {
    return privateVarName(Name.from(field.getSimpleName()));
  }

  /**
   * Returns true if the request object param type for the given field should be imported.
   */
  public boolean shouldImportRequestObjectParamType(Field field) {
    return true;
  }

  /**
   * Returns true if the request object param element type for the given field should be imported.
   */
  public boolean shouldImportRequestObjectParamElementType(Field field) {
    return true;
  }

  /** Converts the given text to doc lines in the format of the current language. */
  public List<String> getDocLines(String text) {
    return CommonRenderingUtil.getDocLines(text);
  }

  /** Provides the doc lines for the given proto element in the current language. */
  public List<String> getDocLines(ProtoElement element) {
    return getDocLines(DocumentationUtil.getDescription(element));
  }

  /** The doc lines that declare what exception(s) are thrown for an API method. */
  public List<String> getThrowsDocLines() {
    return new ArrayList<>();
  }

  /** The public access modifier for the current language. */
  public String getPublicAccessModifier() {
    return "public";
  }

  /** The private access modifier for the current language. */
  public String getPrivateAccessModifier() {
    return "private";
  }

  /**
   * The name used in Grpc for the given API method.
   * This needs to match what Grpc generates.
   */
  public String getGrpcMethodName(Method method) {
    // This might seem silly, but it makes clear what we're dealing with (upper camel).
    // This is language-independent because of gRPC conventions.
    return Name.upperCamel(method.getSimpleName()).toUpperCamel();
  }

  /** The type name for retry settings. */
  public String getRetrySettingsTypeName() {
    return getNotImplementedString("SurfaceNamer.getRetrySettingsClassName");
  }

  /** Type name of the status code */
  public String getStatusCodeName(Status.Code code) {
    return inittedConstantName(Name.upperUnderscore(code.toString()));
  }

  /** The type name for an optional array argument; not used in most languages. */
  public String getOptionalArrayTypeName() {
    return getNotImplementedString("SurfaceNamer.getOptionalArrayTypeName");
  }

  /** The return type name in a dynamic language for the given method. */
  public String getDynamicLangReturnTypeName(Method method, MethodConfig methodConfig) {
    return getNotImplementedString("SurfaceNamer.getDynamicReturnTypeName");
  }

  /** The return type name in a static language for the given method. */
  public String getStaticLangReturnTypeName(Method method, MethodConfig methodConfig) {
    return getNotImplementedString("SurfaceNamer.getStaticReturnTypeName");
  }

  /** The name of the paged callable variant of the given method. */
  public String getPagedCallableMethodName(Method method) {
    return publicMethodName(Name.upperCamel(method.getSimpleName(), "PagedCallable"));
  }

  /** The name of the example for the paged callable variant. */
  public String getPagedCallableMethodExampleName(Interface interfaze, Method method) {
    return getPagedCallableMethodName(method);
  }

  /** The name of the callable for the paged callable variant of the given method. */
  public String getPagedCallableName(Method method) {
    return privateVarName(Name.upperCamel(method.getSimpleName(), "PagedCallable"));
  }
  /** The name of the plain callable variant of the given method. */
  public String getCallableMethodName(Method method) {
    return publicMethodName(Name.upperCamel(method.getSimpleName(), "Callable"));
  }

  /** The name of the example for the plain callable variant. */
  public String getCallableMethodExampleName(Interface interfaze, Method method) {
    return getCallableMethodName(method);
  }

  /** The name of the plain callable for the given method. */
  public String getCallableName(Method method) {
    return privateVarName(Name.upperCamel(method.getSimpleName(), "Callable"));
  }

  /** The name of the settings member name for the given method. */
  public String getCallSettingsMemberName(Method method) {
    return publicMethodName(Name.upperCamel(method.getSimpleName(), "Settings"));
  }

  /** The getter function name for the settings for the given method. */
  public String getCallSettingsFunctionName(Method method) {
    return getCallSettingsMemberName(method);
  }

  /** The type name of call options */
  public String getCallSettingsTypeName(Interface service) {
    return className(Name.upperCamel(service.getSimpleName(), "Settings"));
  }

  /** The function name to retrieve default call option */
  public String getDefaultCallSettingsFunctionName(Interface service) {
    return publicMethodName(Name.upperCamel(service.getSimpleName(), "Settings"));
  }

  /**
   * The generic-aware response type name for the given type.
   * For example, in Java, this will be the type used for ListenableFuture&lt;...&gt;.
   */
  public String getGenericAwareResponseTypeName(TypeRef outputType) {
    return getNotImplementedString("SurfaceNamer.getGenericAwareResponseType");
  }

  /**
   * The function name to get the given proto field as a list.
   *
   * @throws IllegalArgumentException if the field is not a repeated field.
   */
  public String getGetResourceListCallName(Field resourcesField) {
    if (resourcesField.isRepeated()) {
      return publicMethodName(Name.from("get", resourcesField.getSimpleName(), "list"));
    } else {
      throw new IllegalArgumentException(
          "Non-repeated field "
              + resourcesField.getSimpleName()
              + " cannot be accessed as a list.");
    }
  }

  /**
   * Computes the nickname of the response type name for the given resource type, saves it in the
   * given type table, and returns it.
   */
  public String getAndSavePagedResponseTypeName(
      ModelTypeTable typeTable, TypeRef inputType, TypeRef outputType, TypeRef resourceType) {
    return getNotImplementedString("SurfaceNamer.getAndSavePagedResponseTypeName");
  }

  /**
   * The test case name for the given method.
   */
  public String getTestCaseName(SymbolTable symbolTable, Method method) {
    Name testCaseName = symbolTable.getNewSymbol(Name.upperCamel(method.getSimpleName(), "Test"));
    return publicMethodName(testCaseName);
  }

  /** The unit test class name for the given API service. */
  public String getUnitTestClassName(Interface service) {
    return className(Name.upperCamel(service.getSimpleName(), "Test"));
  }

  /** The smoke test class name for the given API service. */
  public String getSmokeTestClassName(Interface service) {
    return className(Name.upperCamel(service.getSimpleName(), "Smoke", "Test"));
  }

  /** The class name of the mock gRPC service for the given API service. */
  public String getMockServiceClassName(Interface service) {
    return className(Name.upperCamel("Mock", service.getSimpleName()));
  }

  /** The class name of a variable to hold the mock gRPC service for the given API service. */
  public String getMockServiceVarName(Interface service) {
    return privateVarName(Name.upperCamel("Mock", service.getSimpleName()));
  }

  /** The class name of the mock gRPC service implementation for the given API service. */
  public String getMockGrpcServiceImplName(Interface service) {
    return className(Name.upperCamel("Mock", service.getSimpleName(), "Impl"));
  }

  /** The file name for an API service. */
  public String getServiceFileName(Interface service, String packageName) {
    return getNotImplementedString("SurfaceNamer.getApiName");
  }

  /**
   * The fully qualified class name of a an API service.
   * TODO: Support the general pattern of package + class name in NameFormatter.
   */
  public String getFullyQualifiedApiWrapperClassName(Interface interfaze, String packageName) {
    return getNotImplementedString("SurfaceNamer.getFullyQualifiedApiWrapperClassName");
  }

  /** The name of the variable that will hold the stub for a service. */
  public String getStubName(Interface service) {
    return privateVarName(Name.upperCamel(service.getSimpleName(), "Stub"));
  }

  /** The name of the function that will create a stub. */
  public String getCreateStubFunctionName(Interface service) {
    return privateVarName(Name.upperCamel("Create", service.getSimpleName(), "Stub", "Function"));
  }

  /** The name of the array which will hold the methods for a given stub. */
  public String getStubMethodsArrayName(Interface service) {
    return privateVarName(Name.upperCamel(service.getSimpleName(), "Stub", "Methods"));
  }

  /** The name of the import for a specific grpcClient */
  public String getGrpcClientImportName(Interface service) {
    return getNotImplementedString("SurfaceNamer.getGrpcClientImportName");
  }

  /** The fully qualified type name for the stub of a service. */
  public String getFullyQualifiedStubType(Interface service) {
    return getNotImplementedString("SurfaceNamer.getFullyQualifiedStubType");
  }

  /** The name of the variable to hold the grpc client of a service. */
  public String getGrpcClientVariableName(Interface service) {
    return privateVarName(Name.upperCamel(service.getSimpleName(), "Client"));
  }

  /** The qualified namespace of a service. */
  public String getNamespace(Interface service) {
    NamePath namePath = typeNameConverter.getNamePath(modelTypeFormatter.getFullNameFor(service));
    return qualifiedName(namePath.withoutHead());
  }

  public String getProtoFileImportFromService(Interface service) {
    return getNotImplementedString("SurfaceNamer.getProtoFileImportFromService");
  }

  /**
   * Returns the service name with common suffixes removed.
   *
   * For example:
   *  "LoggingServiceV2" becomes Name("Logging")
   */
  public Name getReducedServiceName(Interface service) {
    String name = service.getSimpleName().replaceAll("V[0-9]+$", "");
    name = name.replaceAll("Service$", "");
    return Name.upperCamel(name);
  }
}
