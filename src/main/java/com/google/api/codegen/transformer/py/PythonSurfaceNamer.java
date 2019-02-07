/* Copyright 2017 Google LLC
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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.gapic.ServiceMessages;
import com.google.api.codegen.metacode.InitFieldConfig;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.Synchronicity;
import com.google.api.codegen.transformer.TransformationContext;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.VersionMatcher;
import com.google.api.codegen.util.py.PythonCommentReformatter;
import com.google.api.codegen.util.py.PythonDocstringUtil;
import com.google.api.codegen.util.py.PythonNameFormatter;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.tools.framework.model.EnumType;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The SurfaceNamer for Python. */
public class PythonSurfaceNamer extends SurfaceNamer {
  public PythonSurfaceNamer(String packageName) {
    super(
        new PythonNameFormatter(),
        new ModelTypeFormatterImpl(new PythonModelTypeNameConverter(packageName)),
        new PythonTypeTable(packageName),
        new PythonCommentReformatter(),
        packageName,
        packageName);
  }

  @Override
  public SurfaceNamer cloneWithPackageName(String packageName) {
    return new PythonSurfaceNamer(packageName);
  }

  @Override
  public String getServicePhraseName(InterfaceConfig interfaceConfig) {
    return interfaceConfig.getInterfaceModel().getParentFullName()
        + " "
        + interfaceConfig.getInterfaceModel().getSimpleName()
        + " API";
  }

  @Override
  public String getApiWrapperClassConstructorName(InterfaceConfig interfaceConfig) {
    return getApiWrapperClassName(interfaceConfig.getInterfaceModel().getSimpleName());
  }

  @Override
  public String getApiWrapperModuleName() {
    String namespace = getVersionedDirectoryNamespace();
    return namespace.substring(namespace.lastIndexOf('.') + 1);
  }

  @Override
  public String getTopLevelNamespace() {
    String namespace = getVersionedDirectoryNamespace();
    if (namespace.lastIndexOf('.') > -1) {
      return namespace.substring(0, namespace.lastIndexOf('.'));
    }
    return "";
  }

  @Override
  public String getVersionedDirectoryNamespace() {
    Pattern versionNamespacePattern =
        Pattern.compile(
            "(.+?)_"
                + "([vV]\\d+)" // Major version eg: v1
                + "([pP_]\\d+)?" // Point release eg: p2
                + "(([aA]lpha|[bB]eta)\\d*)?"); //  Release level eg: alpha3
    List<String> names = new ArrayList<>();
    String namespace = getPackageName();
    for (String n : namespace.split("\\.")) {
      names.add(n);
      if (versionNamespacePattern.matcher(n).matches()) {
        return Joiner.on(".").join(names);
      }
    }
    return namespace.substring(0, namespace.lastIndexOf('.'));
  }

  @Override
  public String getGapicPackageName(String configPackageName) {
    List<String> parts = Arrays.asList(configPackageName.split("-"));
    if (VersionMatcher.isVersion(parts.get(parts.size() - 1))) {
      return Joiner.on("-").join(parts.subList(0, parts.size() - 1));
    }
    return configPackageName;
  }

  @Override
  public String getFormattedVariableName(Name identifier) {
    return localVarName(identifier);
  }

  @Override
  public String getRequestVariableName(MethodModel method) {
    return method.getRequestStreaming() ? "requests" : "request";
  }

  @Override
  public String getApiWrapperClassName(InterfaceConfig interfaceConfig) {
    return getApiWrapperClassName(getInterfaceName(interfaceConfig));
  }

  private String getApiWrapperClassName(String interfaceName) {
    return publicClassName(Name.upperCamelKeepUpperAcronyms(interfaceName, "Client"));
  }

  @Override
  public String getApiSampleFileName(String className) {
    return Name.anyCamel(className).toLowerUnderscore() + ".py";
  }

  @Override
  public String getFullyQualifiedApiWrapperClassName(InterfaceConfig interfaceConfig) {
    return Joiner.on(".")
        .join(getVersionedDirectoryNamespace(), getApiWrapperClassName(interfaceConfig));
  }

  @Override
  public String getMessageTypeName(ImportTypeTable typeTable, MessageType message) {
    return publicClassName(Name.upperCamel(message.getSimpleName()));
  }

