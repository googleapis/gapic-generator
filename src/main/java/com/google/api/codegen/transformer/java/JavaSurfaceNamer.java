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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.ResourceNameType;
import com.google.api.codegen.metacode.InitFieldConfig;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.java.JavaCommentReformatter;
import com.google.api.codegen.util.java.JavaNameFormatter;
import com.google.api.codegen.util.java.JavaRenderingUtil;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ServiceMethodType;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** The SurfaceNamer for Java. */
public class JavaSurfaceNamer extends SurfaceNamer {

  private final Pattern versionPattern = Pattern.compile("^v\\d+");

  public JavaSurfaceNamer(String rootPackageName, String packageName) {
    super(
        new JavaNameFormatter(),
        new ModelTypeFormatterImpl(new JavaModelTypeNameConverter(packageName)),
        new JavaTypeTable(packageName),
        new JavaCommentReformatter(),
        rootPackageName,
        packageName);
  }

  @Override
  public SurfaceNamer cloneWithPackageName(String packageName) {
    return new JavaSurfaceNamer(getRootPackageName(), packageName);
  }

  @Override
  public String getApiSnippetsClassName(Interface apiInterface) {
    return publicClassName(Name.upperCamel(apiInterface.getSimpleName(), "ClientSnippets"));
  }

  @Override
  public String getSourceFilePath(String path, String publicClassName) {
    return path + File.separator + publicClassName + ".java";
  }

  @Override
  public boolean shouldImportRequestObjectParamElementType(Field field) {
    if (Field.IS_MAP.apply(field)) {
      return false;
    } else {
      return true;
    }
  }

  @Override
  public List<String> getDocLines(String text) {
    return JavaRenderingUtil.getDocLines(text);
  }

  @Override
  public List<String> getThrowsDocLines(GapicMethodConfig methodConfig) {
    return Arrays.asList("@throws com.google.api.gax.rpc.ApiException if the remote call fails");
  }

  @Override
  public String getStaticLangReturnTypeName(Method method, GapicMethodConfig methodConfig) {
    if (ServiceMessages.s_isEmptyType(method.getOutputType())) {
      return "void";
    }
    return getModelTypeFormatter().getFullNameFor(method.getOutputType());
  }

  @Override
  public String getAndSaveOperationResponseTypeName(
      Method method, ModelTypeTable typeTable, GapicMethodConfig methodConfig) {
    String responseTypeName =
        typeTable.getFullNameFor(methodConfig.getLongRunningConfig().getReturnType());
    String metadataTypeName =
        typeTable.getFullNameFor(methodConfig.getLongRunningConfig().getMetadataType());
    return typeTable.getAndSaveNicknameForContainer(
        "com.google.api.gax.grpc.OperationFuture", responseTypeName, metadataTypeName);
  }

  @Override
  public String getLongRunningOperationTypeName(ModelTypeTable typeTable, TypeRef type) {
    return typeTable.getAndSaveNicknameForElementType(type);
  }

  @Override
  public String getGenericAwareResponseTypeName(TypeRef outputType) {
    if (ServiceMessages.s_isEmptyType(outputType)) {
      return "Void";
    } else {
      return getModelTypeFormatter().getFullNameFor(outputType);
    }
  }

  @Override
  public String getPagedResponseIterateMethod() {
    return publicMethodName(Name.from("iterate_all"));
  }

  @Override
  public String getResourceTypeParseMethodName(
      ModelTypeTable typeTable, FieldConfig resourceFieldConfig) {
    String resourceTypeName = getAndSaveElementResourceTypeName(typeTable, resourceFieldConfig);
    String concreteResourceTypeName;
    if (resourceFieldConfig.getResourceNameType() == ResourceNameType.ANY) {
      concreteResourceTypeName = publicClassName(Name.from("untyped_resource_name"));
    } else {
      concreteResourceTypeName = resourceTypeName;
    }
    return concreteResourceTypeName + "." + publicMethodName(Name.from("parse"));
  }

