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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.metacode.InitFieldConfig;
import com.google.api.codegen.ruby.RubyUtil;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.Synchronicity;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.ruby.RubyCommentReformatter;
import com.google.api.codegen.util.ruby.RubyNameFormatter;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** The SurfaceNamer for Ruby. */
public class RubySurfaceNamer extends SurfaceNamer {
  public RubySurfaceNamer(String packageName) {
    super(
        new RubyNameFormatter(),
        new ModelTypeFormatterImpl(new RubyModelTypeNameConverter(packageName)),
        new RubyTypeTable(packageName),
        new RubyCommentReformatter(),
        packageName,
        packageName);
  }

  @Override
  public SurfaceNamer cloneWithPackageName(String packageName) {
    return new RubySurfaceNamer(packageName);
  }

  /** The name of the class that implements snippets for a particular proto interface. */
  @Override
  public String getApiSnippetsClassName(Interface apiInterface) {
    return publicClassName(Name.upperCamel(apiInterface.getSimpleName(), "ClientSnippets"));
  }

  /** The function name to set a field having the given type and name. */
  @Override
  public String getFieldSetFunctionName(TypeRef type, Name identifier) {
    return getFieldGetFunctionName(type, identifier);
  }

  /** The function name to format the entity for the given collection. */
  @Override
  public String getFormatFunctionName(
      Interface apiInterface, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(Name.from(resourceNameConfig.getEntityName(), "path"));
  }

  @Override
  public String getParseFunctionName(String var, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(
        Name.from("match", var, "from", resourceNameConfig.getEntityName(), "name"));
  }

  @Override
  public String getClientConfigPath(Interface apiInterface) {
    return Name.upperCamel(apiInterface.getSimpleName()).join("client_config").toLowerUnderscore()
        + ".json";
  }

  @Override
  public String getRequestVariableName(Method method) {
    return method.getRequestStreaming() ? "reqs" : "req";
  }

  /**
   * The type name of the Grpc client class. This needs to match what Grpc generates for the
   * particular language.
   */
  @Override
  public String getGrpcClientTypeName(Interface apiInterface) {
    return getModelTypeFormatter().getFullNameFor(apiInterface);
  }

  @Override
  public String getParamTypeName(ModelTypeTable typeTable, TypeRef type) {
    if (type.isMap()) {
      String keyTypeName = typeTable.getFullNameForElementType(type.getMapKeyField().getType());
      String valueTypeName = typeTable.getFullNameForElementType(type.getMapValueField().getType());
      if (type.getMapValueField().getType().isMessage()) {
        valueTypeName += " | Hash";
      }
      return new TypeName(
              typeTable.getFullNameFor(type),
              typeTable.getNicknameFor(type),
              "%s{%i => %i}",
              new TypeName(keyTypeName),
              new TypeName(valueTypeName))
          .getFullName();
    }

    String elementTypeName = typeTable.getFullNameForElementType(type);
    if (type.isMessage()) {
      elementTypeName += " | Hash";
    }
    if (type.isRepeated()) {
      return new TypeName(
              typeTable.getFullNameFor(type),
              typeTable.getNicknameFor(type),
              "%s<%i>",
              new TypeName(elementTypeName))
          .getFullName();
    }

    return elementTypeName;
  }

  /** The type name for the message property */
  public String getMessagePropertyTypeName(ModelTypeTable typeTable, TypeRef type) {
    if (type.isMap()) {
      String keyTypeName = typeTable.getFullNameForElementType(type.getMapKeyField().getType());
      String valueTypeName = typeTable.getFullNameForElementType(type.getMapValueField().getType());
      return new TypeName(
              typeTable.getFullNameFor(type),
              typeTable.getNicknameFor(type),
              "%s{%i => %i}",
              new TypeName(keyTypeName),
              new TypeName(valueTypeName))
          .getFullName();
    }

    if (type.isRepeated()) {
      String elementTypeName = typeTable.getFullNameForElementType(type);
      return new TypeName(
              typeTable.getFullNameFor(type),
              typeTable.getNicknameFor(type),
              "%s<%i>",
              new TypeName(elementTypeName))
          .getFullName();
    }

    return typeTable.getFullNameForElementType(type);
  }

