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

import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.VisibilityConfig;
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
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.File;
import java.util.List;

/** The SurfaceNamer for Python. */
public class PythonSurfaceNamer extends SurfaceNamer {
  public PythonSurfaceNamer(String packageName) {
    super(
        new PythonNameFormatter(),
        new ModelTypeFormatterImpl(new PythonModelTypeNameConverter(packageName)),
        new PythonTypeTable(packageName),
        new PythonCommentReformatter(),
        packageName);
  }

  @Override
  public String getApiWrapperClassConstructorName(Interface apiInterface) {
    return getApiWrapperClassName(apiInterface.getSimpleName());
  }

  @Override
  public String getFormattedVariableName(Name identifier) {
    return localVarName(identifier);
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
        .join(
            getPackageName(),
            getApiWrapperVariableName(interfaceConfig),
            getApiWrapperClassName(interfaceConfig));
  }

  @Override
  public String getParamTypeName(ModelTypeTable typeTable, TypeRef type) {
    if (type.isMap()) {
      TypeName mapTypeName = new TypeName("dict");
      TypeName keyTypeName =
          new TypeName(getParamTypeNameForElementType(type.getMapKeyField().getType()));
      TypeName valueTypeName =
          new TypeName(getParamTypeNameForElementType(type.getMapValueField().getType()));
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

  private String getParamTypeNameForElementType(TypeRef type) {
    String typeName = getModelTypeFormatter().getFullNameForElementType(type);

    if (type.isMessage()) {
      return ":class:`" + typeName + "`";
    }

    if (type.isEnum()) {
      return "enum :class:`" + typeName + "`";
    }

    return typeName;
  }

  @Override
  public String getFormatFunctionName(
      Interface apiInterface, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(Name.from(resourceNameConfig.getEntityName(), "path"));
  }

  @Override
  public String getCreateStubFunctionName(Interface apiInterface) {
    return getGrpcClientTypeName(apiInterface);
  }

  @Override
  public String getGrpcClientTypeName(Interface apiInterface) {
    String fullName = getModelTypeFormatter().getFullNameFor(apiInterface) + "Stub";
    return getTypeNameConverter().getTypeName(fullName).getNickname();
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
      return ImmutableList.of("iterator[" + classInfo + "].");
    }

    if (methodConfig.isPageStreaming()) {
      TypeRef resourceType = methodConfig.getPageStreaming().getResourcesField().getType();
      return ImmutableList.of(
          "A :class:`google.gax.PageIterator` instance. By default, this",
          "is an iterable of " + getParamTypeNameForElementType(resourceType) + " instances.",
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
  public String getUnitTestClassName(GapicInterfaceConfig interfaceConfig) {
    return publicClassName(
        Name.upperCamelKeepUpperAcronyms("Test", getInterfaceName(interfaceConfig), "Client"));
  }

  @Override
  public String getTestPackageName() {
    return "test." + getPackageName();
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
  public String quoted(String text) {
    return "'" + text + "'";
  }
}
