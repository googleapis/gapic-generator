/* Copyright 2017 Google Inc
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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.metacode.InitFieldConfig;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.Synchronicity;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.py.PythonCommentReformatter;
import com.google.api.codegen.util.py.PythonNameFormatter;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.tools.framework.model.EnumType;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
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

  private static final Pattern VERSION_PATTERN =
      Pattern.compile(
          "^([vV]\\d+)" // Major version eg: v1
              + "([pP]\\d+)?" // Point release eg: p2
              + "(([aA]lpha|[bB]eta)\\d*)?"); //  Release level eg: alpha3

  @Override
  public SurfaceNamer cloneWithPackageName(String packageName) {
    return new PythonSurfaceNamer(packageName);
  }

  @Override
  public String getServicePhraseName(Interface apiInterface) {
    return apiInterface.getParent().getFullName() + " " + apiInterface.getSimpleName() + " API";
  }

  @Override
  public String getApiWrapperClassConstructorName(Interface apiInterface) {
    return getApiWrapperClassName(apiInterface.getSimpleName());
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
    String namespace = getPackageName();
    return namespace.substring(0, namespace.lastIndexOf('.'));
  }

  @Override
  public String getGapicPackageName(String configPackageName) {
    List<String> parts = Arrays.asList(configPackageName.split("-"));
    if (VERSION_PATTERN.matcher(parts.get(parts.size() - 1)).matches()) {
      return Joiner.on("-").join(parts.subList(0, parts.size() - 1));
    }
    return configPackageName;
  }

  @Override
  public String getFormattedVariableName(Name identifier) {
    return localVarName(identifier);
  }

  @Override
  public String getRequestVariableName(Method method) {
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
  public String getFullyQualifiedApiWrapperClassName(GapicInterfaceConfig interfaceConfig) {
    return Joiner.on(".")
        .join(getVersionedDirectoryNamespace(), getApiWrapperClassName(interfaceConfig));
  }

  @Override
  public String getMessageTypeName(ModelTypeTable typeTable, MessageType message) {
    return publicClassName(Name.upperCamel(message.getSimpleName()));
  }

  @Override
  public String getEnumTypeName(ModelTypeTable typeTable, EnumType enumType) {
    return publicClassName(Name.upperCamel(enumType.getSimpleName()));
  }

  @Override
  public String getRequestTypeName(ModelTypeTable typeTable, TypeRef type) {
    return typeTable.getAndSaveNicknameFor(type);
  }

  @Override
  public String getLongRunningOperationTypeName(ModelTypeTable typeTable, TypeRef type) {
    return typeTable.getAndSaveNicknameFor(type);
  }

  @Override
  public String getParamTypeName(ModelTypeTable typeTable, TypeRef type) {
    if (type.isMap()) {
      TypeName mapTypeName = new TypeName("dict");
      TypeName keyTypeName =
          new TypeName(getParamTypeNameForElementType(typeTable, type.getMapKeyField().getType()));
      TypeName valueTypeName =
          new TypeName(
              getParamTypeNameForElementType(typeTable, type.getMapValueField().getType()));
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
      TypeName elementTypeName = new TypeName(getParamTypeNameForElementType(typeTable, type));
      return new TypeName(
              listTypeName.getFullName(), listTypeName.getNickname(), "%s[%i]", elementTypeName)
          .getFullName();
    }

    return getParamTypeNameForElementType(typeTable, type);
  }

  @Override
  public String getAndSavePagedResponseTypeName(
      Method method, ModelTypeTable typeTable, FieldConfig resourcesFieldConfig) {
    return typeTable.getAndSaveNicknameFor(method.getOutputType());
  }

  private String getParamTypeNameForElementType(ModelTypeTable typeTable, TypeRef type) {
    String typeName = getModelTypeFormatter().getFullNameForElementType(type);

    if (type.isMessage()) {
      return "Union[dict|:class:`" + typeName + "`]";
    }

    if (type.isEnum()) {
      String nickname = typeTable.cloneEmpty().getAndSaveNicknameFor(typeName);
      return "~." + nickname;
    }

    return typeName;
  }

  private String getResponseTypeNameForElementType(ModelTypeTable typeTable, TypeRef type) {
    if (type.isMessage()) {
      String typeName = getModelTypeFormatter().getFullNameForElementType(type);
      return ":class:`" + typeName + "`";
    }

    return getParamTypeNameForElementType(typeTable, type);
  }

  @Override
  public String getPathTemplateName(
      Interface service, SingleResourceNameConfig resourceNameConfig) {
    return "_"
        + inittedConstantName(Name.from(resourceNameConfig.getEntityName(), "path", "template"));
  }

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
  public String getGrpcClientTypeName(Interface apiInterface) {
    String fullName = getModelTypeFormatter().getFullNameFor(apiInterface) + "Stub";
    return getTypeNameConverter().getTypeName(fullName).getNickname();
  }

  @Override
  public String getClientConfigPath(Interface apiInterface) {
    return classFileNameBase(Name.upperCamel(apiInterface.getSimpleName()).join("client_config"))
        + ".json";
  }

  @Override
  public List<String> getThrowsDocLines(GapicMethodConfig methodConfig) {
    ImmutableList.Builder<String> lines = ImmutableList.builder();
    lines.add(":exc:`google.gax.errors.GaxError` if the RPC is aborted.");
    if (hasParams(methodConfig)) {
      lines.add(":exc:`ValueError` if the parameters are invalid.");
    }
    return lines.build();
  }

  private boolean hasParams(GapicMethodConfig methodConfig) {
    if (!Iterables.isEmpty(methodConfig.getRequiredFieldConfigs())) {
      return true;
    }

    int optionalParamCount = Iterables.size(methodConfig.getOptionalFieldConfigs());
    // Must have at least one parameter that is not the page token parameter.
    return optionalParamCount > (methodConfig.getPageStreaming() == null ? 0 : 1);
  }

  @Override
  public List<String> getReturnDocLines(
      GapicInterfaceContext context, GapicMethodConfig methodConfig, Synchronicity synchronicity) {
    TypeRef outputType = methodConfig.getMethod().getOutputType();
    if (ServiceMessages.s_isEmptyType(outputType)) {
      return ImmutableList.<String>of();
    }

    String returnTypeName =
        methodConfig.isLongRunningOperation()
            ? "google.gax._OperationFuture"
            : getModelTypeFormatter().getFullNameFor(outputType);
    String classInfo = ":class:`" + returnTypeName + "`";

    if (methodConfig.getMethod().getResponseStreaming()) {
      return ImmutableList.of("Iterable[" + classInfo + "].");
    }

    if (methodConfig.isPageStreaming()) {
      TypeRef resourceType = methodConfig.getPageStreaming().getResourcesField().getType();
      return ImmutableList.of(
          "A :class:`google.gax.PageIterator` instance. By default, this",
          "is an iterable of "
              + getResponseTypeNameForElementType(context.getModelTypeTable(), resourceType)
              + " instances.",
          "This object can also be configured to iterate over the pages",
          "of the response through the `CallOptions` parameter.");
    }

    return ImmutableList.of("A " + classInfo + " instance.");
  }

  @Override
  public String getSourceFilePath(String path, String publicClassName) {
    return path + File.separator + classFileNameBase(Name.upperCamel(publicClassName)) + ".py";
  }

  @Override
  public String getLroApiMethodName(Method method, VisibilityConfig visibility) {
    return getApiMethodName(method, visibility);
  }

  @Override
  public String getGrpcStubCallString(Interface apiInterface, Method method) {
    return getGrpcMethodName(method);
  }

  @Override
  public String getFieldGetFunctionName(TypeRef type, Name identifier) {
    return publicFieldName(identifier);
  }

  @Override
  public String getProtoFileName(ProtoFile file) {
    String protoFilename = file.getSimpleName();
    return protoFilename.substring(0, protoFilename.lastIndexOf('.')) + ".py";
  }

  @Override
  public String getUnitTestClassName(GapicInterfaceConfig interfaceConfig) {
    return publicClassName(
        Name.upperCamelKeepUpperAcronyms("Test", getInterfaceName(interfaceConfig), "Client"));
  }

  @Override
  public String getSmokeTestClassName(GapicInterfaceConfig interfaceConfig) {
    return publicClassName(
        Name.upperCamelKeepUpperAcronyms("Test", "System", getInterfaceName(interfaceConfig)));
  }

  @Override
  public String getTestPackageName() {
    return "tests." + getPackageName();
  }

  @Override
  public String getTestCaseName(SymbolTable symbolTable, Method method) {
    Name testCaseName = symbolTable.getNewSymbol(Name.upperCamel("Test", method.getSimpleName()));
    return publicMethodName(testCaseName);
  }

  @Override
  public String getExceptionTestCaseName(SymbolTable symbolTable, Method method) {
    Name testCaseName =
        symbolTable.getNewSymbol(Name.upperCamel("Test", method.getSimpleName(), "Exception"));
    return publicMethodName(testCaseName);
  }

  @Override
  public String injectRandomStringGeneratorCode(String randomString) {
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

  @Override
  public String quoted(String text) {
    return "'" + text + "'";
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
}
