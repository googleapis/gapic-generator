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
package com.google.api.codegen.transformer.php;

import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.php.PhpCommentReformatter;
import com.google.api.codegen.util.php.PhpNameFormatter;
import com.google.api.codegen.util.php.PhpPackageUtil;
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import java.util.ArrayList;

/** The SurfaceNamer for PHP. */
public class PhpSurfaceNamer extends SurfaceNamer {
  public PhpSurfaceNamer(String packageName) {
    super(
        new PhpNameFormatter(),
        new ModelTypeFormatterImpl(new PhpModelTypeNameConverter(packageName)),
        new PhpTypeTable(packageName),
        new PhpCommentReformatter(),
        packageName);
  }

  @Override
  public String getLroApiMethodName(Method method, VisibilityConfig visibility) {
    return getApiMethodName(method, visibility);
  }

  @Override
  public String getFieldSetFunctionName(TypeRef type, Name identifier) {
    return publicMethodName(Name.from("set").join(identifier));
  }

  @Override
  public String getFieldAddFunctionName(TypeRef type, Name identifier) {
    return publicMethodName(Name.from("add").join(identifier));
  }

  @Override
  public String getFieldGetFunctionName(TypeRef type, Name identifier) {
    if (type.isRepeated() && !type.isMap()) {
      return publicMethodName(Name.from("get").join(identifier).join("list"));
    } else {
      return publicMethodName(Name.from("get").join(identifier));
    }
  }

  @Override
  public String getPathTemplateName(
      Interface apiInterface, SingleResourceNameConfig resourceNameConfig) {
    return inittedConstantName(Name.from(resourceNameConfig.getEntityName(), "name", "template"));
  }

  @Override
  public String getClientConfigPath(Interface apiInterface) {
    return "resources/"
        + Name.upperCamel(apiInterface.getSimpleName()).join("client_config").toLowerUnderscore()
        + ".json";
  }

  @Override
  public boolean shouldImportRequestObjectParamType(Field field) {
    return field.getType().isMap();
  }

  @Override
  public String getRetrySettingsTypeName() {
    return "\\Google\\GAX\\RetrySettings";
  }

  @Override
  public String getOptionalArrayTypeName() {
    return "array";
  }

  @Override
  public String getDynamicLangReturnTypeName(Method method, GapicMethodConfig methodConfig) {
    if (new ServiceMessages().isEmptyType(method.getOutputType())) {
      return "";
    }
    if (methodConfig.isPageStreaming()) {
      return "\\Google\\GAX\\PagedListResponse";
    }
    switch (methodConfig.getGrpcStreamingType()) {
      case NonStreaming:
        return getModelTypeFormatter().getFullNameFor(method.getOutputType());
      case BidiStreaming:
        return "\\Google\\GAX\\BidiStreamingResponse";
      case ClientStreaming:
        return "\\Google\\GAX\\ClientStreamingResponse";
      case ServerStreaming:
        return "\\Google\\GAX\\ServerStreamingResponse";
      default:
        return getNotImplementedString(
            "SurfaceNamer.getDynamicReturnTypeName grpcStreamingType:"
                + methodConfig.getGrpcStreamingType().toString());
    }
  }

  @Override
  public String getFullyQualifiedApiWrapperClassName(GapicInterfaceConfig interfaceConfig) {
    return getPackageName() + "\\" + getApiWrapperClassName(interfaceConfig);
  }

  @Override
  public String getGrpcClientTypeName(Interface apiInterface) {
    return qualifiedName(getGrpcClientTypeName(apiInterface, "GrpcClient"));
  }

  private NamePath getGrpcClientTypeName(Interface apiInterface, String suffix) {
    NamePath namePath =
        getTypeNameConverter().getNamePath(getModelTypeFormatter().getFullNameFor(apiInterface));
    String publicClassName =
        publicClassName(Name.upperCamelKeepUpperAcronyms(namePath.getHead(), suffix));
    return namePath.withHead(publicClassName);
  }

  @Override
  public String getLongRunningOperationTypeName(ModelTypeTable typeTable, TypeRef type) {
    return typeTable.getAndSaveNicknameFor(type);
  }

  @Override
  public String getRequestTypeName(ModelTypeTable typeTable, TypeRef type) {
    return typeTable.getAndSaveNicknameFor(type);
  }

  @Override
  public String getGrpcStubCallString(Interface apiInterface, Method method) {
    return '/' + apiInterface.getFullName() + '/' + getGrpcMethodName(method);
  }

  @Override
  public String getTestPackageName() {
    return getTestPackageName(getPackageName());
  }

  /** Insert "Tests" into the package name after "Google\Cloud" standard prefix */
  private static String getTestPackageName(String packageName) {
    final String[] PACKAGE_PREFIX = PhpPackageUtil.getStandardPackagePrefix();

    ArrayList<String> packageComponents = new ArrayList<>();
    String[] packageSplit = PhpPackageUtil.splitPackageName(packageName);
    int packageStartIndex = 0;
    for (int i = 0; i < PACKAGE_PREFIX.length && i < packageSplit.length; i++) {
      if (packageSplit[i].equals(PACKAGE_PREFIX[i])) {
        packageStartIndex++;
      } else {
        break;
      }
    }
    for (int i = 0; i < packageStartIndex; i++) {
      packageComponents.add(packageSplit[i]);
    }
    packageComponents.add("Tests");
    for (int i = packageStartIndex; i < packageSplit.length; i++) {
      packageComponents.add(packageSplit[i]);
    }
    return PhpPackageUtil.buildPackageName(packageComponents);
  }

  @Override
  public boolean methodHasRetrySettings(GapicMethodConfig methodConfig) {
    return !methodConfig.isGrpcStreaming();
  }

  public String getRequestTokenFieldName(PageStreamingConfig pageStreaming) {
    // Not using keyName since PHP maps the string "requestPageTokenField" to the raw request token
    // field name from the page streaming config.
    return pageStreaming.getRequestTokenField().getSimpleName();
  }

  public String getPageSizeFieldName(PageStreamingConfig pageStreaming) {
    // Not using keyName since PHP maps the string "requestPageSizeField" to the raw page size
    // field name from the page streaming config.
    return pageStreaming.getPageSizeField().getSimpleName();
  }

  public String getResponseTokenFieldName(PageStreamingConfig pageStreaming) {
    // Not using keyName since PHP maps the string "responsePageTokenField" to the raw response
    // token field name from the page streaming config.
    return pageStreaming.getResponseTokenField().getSimpleName();
  }

  public String getResourcesFieldName(PageStreamingConfig pageStreaming) {
    // Not using keyName since PHP maps the string "resourceField" to the raw resource field
    // from name the page streaming config.
    return pageStreaming.getResourcesFieldName();
  }
}
