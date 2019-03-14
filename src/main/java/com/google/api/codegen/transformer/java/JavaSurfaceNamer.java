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
package com.google.api.codegen.transformer.java;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoInterfaceModel;
import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.config.ResourceNameConfig;
import com.google.api.codegen.config.ResourceNameType;
import com.google.api.codegen.config.TransportProtocol;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.metacode.InitFieldConfig;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.SchemaTypeFormatterImpl;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Inflector;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.StringUtil;
import com.google.api.codegen.util.java.JavaCommentReformatter;
import com.google.api.codegen.util.java.JavaNameFormatter;
import com.google.api.codegen.util.java.JavaRenderingUtil;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ServiceMethodType;
import com.google.common.base.Joiner;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** The SurfaceNamer for Java. */
public class JavaSurfaceNamer extends SurfaceNamer {

  private final JavaNameFormatter nameFormatter;

  public JavaSurfaceNamer(String rootPackageName, String packageName) {
    super(
        new JavaNameFormatter(),
        new ModelTypeFormatterImpl(new JavaModelTypeNameConverter(packageName)),
        new JavaTypeTable(packageName),
        new JavaCommentReformatter(),
        rootPackageName,
        packageName);
    nameFormatter = (JavaNameFormatter) super.getNameFormatter();
  }

  /* Create a JavaSurfaceNamer for a Discovery-based API. */
  public JavaSurfaceNamer(String rootPackageName, String packageName, JavaNameFormatter formatter) {
    super(
        formatter,
        new SchemaTypeFormatterImpl(new JavaSchemaTypeNameConverter(packageName, formatter)),
        new JavaTypeTable(packageName),
        new JavaCommentReformatter(),
        rootPackageName,
        packageName);
    nameFormatter = formatter;
  }

  @Override
  public SurfaceNamer cloneWithPackageName(String packageName) {
    return new JavaSurfaceNamer(getRootPackageName(), packageName);
  }

  @Override
  public SurfaceNamer cloneWithPackageNameForDiscovery(String packageName) {
    return new JavaSurfaceNamer(getRootPackageName(), packageName, getNameFormatter());
  }

  @Override
  public JavaNameFormatter getNameFormatter() {
    return nameFormatter;
  }

  @Override
  public String getApiSnippetsClassName(InterfaceConfig interfaceConfig) {
    return publicClassName(
        Name.upperCamel(interfaceConfig.getInterfaceModel().getSimpleName(), "ClientSnippets"));
  }

  @Override
  public String getApiSampleFileName(String className) {
    return className + ".java";
  }

  @Override
  public String getSourceFilePath(String path, String publicClassName) {
    return path + File.separator + publicClassName + ".java";
  }

  @Override
  public boolean shouldImportRequestObjectParamElementType(FieldModel field) {
    return !field.isMap();
  }

  @Override
  public String getParamDocText(String paramName, String paramTypeName, String text) {
    return String.format("@param %s %s", paramName, getCommentReformatter().reformat(text));
  }

  @Override
  public List<String> getDocLines(String text) {
    return JavaRenderingUtil.getDocLines(text);
  }

  @Override
  public List<String> getThrowsDocLines(MethodConfig methodConfig) {
    return Arrays.asList("@throws com.google.api.gax.rpc.ApiException if the remote call fails");
  }

  @Override
  public String getStaticLangReturnTypeName(MethodContext methodContext) {
    MethodModel method = methodContext.getMethodModel();
    if (method.isOutputTypeEmpty()) {
      return "void";
    }
    return method.getOutputTypeName(methodContext.getTypeTable()).getFullName();
  }

  @Override
  public String getAndSaveOperationResponseTypeName(
      MethodContext methodContext, ImportTypeTable typeTable) {
    String responseTypeName =
        typeTable.getFullNameFor(methodContext.getLongRunningConfig().getReturnType());
    String metadataTypeName =
        typeTable.getFullNameFor(methodContext.getLongRunningConfig().getMetadataType());
    return typeTable.getAndSaveNicknameForContainer(
        "com.google.api.gax.longrunning.OperationFuture", responseTypeName, metadataTypeName);
  }

  @Override
  public String getLongRunningOperationTypeName(ImportTypeTable typeTable, TypeModel type) {
    return typeTable.getAndSaveNicknameForElementType(type);
  }

  @Override
  public String getGenericAwareResponseTypeName(MethodContext methodContext) {
    MethodModel method = methodContext.getMethodModel();
    if (method.isOutputTypeEmpty()) {
      return "Void";
    } else {
      return method.getOutputTypeName(methodContext.getTypeTable()).getFullName();
    }
  }

  @Override
  public String getPagedResponseIterateMethod() {
    return publicMethodName(Name.from("iterate_all"));
  }

