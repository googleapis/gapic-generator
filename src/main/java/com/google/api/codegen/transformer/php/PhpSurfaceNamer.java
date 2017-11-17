/* Copyright 2016 Google LLC
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

import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.metacode.InitFieldConfig;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.php.PhpCommentReformatter;
import com.google.api.codegen.util.php.PhpNameFormatter;
import com.google.api.codegen.util.php.PhpPackageUtil;
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Joiner;
import java.io.File;
import java.util.ArrayList;

/** The SurfaceNamer for PHP. */
public class PhpSurfaceNamer extends SurfaceNamer {

  public PhpSurfaceNamer(String packageName) {
    super(
        new PhpNameFormatter(),
        new ModelTypeFormatterImpl(new PhpModelTypeNameConverter(packageName)),
        new PhpTypeTable(packageName),
        new PhpCommentReformatter(),
        packageName,
        packageName);
  }

  public SurfaceNamer cloneWithPackageName(String packageName) {
    return new PhpSurfaceNamer(packageName);
  }

  @Override
  public String getLroApiMethodName(MethodModel method, VisibilityConfig visibility) {
    return getApiMethodName(method, visibility);
  }

  @Override
  public String getFieldSetFunctionName(TypeModel type, Name identifier) {
    return publicMethodName(Name.from("set").join(identifier));
  }

  @Override
  public String getFieldSetFunctionName(FieldModel field) {
    return publicMethodName(Name.from("set").join(field.getSimpleName()));
  }

  @Override
  public String getFieldAddFunctionName(TypeModel type, Name identifier) {
    return publicMethodName(Name.from("add").join(identifier));
  }

  @Override
  public String getFieldAddFunctionName(FieldModel field) {
    return publicMethodName(Name.from("add").join(field.getSimpleName()));
  }

  @Override
  public String getFieldGetFunctionName(FieldModel field) {
    return publicMethodName(Name.from("get").join(field.getSimpleName()));
  }

  @Override
  public String getFieldGetFunctionName(FieldModel type, Name identifier) {
    return publicMethodName(Name.from("get").join(identifier));
  }

  /** The function name to format the entity for the given collection. */
  @Override
  public String getFormatFunctionName(
      InterfaceModel apiInterface, SingleResourceNameConfig resourceNameConfig) {
    return publicMethodName(Name.from(resourceNameConfig.getEntityName(), "name"));
  }

  @Override
  public String getPathTemplateName(
      InterfaceModel apiInterface, SingleResourceNameConfig resourceNameConfig) {
    return inittedConstantName(Name.from(resourceNameConfig.getEntityName(), "name", "template"));
  }

  @Override
  public String getClientConfigPath(InterfaceModel apiInterface) {
    return "../resources/"
        + Name.upperCamel(apiInterface.getSimpleName()).join("client_config").toLowerUnderscore()
        + ".json";
  }

  @Override
  public boolean shouldImportRequestObjectParamType(FieldModel field) {
    return field.isMap();
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
  public String getDynamicLangReturnTypeName(MethodContext methodContext) {
    MethodModel method = methodContext.getMethodModel();
    MethodConfig methodConfig = methodContext.getMethodConfig();
    if (method.isOutputTypeEmpty()) {
      return "";
    }
    if (methodConfig.isPageStreaming()) {
      return "\\Google\\GAX\\PagedListResponse";
    }
    if (methodConfig.isLongRunningOperation()) {
      return "\\Google\\GAX\\OperationResponse";
    }
    switch (methodConfig.getGrpcStreamingType()) {
      case NonStreaming:
        return method.getOutputTypeName(methodContext.getTypeTable()).getFullName();
      case BidiStreaming:
        return "\\Google\\GAX\\BidiStream";
      case ClientStreaming:
        return "\\Google\\GAX\\ClientStream";
      case ServerStreaming:
        return "\\Google\\GAX\\ServerStream";
      default:
        return getNotImplementedString(
            "SurfaceNamer.getDynamicReturnTypeName grpcStreamingType:"
                + methodConfig.getGrpcStreamingType().toString());
    }
  }

  @Override
  public String getFullyQualifiedApiWrapperClassName(InterfaceConfig interfaceConfig) {
    return getPackageName() + "\\" + getApiWrapperClassName(interfaceConfig);
  }

  @Override
  public String getApiWrapperClassImplName(InterfaceConfig interfaceConfig) {
    return publicClassName(Name.upperCamel(getInterfaceName(interfaceConfig), "GapicClient"));
  }

  @Override
  public String getGrpcClientTypeName(InterfaceModel apiInterface) {
    return qualifiedName(getGrpcClientTypeName(apiInterface, "GrpcClient"));
  }

  private NamePath getGrpcClientTypeName(InterfaceModel apiInterface, String suffix) {
    NamePath namePath =
        getTypeNameConverter().getNamePath(getModelTypeFormatter().getFullNameFor(apiInterface));
    String publicClassName =
        publicClassName(Name.upperCamelKeepUpperAcronyms(namePath.getHead(), suffix));
    return namePath.withHead(publicClassName);
  }

  @Override
  public String getLongRunningOperationTypeName(ImportTypeTable typeTable, TypeModel type) {
    return ((ModelTypeTable) typeTable).getAndSaveNicknameFor(type);
  }

  @Override
  public String getRequestTypeName(ImportTypeTable typeTable, TypeRef type) {
    return ((ModelTypeTable) typeTable).getAndSaveNicknameFor(type);
  }

  @Override
  public String getGrpcStubCallString(InterfaceModel apiInterface, MethodModel method) {
    return '/' + apiInterface.getFullName() + '/' + getGrpcMethodName(method);
  }

  @Override
  public String getGapicImplNamespace() {
    return PhpPackageUtil.buildPackageName(getPackageName(), "Gapic");
  }

  @Override
  public String getTestPackageName(TestKind testKind) {
    return getTestPackageName(getPackageName(), testKind);
  }

  /** Insert "Tests" into the package name after "Google\Cloud" standard prefix */
  private static String getTestPackageName(String packageName, TestKind testKind) {
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
    switch (testKind) {
      case UNIT:
        packageComponents.add("Unit");
        break;
      case SYSTEM:
        packageComponents.add("System");
        break;
    }
    for (int i = packageStartIndex; i < packageSplit.length; i++) {
      packageComponents.add(packageSplit[i]);
    }
    return PhpPackageUtil.buildPackageName(packageComponents);
  }

  @Override
  public boolean methodHasRetrySettings(MethodConfig methodConfig) {
    return !methodConfig.isGrpcStreaming();
  }

  @Override
  public boolean methodHasTimeoutSettings(MethodConfig methodConfig) {
    return methodConfig.isGrpcStreaming();
  }

  @Override
  public String getSourceFilePath(String path, String className) {
    return path + File.separator + className + ".php";
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
          stringParts.add("time()");
        } else {
          stringParts.add('\'' + token + '\'');
        }
      }
    }
    return Joiner.on(". ").join(stringParts);
  }
}
