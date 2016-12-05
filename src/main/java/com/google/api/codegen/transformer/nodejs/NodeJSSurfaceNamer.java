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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.nodejs.NodeJSNameFormatter;
import com.google.api.codegen.util.nodejs.NodeJSTypeTable;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Splitter;
import java.util.List;

/** The SurfaceNamer for NodeJS. */
public class NodeJSSurfaceNamer extends SurfaceNamer {
  public NodeJSSurfaceNamer(String packageName) {
    super(
        new NodeJSNameFormatter(),
        new ModelTypeFormatterImpl(new NodeJSModelTypeNameConverter(packageName)),
        new NodeJSTypeTable(packageName),
        packageName);
  }

  /**
   * NodeJS uses a special format for ApiWrapperModuleName.
   *
   * <p>The name for the module for this vkit module. This assumes that the package_name in the API
   * config will be in the format of 'apiname.version', and extracts the 'apiname' and 'version'
   * part and combine them to lower-camelcased style (like pubsubV1).
   *
   * <p>Based on {@link com.google.api.codegen.nodejs.NodeJSGapicContext#getModuleName}.
   */
  @Override
  public String getApiWrapperModuleName() {
    List<String> names = Splitter.on(".").splitToList(getPackageName());
    if (names.size() < 2) {
      return getPackageName();
    }
    return names.get(0) + Name.from(names.get(1)).toUpperCamel();
  }

  @Override
  public String getApiWrapperClassName(Interface interfaze) {
    return publicClassName(Name.upperCamel(interfaze.getSimpleName(), "Client"));
  }

  @Override
  public String getApiWrapperClassConstructorName(Interface interfaze) {
    return publicClassName(Name.upperCamel(interfaze.getSimpleName(), "Client"));
  }

  @Override
  public String getApiWrapperVariableName(Interface interfaze) {
    return localVarName(Name.upperCamel(interfaze.getSimpleName(), "Client"));
  }

  @Override
  public String getFieldSetFunctionName(TypeRef type, Name identifier) {
    if (type.isMap() || type.isRepeated()) {
      return publicMethodName(Name.from("add").join(identifier));
    } else {
      return publicMethodName(Name.from("set").join(identifier));
    }
  }

  @Override
  public String getPathTemplateName(
      Interface service, SingleResourceNameConfig resourceNameConfig) {
    return inittedConstantName(Name.from(resourceNameConfig.getEntityName(), "name", "template"));
  }

  @Override
  public String getFormatFunctionName(
      Interface service, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(Name.from(resourceNameConfig.getEntityName(), "path"));
  }

  @Override
  public String getClientConfigPath(Interface service) {
    return "./resources/"
        + Name.upperCamel(service.getSimpleName()).join("client_config").toLowerUnderscore()
        + ".json";
  }

  @Override
  public boolean shouldImportRequestObjectParamType(Field field) {
    return field.getType().isMap();
  }

  @Override
  public String getOptionalArrayTypeName() {
    return "gax.CallOptions";
  }

  @Override
  public String getDynamicLangReturnTypeName(Method method, MethodConfig methodConfig) {
    if (new ServiceMessages().isEmptyType(method.getOutputType())) {
      return "";
    }

    return getModelTypeFormatter().getFullNameFor(method.getOutputType());
  }

  @Override
  public String getFullyQualifiedStubType(Interface service) {
    return getModelTypeFormatter().getFullNameFor(service);
  }

  @Override
  public String getGrpcClientImportName(Interface service) {
    return "grpc-" + NamePath.dotted(service.getFile().getFullName()).toDashed();
  }
}
