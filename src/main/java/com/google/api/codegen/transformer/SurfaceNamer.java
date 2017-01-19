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

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.ResourceNameConfig;
import com.google.api.codegen.config.ResourceNameType;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NameFormatter;
import com.google.api.codegen.util.NameFormatterDelegator;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.viewmodel.ServiceMethodType;
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
 * <p>Naming is composed of two steps:
 *
 * <p>1. Composing a Name instance with the name pieces 2. Formatting the Name for the particular
 * type of identifier needed.
 *
 * <p>This class delegates step 2 to the provided name formatter, which generally would be a
 * language-specific namer.
 */
public class SurfaceNamer extends NameFormatterDelegator {
  private final ModelTypeFormatter modelTypeFormatter;
  private final TypeNameConverter typeNameConverter;
  private final String packageName;

  public SurfaceNamer(
      NameFormatter languageNamer,
      ModelTypeFormatter modelTypeFormatter,
      TypeNameConverter typeNameConverter,
      String packageName) {
    super(languageNamer);
    this.modelTypeFormatter = modelTypeFormatter;
    this.typeNameConverter = typeNameConverter;
    this.packageName = packageName;
  }

  public ModelTypeFormatter getModelTypeFormatter() {
    return modelTypeFormatter;
  }

  public TypeNameConverter getTypeNameConverter() {
    return typeNameConverter;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getTestPackageName() {
    return getNotImplementedString("SurfaceNamer.getTestPackageName");
  }

  public String getNotImplementedString(String feature) {
    return "$ NOT IMPLEMENTED: " + feature + " $";
  }

  /** The full path to the source file */
  public String getSourceFilePath(String path, String publicClassName) {
    return getNotImplementedString("SurfaceNamer.getSourceFilePath");
  }

  /** The name of the class that implements a particular proto interface. */
  public String getApiWrapperClassName(Interface interfaze) {
    return publicClassName(Name.upperCamel(interfaze.getSimpleName(), "Client"));
  }

  /** The name of the implementation class that implements a particular proto interface. */
  public String getApiWrapperClassImplName(Interface interfaze) {
    return getNotImplementedString("SurfaceNamer.getApiWrapperClassImplName");
  }

  /** The name of the class that implements snippets for a particular proto interface. */
  public String getApiSnippetsClassName(Interface interfaze) {
    return publicClassName(Name.upperCamel(interfaze.getSimpleName(), "ApiSnippets"));
  }

  /** The name of the class that contains paged list response wrappers. */
  public String getPagedResponseWrappersClassName() {
    return publicClassName(Name.upperCamel("PagedResponseWrappers"));
  }

  protected Name getResourceTypeNameObject(ResourceNameConfig resourceNameConfig) {
    String entityName = resourceNameConfig.getEntityName();
    ResourceNameType resourceNameType = resourceNameConfig.getResourceNameType();
    switch (resourceNameType) {
      case ANY:
        return getAnyResourceTypeName();
      case FIXED:
        return Name.from(entityName).join("name_fixed");
      case ONEOF:
        // Remove suffix "_oneof". This allows the collection oneof config to "share" an entity name
        // with a collection config.
        entityName = removeSuffix(entityName, "_oneof");
        return Name.from(entityName).join("name_oneof");
      case SINGLE:
        return Name.from(entityName).join("name");
      case NONE:
      default:
        throw new UnsupportedOperationException("unexpected entity name type");
    }
  }

  protected Name getAnyResourceTypeName() {
    return Name.from("resource_name");
  }

  private static String removeSuffix(String original, String suffix) {
    if (original.endsWith(suffix)) {
      original = original.substring(0, original.length() - suffix.length());
    }
    return original;
  }

  public String getResourceTypeName(ResourceNameConfig resourceNameConfig) {
    return publicClassName(getResourceTypeNameObject(resourceNameConfig));
  }

  public String getResourceParameterName(ResourceNameConfig resourceNameConfig) {
    return localVarName(getResourceTypeNameObject(resourceNameConfig));
  }

  public String getResourcePropertyName(ResourceNameConfig resourceNameConfig) {
    return publicMethodName(getResourceTypeNameObject(resourceNameConfig));
  }

  public String getResourceEnumName(ResourceNameConfig resourceNameConfig) {
    return getResourceTypeNameObject(resourceNameConfig).toUpperUnderscore().toUpperCase();
  }

  public String getResourceTypeParseMethodName(
      ModelTypeTable typeTable, FieldConfig resourceFieldConfig) {
    return getNotImplementedString("SurfaceNamer.getResourceTypeParseMethodName");
  }

  /**
   * The name of the iterate method of the PagedListResponse type for a field, returning the
   * resource type iterate method if available
   */
  public String getPagedResponseIterateMethod(
      FeatureConfig featureConfig, FieldConfig fieldConfig) {
    if (featureConfig.useResourceNameFormatOption(fieldConfig)) {
      Name resourceName = getResourceTypeNameObject(fieldConfig.getResourceNameConfig());
      return publicMethodName(Name.from("iterate_all_as").join(resourceName));
    } else {
      return getPagedResponseIterateMethod();
    }
  }

  /** The name of the iterate method of the PagedListResponse type for a field */
  public String getPagedResponseIterateMethod() {
    return publicMethodName(Name.from("iterate_all_elements"));
  }

  /** The name of the create method for the resource one-of for the given field config */
  public String getResourceOneofCreateMethod(ModelTypeTable typeTable, FieldConfig fieldConfig) {
    return getAndSaveResourceTypeName(typeTable, fieldConfig.getMessageFieldConfig())
        + "."
        + publicMethodName(Name.from("from"));
  }

  /** The name of the constructor for the service client. The client is VKit generated, not GRPC. */
  public String getApiWrapperClassConstructorName(Interface interfaze) {
    return publicClassName(Name.upperCamel(interfaze.getSimpleName(), "Client"));
  }

  /**
   * The name of example of the constructor for the service client. The client is VKit generated,
   * not GRPC.
   */
  public String getApiWrapperClassConstructorExampleName(Interface interfaze) {
    return getApiWrapperClassConstructorName(interfaze);
  }

  /** Constructor name for the type with the given nickname. */
  public String getTypeConstructor(String typeNickname) {
    return typeNickname;
  }

  /**
   * The name of a variable that holds an instance of the class that implements a particular proto
   * interface.
   */
  public String getApiWrapperVariableName(Interface interfaze) {
    return localVarName(Name.upperCamel(interfaze.getSimpleName(), "Client"));
  }

  /**
   * The name of a variable that holds an instance of the module that contains the implementation of
   * a particular proto interface. So far it is used by just NodeJS.
   */
  public String getApiWrapperModuleName() {
    return getNotImplementedString("SurfaceNamer.getApiWrapperModuleName");
  }

  /**
   * The version of a variable that holds an instance of the module that contains the implementation
   * of a particular proto interface. So far it is used by just NodeJS.
   */
  public String getApiWrapperModuleVersion() {
    return getNotImplementedString("SurfaceNamer.getApiWrapperModuleVersion");
  }

  /**
   * The name of the settings class for a particular proto interface; not used in most languages.
   */
  public String getApiSettingsClassName(Interface interfaze) {
    return publicClassName(Name.upperCamel(interfaze.getSimpleName(), "Settings"));
  }

  /** The function name to retrieve default client option */
  public String getDefaultApiSettingsFunctionName(Interface service) {
    return getNotImplementedString("SurfaceNamer.getDefaultClientOptionFunctionName");
  }

  /**
   * The name of a variable that holds the settings class for a particular proto interface; not used
   * in most languages.
   */
  public String getApiSettingsVariableName(Interface interfaze) {
    return localVarName(Name.upperCamel(interfaze.getSimpleName(), "Settings"));
  }

  /**
   * The name of the builder class for the settings class for a particular proto interface; not used
   * in most languages.
   */
  public String getApiSettingsBuilderVarName(Interface interfaze) {
    return localVarName(Name.upperCamel(interfaze.getSimpleName(), "SettingsBuilder"));
  }

  /** The variable name for the given identifier that is formatted. */
  public String getFormattedVariableName(Name identifier) {
    return localVarName(Name.from("formatted").join(identifier));
  }

  /** The name of the field. */
  public String getFieldName(Field field) {
    return publicFieldName(Name.from(field.getSimpleName()));
  }

  /** The function name to set the given proto field. */
  public String getFieldSetFunctionName(FeatureConfig featureConfig, FieldConfig fieldConfig) {
    Field field = fieldConfig.getField();
    if (featureConfig.useResourceNameFormatOption(fieldConfig)) {
      return getResourceNameFieldSetFunctionName(fieldConfig.getMessageFieldConfig());
    } else {
      return getFieldSetFunctionName(field);
    }
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

  /** The function name to add an element to a map or repeated field. */
  public String getFieldAddFunctionName(Field field) {
    return getFieldAddFunctionName(field.getType(), Name.from(field.getSimpleName()));
  }

  /** The function name to add an element to a map or repeated field. */
  public String getFieldAddFunctionName(TypeRef type, Name identifier) {
    return getNotImplementedString("SurfaceNamer.getFieldAddFunctionName");
  }

  /** The function name to set a field that is a resource name class. */
  public String getResourceNameFieldSetFunctionName(FieldConfig fieldConfig) {
    TypeRef type = fieldConfig.getField().getType();
    Name identifier = Name.from(fieldConfig.getField().getSimpleName());
    Name resourceName = getResourceTypeNameObject(fieldConfig.getResourceNameConfig());
    if (type.isMap()) {
      return getNotImplementedString("SurfaceNamer.getResourceNameFieldSetFunctionName:map-type");
    } else if (type.isRepeated()) {
      return publicMethodName(
          Name.from("add", "all").join(identifier).join("with").join(resourceName).join("list"));
    } else {
      return publicMethodName(Name.from("set").join(identifier).join("with").join(resourceName));
    }
  }

  /** The function name to get the given proto field. */
  public String getFieldGetFunctionName(FeatureConfig featureConfig, FieldConfig fieldConfig) {
    Field field = fieldConfig.getField();
    if (featureConfig.useResourceNameFormatOption(fieldConfig)) {
      return getResourceNameFieldGetFunctionName(fieldConfig.getMessageFieldConfig());
    } else {
      return getFieldGetFunctionName(field);
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

  /** The function name to get a field that is a resource name class. */
  public String getResourceNameFieldGetFunctionName(FieldConfig fieldConfig) {
    TypeRef type = fieldConfig.getField().getType();
    Name identifier = Name.from(fieldConfig.getField().getSimpleName());
    Name resourceName = getResourceTypeNameObject(fieldConfig.getResourceNameConfig());
    if (type.isMap()) {
      return getNotImplementedString("SurfaceNamer.getResourceNameFieldGetFunctionName:map-type");
    } else if (type.isRepeated()) {
      return publicMethodName(
          Name.from("get").join(identifier).join("list_as").join(resourceName).join("list"));
    } else {
      return publicMethodName(Name.from("get").join(identifier).join("as").join(resourceName));
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

  /** The name of the example package */
  public String getExamplePackageName() {
    return getNotImplementedString("SurfaceNamer.getExamplePackageName");
  }

  /** The local (unqualified) name of the package */
  public String getLocalPackageName() {
    return getNotImplementedString("SurfaceNamer.getLocalPackageName");
  }

  /** The local (unqualified) name of the example package */
  public String getLocalExamplePackageName() {
    return getNotImplementedString("SurfaceNamer.getLocalExamplePackageName");
  }

  /**
   * The name of a path template constant for the given collection, to be held in an API wrapper
   * class.
   */
  public String getPathTemplateName(
      Interface service, SingleResourceNameConfig resourceNameConfig) {
    return inittedConstantName(Name.from(resourceNameConfig.getEntityName(), "path", "template"));
  }

  /** The name of a getter function to get a particular path template for the given collection. */
  public String getPathTemplateNameGetter(
      Interface service, SingleResourceNameConfig resourceNameConfig) {
    return publicMethodName(
        Name.from("get", resourceNameConfig.getEntityName(), "name", "template"));
  }

  /** The name of the path template resource, in human format. */
  public String getPathTemplateResourcePhraseName(SingleResourceNameConfig resourceNameConfig) {
    return Name.from(resourceNameConfig.getEntityName()).toPhrase();
  }

  /** The function name to format the entity for the given collection. */
  public String getFormatFunctionName(
      Interface service, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(Name.from("format", resourceNameConfig.getEntityName(), "name"));
  }

  /**
   * The function name to parse a variable from the string representing the entity for the given
   * collection.
   */
  public String getParseFunctionName(String var, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(
        Name.from("parse", var, "from", resourceNameConfig.getEntityName(), "name"));
  }

  /** The entity name for the given collection. */
  public String getEntityName(SingleResourceNameConfig resourceNameConfig) {
    return localVarName(Name.from(resourceNameConfig.getEntityName()));
  }

  /** The parameter name for the entity for the given collection config. */
  public String getEntityNameParamName(SingleResourceNameConfig resourceNameConfig) {
    return localVarName(Name.from(resourceNameConfig.getEntityName(), "name"));
  }

  /** The parameter name for the given lower-case field name. */
  public String getParamName(String var) {
    return localVarName(Name.from(var));
  }

  public String getPropertyName(String var) {
    return publicMethodName(Name.from(var));
  }

  /** The documentation name of a parameter for the given lower-case field name. */
  public String getParamDocName(String var) {
    return localVarName(Name.from(var));
  }

  /** The method name of the retry filter for the given key */
  public String retryFilterMethodName(String key) {
    return privateMethodName(Name.from(key).join("retry").join("filter"));
  }

  /** The method name of the retry backoff for the given key */
  public String retryBackoffMethodName(String key) {
    return privateMethodName(Name.from("get").join(key).join("retry").join("backoff"));
  }

  /** The method name of the timeout backoff for the given key */
  public String timeoutBackoffMethodName(String key) {
    return privateMethodName(Name.from("get").join(key).join("timeout").join("backoff"));
  }

  /** The page streaming descriptor name for the given method. */
  public String getPageStreamingDescriptorName(Method method) {
    return privateFieldName(Name.upperCamel(method.getSimpleName(), "PageStreamingDescriptor"));
  }

  /** The name of the constant to hold the page streaming descriptor for the given method. */
  public String getPageStreamingDescriptorConstName(Method method) {
    return inittedConstantName(Name.upperCamel(method.getSimpleName()).join("page_str_desc"));
  }

  /** The page streaming factory name for the given method. */
  public String getPagedListResponseFactoryName(Method method) {
    return privateFieldName(Name.upperCamel(method.getSimpleName(), "PagedListResponseFactory"));
  }

  /** The name of the constant to hold the page streaming factory for the given method. */
  public String getPagedListResponseFactoryConstName(Method method) {
    return inittedConstantName(Name.upperCamel(method.getSimpleName()).join("page_str_fact"));
  }

  /** The name of the constant to hold the bundling descriptor for the given method. */
  public String getBundlingDescriptorConstName(Method method) {
    return inittedConstantName(Name.upperCamel(method.getSimpleName()).join("bundling_desc"));
  }

  /** The key to use in a dictionary for the given method. */
  public String getMethodKey(Method method) {
    return keyName(Name.upperCamel(method.getSimpleName()));
  }

  /** The path to the client config for the given interface. */
  public String getClientConfigPath(Interface service) {
    return getNotImplementedString("SurfaceNamer.getClientConfigPath");
  }

  /** Human-friendly name of this service */
  public String getServicePhraseName(Interface service) {
    return Name.upperCamel(service.getSimpleName()).toPhrase();
  }

  /**
   * The type name of the Grpc server class. This needs to match what Grpc generates for the
   * particular language.
   */
  public String getGrpcServerTypeName(Interface service) {
    return getNotImplementedString("SurfaceNamer.getGrpcServerTypeName");
  }

  /**
   * The type name of the Grpc client class. This needs to match what Grpc generates for the
   * particular language.
   */
  public String getGrpcClientTypeName(Interface service) {
    return getNotImplementedString("SurfaceNamer.getGrpcClientTypeName");
  }

  /**
   * Gets the type name of the Grpc client class, saves it to the type table provided, and returns
   * the nickname.
   */
  public String getAndSaveNicknameForGrpcClientTypeName(
      ModelTypeTable typeTable, Interface service) {
    return typeTable.getAndSaveNicknameFor(getGrpcClientTypeName(service));
  }

  /**
   * The type name of the Grpc container class. This needs to match what Grpc generates for the
   * particular language.
   */
  public String getGrpcContainerTypeName(Interface service) {
    NamePath namePath = typeNameConverter.getNamePath(modelTypeFormatter.getFullNameFor(service));
    String publicClassName =
        publicClassName(Name.upperCamelKeepUpperAcronyms(namePath.getHead(), "Grpc"));
    return qualifiedName(namePath.withHead(publicClassName));
  }

  /**
   * The type name of the Grpc service class This needs to match what Grpc generates for the
   * particular language.
   */
  public String getGrpcServiceClassName(Interface service) {
    NamePath namePath = typeNameConverter.getNamePath(modelTypeFormatter.getFullNameFor(service));
    String grpcContainerName =
        publicClassName(Name.upperCamelKeepUpperAcronyms(namePath.getHead(), "Grpc"));
    String serviceClassName =
        publicClassName(Name.upperCamelKeepUpperAcronyms(service.getSimpleName(), "ImplBase"));
    return qualifiedName(namePath.withHead(grpcContainerName).append(serviceClassName));
  }

  /**
   * The type name of the method constant in the Grpc container class. This needs to match what Grpc
   * generates for the particular language.
   */
  public String getGrpcMethodConstant(Method method) {
    return inittedConstantName(
        Name.from("method").join(Name.upperCamelKeepUpperAcronyms(method.getSimpleName())));
  }

  /** The variable name of the rerouted gRPC client. Used in C# */
  public String getReroutedGrpcClientVarName(MethodConfig methodConfig) {
    return getNotImplementedString("SurfaceNamer.getGrpcClientName");
  }

  /** The method name to create a rerouted gRPC client. Used in C# */
  public String getReroutedGrpcMethodName(MethodConfig methodConfig) {
    return getNotImplementedString("SurfaceNamer.getReroutedGrpcMethodName");
  }

  /** The name of the surface method which can call the given API method. */
  public String getApiMethodName(Method method, VisibilityConfig visibility) {
    return getApiMethodName(Name.upperCamel(method.getSimpleName()), visibility);
  }

  /** The name of the async surface method which can call the given API method. */
  public String getAsyncApiMethodName(Method method, VisibilityConfig visibility) {
    return getApiMethodName(Name.upperCamel(method.getSimpleName()).join("async"), visibility);
  }

  protected String getApiMethodName(Name name, VisibilityConfig visibility) {
    switch (visibility) {
      case PUBLIC:
        return publicMethodName(name);
      case PACKAGE:
      case PRIVATE:
        return privateMethodName(name);
      default:
        throw new IllegalArgumentException("cannot name method with visibility: " + visibility);
    }
  }

  /** The keyword controlling the visiblity, eg "public", "protected". */
  public String getVisiblityKeyword(VisibilityConfig visibility) {
    switch (visibility) {
      case PUBLIC:
        return "public";
      case PACKAGE:
        return "/* package-private */";
      case PRIVATE:
        return "private";
      default:
        throw new IllegalArgumentException("invalid visibility: " + visibility);
    }
  }

  /** The name of the LRO surface method which can call the given API method. */
  public String getLroApiMethodName(Method method, VisibilityConfig visibility) {
    return getAsyncApiMethodName(method, visibility);
  }

  /** The name of the example for the method. */
  public String getApiMethodExampleName(Interface interfaze, Method method) {
    return getApiMethodName(method, VisibilityConfig.PUBLIC);
  }

  /** The name of the example for the async variant of the given method. */
  public String getAsyncApiMethodExampleName(Interface interfaze, Method method) {
    return getAsyncApiMethodName(method, VisibilityConfig.PUBLIC);
  }

  /** The name of the GRPC streaming surface method which can call the given API method. */
  public String getGrpcStreamingApiMethodName(Method method, VisibilityConfig visibility) {
    return getApiMethodName(method, visibility);
  }

  /**
   * The name of the example of the GRPC streaming surface method which can call the given API
   * method.
   */
  public String getGrpcStreamingApiMethodExampleName(Interface interfaze, Method method) {
    return getGrpcStreamingApiMethodName(method, VisibilityConfig.PUBLIC);
  }

  /**
   * The name of a variable to hold a value for the given proto message field (such as a flattened
   * parameter).
   */
  public String getVariableName(Field field) {
    return localVarName(Name.from(field.getSimpleName()));
  }

  /** Returns true if the request object param type for the given field should be imported. */
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

  /** Provides the doc lines for the given method element in the current language. */
  public List<String> getDocLines(Method method, MethodConfig methodConfig) {
    return getDocLines(method);
  }

  /** The doc lines that declare what exception(s) are thrown for an API method. */
  public List<String> getThrowsDocLines() {
    return new ArrayList<>();
  }

  /** The doc lines that describe the return value for an API method. */
  public List<String> getReturnDocLines(
      SurfaceTransformerContext context, MethodConfig methodConfig, Synchronicity synchronicity) {
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

  /** The name used in Grpc for the given API method. This needs to match what Grpc generates. */
  public String getGrpcMethodName(Method method) {
    // This might seem silly, but it makes clear what we're dealing with (upper camel).
    // This is language-independent because of gRPC conventions.
    return Name.upperCamelKeepUpperAcronyms(method.getSimpleName()).toUpperCamel();
  }

  /** The string used to identify the method in the gRPC stub. Not all languages will use this. */
  public String getGrpcStubCallString(Interface service, Method method) {
    return getNotImplementedString("SurfaceNamer.getGrpcStubCallString");
  }

  /** The type name for retry settings. */
  public String getRetrySettingsTypeName() {
    return getNotImplementedString("SurfaceNamer.getRetrySettingsClassName");
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
    return getNotImplementedString("SurfaceNamer.getStaticLangReturnTypeName");
  }

  /** The return type name in a static language that is used by the caller */
  public String getStaticLangCallerReturnTypeName(Method method, MethodConfig methodConfig) {
    return getStaticLangReturnTypeName(method, methodConfig);
  }

  /** The async return type name in a static language for the given method. */
  public String getStaticLangAsyncReturnTypeName(Method method, MethodConfig methodConfig) {
    return getNotImplementedString("SurfaceNamer.getStaticLangAsyncReturnTypeName");
  }

  /**
   * Computes the nickname of the operation response type name for the given method, saves it in the
   * given type table, and returns it.
   */
  public String getAndSaveOperationResponseTypeName(
      Method method, ModelTypeTable typeTable, MethodConfig methodConfig) {
    return getNotImplementedString("SurfaceNamer.getAndSaveOperationResponseTypeName");
  }

  /**
   * In languages with pointers, strip the pointer, leaving only the base type. Eg, in C, "int*"
   * would become "int".
   */
  public String valueType(String type) {
    return getNotImplementedString("SurfaceNamer.valueType");
  }

  /** The async return type name in a static language that is used by the caller */
  public String getStaticLangCallerAsyncReturnTypeName(Method method, MethodConfig methodConfig) {
    return getStaticLangAsyncReturnTypeName(method, methodConfig);
  }

  /** The GRPC streaming server type name for a given method. */
  public String getStreamingServerName(Method method) {
    return getNotImplementedString("SurfaceNamer.getStreamingServerName");
  }

  /** The name of the return type of the given grpc streaming method. */
  public String getGrpcStreamingApiReturnTypeName(Method method) {
    return publicClassName(
        Name.upperCamel(method.getOutputType().getMessageType().getSimpleName()));
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
    return privateFieldName(Name.upperCamel(method.getSimpleName(), "PagedCallable"));
  }

  /** The name of the plain callable variant of the given method. */
  public String getCallableMethodName(Method method) {
    return publicMethodName(Name.upperCamel(method.getSimpleName(), "Callable"));
  }

  /** The name of the plain callable variant of the given method. */
  public String getCallableAsyncMethodName(Method method) {
    return publicMethodName(Name.upperCamel(method.getSimpleName(), "CallableAsync"));
  }

  /** The name of the example for the plain callable variant. */
  public String getCallableMethodExampleName(Interface interfaze, Method method) {
    return getCallableMethodName(method);
  }

  /** The name of the operation callable variant of the given method. */
  public String getOperationCallableMethodName(Method method) {
    return publicMethodName(Name.upperCamel(method.getSimpleName(), "OperationCallable"));
  }

  /** The name of the example for the operation callable variant of the given method. */
  public String getOperationCallableMethodExampleName(Interface interfaze, Method method) {
    return getOperationCallableMethodName(method);
  }

  /** The name of the plain callable for the given method. */
  public String getCallableName(Method method) {
    return privateFieldName(Name.upperCamel(method.getSimpleName(), "Callable"));
  }

  /** The name of the operation callable for the given method. */
  public String getOperationCallableName(Method method) {
    return privateFieldName(Name.upperCamel(method.getSimpleName(), "OperationCallable"));
  }

  /** The name of the settings member name for the given method. */
  public String getSettingsMemberName(Method method) {
    return publicMethodName(Name.upperCamel(method.getSimpleName(), "Settings"));
  }

  /** The getter function name for the settings for the given method. */
  public String getSettingsFunctionName(Method method) {
    return getSettingsMemberName(method);
  }

  /** The name of a method to apply modifications to this method request. */
  public String getModifyMethodName(Method method) {
    return getNotImplementedString("SurfaceNamer.getModifyMethodName");
  }

  /** The type name of call options */
  public String getCallSettingsTypeName(Interface service) {
    return publicClassName(Name.upperCamel(service.getSimpleName(), "Settings"));
  }

  /** The function name to retrieve default call option */
  public String getDefaultCallSettingsFunctionName(Interface service) {
    return publicMethodName(Name.upperCamel(service.getSimpleName(), "Settings"));
  }

  /**
   * The generic-aware response type name for the given type. For example, in Java, this will be the
   * type used for ListenableFuture&lt;...&gt;.
   */
  public String getGenericAwareResponseTypeName(TypeRef outputType) {
    return getNotImplementedString("SurfaceNamer.getGenericAwareResponseType");
  }

  /**
   * Computes the nickname of the paged response type name for the given method and resources field,
   * saves it in the given type table, and returns it.
   */
  public String getAndSavePagedResponseTypeName(
      Method method, ModelTypeTable typeTable, FieldConfig resourcesFieldConfig) {
    return getNotImplementedString("SurfaceNamer.getAndSavePagedResponseTypeName");
  }

  /** The inner type name of the paged response type for the given method and resources field. */
  public String getPagedResponseTypeInnerName(
      Method method, ModelTypeTable typeTable, Field resourcesField) {
    return getNotImplementedString("SurfaceNamer.getAndSavePagedResponseTypeInnerName");
  }

  /**
   * Computes the nickname of the async response type name for the given resource type, saves it in
   * the given type table, and returns it.
   */
  public String getAndSaveAsyncPagedResponseTypeName(
      Method method, ModelTypeTable typeTable, FieldConfig resourcesFieldConfig) {
    return getNotImplementedString("SurfaceNamer.getAndSavePagedAsyncResponseTypeName");
  }

  /**
   * Computes the nickname of the response type name for the given resource type, as used by the
   * caller, saves it in the given type table, and returns it.
   */
  public String getAndSaveCallerPagedResponseTypeName(
      Method method, ModelTypeTable typeTable, FieldConfig resourcesFieldConfig) {
    return getAndSavePagedResponseTypeName(method, typeTable, resourcesFieldConfig);
  }

  /**
   * Computes the nickname of the response type name for the given resource type, as used by the
   * caller, saves it in the given type table, and returns it.
   */
  public String getAndSaveCallerAsyncPagedResponseTypeName(
      Method method, ModelTypeTable typeTable, FieldConfig resourcesFieldConfig) {
    return getAndSaveAsyncPagedResponseTypeName(method, typeTable, resourcesFieldConfig);
  }

  /** The class name of the generated resource type from the entity name. */
  public String getAndSaveResourceTypeName(ModelTypeTable typeTable, FieldConfig fieldConfig) {
    String resourceClassName =
        publicClassName(getResourceTypeNameObject(fieldConfig.getResourceNameConfig()));
    return typeTable.getAndSaveNicknameForTypedResourceName(fieldConfig, resourceClassName);
  }

  /** The class name of the generated resource type from the entity name. */
  public String getAndSaveElementResourceTypeName(
      ModelTypeTable typeTable, FieldConfig fieldConfig) {
    String resourceClassName =
        publicClassName(getResourceTypeNameObject(fieldConfig.getResourceNameConfig()));
    return typeTable.getAndSaveNicknameForResourceNameElementType(fieldConfig, resourceClassName);
  }

  /** The test case name for the given method. */
  public String getTestCaseName(SymbolTable symbolTable, Method method) {
    Name testCaseName = symbolTable.getNewSymbol(Name.upperCamel(method.getSimpleName(), "Test"));
    return publicMethodName(testCaseName);
  }

  /** The exception test case name for the given method. */
  public String getExceptionTestCaseName(SymbolTable symbolTable, Method method) {
    Name testCaseName =
        symbolTable.getNewSymbol(Name.upperCamel(method.getSimpleName(), "ExceptionTest"));
    return publicMethodName(testCaseName);
  }

  /** The unit test class name for the given API service. */
  public String getUnitTestClassName(Interface service) {
    return publicClassName(Name.upperCamel(service.getSimpleName(), "Client", "Test"));
  }

  /** The smoke test class name for the given API service. */
  public String getSmokeTestClassName(Interface service) {
    return publicClassName(Name.upperCamel(service.getSimpleName(), "Smoke", "Test"));
  }

  /** The class name of the mock gRPC service for the given API service. */
  public String getMockServiceClassName(Interface service) {
    return publicClassName(Name.upperCamelKeepUpperAcronyms("Mock", service.getSimpleName()));
  }

  /** The class name of a variable to hold the mock gRPC service for the given API service. */
  public String getMockServiceVarName(Interface service) {
    return localVarName(Name.upperCamelKeepUpperAcronyms("Mock", service.getSimpleName()));
  }

  /** The class name of the mock gRPC service implementation for the given API service. */
  public String getMockGrpcServiceImplName(Interface service) {
    return publicClassName(
        Name.upperCamelKeepUpperAcronyms("Mock", service.getSimpleName(), "Impl"));
  }

  /** The file name for an API service. */
  public String getServiceFileName(Interface service) {
    return getNotImplementedString("SurfaceNamer.getServiceFileName");
  }

  /** The file name for the example of an API service. */
  public String getExampleFileName(Interface service) {
    return getNotImplementedString("SurfaceNamer.getExampleFileName");
  }

  /**
   * The fully qualified class name of a an API service. TODO: Support the general pattern of
   * package + class name in NameFormatter.
   */
  public String getFullyQualifiedApiWrapperClassName(Interface interfaze) {
    return getNotImplementedString("SurfaceNamer.getFullyQualifiedApiWrapperClassName");
  }

  /** The name of the variable that will hold the stub for a service. */
  public String getStubName(Interface service) {
    return privateFieldName(Name.upperCamel(service.getSimpleName(), "Stub"));
  }

  /** The name of the function that will create a stub. */
  public String getCreateStubFunctionName(Interface service) {
    return privateMethodName(
        Name.upperCamel("Create", service.getSimpleName(), "Stub", "Function"));
  }

  /** The name of the array which will hold the methods for a given stub. */
  public String getStubMethodsArrayName(Interface service) {
    return privateMethodName(Name.upperCamel(service.getSimpleName(), "Stub", "Methods"));
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
    return localVarName(Name.upperCamel(service.getSimpleName(), "Client"));
  }

  /** The qualified namespace of a service. */
  public String getNamespace(Interface service) {
    NamePath namePath = typeNameConverter.getNamePath(modelTypeFormatter.getFullNameFor(service));
    return qualifiedName(namePath.withoutHead());
  }

  public String getServiceFileImportFromService(Interface service) {
    return getNotImplementedString("SurfaceNamer.getServiceFileImportFromService");
  }

  public String getProtoFileImportFromService(Interface service) {
    return getNotImplementedString("SurfaceNamer.getProtoFileImportFromService");
  }

  /**
   * Returns the service name with common suffixes removed.
   *
   * <p>For example: "LoggingServiceV2" becomes Name("Logging")
   */
  public Name getReducedServiceName(Interface service) {
    String name = service.getSimpleName().replaceAll("V[0-9]+$", "");
    name = name.replaceAll("Service$", "");
    return Name.upperCamel(name);
  }

  /** The name of an RPC status code */
  public String getStatusCodeName(Status.Code code) {
    return privateMethodName(Name.upperUnderscore(code.toString()));
  }

  /* The name of a retry definition */
  public String getRetryDefinitionName(String retryDefinitionKey) {
    return privateMethodName(Name.from(retryDefinitionKey));
  }

  /** The name of the IAM resource getter function. */
  public String getIamResourceGetterFunctionName(Field field) {
    return getNotImplementedString("SurfaceNamer.getIamResourceGetterFunctionName");
  }

  /** The example name of the IAM resource getter function. */
  public String getIamResourceGetterFunctionExampleName(Interface service, Field field) {
    return getIamResourceGetterFunctionName(field);
  }

  /** The parameter name of the IAM resource. */
  public String getIamResourceParamName(Field field) {
    return localVarName(Name.upperCamel(field.getParent().getSimpleName()));
  }

  /** Inject random value generator code to the given string. */
  public String injectRandomStringGeneratorCode(String randomString) {
    return getNotImplementedString("SurfaceNamer.getRandomStringValue");
  }

  /** Function used to register the GRPC server. */
  public String getServerRegisterFunctionName(Interface service) {
    return getNotImplementedString("SurfaceNamer.getServerRegisterFunctionName");
  }

  /** The type name of the API callable class for this service method type. */
  public String getApiCallableTypeName(ServiceMethodType serviceMethodType) {
    return getNotImplementedString("SurfaceNamer.getApiCallableTypeName");
  }

  public String getReleaseAnnotation(ReleaseLevel releaseLevel) {
    return getNotImplementedString("SurfaceNamer.getReleaseAnnotation");
  }
}