  @Override
  public String getDynamicLangReturnTypeName(Method method, GapicMethodConfig methodConfig) {
    if (ServiceMessages.s_isEmptyType(method.getOutputType())) {
      return "";
    }

    String classInfo = getModelTypeFormatter().getFullNameForElementType(method.getOutputType());
    if (method.getResponseStreaming()) {
      return "Enumerable<" + classInfo + ">";
    }

    if (methodConfig.isPageStreaming()) {
      TypeRef resourceType = methodConfig.getPageStreaming().getResourcesField().getType();
      String resourceTypeName = getModelTypeFormatter().getFullNameForElementType(resourceType);
      return "Google::Gax::PagedEnumerable<" + resourceTypeName + ">";
    }

    if (methodConfig.isLongRunningOperation()) {
      return "Google::Gax::Operation";
    }

    return classInfo;
  }

  @Override
  public String getFullyQualifiedStubType(Interface apiInterface) {
    NamePath namePath =
        getTypeNameConverter().getNamePath(getModelTypeFormatter().getFullNameFor(apiInterface));
    return qualifiedName(namePath.append("Stub"));
  }

  @Override
  public String getLongRunningOperationTypeName(ModelTypeTable typeTable, TypeRef type) {
    return typeTable.getFullNameFor(type);
  }

  @Override
  public String getRequestTypeName(ModelTypeTable typeTable, TypeRef type) {
    return typeTable.getFullNameFor(type);
  }

  @Override
  public String getFullyQualifiedCredentialsClassName() {
    if (RubyUtil.isLongrunning(getPackageName())) {
      return "Google::Gax::Credentials";
    }
    return getTopLevelNamespace() + "::Credentials";
  }

  @Override
  public List<String> getThrowsDocLines(GapicMethodConfig methodConfig) {
    return ImmutableList.of("@raise [Google::Gax::GaxError] if the RPC is aborted.");
  }

  @Override
  public List<String> getReturnDocLines(
      GapicInterfaceContext context, GapicMethodConfig methodConfig, Synchronicity synchronicity) {
    Method method = methodConfig.getMethod();
    if (method.getResponseStreaming()) {
      String classInfo = getModelTypeFormatter().getFullNameForElementType(method.getOutputType());
      return ImmutableList.of("An enumerable of " + classInfo + " instances.", "");
    }

    if (methodConfig.isPageStreaming()) {
      TypeRef resourceType = methodConfig.getPageStreaming().getResourcesField().getType();
      String resourceTypeName = getModelTypeFormatter().getFullNameForElementType(resourceType);
      return ImmutableList.of(
          "An enumerable of " + resourceTypeName + " instances.",
          "See Google::Gax::PagedEnumerable documentation for other",
          "operations such as per-page iteration or access to the response",
          "object.");
    }

    return ImmutableList.<String>of();
  }

  /** The file name for an API interface. */
  @Override
  public String getServiceFileName(GapicInterfaceConfig interfaceConfig) {
    return getPackageFilePath()
        + "/"
        + classFileNameBase(Name.upperCamel(getApiWrapperClassName(interfaceConfig)));
  }

  @Override
  public String getSourceFilePath(String path, String publicClassName) {
    return path + File.separator + Name.upperCamel(publicClassName).toLowerUnderscore() + ".rb";
  }

  @Override
  public String getProtoFileName(ProtoFile file) {
    String protoFilename = file.getSimpleName();
    return protoFilename.substring(0, protoFilename.length() - "proto".length()) + "rb";
  }

  @Override
  public String getFullyQualifiedApiWrapperClassName(GapicInterfaceConfig interfaceConfig) {
    return getPackageName() + "::" + getApiWrapperClassName(interfaceConfig);
  }