  @Override
  public String getEnumTypeName(ImportTypeTable typeTable, EnumType enumType) {
    return publicClassName(Name.upperCamel(enumType.getSimpleName()));
  }

  @Override
  public String getAndSaveTypeName(ImportTypeTable typeTable, TypeModel type) {
    return typeTable.getAndSaveNicknameFor(type);
  }

  @Override
  public String getLongRunningOperationTypeName(ImportTypeTable typeTable, TypeModel type) {
    return typeTable.getAndSaveNicknameFor(type);
  }

  @Override
  public String getParamTypeName(ImportTypeTable typeTable, TypeModel type) {
    if (type.isMap()) {
      TypeName mapTypeName = new TypeName("dict");
      TypeName keyTypeName = new TypeName(getParamTypeNameForElementType(type.getMapKeyType()));
      TypeName valueTypeName = new TypeName(getParamTypeNameForElementType(type.getMapValueType()));
      return new TypeName(
              mapTypeName.getFullName(),
              mapTypeName.getNickname(),
              "%s[%i -> %i]",
              keyTypeName,
              valueTypeName)
          .getFullName();
    }

    if (type.isRepeated()) {
      TypeName listTypeName = new TypeName("list");
      TypeName elementTypeName = new TypeName(getParamTypeNameForElementType(type));
      return new TypeName(
              listTypeName.getFullName(), listTypeName.getNickname(), "%s[%i]", elementTypeName)
          .getFullName();
    }

    return getParamTypeNameForElementType(type);
  }

  @Override
  public String getAndSavePagedResponseTypeName(
      MethodContext methodContext, FieldConfig resourcesFieldConfig) {
    return methodContext
        .getMethodModel()
        .getAndSaveResponseTypeName(methodContext.getTypeTable(), methodContext.getNamer());
  }

  private String getParamTypeNameForElementType(TypeModel type) {
    String typeName = getTypeFormatter().getFullNameForElementType(type);

    if (type.isMessage() || type.isEnum()) {
      typeName = PythonDocstringUtil.napoleonType(typeName, getVersionedDirectoryNamespace());
    }

    if (type.isMessage()) {
      return "Union[dict, " + typeName + "]";
    }

    if (type.isEnum()) {
      return typeName;
    }
    return typeName;
  }

  private String getResponseTypeNameForElementType(TypeModel type) {
    if (type.isMessage()) {
      String typeName = getTypeFormatter().getFullNameForElementType(type);
      return PythonDocstringUtil.napoleonType(typeName, getVersionedDirectoryNamespace());
    }

    return getParamTypeNameForElementType(type);
  }

  @Override
  public String getPathTemplateName(
      InterfaceConfig interfaceConfig, SingleResourceNameConfig resourceNameConfig) {
    return "_"
        + inittedConstantName(Name.from(resourceNameConfig.getEntityName(), "path", "template"));
  }

  @Override
  public String getFormatFunctionName(
      InterfaceConfig interfaceConfig, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(Name.from(resourceNameConfig.getEntityName(), "path"));
  }

  @Override
  public String getParseFunctionName(String var, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(
        Name.from("match", var, "from", resourceNameConfig.getEntityName(), "name"));
  }

  @Override
  public String getGrpcClientTypeName(InterfaceModel apiInterface) {
    String fullName = getModelTypeFormatter().getFullNameFor(apiInterface) + "Stub";
    return getTypeNameConverter().getTypeName(fullName).getFullName();
  }

  @Override
  public String getClientConfigPath(InterfaceConfig interfaceConfig) {
    return String.format("%s.%s", getPackageName(), getClientConfigName(interfaceConfig));
  }

  @Override
  public String getClientConfigName(InterfaceConfig interfaceConfig) {
    return classFileNameBase(
        Name.upperCamel(interfaceConfig.getInterfaceModel().getSimpleName()).join("client_config"));
  }

  @Override
  public List<String> getThrowsDocLines(MethodConfig methodConfig) {
    ImmutableList.Builder<String> lines = ImmutableList.builder();
    lines.add(
        "google.api_core.exceptions.GoogleAPICallError: If the request",
        "        failed for any reason.",
        "google.api_core.exceptions.RetryError: If the request failed due",
        "        to a retryable error and retry attempts failed.");
    if (hasParams(methodConfig)) {
      lines.add("ValueError: If the parameters are invalid.");
    }
    return lines.build();
  }