  @Override
  public String getResourceTypeParseMethodName(
      ImportTypeTable typeTable, FieldConfig resourceFieldConfig) {
    String concreteResourceTypeName = getConcreteResourceTypeName(typeTable, resourceFieldConfig);
    return concreteResourceTypeName + "." + publicMethodName(Name.from("parse"));
  }

  @Override
  public String getResourceTypeParseListMethodName(
      ImportTypeTable typeTable, FieldConfig resourceFieldConfig) {
    String concreteResourceTypeName = getConcreteResourceTypeName(typeTable, resourceFieldConfig);
    return concreteResourceTypeName + "." + publicMethodName(Name.from("parse_list"));
  }

  @Override
  public String getResourceTypeFormatListMethodName(
      ImportTypeTable typeTable, FieldConfig resourceFieldConfig) {
    String concreteResourceTypeName = getConcreteResourceTypeName(typeTable, resourceFieldConfig);
    return concreteResourceTypeName + "." + publicMethodName(Name.from("to_string_list"));
  }

  private String getConcreteResourceTypeName(
      ImportTypeTable typeTable, FieldConfig resourceFieldConfig) {
    String resourceTypeName = getAndSaveElementResourceTypeName(typeTable, resourceFieldConfig);
    if (resourceFieldConfig.getResourceNameType() == ResourceNameType.ANY) {
      return publicClassName(Name.from("untyped_resource_name"));
    } else {
      return resourceTypeName;
    }
  }

  /** The name of the create method for the resource one-of for the given field config */
  public String getResourceTypeParentParseMethod(
      ImportTypeTable typeTable, FieldConfig fieldConfig) {
    return getAndSaveResourceTypeFactoryName(typeTable, fieldConfig.getMessageFieldConfig())
        + "."
        + publicMethodName(Name.from("parse"));
  }

  private String getAndSaveResourceTypeFactoryName(
      ImportTypeTable typeTable, FieldConfig fieldConfig) {
    String resourceClassName =
        publicClassName(getResourceTypeNameObject(fieldConfig.getResourceNameConfig()));
    return typeTable.getAndSaveNicknameForTypedResourceName(
        fieldConfig, Inflector.pluralize(resourceClassName));
  }

  @Override
  public String getAndSavePagedResponseTypeName(
      MethodContext methodContext, FieldConfig resourceFieldConfig) {
    // TODO(michaelbausor) make sure this uses the typeTable correctly
    ImportTypeTable typeTable = methodContext.getTypeTable();
    String fullPackageWrapperName =
        typeTable.getImplicitPackageFullNameFor(
            getApiWrapperClassName(methodContext.getInterfaceConfig()));
    String pagedResponseShortName =
        getPagedResponseTypeInnerName(
            methodContext.getMethodModel(), typeTable, resourceFieldConfig.getField());
    return typeTable.getAndSaveNicknameForInnerType(fullPackageWrapperName, pagedResponseShortName);
  }

  @Override
  public String getPagedResponseTypeInnerName(
      MethodModel method, ImportTypeTable typeTable, FieldModel resourceField) {
    return publicClassName(Name.anyCamel(method.getSimpleName(), "PagedResponse"));
  }

  @Override
  public String getPageTypeInnerName(
      MethodModel method, ImportTypeTable typeTable, FieldModel resourceField) {
    return publicClassName(Name.anyCamel(method.getSimpleName(), "Page"));
  }

  @Override
  public String getFixedSizeCollectionTypeInnerName(
      MethodModel method, ImportTypeTable typeTable, FieldModel resourceField) {
    return publicClassName(Name.anyCamel(method.getSimpleName(), "FixedSizeCollection"));
  }

  @Override
  public String getFullyQualifiedApiWrapperClassName(InterfaceConfig interfaceConfig) {
    return getPackageName() + "." + getApiWrapperClassName(interfaceConfig);
  }

  @Override
  public String getFullyQualifiedRpcStubType(
      InterfaceModel interfaceModel, TransportProtocol transportProtocol) {
    return getStubPackageName() + "." + getApiRpcStubClassName(interfaceModel, transportProtocol);
  }

  /**
   * The type name of the Grpc service class This needs to match what Grpc generates for the
   * particular language.
   */
  @Override
  public String getGrpcServiceClassName(InterfaceModel apiInterface) {
    String fullName =
        JavaModelTypeNameConverter.getGrpcTypeName(
                ((ProtoInterfaceModel) apiInterface).getInterface())
            .getFullName();

    NamePath namePath = getTypeNameConverter().getNamePath(fullName);
    String grpcContainerName =
        publicClassName(Name.upperCamelKeepUpperAcronyms(namePath.getHead(), "Grpc"));
    String serviceClassName =
        publicClassName(Name.upperCamelKeepUpperAcronyms(apiInterface.getSimpleName(), "ImplBase"));
    return qualifiedName(namePath.withHead(grpcContainerName).append(serviceClassName));
  }

