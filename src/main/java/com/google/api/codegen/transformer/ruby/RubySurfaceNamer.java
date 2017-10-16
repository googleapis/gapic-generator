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

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.metacode.InitFieldConfig;
import com.google.api.codegen.ruby.RubyUtil;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.Synchronicity;
import com.google.api.codegen.transformer.TransformationContext;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.VersionMatcher;
import com.google.api.codegen.util.ruby.RubyCommentReformatter;
import com.google.api.codegen.util.ruby.RubyNameFormatter;
import com.google.api.codegen.util.ruby.RubyTypeTable;
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
  public String getApiSnippetsClassName(InterfaceModel apiInterface) {
    return publicClassName(Name.upperCamel(apiInterface.getSimpleName(), "ClientSnippets"));
  }

  /** The function name to set a field. */
  @Override
  public String getFieldSetFunctionName(FieldModel field) {
    return getFieldGetFunctionName(field);
  }

  /** The function name to set a field having the given type and name. */
  @Override
  public String getFieldSetFunctionName(TypeRef type, Name identifier) {
    return getFieldGetFunctionName(type, identifier);
  }

  /** The function name to format the entity for the given collection. */
  @Override
  public String getFormatFunctionName(
      InterfaceModel apiInterface, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(Name.from(resourceNameConfig.getEntityName(), "path"));
  }

  @Override
  public String getParseFunctionName(String var, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(
        Name.from("match", var, "from", resourceNameConfig.getEntityName(), "name"));
  }

  @Override
  public String getClientConfigPath(InterfaceModel apiInterface) {
    return Name.upperCamel(apiInterface.getSimpleName()).join("client_config").toLowerUnderscore()
        + ".json";
  }

  @Override
  public String getRequestVariableName(MethodModel method) {
    return method.getRequestStreaming() ? "reqs" : "req";
  }

  /**
   * The type name of the Grpc client class. This needs to match what Grpc generates for the
   * particular language.
   */
  @Override
  public String getGrpcClientTypeName(InterfaceModel apiInterface) {
    return getTypeFormatter().getFullNameFor(apiInterface);
  }

  @Override
  public String getParamTypeName(ImportTypeTable typeTable, FieldModel type) {
    if (type.isMap()) {
      String keyTypeName = typeTable.getFullNameForElementType(type.getMapKeyField());
      String valueTypeName = typeTable.getFullNameForElementType(type.getMapValueField());
      if (type.getMapValueField().isMessage()) {
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
  @Override
  public String getMessagePropertyTypeName(ImportTypeTable importTypeTable, FieldModel fieldModel) {
    if (fieldModel.isMap()) {
      String keyTypeName = importTypeTable.getFullNameForElementType(fieldModel.getMapKeyField());
      String valueTypeName =
          importTypeTable.getFullNameForElementType(fieldModel.getMapValueField());
      return new TypeName(
              importTypeTable.getFullNameFor(fieldModel),
              importTypeTable.getNicknameFor(fieldModel),
              "%s{%i => %i}",
              new TypeName(keyTypeName),
              new TypeName(valueTypeName))
          .getFullName();
    }

    if (fieldModel.isRepeated()) {
      String elementTypeName = importTypeTable.getFullNameForElementType(fieldModel);
      return new TypeName(
              importTypeTable.getFullNameFor(fieldModel),
              importTypeTable.getNicknameFor(fieldModel),
              "%s<%i>",
              new TypeName(elementTypeName))
          .getFullName();
    }

    return importTypeTable.getFullNameForElementType(fieldModel);
  }

  @Override
  public String getDynamicLangReturnTypeName(MethodContext methodContext) {
    MethodModel method = methodContext.getMethodModel();
    MethodConfig methodConfig = methodContext.getMethodConfig();
    if (method.isOutputTypeEmpty()) {
      return "";
    }

    String classInfo = method.getOutputTypeName(methodContext.getTypeTable()).getFullName();
    if (method.getResponseStreaming()) {
      return "Enumerable<" + classInfo + ">";
    }

    if (methodConfig.isPageStreaming()) {
      String resourceTypeName =
          getModelTypeFormatter()
              .getFullNameForElementType(methodConfig.getPageStreaming().getResourcesField());
      return "Google::Gax::PagedEnumerable<" + resourceTypeName + ">";
    }

    if (methodConfig.isLongRunningOperation()) {
      return "Google::Gax::Operation";
    }

    return classInfo;
  }

  @Override
  public String getFullyQualifiedStubType(InterfaceModel apiInterface) {
    NamePath namePath =
        getTypeNameConverter().getNamePath(getModelTypeFormatter().getFullNameFor(apiInterface));
    return qualifiedName(namePath.append("Stub"));
  }

  @Override
  public String getLongRunningOperationTypeName(ImportTypeTable typeTable, TypeRef type) {
    return ((ModelTypeTable) typeTable).getFullNameFor(type);
  }

  @Override
  public String getRequestTypeName(ImportTypeTable typeTable, TypeRef type) {
    return ((ModelTypeTable) typeTable).getFullNameFor(type);
  }

  @Override
  public String getFullyQualifiedCredentialsClassName() {
    if (RubyUtil.isLongrunning(getPackageName())) {
      return "Google::Auth::Credentials";
    }
    return getTopLevelNamespace() + "::Credentials";
  }

  @Override
  public List<String> getThrowsDocLines(MethodConfig methodConfig) {
    return ImmutableList.of("@raise [Google::Gax::GaxError] if the RPC is aborted.");
  }

  @Override
  public List<String> getReturnDocLines(
      TransformationContext context, MethodContext methodContext, Synchronicity synchronicity) {
    MethodModel method = methodContext.getMethodModel();
    MethodConfig methodConfig = methodContext.getMethodConfig();
    if (method.getResponseStreaming()) {
      String classInfo = method.getOutputTypeName(methodContext.getTypeTable()).getFullName();
      return ImmutableList.of("An enumerable of " + classInfo + " instances.", "");
    }

    if (methodConfig.isPageStreaming()) {
      String resourceTypeName =
          getTypeFormatter()
              .getFullNameForElementType(methodConfig.getPageStreaming().getResourcesField());
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
  public String getServiceFileName(InterfaceConfig interfaceConfig) {
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
  public String getFullyQualifiedApiWrapperClassName(InterfaceConfig interfaceConfig) {
    return getPackageName() + "::" + getApiWrapperClassName(interfaceConfig);
  }

  @Override
  public String getTopLevelAliasedApiClassName(
      InterfaceConfig interfaceConfig, boolean packageHasMultipleServices) {
    if (!RubyUtil.hasMajorVersion(getPackageName())) {
      return getVersionAliasedApiClassName(interfaceConfig, packageHasMultipleServices);
    }
    return packageHasMultipleServices
        ? getTopLevelNamespace() + "::" + getPackageServiceName(interfaceConfig.getInterfaceModel())
        : getTopLevelNamespace();
  }

  @Override
  public String getVersionAliasedApiClassName(
      InterfaceConfig interfaceConfig, boolean packageHasMultipleServices) {
    return packageHasMultipleServices
        ? getPackageName() + "::" + getPackageServiceName(interfaceConfig.getInterfaceModel())
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
    return hasVersionModule(apiModules) ? apiModules.subList(0, apiModules.size() - 1) : apiModules;
  }

  private static boolean hasVersionModule(List<String> apiModules) {
    String versionModule = apiModules.get(apiModules.size() - 1);
    String version = Name.upperCamel(versionModule).toLowerUnderscore();
    return VersionMatcher.isVersion(version);
  }

  @Override
  public String getModuleVersionName() {
    List<String> apiModules = getApiModules();
    return apiModules.get(apiModules.size() - 1);
  }

  @Override
  public String getModuleServiceName() {
    List<String> apiModules = getTopLevelApiModules();
    return apiModules.get(apiModules.size() - 1);
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
  public String getFieldGetFunctionName(FieldModel type, Name identifier) {
    return keyName(identifier);
  }

  @Override
  public String getGrpcStubCallString(InterfaceModel apiInterface, MethodModel method) {
    return getFullyQualifiedStubType(apiInterface);
  }

  @Override
  public String getLroApiMethodName(MethodModel method, VisibilityConfig visibility) {
    return getMethodKey(method);
  }

  @Override
  public String getPackageServiceName(InterfaceModel apiInterface) {
    return publicClassName(getReducedServiceName(apiInterface.getSimpleName()));
  }
}