  private boolean hasParams(MethodConfig methodConfig) {
    if (!Iterables.isEmpty(methodConfig.getRequiredFieldConfigs())) {
      return true;
    }

    int optionalParamCount = Iterables.size(methodConfig.getOptionalFieldConfigs());
    // Must have at least one parameter that is not the page token parameter.
    return optionalParamCount > (methodConfig.getPageStreaming() == null ? 0 : 1);
  }

  @Override
  public List<String> getReturnDocLines(
      TransformationContext context, MethodContext methodContext, Synchronicity synchronicity) {
    MethodConfig methodConfig = methodContext.getMethodConfig();
    TypeRef outputType = ((GapicMethodConfig) methodConfig).getMethod().getOutputType();
    if (ServiceMessages.s_isEmptyType(outputType)) {
      return ImmutableList.<String>of();
    }

    String returnTypeName =
        methodConfig.isLongRunningOperation()
            ? "google.gax._OperationFuture"
            : getModelTypeFormatter().getFullNameFor(outputType);
    String classInfo =
        PythonDocstringUtil.napoleonType(returnTypeName, getVersionedDirectoryNamespace());

    if (((GapicMethodConfig) methodConfig).getMethod().getResponseStreaming()) {
      return ImmutableList.of("Iterable[" + classInfo + "].");
    }

    if (methodConfig.isPageStreaming()) {
      FieldModel fieldModel = methodConfig.getPageStreaming().getResourcesField();
      return ImmutableList.of(
          "A :class:`~google.gax.PageIterator` instance. By default, this",
          "is an iterable of "
              + annotateWithClass(getResponseTypeNameForElementType(fieldModel.getType()))
              + " instances.",
          "This object can also be configured to iterate over the pages",
          "of the response through the `options` parameter.");
    }

    return ImmutableList.of(String.format("A %s instance.", annotateWithClass(classInfo)));
  }

  private String annotateWithClass(String maybeClassWrappedType) {
    if (maybeClassWrappedType.startsWith(":class:")) {
      return maybeClassWrappedType;
    }
    return String.format(":class:`%s`", maybeClassWrappedType);
  }

  @Override
  public String getSourceFilePath(String path, String publicClassName) {
    return path + File.separator + classFileNameBase(Name.upperCamel(publicClassName)) + ".py";
  }

  @Override
  public String getLroApiMethodName(MethodModel method, VisibilityConfig visibility) {
    return getApiMethodName(method, visibility);
  }

  @Override
  public String getGrpcStubCallString(InterfaceModel apiInterface, MethodModel method) {
    return getGrpcMethodName(method);
  }

  @Override
  public String getFieldGetFunctionName(FieldModel type, Name identifier) {
    return publicFieldName(Name.from(type.getSimpleName()));
  }

  @Override
  public String getFieldGetFunctionName(FieldModel field) {
    return publicFieldName(Name.from(field.getSimpleName()));
  }

  @Override
  public String getFieldGetFunctionName(TypeModel type, Name identifier) {
    return publicFieldName(identifier);
  }

  @Override
  public String getProtoFileName(String fileSimpleName) {
    return fileSimpleName.substring(0, fileSimpleName.lastIndexOf('.')) + ".py";
  }

  @Override
  public String getUnitTestClassName(InterfaceConfig interfaceConfig) {
    return publicClassName(
        Name.upperCamelKeepUpperAcronyms("Test", getInterfaceName(interfaceConfig), "Client"));
  }

  @Override
  public String getSmokeTestClassName(InterfaceConfig interfaceConfig) {
    return publicClassName(
        Name.upperCamelKeepUpperAcronyms("Test", "System", getInterfaceName(interfaceConfig)));
  }

  @Override
  public String getTestPackageName() {
    return getPackageName();
  }

  @Override
  public String getTestCaseName(SymbolTable symbolTable, MethodModel method) {
    Name testCaseName = symbolTable.getNewSymbol(Name.upperCamel("Test", method.getSimpleName()));
    return publicMethodName(testCaseName);
  }

  @Override
  public String getExceptionTestCaseName(SymbolTable symbolTable, MethodModel method) {
    Name testCaseName =
        symbolTable.getNewSymbol(Name.upperCamel("Test", method.getSimpleName(), "Exception"));
    return publicMethodName(testCaseName);
  }

