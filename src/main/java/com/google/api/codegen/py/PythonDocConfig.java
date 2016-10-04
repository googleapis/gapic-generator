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
package com.google.api.codegen.py;

import com.google.api.codegen.DocConfig;
import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.metacode.InitCode;
import com.google.api.codegen.metacode.InitCodeLine;
import com.google.api.codegen.metacode.InputParameter;
import com.google.api.codegen.metacode.SimpleInitCodeLine;
import com.google.api.codegen.metacode.StructureInitCodeLine;
import com.google.api.codegen.py.PythonImport.ImportType;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Represents the Python documentation settings for an Api method. */
@AutoValue
abstract class PythonDocConfig extends DocConfig {
  public static PythonDocConfig.Builder newBuilder() {
    return new AutoValue_PythonDocConfig.Builder();
  }

  public abstract ApiConfig getApiConfig();

  public abstract Method getMethod();

  public abstract Interface getInterface();

  abstract PythonImportHandler getImportHandler();

  @Override
  public String getMethodName() {
    return LanguageUtil.upperCamelToLowerUnderscore(getMethod().getSimpleName());
  }

  /** Does this method return an iterable response? */
  public boolean isPageStreaming() {
    Method method = getMethod();
    MethodConfig methodConfig =
        getApiConfig().getInterfaceConfig(getInterface()).getMethodConfig(method);
    return methodConfig.isPageStreaming();
  }

  /** Is this method gRPC-response streaming? */
  public boolean isResponseGrpcStreaming() {
    return getMethod().getResponseStreaming();
  }

  /** Is this method gRPC-request streaming? */
  public boolean isRequestGrpcStreaming() {
    return getMethod().getRequestStreaming();
  }

  /** Get list of import statements for this method's code sample. */
  public List<String> getAppImports() {
    List<String> importStrings = new ArrayList<>();
    importStrings.add(apiImport(getApiName()));
    addProtoImports(importStrings);
    return importStrings;
  }

  private String apiImport(String apiName) {
    String packageName = getApiConfig().getPackageName();
    String moduleName = LanguageUtil.upperCamelToLowerUnderscore(apiName);
    return PythonImport.create(ImportType.APP, packageName, moduleName).importString();
  }

  private void addProtoImports(List<String> importStrings) {
    Set<String> protoImports = new TreeSet<>();
    for (InitCodeLine line : getInitCode().getLines()) {
      TypeRef lineType = null;
      switch (line.getLineType()) {
        case SimpleInitLine:
          lineType = ((SimpleInitCodeLine) line).getType();
          break;
        case StructureInitLine:
          lineType = ((StructureInitCodeLine) line).getType();
          break;
        default:
          // nothing to do
      }

      if (lineType != null) {
        if (lineType.isMessage()) {
          // TODO: What if a file is not imported in the main GAPIC codegen, but is needed for
          // samplegen? For example, subfields of request messages.
          protoImports.add(getImportHandler().fileToImport(lineType.getMessageType().getFile()));
        } else if (lineType.isEnum()) {
          protoImports.add(
              (PythonImport.create(
                      PythonImport.ImportType.APP, getApiConfig().getPackageName(), "enums"))
                  .importString());
        }
      }
    }
    importStrings.addAll(protoImports);
  }

  @AutoValue.Builder
  abstract static class Builder extends DocConfig.Builder<Builder> {
    abstract PythonDocConfig build();

    public abstract Builder setApiConfig(ApiConfig apiConfig);

    public abstract Builder setApiName(String serviceName);

    public abstract Builder setImportHandler(PythonImportHandler importHandler);

    public abstract Builder setMethod(Method method);

    public abstract Builder setInterface(Interface iface);

    public abstract Builder setReturnType(String returnType);

    public abstract Builder setInitCode(InitCode initCode);

    public abstract Builder setParams(ImmutableList<InputParameter> params);

    @Override
    protected Builder setInitCodeProxy(InitCode initCode) {
      return setInitCode(initCode);
    }

    @Override
    protected Builder setParamsProxy(ImmutableList<InputParameter> params) {
      return setParams(params);
    }
  }
}
