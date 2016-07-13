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
package com.google.api.codegen.transformer;

import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.php.PhpDocUtil;
import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

public class PhpSurfaceNamer implements SurfaceNamer {

  @Override
  public String getApiWrapperClassName(Interface interfaze) {
    return interfaze.getSimpleName() + "Api";
  }

  @Override
  public String getApiWrapperVariableName(Interface interfaze) {
    return Name.upperCamel(interfaze.getSimpleName(), "Api").toLowerCamel();
  }

  @Override
  public String getVariableName(String identifier, InitValueConfig initValueConfig) {
    throw new NotImplementedException("PhpIdentifierNamer.getVariableName");
  }

  @Override
  public String getSetFunctionCallName(TypeRef type, String identifier) {
    if (type.isMap() || type.isRepeated()) {
      return Name.from("add", identifier).toLowerCamel();
    } else {
      return Name.from("set", identifier).toLowerCamel();
    }
  }

  @Override
  public String getPathTemplateName(CollectionConfig collectionConfig) {
    return Name.from(collectionConfig.getEntityName(), "name", "template").toLowerCamel();
  }

  @Override
  public String getPathTemplateNameGetter(CollectionConfig collectionConfig) {
    return Name.from("get", collectionConfig.getEntityName(), "name", "template").toLowerCamel();
  }

  @Override
  public String getFormatFunctionName(CollectionConfig collectionConfig) {
    return Name.from("format", collectionConfig.getEntityName(), "name").toLowerCamel();
  }

  @Override
  public String getParseFunctionName(String var, CollectionConfig collectionConfig) {
    return Name.from("parse", var, "from", collectionConfig.getEntityName(), "name").toLowerCamel();
  }

  @Override
  public String getEntityName(CollectionConfig collectionConfig) {
    return Name.from(collectionConfig.getEntityName()).toLowerCamel();
  }

  @Override
  public String getEntityNameParamName(CollectionConfig collectionConfig) {
    return Name.from(collectionConfig.getEntityName(), "name").toLowerCamel();
  }

  @Override
  public String getParamName(String var) {
    return Name.from(var).toLowerCamel();
  }

  @Override
  public String getPageStreamingDescriptorName(Method method) {
    return Name.upperCamel(method.getSimpleName(), "PageStreamingDescriptor").toLowerCamel();
  }

  @Override
  public void addPageStreamingDescriptorImports(ModelTypeTable typeTable) {
    typeTable.saveNicknameFor("Google\\GAX\\PageStreamingDescriptor");
  }

  @Override
  public String getMethodKey(Method method) {
    return Name.upperCamel(method.getSimpleName()).toLowerCamel();
  }

  @Override
  public String getClientConfigPath(Interface service) {
    return "./resources/"
        + Name.upperCamel(service.getSimpleName()).join("client_config").toLowerUnderscore()
        + ".json";
  }

  @Override
  public String getGrpcClientTypeName(Interface service) {
    return service.getFullName().replaceAll("\\.", "\\\\") + "Client";
  }

  @Override
  public String getApiMethodName(Method method) {
    return Name.upperCamel(method.getSimpleName()).toLowerCamel();
  }

  @Override
  public String getVariableName(Field field) {
    return Name.from(field.getSimpleName()).toLowerCamel();
  }

  @Override
  public boolean shouldImportRequestObjectParamType(Field field) {
    return field.getType().isMap();
  }

  @Override
  public String getVariableName(Name name) {
    return name.toLowerCamel();
  }

  @Override
  public List<String> getDocLines(ProtoElement element) {
    return PhpDocUtil.getPhpDocLines(DocumentationUtil.getDescription(element));
  }

  @Override
  public List<String> getThrowsDocLines() {
    throw new NotImplementedException("getThrowsDocLines");
  }

  @Override
  public String getPublicAccessModifier() {
    return "public";
  }

  @Override
  public String getPrivateAccessModifier() {
    return "private";
  }

  @Override
  public String getGrpcMethodName(Method method) {
    return method.getSimpleName();
  }

  @Override
  public String getRetrySettingsClassName() {
    return "Google\\GAX\\RetrySettings";
  }

  @Override
  public String getOptionalArrayTypeName() {
    return "array";
  }

  @Override
  public String getDynamicReturnTypeName(
      ModelTypeTable typeTable, Method method, MethodConfig methodConfig) {
    if (new ServiceMessages().isEmptyType(method.getOutputType())) {
      return "";
    }
    if (methodConfig.isPageStreaming()) {
      return "Google\\GAX\\PageAccessor";
    }
    return typeTable.getFullNameFor(method.getOutputType());
  }

  @Override
  public String getPagedCallableMethodName(Method method) {
    return SurfaceNamer.NOT_IMPLEMENTED;
  }

  @Override
  public String getPagedCallableName(Method method) {
    return SurfaceNamer.NOT_IMPLEMENTED;
  }

  @Override
  public String getCallableMethodName(Method method) {
    return SurfaceNamer.NOT_IMPLEMENTED;
  }

  @Override
  public String getCallableName(Method method) {
    return SurfaceNamer.NOT_IMPLEMENTED;
  }
}
