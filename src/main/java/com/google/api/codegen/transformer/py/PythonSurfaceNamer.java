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

import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.py.PythonCommentReformatter;
import com.google.api.codegen.util.py.PythonNameFormatter;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.common.base.Joiner;
import java.io.File;

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
  public String getApiWrapperClassConstructorName(Interface service) {
    return getApiWrapperClassName(service.getSimpleName());
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
  public String getFullyQualifiedApiWrapperClassName(InterfaceConfig interfaceConfig) {
    return Joiner.on(".")
        .join(
            getPackageName(),
            getApiWrapperVariableName(interfaceConfig),
            getApiWrapperClassName(interfaceConfig));
  }

  @Override
  public String getFormatFunctionName(
      Interface service, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(Name.from(resourceNameConfig.getEntityName(), "path"));
  }

  @Override
  public String getSourceFilePath(String path, String publicClassName) {
    return path + File.separator + classFileNameBase(Name.upperCamel(publicClassName)) + ".py";
  }

  @Override
  public String getGrpcStubCallString(Interface service, Method method) {
    return getGrpcMethodName(method);
  }

  @Override
  public String getUnitTestClassName(InterfaceConfig interfaceConfig) {
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
