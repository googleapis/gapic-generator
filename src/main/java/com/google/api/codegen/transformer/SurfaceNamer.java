/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.transformer;

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.config.AnyResourceNameConfig;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.GrpcStreamingConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.OneofConfig;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.ResourceNameConfig;
import com.google.api.codegen.config.ResourceNameType;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.TransportProtocol;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.util.CommentReformatter;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NameFormatter;
import com.google.api.codegen.util.NameFormatterDelegator;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.StringUtil;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ServiceMethodType;
import com.google.api.tools.framework.model.EnumType;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

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
  private final TypeFormatter typeFormatter;
  private final TypeNameConverter typeNameConverter;
  private final CommentReformatter commentReformatter;
  private final String rootPackageName;
  private final String packageName;
  private final NameFormatter nameFormatter;

  /** Represents a kind of test. */
  public enum TestKind {
    UNIT,
    SYSTEM
  }

  public enum Cardinality implements Comparable<Cardinality> {
    IS_REPEATED(true),
    NOT_REPEATED(false);

    Cardinality(boolean value) {
      this.value = value;
    }

    public static Cardinality ofRepeated(boolean value) {
      return value ? IS_REPEATED : NOT_REPEATED;
    }

    private final boolean value;
  }

  public enum MapType implements Comparable<MapType> {
    IS_MAP(true),
    NOT_MAP(false);

    MapType(boolean value) {
      this.value = value;
    }

    public static MapType ofMap(boolean value) {
      return value ? IS_MAP : NOT_MAP;
    }

    private final boolean value;
  }

  public SurfaceNamer(
      NameFormatter languageNamer,
      ModelTypeFormatter typeFormatter,
      TypeNameConverter typeNameConverter,
      CommentReformatter commentReformatter,
      String rootPackageName,
      String packageName) {
    super(languageNamer);
    this.typeFormatter = typeFormatter;
    this.typeNameConverter = typeNameConverter;
    this.commentReformatter = commentReformatter;
    this.rootPackageName = rootPackageName;
    this.packageName = packageName;
    this.nameFormatter = languageNamer;
  }

  // Create a SurfaceNamer based on Discovery Documents.
  public SurfaceNamer(
      NameFormatter languageNamer,
      SchemaTypeFormatter typeFormatter,
      TypeNameConverter typeNameConverter,
      CommentReformatter commentReformatter,
      String rootPackageName,
      String packageName) {
    super(languageNamer);
    this.typeNameConverter = typeNameConverter;
    this.commentReformatter = commentReformatter;
    this.packageName = packageName;
    this.rootPackageName = rootPackageName;
    this.typeFormatter = typeFormatter;
    this.nameFormatter = languageNamer;
  }

  public SurfaceNamer cloneWithPackageName(String packageName) {
    throw new UnsupportedOperationException("clone needs to be overridden");
  }

  public SurfaceNamer cloneWithPackageNameForDiscovery(String packageName) {
    throw new UnsupportedOperationException("clone needs to be overridden");
  }

  public ModelTypeFormatter getModelTypeFormatter() {
    return (ModelTypeFormatter) typeFormatter;
  }

  public TypeFormatter getTypeFormatter() {
    return typeFormatter;
  }

  public TypeNameConverter getTypeNameConverter() {
    return typeNameConverter;
  }

  public NameFormatter getNameFormatter() {
    return nameFormatter;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getRootPackageName() {
    return rootPackageName;
  }

  public String getStubPackageName() {
    return rootPackageName + ".stub";
  }

  public String getApiLroPackageName() {
    return rootPackageName + ".longrunning";
  }

  public String getApiLroOperationCallableName() {
    return getApiLroPackageName()
        + "."
        + nameFormatter.publicClassName(Name.upperCamel("OperationSnapshotCallable"));
  }

  public String getNotImplementedString(String feature) {
    return "$ NOT IMPLEMENTED: " + feature + " $";
  }

  /////////////////////////////////////// Service names ///////////////////////////////////////////

  /**
   * Returns the service name with common suffixes removed.
   *
   * <p>For example: "LoggingServiceV2" becomes Name("Logging")
   */
  public Name getReducedServiceName(String interfaceSimpleName) {
    String name = interfaceSimpleName.replaceAll("V[0-9]+$", "");
    name = name.replaceAll("Service$", "");
    return Name.upperCamel(name);
  }

  /** Returns the service name exported by the package */
  public String getPackageServiceName(InterfaceConfig interfaceConfig) {
    return getNotImplementedString("SurfaceNamer.getPackageServiceName");
  }

  /** Human-friendly name of this API interface */
  public String getServicePhraseName(InterfaceConfig interfaceConfig) {
    return Name.upperCamel(interfaceConfig.getInterfaceModel().getSimpleName()).toPhrase();
  }

  /////////////////////////////////////// Constructors /////////////////////////////////////////////

  /**
   * The name of the constructor for the interfaceConfig.getInterfaceModel() client. The client is
   * VKit generated, not GRPC.
   */
  public String getApiWrapperClassConstructorName(InterfaceConfig interfaceConfig) {
    return publicClassName(
        Name.upperCamel(interfaceConfig.getInterfaceModel().getSimpleName(), "Client"));
  }

  /** Constructor name for the type with the given nickname. */
  public String getTypeConstructor(String typeNickname) {
    return typeNickname;
  }

  //////////////////////////////////// Package & module names /////////////////////////////////////

  /** The local (unqualified) name of the package */
  public String getLocalPackageName() {
    return getNotImplementedString("SurfaceNamer.getLocalPackageName");
  }

  /**
   * The name of a variable that holds an instance of the module that contains the implementation of
   * a particular proto interface.
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

  /** The qualified namespace of an API interface. */
  public String getNamespace(InterfaceModel apiInterface) {
    NamePath namePath =
        typeNameConverter.getNamePath(getModelTypeFormatter().getFullNameFor(apiInterface));
    return qualifiedName(namePath.withoutHead());
  }

  public String getGapicImplNamespace() {
    return getNotImplementedString("SurfaceNamer.getGapicImplNamespace");
  }

  /** The qualified namespace of an API. */
  public String getTopLevelNamespace() {
    return getNotImplementedString("SurfaceNamer.getTopLevelNamespace");
  }

  /** The versioned namespace of an api. Example: google.cloud.vision_v1 */
  public String getVersionedDirectoryNamespace() {
    return getNotImplementedString("SurfaceNamer.getVersionedDirectoryNamespace");
  }

  /** The modules of the package. */
  public ImmutableList<String> getApiModules() {
    return ImmutableList.<String>of();
  }

  /** The top level modules of the package. */
  public List<String> getTopLevelApiModules() {
    return ImmutableList.of();
  }

  /** The name of the gapic package. */
  public String getGapicPackageName(String configPackageName) {
    return "gapic-" + configPackageName;
  }

  /** The name of the module for the service of an API. */
  public String getModuleServiceName() {
    return getNotImplementedString("SurfaceNamer.getModuleServiceName");
  }

  /////////////////////////////////// Proto methods /////////////////////////////////////////////

  /** The function name to set the given field. */
  public String getFieldSetFunctionName(FeatureConfig featureConfig, FieldConfig fieldConfig) {
    FieldModel field = fieldConfig.getField();
    if (featureConfig.useResourceNameProtoAccessor(fieldConfig)) {
      return getResourceNameFieldSetFunctionName(fieldConfig.getMessageFieldConfig());
    } else {
      return getFieldSetFunctionName(field);
    }
  }

  /** The function name to set the given field. */
  public String getFieldSetFunctionName(FieldModel field) {
    return getFieldSetFunctionName(
        field.getNameAsParameterName(),
        MapType.ofMap(field.isMap()),
        Cardinality.ofRepeated(field.isRepeated()));
  }

  /** The function name to set a field having the given type and name. */
  public String getFieldSetFunctionName(TypeModel type, Name identifier) {
    return getFieldSetFunctionName(
        identifier, MapType.ofMap(type.isMap()), Cardinality.ofRepeated(type.isRepeated()));
  }
  /** The function name to get a field having the given name. */
  public String getFieldSetFunctionName(Name identifier, MapType mapType, Cardinality cardinality) {
    if (mapType == MapType.IS_MAP) {
      return publicMethodName(Name.from("put", "all").join(identifier));
    } else if (cardinality == Cardinality.IS_REPEATED) {
      return publicMethodName(Name.from("add", "all").join(identifier));
    } else {
      return publicMethodName(Name.from("set").join(identifier));
    }
  }

  /** The function name to add an element to a map or repeated field. */
  public String getFieldAddFunctionName(FieldModel field) {
    if (field.isMap()) {
      return publicMethodName(Name.from("put").join(field.getNameAsParameterName()));
    } else if (field.isRepeated()) {
      return publicMethodName(Name.from("add").join(field.getNameAsParameterName()));
    } else {
      return publicMethodName(Name.from("set").join(field.getNameAsParameterName()));
    }
  }

  /** The function name to add an element to a map or repeated field. */
  public String getFieldAddFunctionName(TypeModel type, Name identifier) {
    return getNotImplementedString("SurfaceNamer.getFieldAddFunctionName");
  }

  /** The function name to set a field that is a resource name class. */
  public String getResourceNameFieldSetFunctionName(FieldConfig fieldConfig) {
    FieldModel type = fieldConfig.getField();
    Name identifier = fieldConfig.getField().getNameAsParameterName();
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

  public String getFullNameForElementType(FieldModel type) {
    return getTypeFormatter().getFullNameForElementType(type);
  }

  /** The function name to get the given field. */
  public String getFieldGetFunctionName(FeatureConfig featureConfig, FieldConfig fieldConfig) {
    FieldModel field = fieldConfig.getField();
    if (featureConfig.useResourceNameProtoAccessor(fieldConfig)) {
      return getResourceNameFieldGetFunctionName(fieldConfig.getMessageFieldConfig());
    } else {
      return getFieldGetFunctionName(field);
    }
  }

  /** The function name to get the given field. */
  public String getFieldGetFunctionName(FieldModel field) {
    return getFieldGetFunctionName(field, field.getNameAsParameterName());
  }

  /** The function name to get a field having the given name. */
  public String getFieldGetFunctionName(Name identifier, MapType mapType, Cardinality cardinality) {
    if (mapType != MapType.IS_MAP && cardinality == Cardinality.IS_REPEATED) {
      return publicMethodName(Name.from("get").join(identifier).join("list"));
    } else if (mapType == MapType.IS_MAP) {
      return publicMethodName(Name.from("get").join(identifier).join("map"));
    } else {
      return publicMethodName(Name.from("get").join(identifier));
    }
  }

  /** The function name to get a field having the given type and name. */
  public String getFieldGetFunctionName(TypeModel type, Name identifier) {
    return getFieldGetFunctionName(
        identifier, MapType.ofMap(type.isMap()), Cardinality.ofRepeated(type.isRepeated()));
  }

  /** The function name to get a field having the given type and name. */
  public String getFieldGetFunctionName(FieldModel type, Name identifier) {
    return getFieldGetFunctionName(
        identifier, MapType.ofMap(type.isMap()), Cardinality.ofRepeated(type.isRepeated()));
  }

  /** The function name to get a field that is a resource name class. */
  public String getResourceNameFieldGetFunctionName(FieldConfig fieldConfig) {
    FieldModel type = fieldConfig.getField();
    Name identifier = fieldConfig.getField().getNameAsParameterName();
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
  public String getFieldCountGetFunctionName(FieldModel field) {
    if (field.isRepeated()) {
      return publicMethodName(Name.from("get").join(field.getNameAsParameterName()).join("count"));
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
  public String getByIndexGetFunctionName(FieldModel field) {
    if (field.isRepeated()) {
      return publicMethodName(Name.from("get", field.getSimpleName()));
    } else {
      throw new IllegalArgumentException(
          "Non-repeated field " + field.getSimpleName() + " has no get-by-index function.");
    }
  }

  ///////////////////////////////// Function & Callable names /////////////////////////////////////

  /** The function name to retrieve default client option */
  public String getDefaultApiSettingsFunctionName(InterfaceConfig interfaceConfig) {
    return getNotImplementedString("SurfaceNamer.getDefaultClientOptionFunctionName");
  }

  /** The method name to create a rerouted gRPC client. Used in C# */
  public String getReroutedGrpcMethodName(MethodConfig methodConfig) {
    return getNotImplementedString("SurfaceNamer.getReroutedGrpcMethodName");
  }

  /** The type name of a rerouted gRPC type. Used in C# */
  public String getReroutedGrpcTypeName(ImportTypeTable typeTable, MethodConfig methodConfig) {
    return getNotImplementedString("SurfaceNamer.getReroutedGrpcTypeName");
  }

  /** The name of the surface method which can call the given API method. */
  public String getApiMethodName(MethodModel method, VisibilityConfig visibility) {
    return getApiMethodName(method.asName(), visibility);
  }

  /** The name of the async surface method which can call the given API method. */
  public String getAsyncApiMethodName(MethodModel method, VisibilityConfig visibility) {
    return getApiMethodName(method.asName().join("async"), visibility);
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

  /** The name of the paged resource variable name used in generated test cases. */
  public String getPagedResourceName() {
    return localVarName(Name.from("resources"));
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

  /**
   * The name of the get values method of the Page type for a field, returning the resource type get
   * values method if available
   */
  public String getPageGetValuesMethod(FeatureConfig featureConfig, FieldConfig fieldConfig) {
    if (featureConfig.useResourceNameFormatOption(fieldConfig)) {
      Name resourceName = getResourceTypeNameObject(fieldConfig.getResourceNameConfig());
      return publicMethodName(Name.from("get_values_as").join(resourceName));
    } else {
      return getPageGetValuesMethod();
    }
  }

  /** The name of the get values method of the Page type for a field */
  public String getPageGetValuesMethod() {
    return publicMethodName(Name.from("get_values"));
  }

  public String getResourceTypeParseMethodName(
      ImportTypeTable typeTable, FieldConfig resourceFieldConfig) {
    return getNotImplementedString("SurfaceNamer.getResourceTypeParseMethodName");
  }

  public String getResourceTypeParseListMethodName(
      ImportTypeTable typeTable, FieldConfig resourceFieldConfig) {
    return getNotImplementedString("SurfaceNamer.getResourceTypeParseListMethodName");
  }

  public String getResourceTypeFormatListMethodName(
      ImportTypeTable typeTable, FieldConfig resourceFieldConfig) {
    return getNotImplementedString("SurfaceNamer.getResourceTypeFormatListMethodName");
  }

  /** The name of the create method for the resource one-of for the given field config */
  public String getResourceOneofCreateMethod(ImportTypeTable typeTable, FieldConfig fieldConfig) {
    return getAndSaveResourceTypeName(typeTable, fieldConfig.getMessageFieldConfig())
        + "."
        + publicMethodName(Name.from("from"));
  }

  /** The name of the create method for the resource one-of for the given field config */
  public String getResourceTypeParentParseMethod(
      ImportTypeTable typeTable, FieldConfig resourceFieldConfig) {
    return getNotImplementedString("SurfaceNamer.getResourceTypeParentParseMethod");
  }

  public String getResourceNameFormatMethodName() {
    return "toString";
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

  /** The name of the GRPC streaming surface method which can call the given API method. */
  public String getGrpcStreamingApiMethodName(MethodModel method, VisibilityConfig visibility) {
    return getApiMethodName(method, visibility);
  }

  /** The name of the callable for the paged callable variant of the given method. */
  public String getPagedCallableName(MethodModel method) {
    return privateFieldName(method.asName().join(Name.from("paged", "callable")));
  }

  /** The name of the paged callable variant of the given method. */
  public String getPagedCallableMethodName(MethodModel method) {
    return publicMethodName(method.asName().join(Name.from("paged", "callable")));
  }

  /** The name of the plain callable variant of the given method. */
  public String getCallableMethodName(MethodModel method) {
    return publicMethodName(method.asName().join("callable"));
  }

  /** The name of the plain callable variant of the given method. */
  public String getCallableAsyncMethodName(MethodModel method) {
    return publicMethodName(method.asName().join(Name.from("callable", "async")));
  }

  /** The name of the operation callable variant of the given method. */
  public String getOperationCallableMethodName(MethodModel method) {
    return publicMethodName(method.asName().join(Name.from("operation", "callable")));
  }

  public String getOperationClientName(InterfaceContext context) {
    return publicClassName(Name.anyCamel(context.getOperationServiceName(), "client"));
  }

  /** The name of the plain callable for the given method. */
  public String getCallableName(MethodModel method) {
    return privateFieldName(method.asName().join("callable"));
  }

  /** The name of the operation callable for the given method. */
  public String getOperationCallableName(MethodModel method) {
    return privateFieldName(method.asName().join(Name.from("operation", "callable")));
  }

  public String getMethodDescriptorName(MethodModel method) {
    return privateFieldName(Name.anyCamel(method.getSimpleName(), "MethodDescriptor"));
  }

  public String getTransportSettingsVar(MethodModel method) {
    return localVarName(Name.anyCamel(method.getSimpleName(), "TransportSettings"));
  }

  /** The name of the settings member name for the given method. */
  public String getSettingsMemberName(MethodModel method) {
    return publicMethodName(method.asName().join("settings"));
  }

  /** The name of the settings member name for the given method. */
  public String getOperationSettingsMemberName(MethodModel method) {
    return getSettingsMemberName(method);
  }

  /** The getter function name for the settings for the given method. */
  public String getSettingsFunctionName(MethodModel method) {
    return getSettingsMemberName(method);
  }

  /** The getter function name for the settings for the given method. */
  public String getOperationSettingsFunctionName(MethodModel method) {
    return getOperationSettingsMemberName(method);
  }

  /** The name of a method to apply modifications to this method request. */
  public String getModifyMethodName(MethodContext method) {
    return getNotImplementedString("SurfaceNamer.getModifyMethodName");
  }

  /** The function name to retrieve default call option */
  public String getDefaultCallSettingsFunctionName(InterfaceConfig interfaceConfig) {
    return publicMethodName(
        Name.upperCamel(interfaceConfig.getInterfaceModel().getSimpleName(), "Settings"));
  }

  /** The name of the IAM resource getter function. */
  public String getIamResourceGetterFunctionName(FieldModel field) {
    return getNotImplementedString("SurfaceNamer.getIamResourceGetterFunctionName");
  }

  /** The name of the function that will create a stub. */
  public String getCreateStubFunctionName(InterfaceModel apiInterface) {
    return privateMethodName(
        Name.upperCamel("Create", apiInterface.getSimpleName(), "Stub", "Function"));
  }

  /** Function used to register the GRPC server. */
  public String getServerRegisterFunctionName(InterfaceModel apiInterface) {
    return getNotImplementedString("SurfaceNamer.getServerRegisterFunctionName");
  }

  /** The name of the LRO surface method which can call the given API method. */
  public String getLroApiMethodName(MethodModel method, VisibilityConfig visibility) {
    return getAsyncApiMethodName(method, visibility);
  }

  public String getByteLengthFunctionName(FieldModel field) {
    return getNotImplementedString("SurfaceNamer.getByteLengthFunctionName");
  }

  /////////////////////////////////////// Variable names //////////////////////////////////////////

  /**
   * The name of a variable to hold a value for the given proto message field (such as a flattened
   * parameter).
   */
  public String getVariableName(FieldModel field) {
    return localVarName(field.getNameAsParameterName());
  }

  /**
   * The name of a variable that holds an instance of the class that implements a particular proto
   * interface.
   */
  public String getApiWrapperVariableName(InterfaceConfig interfaceConfig) {
    return localVarName(Name.anyCamel(getInterfaceName(interfaceConfig), "Client"));
  }

  /**
   * The name of a variable that holds the settings class for a particular proto interface; not used
   * in most languages.
   */
  public String getApiSettingsVariableName(InterfaceConfig interfaceConfig) {
    return localVarName(Name.anyCamel(getInterfaceName(interfaceConfig), "Settings"));
  }

  /**
   * The name of the builder class for the settings class for a particular proto interface; not used
   * in most languages.
   */
  public String getApiSettingsBuilderVarName(InterfaceConfig interfaceConfig) {
    return localVarName(Name.anyCamel(getInterfaceName(interfaceConfig), "SettingsBuilder"));
  }

  /** The variable name for the given identifier that is formatted. */
  public String getFormattedVariableName(Name identifier) {
    return localVarName(Name.from("formatted").join(identifier));
  }

  /** The variable name of the rerouted gRPC client. Used in C# */
  public String getReroutedGrpcClientVarName(MethodConfig methodConfig) {
    return getNotImplementedString("SurfaceNamer.getGrpcClientName");
  }

  /** The name of the variable that will hold the stub for an API interface. */
  public String getStubName(InterfaceModel apiInterface) {
    return privateFieldName(Name.upperCamel(apiInterface.getSimpleName(), "Stub"));
  }

  /** The name of the array which will hold the methods for a given stub. */
  public String getStubMethodsArrayName(InterfaceModel apiInterface) {
    return privateMethodName(Name.upperCamel(apiInterface.getSimpleName(), "Stub", "Methods"));
  }

  /** The parameter name for the given lower-case field name. */
  public String getParamName(String var) {
    return localVarName(Name.from(var));
  }

  public String getPropertyName(String var) {
    return publicMethodName(Name.from(var));
  }

  /** The name of a retry definition */
  public String getRetryDefinitionName(String retryDefinitionKey) {
    return privateMethodName(Name.from(retryDefinitionKey));
  }

  /** The name of the field. */
  public String getFieldName(FieldModel field) {
    return publicFieldName(field.getNameAsParameterName());
  }

  /** The page streaming descriptor name for the given method. */
  public String getPageStreamingDescriptorName(MethodModel method) {
    return privateFieldName(method.asName().join(Name.from("page", "streaming", "descriptor")));
  }

  /** The variable name of the gRPC request object. */
  public String getRequestVariableName(MethodModel method) {
    return getNotImplementedString("SurfaceNamer.getRequestVariableName");
  }

  /////////////////////////////////////// Type names /////////////////////////////////////////////

  protected String getInterfaceName(InterfaceConfig interfaceConfig) {
    return interfaceConfig.getName();
  }

  /** The name of the class that operates on a particular resource type. */
  public String getApiWrapperClassName(InterfaceConfig interfaceConfig) {
    return publicClassName(Name.anyCamel(getInterfaceName(interfaceConfig), "Client"));
  }

  /** The name of the class that operates on a particular Discovery Document resource type. */
  public String getApiWrapperClassName(Document document) {
    return publicClassName(Name.anyCamel(document.name(), "Client"));
  }

  /** The name of the class that wraps Callables for the API LRO client. */
  public String getApiLroOperationCallableName(Document document) {
    return publicClassName(Name.anyCamel(document.name(), "OperationSnapshotCallable"));
  }

  public String getGrpcTransportClassName(InterfaceConfig interfaceConfig) {
    return publicClassName(Name.anyCamel(getInterfaceName(interfaceConfig), "GrpcTransport"));
  }

  /** The name of the implementation class that implements a particular proto interface. */
  public String getApiWrapperClassImplName(InterfaceConfig interfaceConfig) {
    return getNotImplementedString("SurfaceNamer.getApiWrapperClassImplName");
  }

  /**
   * The name of the class that holds a sample for an API method, constructed from {@code parts}.
   */
  public String getApiSampleClassName(String... parts) {
    return publicClassName(Name.anyLower(parts));
  }

  /**
   * The name of the file holding the sample class for a single API method and variant. The variant
   * is typically a calling form.
   */
  public String getApiSampleFileName(String className) {
    return getNotImplementedString("SurfaceNamer.getApiSampleFileName");
  }

  /** The name of the class that implements snippets for a particular proto interface. */
  public String getApiSnippetsClassName(InterfaceConfig interfaceConfig) {
    return publicClassName(
        Name.upperCamel(interfaceConfig.getInterfaceModel().getSimpleName(), "ApiSnippets"));
  }

  /**
   * The name of the settings class for a particular proto interface; not used in most languages.
   */
  public String getApiSettingsClassName(InterfaceConfig interfaceConfig) {
    return publicClassName(Name.anyCamel(getInterfaceName(interfaceConfig), "Settings"));
  }

  /**
   * The name of the stub interface for a particular proto interface; not used in most languages.
   */
  public String getApiStubInterfaceName(InterfaceConfig interfaceConfig) {
    return publicClassName(Name.upperCamel(interfaceConfig.getRawName(), "Stub"));
  }

  /**
   * The name of the stub interface for a particular proto interface; not used in most languages.
   */
  public String getApiStubSettingsClassName(InterfaceConfig interfaceConfig) {
    return publicClassName(Name.upperCamel(interfaceConfig.getRawName(), "Stub", "Settings"));
  }

  /**
   * The name of the callable factory for a particular stub interface; not used in most languages.
   */
  public String getCallableFactoryClassName(
      InterfaceConfig interfaceConfig, TransportProtocol transportProtocol) {
    return publicClassName(
        getTransportProtocolName(transportProtocol)
            .join(Name.upperCamel(interfaceConfig.getRawName(), "Callable", "Factory")));
  }

  /** The name of the RPC stub for a particular proto interface; not used in most languages. */
  public String getApiRpcStubClassName(
      InterfaceModel interfaceModel, TransportProtocol transportProtocol) {
    return publicClassName(
        getTransportProtocolName(transportProtocol)
            .join(Name.anyCamel(interfaceModel.getSimpleName(), "Stub")));
  }

  /**
   * The type name of the Grpc service class This needs to match what Grpc generates for the
   * particular language.
   */
  public String getGrpcServiceClassName(InterfaceModel apiInterface) {
    NamePath namePath =
        typeNameConverter.getNamePath(getTypeFormatter().getFullNameFor(apiInterface));
    String grpcContainerName =
        publicClassName(Name.upperCamelKeepUpperAcronyms(namePath.getHead(), "Grpc"));
    String serviceClassName =
        publicClassName(Name.upperCamelKeepUpperAcronyms(apiInterface.getSimpleName(), "ImplBase"));
    return qualifiedName(namePath.withHead(grpcContainerName).append(serviceClassName));
  }

  /**
   * The fully qualified class name of an API interface.
   *
   * <p>TODO: Support the general pattern of package + class name in NameFormatter.
   */
  public String getFullyQualifiedApiWrapperClassName(InterfaceConfig interfaceConfig) {
    return getNotImplementedString("SurfaceNamer.getFullyQualifiedApiWrapperClassName");
  }

  public String getTopLevelAliasedApiClassName(
      InterfaceConfig interfaceConfig, boolean packageHasMultipleServices) {
    return getNotImplementedString("SurfaceNamer.getTopLevelAliasedApiClassName");
  }

  public String getVersionAliasedApiClassName(
      InterfaceConfig interfaceConfig, boolean packageHasMultipleServices) {
    return getNotImplementedString("SurfaceNamer.getVersionAliasedApiClassName");
  }

  protected Name getResourceTypeNameObject(ResourceNameConfig resourceNameConfig) {
    String entityName = resourceNameConfig.getEntityName();
    ResourceNameType resourceNameType = resourceNameConfig.getResourceNameType();
    // Proto annotations use UpperCamelCase for resource names,
    // and GAPIC config uses lower_snake_case, so we have to support both formats.
    Function<String, Name> formatNameFunc;
    if (entityName.length() > 0 && Character.isUpperCase(entityName.charAt(0))) {
      formatNameFunc = Name::upperCamel;
    } else {
      formatNameFunc = Name::anyLower;
    }
    switch (resourceNameType) {
      case ANY:
        return getAnyResourceTypeName();
      case FIXED:
        return Name.anyLower(entityName).join("name_fixed");
      case ONEOF:
        // Remove suffix "_oneof". This allows the collection oneof config to "share" an entity name
        // with a collection config.
        entityName = StringUtil.removeSuffix(entityName, "_oneof");
        return formatNameFunc.apply(entityName).join("name_oneof");
      case SINGLE:
        return formatNameFunc.apply(entityName).join("name");
      case NONE:
      default:
        throw new UnsupportedOperationException("unexpected entity name type");
    }
  }

  protected Name getAnyResourceTypeName() {
    return Name.from(AnyResourceNameConfig.ENTITY_NAME);
  }

  public String getResourceTypeName(ResourceNameConfig resourceNameConfig) {
    String commonResourceName = resourceNameConfig.getCommonResourceName();
    return commonResourceName != null
        ? commonResourceName
        : publicClassName(getResourceTypeNameObject(resourceNameConfig));
  }

  /**
   * The type name of the Grpc server class. This needs to match what Grpc generates for the
   * particular language.
   */
  public String getGrpcServerTypeName(InterfaceModel apiInterface) {
    return getNotImplementedString("SurfaceNamer.getGrpcServerTypeName");
  }

  /** The imported name of the default client config. */
  public String getClientConfigName(InterfaceConfig interfaceConfig) {
    return getNotImplementedString("SurfaceNamer.getClientConfigName");
  }

  /**
   * The type name of the Grpc client class. This needs to match what Grpc generates for the
   * particular language.
   */
  public String getGrpcClientTypeName(InterfaceModel apiInterface) {
    return getNotImplementedString("SurfaceNamer.getGrpcClientTypeName");
  }

  /**
   * Gets the type name of the Grpc client class, saves it to the type table provided, and returns
   * the nickname.
   */
  public String getAndSaveNicknameForGrpcClientTypeName(
      ImportTypeTable typeTable, InterfaceModel apiInterface) {
    return typeTable.getAndSaveNicknameFor(getGrpcClientTypeName(apiInterface));
  }

  /**
   * The type name of the Grpc container class. This needs to match what Grpc generates for the
   * particular language.
   */
  public String getGrpcContainerTypeName(InterfaceModel apiInterface) {
    NamePath namePath =
        typeNameConverter.getNamePath(getTypeFormatter().getFullNameFor(apiInterface));
    String publicClassName =
        publicClassName(Name.upperCamelKeepUpperAcronyms(namePath.getHead(), "Grpc"));
    return qualifiedName(namePath.withHead(publicClassName));
  }

  /** The type name for the method param */
  public String getParamTypeName(ImportTypeTable typeTable, TypeModel type) {
    return getNotImplementedString("SurfaceNamer.getParamTypeName");
  }

  /** The type name for the message property */
  public String getMessagePropertyTypeName(ImportTypeTable typeTable, FieldModel type) {
    return getParamTypeName(typeTable, type.getType());
  }

  /** The type name for an optional array argument; not used in most languages. */
  public String getOptionalArrayTypeName() {
    return getNotImplementedString("SurfaceNamer.getOptionalArrayTypeName");
  }

  /** The return type name in a dynamic language for the given method. */
  public String getDynamicLangReturnTypeName(MethodContext methodContext) {
    return getNotImplementedString("SurfaceNamer.getDynamicReturnTypeName");
  }

  /** The return type name in a static language for the given method. */
  public String getStaticLangReturnTypeName(MethodContext methodContext) {
    return getNotImplementedString("SurfaceNamer.getStaticLangReturnTypeName");
  }

  /** The return type name in a static language that is used by the caller */
  public String getStaticLangCallerReturnTypeName(MethodContext methodContext) {
    return getStaticLangReturnTypeName(methodContext);
  }

  /** The async return type name in a static language for the given method. */
  public String getStaticLangAsyncReturnTypeName(MethodContext methodContext) {
    return getNotImplementedString("SurfaceNamer.getStaticLangAsyncReturnTypeName");
  }

  /**
   * Computes the nickname of the operation response type name for the given method, saves it in the
   * given type table, and returns it.
   */
  public String getAndSaveOperationResponseTypeName(
      MethodContext methodContext, ImportTypeTable typeTable) {
    return getNotImplementedString("SurfaceNamer.getAndSaveOperationResponseTypeName");
  }

  /** The async return type name in a static language that is used by the caller */
  public String getStaticLangCallerAsyncReturnTypeName(MethodContext methodContext) {
    return getStaticLangAsyncReturnTypeName(methodContext);
  }

  /** The name used in Grpc for the given API method. This needs to match what Grpc generates. */
  public String getGrpcMethodName(MethodModel method) {
    // This might seem silly, but it makes clear what we're dealing with (upper camel).
    // This is language-independent because of gRPC conventions.
    return Name.anyCamelKeepUpperAcronyms(method.getSimpleName()).toUpperCamel();
  }

  /**
   * The name used in Grpc for the given API async method. This needs to match what Grpc generates.
   */
  public String getAsyncGrpcMethodName(MethodModel method) {
    return getNotImplementedString("SurfaceNamer.getAsyncGrpcMethodName");
  }

  /** The GRPC streaming server type name for a given method. */
  public String getStreamingServerName(MethodModel method) {
    return getNotImplementedString("SurfaceNamer.getStreamingServerName");
  }

  /** The type name of call options */
  public String getCallSettingsTypeName(InterfaceConfig interfaceConfig) {
    return publicClassName(
        Name.upperCamel(interfaceConfig.getInterfaceModel().getSimpleName(), "Settings"));
  }

  /** The name of the return type of the given grpc streaming method. */
  public String getGrpcStreamingApiReturnTypeName(
      MethodContext methodContext, ImportTypeTable typeTable) {
    return publicClassName(
        Name.upperCamel(methodContext.getMethodModel().getOutputTypeSimpleName()));
  }

  /**
   * The generic-aware response type name for the given type. For example, in Java, this will be the
   * type used for Future&lt;...&gt;.
   */
  public String getGenericAwareResponseTypeName(MethodContext methodContext) {
    return getNotImplementedString("SurfaceNamer.getGenericAwareResponseType");
  }

  /**
   * Computes the nickname of the paged response type name for the given method and resources field,
   * saves it in the given type table, and returns it.
   */
  public String getAndSavePagedResponseTypeName(
      MethodContext methodContext, FieldConfig resourcesFieldConfig) {
    return getNotImplementedString("SurfaceNamer.getAndSavePagedResponseTypeName");
  }

  /** The inner type name of the paged response type for the given method and resources field. */
  public String getPagedResponseTypeInnerName(
      MethodModel method, ImportTypeTable typeTable, FieldModel resourcesField) {
    return getNotImplementedString("SurfaceNamer.getAndSavePagedResponseTypeInnerName");
  }

  /** The inner type name of the page type for the given method and resources field. */
  public String getPageTypeInnerName(
      MethodModel method, ImportTypeTable typeTable, FieldModel resourceField) {
    return getNotImplementedString("SurfaceNamer.getPageTypeInnerName");
  }

  /**
   * The inner type name of the fixed size collection type for the given method and resources field.
   */
  public String getFixedSizeCollectionTypeInnerName(
      MethodModel method, ImportTypeTable typeTable, FieldModel resourceField) {
    return getNotImplementedString("SurfaceNamer.getPageTypeInnerName");
  }

  /**
   * Computes the nickname of the async response type name for the given resource type, saves it in
   * the given type table, and returns it.
   */
  public String getAndSaveAsyncPagedResponseTypeName(
      MethodContext methodContext, FieldConfig resourcesFieldConfig) {
    return getNotImplementedString("SurfaceNamer.getAndSavePagedAsyncResponseTypeName");
  }

  /**
   * Computes the nickname of the response type name for the given resource type, as used by the
   * caller, saves it in the given type table, and returns it.
   */
  public String getAndSaveCallerPagedResponseTypeName(
      MethodContext methodContext, FieldConfig resourcesFieldConfig) {
    return getAndSavePagedResponseTypeName(methodContext, resourcesFieldConfig);
  }

  /**
   * Computes the nickname of the response type name for the given resource type, as used by the
   * caller, saves it in the given type table, and returns it.
   */
  public String getAndSaveCallerAsyncPagedResponseTypeName(
      MethodContext method, FieldConfig resourcesFieldConfig) {
    return getAndSaveAsyncPagedResponseTypeName(method, resourcesFieldConfig);
  }

  /** The class name of the generated resource type from the entity name. */
  public String getAndSaveResourceTypeName(ImportTypeTable typeTable, FieldConfig fieldConfig) {
    String commonResourceName = fieldConfig.getResourceNameConfig().getCommonResourceName();
    String resourceClassName =
        commonResourceName != null
            ? commonResourceName
            : publicClassName(getResourceTypeNameObject(fieldConfig.getResourceNameConfig()));
    return typeTable.getAndSaveNicknameForTypedResourceName(fieldConfig, resourceClassName);
  }

  /** The class name of the generated resource type from the entity name. */
  public String getAndSaveElementResourceTypeName(
      ImportTypeTable typeTable, FieldConfig fieldConfig) {
    ResourceNameConfig resourceNameConfig = fieldConfig.getResourceNameConfig();
    if (resourceNameConfig.getCommonResourceName() != null) {
      String resourceClassName = resourceNameConfig.getCommonResourceName();
      return typeTable.getAndSaveNicknameFor(resourceClassName);
    }
    String resourceClassName = publicClassName(getResourceTypeNameObject(resourceNameConfig));
    return typeTable.getAndSaveNicknameForResourceNameElementType(fieldConfig, resourceClassName);
  }

  /** The fully qualified type name for the stub of an API interface. */
  public String getFullyQualifiedStubType(InterfaceModel apiInterface) {
    return getNotImplementedString("SurfaceNamer.getFullyQualifiedStubType");
  }

  /** The fully qualified type name for the RPC stub of an API interface. */
  public String getFullyQualifiedRpcStubType(
      InterfaceModel interfaceModel, TransportProtocol transportProtocol) {
    return getNotImplementedString("SurfaceNamer.getFullyQualifiedStubType");
  }

  /** The type name of the API callable class for this service method type. */
  public String getApiCallableTypeName(ServiceMethodType serviceMethodType) {
    return getNotImplementedString("SurfaceNamer.getApiCallableTypeName");
  }

  /** Return the type name used to discriminate oneof variants. */
  public String getOneofVariantTypeName(OneofConfig oneof) {
    return getNotImplementedString("SurfaceNamer.getOneofVariantTypeName");
  }

  /**
   * The formatted name of a type used in long running operations, i.e. the operation payload and
   * metadata,
   */
  public String getLongRunningOperationTypeName(ImportTypeTable typeTable, TypeModel type) {
    return getNotImplementedString("SurfaceNamer.getLongRunningOperationTypeName");
  }

  /** The type name for the gPRC request. */
  public String getAndSaveTypeName(ImportTypeTable typeTable, TypeModel type) {
    return getNotImplementedString("SurfaceNamer.getAndSaveTypeName");
  }

  public String getMessageTypeName(ImportTypeTable typeTable, MessageType message) {
    return ((ModelTypeTable) typeTable).getNicknameFor(TypeRef.of(message));
  }

  public String getEnumTypeName(ImportTypeTable typeTable, EnumType enumType) {
    return ((ModelTypeTable) typeTable).getNicknameFor(TypeRef.of(enumType));
  }

  public String getStreamTypeName(GrpcStreamingConfig.GrpcStreamingType type) {
    return getNotImplementedString("SurfaceNamer.getStreamTypeName");
  }

  public String getMockCredentialsClassName(Interface anInterface) {
    return getNotImplementedString("SurfaceNamer.getMockCredentialsClassName");
  }

  public String getFullyQualifiedCredentialsClassName() {
    return getNotImplementedString("SurfaceNamer.getFullyQualifiedCredentialsClassName");
  }

  /////////////////////////////////////// Resource names //////////////////////////////////////////

  public String getResourceParameterName(ResourceNameConfig resourceNameConfig) {
    return localVarName(getResourceTypeNameObject(resourceNameConfig));
  }

  public String getResourcePropertyName(ResourceNameConfig resourceNameConfig) {
    return publicMethodName(getResourceTypeNameObject(resourceNameConfig));
  }

  public String getResourceEnumName(ResourceNameConfig resourceNameConfig) {
    return getResourceTypeNameObject(resourceNameConfig).toUpperUnderscore().toUpperCase();
  }

  /** The parameter name of the IAM resource. */
  public String getIamResourceParamName(FieldModel field) {
    return localVarName(Name.upperCamel(field.getParentSimpleName()));
  }

  public String formatSpec() {
    return "%s";
  }

  /////////////////////////////////////// Path Template ////////////////////////////////////////

  /**
   * The name of a path template constant for the given collection, to be held in an API wrapper
   * class.
   */
  public String getPathTemplateName(
      InterfaceConfig interfaceConfig, SingleResourceNameConfig resourceNameConfig) {
    return inittedConstantName(Name.from(resourceNameConfig.getEntityName(), "path", "template"));
  }

  /** The name of a getter function to get a particular path template for the given collection. */
  public String getPathTemplateNameGetter(
      InterfaceConfig interfaceConfig, SingleResourceNameConfig resourceNameConfig) {
    return publicMethodName(
        Name.from("get", resourceNameConfig.getEntityName(), "name", "template"));
  }

  /** The name of the path template resource, in human format. */
  public String getPathTemplateResourcePhraseName(SingleResourceNameConfig resourceNameConfig) {
    return Name.from(resourceNameConfig.getEntityName()).toPhrase();
  }

  /** The function name to format the entity for the given collection. */
  public String getFormatFunctionName(
      InterfaceConfig interfaceConfig, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(Name.anyLower("format", resourceNameConfig.getEntityName(), "name"));
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

  /////////////////////////////////////// Page Streaming ////////////////////////////////////////

  /** The formatted field name of a page streaming request token. */
  public String getRequestTokenFieldName(PageStreamingConfig pageStreaming) {
    return keyName(Name.from(pageStreaming.getRequestTokenField().getSimpleName()));
  }

  /** The formatted field name of a page streaming response token. */
  public String getResponseTokenFieldName(PageStreamingConfig pageStreaming) {
    return keyName(Name.from(pageStreaming.getResponseTokenField().getSimpleName()));
  }

  /** The formatted name of a page streaming resources field. */
  public String getResourcesFieldName(PageStreamingConfig pageStreaming) {
    return keyName(Name.from(pageStreaming.getResourcesFieldName()));
  }

  ///////////////////////////////////// Constant & Keyword ////////////////////////////////////////

  /** The name of the constant to hold the batching descriptor for the given method. */
  public String getBatchingDescriptorConstName(MethodModel method) {
    return inittedConstantName(Name.anyCamel(method.getSimpleName()).join("bundling_desc"));
  }

  /** The key to use in a dictionary for the given method. */
  public String getMethodKey(MethodModel method) {
    return keyName(method.asName());
  }

  /** The key to use in a dictionary for the given field. */
  public String getFieldKey(FieldModel field) {
    return keyName(field.getNameAsParameterName());
  }

  /** The path to the client config for the given interface. */
  public String getClientConfigPath(InterfaceConfig interfaceConfig) {
    return getNotImplementedString("SurfaceNamer.getClientConfigPath");
  }

  /** The path to a config with a specified name. */
  public String getConfigPath(InterfaceConfig interfaceConfig, String name) {
    return getNotImplementedString("SurfaceNamer.getConfigPath");
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

  /** The private access modifier for the current language. */
  public String getPrivateAccessModifier() {
    return "private";
  }

  /** The name of an RPC status code */
  public String getStatusCodeName(String code) {
    return privateMethodName(Name.upperUnderscore(code));
  }

  /** The name of the constant to hold the page streaming descriptor for the given method. */
  public String getPageStreamingDescriptorConstName(MethodModel method) {
    return inittedConstantName(Name.anyCamel(method.getSimpleName()).join("page_str_desc"));
  }

  /** The name of the constant to hold the page streaming factory for the given method. */
  public String getPagedListResponseFactoryConstName(MethodModel method) {
    return inittedConstantName(Name.anyCamel(method.getSimpleName()).join("page_str_fact"));
  }

  /** The string used to identify the method in the gRPC stub. Not all languages will use this. */
  public String getGrpcStubCallString(InterfaceModel apiInterface, MethodModel method) {
    return getNotImplementedString("SurfaceNamer.getGrpcStubCallString");
  }

  ///////////////////////////////////////// Imports ///////////////////////////////////////////////

  /** Returns true if the request object param type for the given field should be imported. */
  public boolean shouldImportRequestObjectParamType(FieldModel field) {
    return true;
  }

  /**
   * Returns true if the request object param element type for the given field should be imported.
   */
  public boolean shouldImportRequestObjectParamElementType(FieldModel field) {
    return true;
  }

  public String getServiceFileImportName(String filename) {
    return getNotImplementedString("SurfaceNamer.getServiceFileImportName");
  }

  public String getProtoFileImportName(String filename) {
    return getNotImplementedString("SurfaceNamer.getProtoFileImportName");
  }

  public String getGrpcTransportImportName(InterfaceConfig interfaceConfig) {
    return getNotImplementedString("SurfaceNamer.getGrpcTransportImportName");
  }

  public String getVersionIndexFileImportName() {
    return getNotImplementedString("SurfaceNamer.getVersionIndexFileImportName");
  }

  public String getTopLevelIndexFileImportName() {
    return getNotImplementedString("SurfaceNamer.getTopLevelIndexFileImportName");
  }

  public String getCredentialsClassImportName() {
    return getNotImplementedString("SurfaceNamer.getCredentialsClassImportName");
  }
  /////////////////////////////////// Docs & Annotations //////////////////////////////////////////

  /** The documentation name of a parameter for the given lower-case field name. */
  public String getParamDocName(String var) {
    return localVarName(Name.from(var));
  }

  /** Converts the given text to doc lines in the format of the current language. */
  public List<String> getDocLines(String text) {
    return CommonRenderingUtil.getDocLines(commentReformatter.reformat(text));
  }

  /** Converts the given text to doc lines in the format of the current language. */
  public List<String> getDocLines(Schema schema) {
    StringBuffer description = new StringBuffer(schema.description());
    if (schema.additionalProperties() != null
        && !Strings.isNullOrEmpty(schema.additionalProperties().reference())
        && !Strings.isNullOrEmpty(schema.additionalProperties().description())) {
      description
          .append("\nThe key for the map is: ")
          .append(schema.additionalProperties().description());
    }
    return CommonRenderingUtil.getDocLines(commentReformatter.reformat(description.toString()));
  }

  /** Provides the doc lines for the given field in the current language. */
  public List<String> getDocLines(FieldModel field) {
    return getDocLines(field.getScopedDocumentation());
  }

  /** Provides the doc lines for the given method element in the current language. */
  public List<String> getDocLines(MethodModel method, MethodConfig methodConfig) {
    return getDocLines(method.getScopedDescription());
  }

  /** The doc lines that declare what exception(s) are thrown for an API method. */
  public List<String> getThrowsDocLines(MethodConfig methodConfig) {
    return new ArrayList<>();
  }

  /** The doc lines that describe the return value for an API method. */
  public List<String> getReturnDocLines(
      TransformationContext context, MethodContext methodContext, Synchronicity synchronicity) {
    return Collections.singletonList(getNotImplementedString("SurfaceNamer.getReturnDocLines"));
  }

  public String getReleaseAnnotation(ReleaseLevel releaseLevel) {
    return getNotImplementedString("SurfaceNamer.getReleaseAnnotation");
  }

  /** The name of a type with with qualifying articles and descriptions. */
  public String getTypeNameDoc(ImportTypeTable typeTable, TypeModel type) {
    return getNotImplementedString("SurfaceNamer.getTypeNameDoc");
  }

  //////////////////////////////////////// File names ////////////////////////////////////////////

  /** The file name for an API interface. */
  public String getServiceFileName(InterfaceConfig interfaceConfig) {
    return getNotImplementedString("SurfaceNamer.getServiceFileName");
  }

  public String getSourceFilePath(String path, String publicClassName) {
    return getNotImplementedString("SurfaceNamer.getSourceFilePath");
  }

  /** The language-specific file name for a proto file. */
  public String getProtoFileName(String fileSimpleName) {
    return getNotImplementedString("SurfaceNamer.getProtoFileName");
  }

  ////////////////////////////////////////// Test /////////////////////////////////////////////

  public String getTestPackageName() {
    return getNotImplementedString("SurfaceNamer.getTestPackageName");
  }

  public String getTestPackageName(TestKind testKind) {
    return getNotImplementedString("SurfaceNamer.getTestPackageName");
  }

  /** The test case name for the given method. */
  public String getTestCaseName(SymbolTable symbolTable, MethodModel method) {
    Name testCaseName = symbolTable.getNewSymbol(method.asName().join("test"));
    return publicMethodName(testCaseName);
  }

  public String getAsyncTestCaseName(SymbolTable symbolTable, MethodModel method) {
    return getNotImplementedString("SurfaceNamer.getAsyncTestCaseName");
  }

  /** The exception test case name for the given method. */
  public String getExceptionTestCaseName(SymbolTable symbolTable, MethodModel method) {
    Name testCaseName =
        symbolTable.getNewSymbol(method.asName().join(Name.from("exception", "test")));
    return publicMethodName(testCaseName);
  }

  /** The unit test class name for the given API interface. */
  public String getUnitTestClassName(InterfaceConfig interfaceConfig) {
    return publicClassName(Name.anyCamel(getInterfaceName(interfaceConfig), "Client", "Test"));
  }

  /** The smoke test class name for the given API interface. */
  public String getSmokeTestClassName(InterfaceConfig interfaceConfig) {
    return publicClassName(Name.upperCamel(getInterfaceName(interfaceConfig), "Smoke", "Test"));
  }

  /** The class name of the mock gRPC service for the given API interface. */
  public String getMockServiceClassName(InterfaceModel apiInterface) {
    return publicClassName(Name.upperCamelKeepUpperAcronyms("Mock", apiInterface.getSimpleName()));
  }

  /** The class name of a variable to hold the mock gRPC service for the given API interface. */
  public String getMockServiceVarName(InterfaceModel apiInterface) {
    return localVarName(Name.upperCamelKeepUpperAcronyms("Mock", apiInterface.getSimpleName()));
  }

  /** The class name of the mock gRPC service implementation for the given API interface. */
  public String getMockGrpcServiceImplName(InterfaceModel apiInterface) {
    return publicClassName(
        Name.upperCamelKeepUpperAcronyms("Mock", apiInterface.getSimpleName(), "Impl"));
  }

  /** Inject random value generator code to the given string. */
  public String injectRandomStringGeneratorCode(String randomString) {
    return getNotImplementedString("SurfaceNamer.injectRandomStringGeneratorCode");
  }

  ////////////////////////////////////////// Examples ////////////////////////////////////////////

  /** The name of the example package */
  public String getExamplePackageName() {
    return getNotImplementedString("SurfaceNamer.getExamplePackageName");
  }

  /** The local (unqualified) name of the example package */
  public String getLocalExamplePackageName() {
    return getNotImplementedString("SurfaceNamer.getLocalExamplePackageName");
  }

  /**
   * The name of example of the constructor for the service client. The client is VKit generated,
   * not GRPC.
   */
  public String getApiWrapperClassConstructorExampleName(InterfaceConfig interfaceConfig) {
    return getApiWrapperClassConstructorName(interfaceConfig);
  }

  /** The name of the example for the paged callable variant. */
  public String getPagedCallableMethodExampleName(MethodModel method) {
    return getPagedCallableMethodName(method);
  }

  /** The name of the example for the plain callable variant. */
  public String getCallableMethodExampleName(MethodModel method) {
    return getCallableMethodName(method);
  }

  /** The name of the example for the operation callable variant of the given method. */
  public String getOperationCallableMethodExampleName(MethodModel method) {
    return getOperationCallableMethodName(method);
  }

  /** The name of the example for the method. */
  public String getApiMethodExampleName(InterfaceConfig interfaceConfig, MethodModel method) {
    return getApiMethodName(
        Name.anyCamel(interfaceConfig.getInterfaceModel().getSimpleName()),
        VisibilityConfig.PUBLIC);
  }

  /** The name of the example for the async variant of the given method. */
  public String getAsyncApiMethodExampleName(MethodModel method) {
    return getAsyncApiMethodName(method, VisibilityConfig.PUBLIC);
  }

  /**
   * The name of the example of the GRPC streaming surface method which can call the given API
   * method.
   */
  public String getGrpcStreamingApiMethodExampleName(
      InterfaceConfig interfaceConfig, MethodModel method) {
    return getGrpcStreamingApiMethodName(method, VisibilityConfig.PUBLIC);
  }

  /** The example name of the IAM resource getter function. */
  public String getIamResourceGetterFunctionExampleName(
      InterfaceConfig interfaceConfig, FieldModel field) {
    return getIamResourceGetterFunctionName(field);
  }

  /** The file name for the example of an API interface. */
  public String getExampleFileName(InterfaceConfig interfaceConfig) {
    return getNotImplementedString("SurfaceNamer.getExampleFileName");
  }

  /**
   * Translate C-printf-spec and a list of args into those expected by the language's format
   * utilities.
   *
   * <p>For languages that prefer interpolating the arguments in the format string, the returned
   * list will only have one element, the interpolated format string.
   *
   * <p>For languages that prefer using placeholders in the format string, the returned list will be
   * a format string followed by all the arguments to replace the placeholders.
   */
  public List<String> getPrintSpecs(String spec, List<String> args) {
    StringBuilder sb = new StringBuilder();
    int p = 0;
    while (true) {
      int pos = spec.indexOf('%', p);
      if (pos == -1) {
        sb.append(spec, p, spec.length());
        return ImmutableList.<String>builder()
            .add(
                sb.toString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\t", "\\t")
                    .replace("\n", "\\n"))
            .addAll(args)
            .build();
      } else {
        sb.append(spec, p, pos);
      }
      if (spec.startsWith("%s", pos)) {
        sb.append("%s");
        p = pos + 2;
      } else if (spec.startsWith("%%", pos)) {
        sb.append("%%");
        p = pos + 2;
      } else {
        throw new IllegalArgumentException(String.format("bad format verb: %s", spec));
      }
    }
  }

  /** Returns the formatted expression to nicely print a field of a variable. */
  public String getFormattedPrintArgName(TypeModel type, String variable, List<String> accessors) {
    if (accessors.isEmpty()) {
      return variable;
    }
    return variable + String.join("", accessors);
  }

  /**
   * Returns the expression to access an element of a collection by index.
   *
   * <p>Note that the returned value includes the language-specific syntax for accessing an element
   * of a collection. For example, the returned value will be `.get(i)` in Java, and `[i]` in PHP.
   */
  public String getIndexAccessorName(int index) {
    return getNotImplementedString("SurfaceNamer.getIndexAccessorName");
  }

  /**
   * Returns the expression to access a field of a protobuf object.
   *
   * <p>Note that the returned value includes not only the field name, but also the
   * language-specific syntax for accessing a protobuf object field. For example, the returned value
   * will be `.getField()` in Java, and `-&gt;getField()` in PHP.
   */
  public String getFieldAccessorName(FieldModel field) {
    return getNotImplementedString("SurfaceNamer.getFieldAccessorName");
  }

  /**
   * Returns the expression to get the value of a map entry by its key.
   *
   * <p>Note that the returned value includes the language-specific syntax for accessing a map entry
   * by key. For example, the returned value will be `.get("key")` in Java, and `["key"]` in Python.
   */
  public String getMapKeyAccessorName(TypeModel keyType, String key) {
    return getNotImplementedString("SurfaceNamer.getMapKeyAccessorName");
  }

  public String getSampleResponseVarName(MethodContext context, CallingForm form) {
    MethodConfig config = context.getMethodConfig();
    if (config.getPageStreaming() != null) {
      return "responseItem";
    }
    if (config.getGrpcStreaming() != null) {
      GrpcStreamingConfig.GrpcStreamingType type = config.getGrpcStreaming().getType();
      if (type == GrpcStreamingConfig.GrpcStreamingType.ServerStreaming
          || type == GrpcStreamingConfig.GrpcStreamingType.BidiStreaming) {
        return "responseItem";
      }
    }
    return "response";
  }

  public Set<String> getSampleUsedVarNames(MethodContext context, CallingForm form) {
    // TODO: Change this to an empty set once the PHP, Java, and Python implementations of this
    // method are in place to define these identifiers. These values are here for now so we don't
    // break those languages in the interim.
    return ImmutableSet.of("response", "responseItem");
  }

  public String getSampleFunctionName(MethodModel method) {
    return getApiMethodName(Name.from("sample").join(method.asName()), VisibilityConfig.PRIVATE);
  }

  /////////////////////////////////// Transport Protocol /////////////////////////////////////////

  public Name getTransportProtocolName(TransportProtocol protocol) {
    switch (protocol) {
      case HTTP:
        return Name.from("http", "json");
      case GRPC:
      default:
        return Name.from("grpc");
    }
  }

  public String getInstantiatingChannelProvider(TransportProtocol protocol) {
    return publicClassName(
        Name.from("instantiating")
            .join(getTransportProtocolName(protocol).join("channel").join("provider")));
  }

  public String getTransportProvider(TransportProtocol protocol) {
    Name protocolName = getTransportProtocolName(protocol);
    return publicClassName(protocolName.join("transport").join("provider"));
  }

  public String getDefaultTransportProviderBuilder(TransportProtocol protocol) {
    Name protocolName = getTransportProtocolName(protocol);
    return privateMethodName(
        Name.from("default").join(protocolName).join("transport").join("provider").join("builder"));
  }

  public String getDefaultChannelProviderBuilder(TransportProtocol protocol) {
    Name protocolName = getTransportProtocolName(protocol);
    return privateMethodName(
        Name.from("default").join(protocolName).join("channel").join("provider").join("builder"));
  }

  public String getTransporNameGetMethod(TransportProtocol protocol) {
    Name protocolName = getTransportProtocolName(protocol);
    return privateMethodName(Name.from("get").join(protocolName).join("transport").join("name"));
  }

  public String getTransportClassName(TransportProtocol protocol) {
    Name protocolName = getTransportProtocolName(protocol);
    return publicClassName(protocolName.join(Name.anyCamel("TransportChannel")));
  }

  ////////////////////////////////////////// Utility /////////////////////////////////////////////

  /** Indicates whether the specified method supports retry settings. */
  public boolean methodHasRetrySettings(MethodConfig methodConfig) {
    return true;
  }

  /** Indicates whether the specified method supports timeout settings. */
  public boolean methodHasTimeoutSettings(MethodConfig methodConfig) {
    return true;
  }

  /** Make the given type name able to accept nulls, if it is a primitive type */
  public String makePrimitiveTypeNullable(String typeName, FieldModel type) {
    return typeName;
  }

  /** Is this type a primitive, according to target language. */
  public boolean isPrimitive(TypeModel type) {
    return type.isPrimitive();
  }

  /** Is this type a primitive, according to target language. */
  public boolean isPrimitive(FieldModel type) {
    return type.isPrimitive();
  }

  /** The default value for an optional field, null if no default value required. */
  public String getOptionalFieldDefaultValue(FieldConfig fieldConfig, MethodContext context) {
    return getNotImplementedString("SurfaceNamer.getOptionalFieldDefaultValue");
  }

  public String getToStringMethod() {
    return getNotImplementedString("SurfaceNamer.getToStringMethod");
  }
}
