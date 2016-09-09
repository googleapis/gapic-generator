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

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.DocConfig;
import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.MethodConfig;
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

/**
 * Represents the Python documentation settings for an Api method.
 */
@AutoValue
abstract class PythonDocConfig extends DocConfig {
  public static PythonDocConfig.Builder newBuilder() {
    return new AutoValue_PythonDocConfig.Builder();
  }

  public abstract ApiConfig getApiConfig();

  public abstract Method getMethod();

  public abstract Interface getInterface();

  public abstract PythonImportHandler getImportHandler();

  @Override
  public String getMethodName() {
    return LanguageUtil.upperCamelToLowerUnderscore(getMethod().getSimpleName());
  }

  /**
   * Does this method return an iterable response?
   */
  public boolean isIterableResponse() {
    Method method = getMethod();
    MethodConfig methodConfig =
        getApiConfig().getInterfaceConfig(getInterface()).getMethodConfig(method);
    return methodConfig.isPageStreaming();
  }

  /**
   * Get list of import statements for this method's code sample.
   */
  public List<String> getAppImports() {
    List<String> importStrings = new ArrayList<>();
    importStrings.add(apiImport(getApiName()));
    addProtoImports(importStrings);
    return importStrings;
  }

  private String apiImport(String apiName) {
    String packageName = getApiConfig().getPackageName();
    String moduleName = packageName + "." + LanguageUtil.lowerCamelToLowerUnderscore(apiName);
    return PythonImport.create(ImportType.APP, moduleName, apiName).importString();
  }

  private List<String> addProtoImports(List<String> importStrings) {
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

      if (lineType != null && lineType.isMessage()) {
        importStrings.addAll(getImportHandler().calculateImports());
        break;
      }
    }
    return importStrings;
  }

  @AutoValue.Builder
  abstract static class Builder extends DocConfig.Builder<Builder> {
    abstract PythonDocConfig autoBuild();

    abstract Method getMethod();

    abstract Builder setImportHandler(PythonImportHandler importHandler);

    public PythonDocConfig build() {
      setImportHandler(new PythonImportHandler(getMethod()));
      return autoBuild();
    }

    public abstract Builder setApiConfig(ApiConfig apiConfig);

    public abstract Builder setApiName(String serviceName);

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
