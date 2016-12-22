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
package com.google.api.codegen.transformer.php;

import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.php.PhpNameFormatter;
import com.google.api.codegen.util.php.PhpRenderingUtil;
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import java.util.List;

/** The SurfaceNamer for PHP. */
public class PhpSurfaceNamer extends SurfaceNamer {
  public PhpSurfaceNamer(String packageName) {
    super(
        new PhpNameFormatter(),
        new ModelTypeFormatterImpl(new PhpModelTypeNameConverter(packageName)),
        new PhpTypeTable(packageName),
        packageName);
  }

  @Override
  public String getFieldSetFunctionName(TypeRef type, Name identifier) {
    return publicMethodName(Name.from("set").join(identifier));
  }

  @Override
  public String getFieldAddFunctionName(TypeRef type, Name identifier) {
    return publicMethodName(Name.from("add").join(identifier));
  }

  @Override
  public String getPathTemplateName(
      Interface service, SingleResourceNameConfig resourceNameConfig) {
    return inittedConstantName(Name.from(resourceNameConfig.getEntityName(), "name", "template"));
  }

  @Override
  public String getClientConfigPath(Interface service) {
    return "resources/"
        + Name.upperCamel(service.getSimpleName()).join("client_config").toLowerUnderscore()
        + ".json";
  }

  @Override
  public boolean shouldImportRequestObjectParamType(Field field) {
    return field.getType().isMap();
  }

  @Override
  public List<String> getDocLines(String text) {
    return PhpRenderingUtil.getDocLines(text);
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
  public String getDynamicLangReturnTypeName(Method method, MethodConfig methodConfig) {
    if (new ServiceMessages().isEmptyType(method.getOutputType())) {
      return "";
    }
    if (methodConfig.isPageStreaming()) {
      return "\\Google\\GAX\\PagedListResponse";
    }
    return getModelTypeFormatter().getFullNameFor(method.getOutputType());
  }

  @Override
  public String getFullyQualifiedApiWrapperClassName(Interface service) {
    return getPackageName() + "\\" + getApiWrapperClassName(service);
  }

  @Override
  public String getGrpcClientTypeName(Interface service) {
    return qualifiedName(getGrpcClientTypeName(service, "GrpcClient"));
  }

  private NamePath getGrpcClientTypeName(Interface service, String suffix) {
    NamePath namePath =
        getTypeNameConverter().getNamePath(getModelTypeFormatter().getFullNameFor(service));
    String publicClassName =
        publicClassName(Name.upperCamelKeepUpperAcronyms(namePath.getHead(), suffix));
    return namePath.withHead(publicClassName);
  }

  @Override
  public String getFullyQualifiedStubType(Interface service) {
    return getGrpcClientTypeName(service);
  }
}