  @Override
  public String injectRandomStringGeneratorCode(String randomString) {
    // "randomString" -> 'randomString'.
    randomString = '\'' + CommonRenderingUtil.stripQuotes(randomString) + '\'';
    Matcher m = InitFieldConfig.RANDOM_TOKEN_PATTERN.matcher(randomString);
    StringBuffer sb = new StringBuffer();
    List<String> stringParts = new ArrayList<>();
    while (m.find()) {
      m.appendReplacement(sb, "{" + stringParts.size() + "}");
      stringParts.add("time.time()");
    }
    m.appendTail(sb);
    if (!stringParts.isEmpty()) {
      sb.append(".format(").append(Joiner.on(", ").join(stringParts)).append(")");
    }
    return sb.toString();
  }

  /**
   * Somewhat misleadingly named; in the Python case, this converts the ReleaseLevel to a Trove
   * classifier, rather than an annotation.
   */
  @Override
  public String getReleaseAnnotation(ReleaseLevel releaseLevel) {
    switch (releaseLevel) {
      case UNSET_RELEASE_LEVEL:
        // fallthrough
      case ALPHA:
        return "3 - Alpha";
      case BETA:
        return "4 - Beta";
      case GA:
        return "5 - Production/Stable";
      case DEPRECATED:
        return "7 - Inactive";
      default:
        throw new IllegalStateException("Invalid development status");
    }
  }

  @Override
  public String getGrpcTransportImportName(InterfaceConfig interfaceConfig) {
    return packageFilePathPiece(Name.anyCamel(getGrpcTransportClassName(interfaceConfig)));
  }

  @Override
  public List<String> getPrintSpecs(String spec, List<String> args) {
    // com.google.common.escape.Escaper doesn't work here. It only maps from characters to strings.
    StringBuilder sb = new StringBuilder();
    int cursor = 0;
    while (true) {
      int p = spec.indexOf('%', cursor);
      if (p < 0) {
        sb.append(spec, cursor, spec.length());
        break;
      }
      sb.append(spec, cursor, p);

      if (spec.startsWith("%%", p)) {
        sb.append('%');
      } else if (spec.startsWith("%s", p)) {
        sb.append("{}");
      } else {
        throw new IllegalArgumentException(String.format("bad format verb: %s", spec));
      }
      cursor = p + 2;
    }
    return ImmutableList.<String>builder()
        .add(sb.toString().replace("'", "\\'"))
        .addAll(args)
        .build();
  }

  @Override
  /**
   * If the argument is a protobuf enum, returns an expression that translates the enum to a
   * descriptive string, such as `enums.message_type.enum_type(var.foo.bar).name()`. Otherwise,
   * returns the argument as it is.
   */
  public String getFormattedPrintArgName(TypeModel type, String variable, List<String> accessors) {
    String arg = variable + String.join("", accessors);
    // We print the argument as it is if it's not an enum type
    if (!(type instanceof ProtoTypeRef) || !((ProtoTypeRef) type).isEnum()) {
      return arg;
    }
    // Find all the parent elements of this enum type, stopping at the top-level message or enum
    TypeRef protoType = ((ProtoTypeRef) type).getProtoType();
    ProtoElement t = protoType.getEnumType();
    List<String> names = new ArrayList<>();
    while (!(t instanceof ProtoFile)) {
      names.add(t.getSimpleName());
      t = t.getParent();
    }
    // Wrap the enum value with the helper function that translates it to a descriptive string
    names.add("enums");
    StringBuilder builder = new StringBuilder();
    for (String name : Lists.reverse(names)) {
      builder.append(name).append(".");
    }
    builder.setLength(builder.length() - 1);
    return builder.append("(").append(arg).append(").name").toString();
  }

  @Override
  public String getIndexAccessorName(int index) {
    return String.format("[%d]", index);
  }

  @Override
  public String getFieldAccessorName(FieldModel field) {
    return "." + getFieldGetFunctionName(field);
  }

  @Override
  public String getSampleResponseVarName(MethodContext context, CallingForm form) {
    return Name.anyCamel(super.getSampleResponseVarName(context, form))
        .toLowerUnderscore()
        .toString();
  }
}