  @Override
  public String getTopLevelAliasedApiClassName(
      GapicInterfaceConfig interfaceConfig, boolean packageHasMultipleServices) {
    if (!RubyUtil.hasMajorVersion(getPackageName())) {
      return getVersionAliasedApiClassName(interfaceConfig, packageHasMultipleServices);
    }
    return packageHasMultipleServices
        ? getTopLevelNamespace() + "::" + getPackageServiceName(interfaceConfig.getInterface())
        : getTopLevelNamespace();
  }

  @Override
  public String getVersionAliasedApiClassName(
      GapicInterfaceConfig interfaceConfig, boolean packageHasMultipleServices) {
    return packageHasMultipleServices
        ? getPackageName() + "::" + getPackageServiceName(interfaceConfig.getInterface())
        : getPackageName();
  }

  @Override
  public String getTopLevelNamespace() {
    return Joiner.on("::").join(getTopLevelApiModules());
  }

  @Override
  public ImmutableList<String> getApiModules() {
    return ImmutableList.copyOf(Splitter.on("::").split(getPackageName()));
  }

  @Override
  public List<String> getTopLevelApiModules() {
    List<String> apiModules = getApiModules();
    return apiModules.subList(0, apiModules.size() - 1);
  }

  @Override
  public String getServiceFileImportName(String filename) {
    return filename.replace(".proto", "_services_pb");
  }

  @Override
  public String getProtoFileImportName(String filename) {
    return filename.replace(".proto", "_pb");
  }

  @Override
  public String injectRandomStringGeneratorCode(String randomString) {
    String delimiter = ",";
    String[] split =
        CommonRenderingUtil.stripQuotes(randomString)
            .replace(
                InitFieldConfig.RANDOM_TOKEN, delimiter + InitFieldConfig.RANDOM_TOKEN + delimiter)
            .split(delimiter);
    ArrayList<String> stringParts = new ArrayList<>();
    for (String token : split) {
      if (token.length() > 0) {
        if (token.equals(InitFieldConfig.RANDOM_TOKEN)) {
          stringParts.add("Time.new.to_i.to_s");
        } else {
          stringParts.add("\"" + token + "\"");
        }
      }
    }
    return Joiner.on(" + ").join(stringParts);
  }

  @Override
  public String getVersionIndexFileImportName() {
    return getPackageFilePath();
  }

  @Override
  public String getTopLevelIndexFileImportName() {
    List<String> newNames = new ArrayList<>();
    for (String name : getTopLevelNamespace().split("::")) {
      newNames.add(packageFilePathPiece(Name.upperCamel(name)));
    }
    return Joiner.on(File.separator).join(newNames.toArray());
  }

  @Override
  public String getCredentialsClassImportName() {
    // Place credentials in top-level namespace.
    List<String> paths = new ArrayList<>();
    for (String part : getTopLevelApiModules()) {
      paths.add(packageFilePathPiece(Name.upperCamel(part)));
    }
    paths.add("credentials");
    return Joiner.on(File.separator).join(paths);
  }

  private String getPackageFilePath() {
    List<String> newNames = new ArrayList<>();
    for (String name : getPackageName().split("::")) {
      newNames.add(packageFilePathPiece(Name.upperCamel(name)));
    }
    return Joiner.on("/").join(newNames.toArray());
  }

  @Override
  public String getFieldGetFunctionName(FeatureConfig featureConfig, FieldConfig fieldConfig) {
    return getFieldKey(fieldConfig.getField());
  }

  @Override
  public String getFieldGetFunctionName(TypeRef type, Name identifier) {
    return keyName(identifier);
  }

  @Override
  public String getGrpcStubCallString(Interface apiInterface, Method method) {
    return getFullyQualifiedStubType(apiInterface);
  }

  @Override
  public String getLroApiMethodName(Method method, VisibilityConfig visibility) {
    return getMethodKey(method);
  }

  @Override
  public String getPackageServiceName(Interface apiInterface) {
    return publicClassName(getReducedServiceName(apiInterface.getSimpleName()));
  }
}