  @Override
  public String getAndSavePagedResponseTypeName(
      Method method, ModelTypeTable typeTable, FieldConfig resourceFieldConfig) {
    // TODO(michaelbausor) make sure this uses the typeTable correctly

    String fullPackageWrapperName =
        typeTable.getImplicitPackageFullNameFor(getPagedResponseWrappersClassName());
    String pagedResponseShortName =
        getPagedResponseTypeInnerName(method, typeTable, resourceFieldConfig.getField());
    return typeTable.getAndSaveNicknameForInnerType(fullPackageWrapperName, pagedResponseShortName);
  }

  @Override
  public String getPagedResponseTypeInnerName(
      Method method, ModelTypeTable typeTable, Field resourceField) {
    return publicClassName(Name.upperCamel(method.getSimpleName(), "PagedResponse"));
  }

  @Override
  public String getPageTypeInnerName(Method method, ModelTypeTable typeTable, Field resourceField) {
    return publicClassName(Name.upperCamel(method.getSimpleName(), "Page"));
  }

  @Override
  public String getFixedSizeCollectionTypeInnerName(
      Method method, ModelTypeTable typeTable, Field resourceField) {
    return publicClassName(Name.upperCamel(method.getSimpleName(), "FixedSizeCollection"));
  }

  @Override
  public String getFullyQualifiedApiWrapperClassName(GapicInterfaceConfig interfaceConfig) {
    return getPackageName() + "." + getApiWrapperClassName(interfaceConfig);
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
          stringParts.add("System.currentTimeMillis()");
        } else {
          stringParts.add("\"" + token + "\"");
        }
      }
    }
    return Joiner.on(" + ").join(stringParts);
  }

  @Override
  public String getApiCallableTypeName(ServiceMethodType serviceMethodType) {
    switch (serviceMethodType) {
      case UnaryMethod:
        return "UnaryCallable";
      case GrpcBidiStreamingMethod:
        return "BidiStreamingCallable";
      case GrpcServerStreamingMethod:
        return "ServerStreamingCallable";
      case GrpcClientStreamingMethod:
        return "ClientStreamingCallable";
      case LongRunningMethod:
        return "OperationCallable";
      default:
        return getNotImplementedString("getApiCallableTypeName() for " + serviceMethodType);
    }
  }

  @Override
  public String getDirectCallableTypeName(ServiceMethodType serviceMethodType) {
    switch (serviceMethodType) {
      case UnaryMethod:
        return "UnaryCallable";
      case GrpcBidiStreamingMethod:
        return "BidiStreamingCallable";
      case GrpcServerStreamingMethod:
        return "ServerStreamingCallable";
      case GrpcClientStreamingMethod:
        return "ClientStreamingCallable";
      default:
        return getNotImplementedString("getDirectCallableTypeName() for " + serviceMethodType);
    }
  }

  @Override
  public String getCreateCallableFunctionName(ServiceMethodType serviceMethodType) {
    switch (serviceMethodType) {
      case UnaryMethod:
        return "createDirectCallable";
      case GrpcBidiStreamingMethod:
        return "createDirectBidiStreamingCallable";
      case GrpcServerStreamingMethod:
        return "createDirectServerStreamingCallable";
      case GrpcClientStreamingMethod:
        return "createDirectClientStreamingCallable";
      default:
        return getNotImplementedString("getDirectCallableTypeName() for " + serviceMethodType);
    }
  }

  @Override
  public String getReleaseAnnotation(ReleaseLevel releaseLevel) {
    switch (releaseLevel) {
      case UNSET_RELEASE_LEVEL:
      case ALPHA:
        return "@BetaApi";
      case BETA:
        return "@BetaApi";
      case DEPRECATED:
        return "@Deprecated";
      default:
        return "";
    }
  }

  @Override
  public String getBatchingDescriptorConstName(Method method) {
    return inittedConstantName(Name.upperCamel(method.getSimpleName()).join("batching_desc"));
  }

  @Override
  public String getPackagePath() {
    List<String> packagePath = Splitter.on(".").splitToList(getPackageName());
    int endIndex = packagePath.size();
    // strip off the last leg of the path if it is a version
    if (versionPattern.matcher(packagePath.get(packagePath.size() - 1)).find()) {
      endIndex--;
    }
    return Joiner.on("/").join(packagePath.subList(0, endIndex));
  }

  @Override
  public String getToStringMethod() {
    return "Objects.toString";
  }
}