  /**
   * The type name of the Grpc container class. This needs to match what Grpc generates for the
   * particular language.
   */
  public String getGrpcContainerTypeName(InterfaceModel apiInterface) {
    String fullName =
        JavaModelTypeNameConverter.getGrpcTypeName(
                ((ProtoInterfaceModel) apiInterface).getInterface())
            .getFullName();

    NamePath namePath = getTypeNameConverter().getNamePath(fullName);
    String publicClassName =
        publicClassName(Name.upperCamelKeepUpperAcronyms(namePath.getHead(), "Grpc"));
    return qualifiedName(namePath.withHead(publicClassName));
  }

  @Override
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
        return formatNameFunc.apply(entityName).join("name_fixed");
      case ONEOF:
        // Remove suffix "_oneof". This allows the collection oneof config to "share" an entity name
        // with a collection config.
        entityName = StringUtil.removeSuffix(entityName, "_oneof");
        return formatNameFunc.apply(entityName).join("name");
      case SINGLE:
        return formatNameFunc.apply(entityName).join("name");
      case NONE:
      default:
        throw new UnsupportedOperationException("unexpected entity name type");
    }
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
  public String getBatchingDescriptorConstName(MethodModel method) {
    return inittedConstantName(Name.upperCamel(method.getSimpleName()).join("batching_desc"));
  }

  @Override
  /* The name of the settings member name for the given method. */
  public String getOperationSettingsMemberName(MethodModel method) {
    return publicMethodName(Name.upperCamel(method.getSimpleName(), "OperationSettings"));
  }

  @Override
  public String getAndSaveResourceTypeName(ImportTypeTable typeTable, FieldConfig fieldConfig) {
    String commonResourceName = fieldConfig.getResourceNameConfig().getCommonResourceName();

    String resourceClassName;
    if (commonResourceName == null) {
      resourceClassName =
          publicClassName(getResourceTypeNameObject(fieldConfig.getResourceNameConfig()));
    } else {
      // Common resource name is fully-qualified.
      resourceClassName = commonResourceName.substring(commonResourceName.lastIndexOf(".") + 1);
    }
    return typeTable.getAndSaveNicknameForTypedResourceName(fieldConfig, resourceClassName);
  }

  @Override
  public String getToStringMethod() {
    return "Objects.toString";
  }

  @Override
  public String getFieldAccessorName(FieldModel field) {
    return String.format(".%s()", getFieldGetFunctionName(field));
  }

  @Override
  public String getIndexAccessorName(int index) {
    return String.format(".get(%d)", index);
  }

  @Override
  public String getMapKeyAccessorName(TypeModel keyType, String key) {
    return String.format(
        ".get(%s)", getModelTypeFormatter().renderPrimitiveValue((ProtoTypeRef) keyType, key));
  }

  @Override
  public List<CallingForm> getCallingForms(MethodContext context) {
    return CallingForm.getCallingForms(context, TargetLanguage.JAVA);
  }

  @Override
  public String getAndSaveTypeName(ImportTypeTable typeTable, TypeModel type) {
    return typeTable.getAndSaveNicknameForElementType(type);
  }

  /**
   * Returns the package name of standalone samples.
   *
   * <p>Currently we assume that package names always start with "com.google.". For example, if
   * package name is "com.google.foo", the sample package name returned by this method will be
   * "com.google.foo.examples.snippets". If package name is "com.google.foo.bar", the sample package
   * name returned by this method will be "com.google.foo.examples.bar.snippets".
   *
   * <p>We structure the example package name in this way because in the case of a package named
   * "com.google.foo.bar", 'foo' is very often the organization name, and this lets us group
   * examples from the same org into a common package. E.g. "com.google.cloud.library.v1"
   */
  @Override
  public String getExamplePackageName() {
    return getExamplePackageName(getPackageName());
  }

  public static String getExamplePackageName(String packageName) {
    checkArgument(
        packageName.startsWith("com.google."),
        "We currently only support packages beginning with 'com.google'");
    packageName = packageName.replaceFirst("com.google.", "");
    checkArgument(
        !packageName.isEmpty(),
        "package name should have at least one more component than 'com.google'");
    int index = packageName.indexOf('.');
    String firstComponent = index < 0 ? packageName : packageName.substring(0, index);
    String remainingComponents = packageName.replaceFirst(firstComponent, "");
    // Ideally we want to change `.examples` to `.samples`. We call it `examples` for
    // now to be consistent with manual samples. Consider change it to `samples` to
    // be consistent across languages once the sample publish infrastructure in set.
    return "com.google." + firstComponent + ".examples" + remainingComponents;
  }
}
