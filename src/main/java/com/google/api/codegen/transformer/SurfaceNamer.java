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
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NameFormatter;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.ViewNamer;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;

import java.util.ArrayList;
import java.util.List;

/**
 * A SurfaceNamer provides language-specific names or other strings.
 */
public class SurfaceNamer extends ViewNamer {
  public SurfaceNamer(NameFormatter languageNamer) {
    super(languageNamer);
  }

  public static final String NOT_IMPLEMENTED = "$ NOT IMPLEMENTED $";

  public String getApiWrapperClassName(Interface interfaze) {
    return className(Name.upperCamel(interfaze.getSimpleName(), "Api"));
  }

  public String getApiWrapperVariableName(Interface interfaze) {
    return varName(Name.upperCamel(interfaze.getSimpleName(), "Api"));
  }

  public String getApiSettingsClassName(Interface interfaze) {
    return className(Name.upperCamel(interfaze.getSimpleName(), "Settings"));
  }

  public String getVariableName(Name identifier, InitValueConfig initValueConfig) {
    if (initValueConfig == null || !initValueConfig.hasFormattingConfig()) {
      return varName(identifier);
    } else {
      return varName(Name.from("formatted").join(identifier));
    }
  }

  public String getSetFunctionCallName(TypeRef type, Name identifier) {
    if (type.isMap()) {
      return methodName(Name.from("put", "all").join(identifier));
    } else if (type.isRepeated()) {
      return methodName(Name.from("add", "all").join(identifier));
    } else {
      return methodName(Name.from("set").join(identifier));
    }
  }

  public String getPathTemplateName(CollectionConfig collectionConfig) {
    return inittedConstantName(Name.from(collectionConfig.getEntityName(), "path", "template"));
  }

  public String getPathTemplateNameGetter(CollectionConfig collectionConfig) {
    return methodName(Name.from("get", collectionConfig.getEntityName(), "name", "template"));
  }

  public String getFormatFunctionName(CollectionConfig collectionConfig) {
    return staticFunctionName(Name.from("format", collectionConfig.getEntityName(), "name"));
  }

  public String getParseFunctionName(String var, CollectionConfig collectionConfig) {
    return staticFunctionName(
        Name.from("parse", var, "from", collectionConfig.getEntityName(), "name"));
  }

  public String getEntityName(CollectionConfig collectionConfig) {
    return varName(Name.from(collectionConfig.getEntityName()));
  }

  public String getEntityNameParamName(CollectionConfig collectionConfig) {
    return varName(Name.from(collectionConfig.getEntityName(), "name"));
  }

  public String getParamName(String var) {
    return varName(Name.from(var));
  }

  public String getPageStreamingDescriptorName(Method method) {
    return varName(Name.upperCamel(method.getSimpleName(), "PageStreamingDescriptor"));
  }

  public void addPageStreamingDescriptorImports(ModelTypeTable typeTable) {
    // do nothing
  }

  public String getMethodKey(Method method) {
    return keyName(Name.upperCamel(method.getSimpleName()));
  }

  public String getClientConfigPath(Interface service) {
    return SurfaceNamer.NOT_IMPLEMENTED;
  }

  public String getGrpcClientTypeName(Interface service) {
    NamePath namePath = NamePath.dotted(service.getFullName());
    String className = className(Name.upperCamel(namePath.getHead(), "Client"));
    return qualifiedName(namePath.withHead(className));
  }

  public String getApiMethodName(Method method) {
    return methodName(Name.upperCamel(method.getSimpleName()));
  }

  public String getVariableName(Field field) {
    return varName(Name.from(field.getSimpleName()));
  }

  public boolean shouldImportRequestObjectParamType(Field field) {
    return true;
  }

  public List<String> getDocLines(String text) {
    return CommonRenderingUtil.getDocLines(text);
  }

  public List<String> getDocLines(ProtoElement element) {
    return getDocLines(DocumentationUtil.getDescription(element));
  }

  public List<String> getThrowsDocLines() {
    return new ArrayList<>();
  }

  public String getPublicAccessModifier() {
    return "public";
  }

  public String getPrivateAccessModifier() {
    return "private";
  }

  public String getGrpcMethodName(Method method) {
    // This might seem silly, but it makes clear what we're dealing with (upper camel).
    // This is language-independent because of gRPC conventions.
    return Name.upperCamel(method.getSimpleName()).toUpperCamel();
  }

  public String getRetrySettingsClassName() {
    return SurfaceNamer.NOT_IMPLEMENTED;
  }

  public String getOptionalArrayTypeName() {
    return SurfaceNamer.NOT_IMPLEMENTED;
  }

  public String getDynamicReturnTypeName(
      ModelTypeTable typeTable, Method method, MethodConfig methodConfig) {
    return SurfaceNamer.NOT_IMPLEMENTED;
  }

  public String getStaticReturnTypeName(
      ModelTypeTable typeTable, Method method, MethodConfig methodConfig) {
    return SurfaceNamer.NOT_IMPLEMENTED;
  }

  public String getPagedCallableMethodName(Method method) {
    return methodName(Name.upperCamel(method.getSimpleName(), "PagedCallable"));
  }

  public String getPagedCallableName(Method method) {
    return varName(Name.upperCamel(method.getSimpleName(), "PagedCallable"));
  }

  public String getCallableMethodName(Method method) {
    return methodName(Name.upperCamel(method.getSimpleName(), "Callable"));
  }

  public String getCallableName(Method method) {
    return varName(Name.upperCamel(method.getSimpleName(), "Callable"));
  }

  public String getSettingsFunctionName(Method method) {
    return methodName(Name.upperCamel(method.getSimpleName(), "Settings"));
  }

  public String getGenericAwareResponseType(ModelTypeTable typeTable, TypeRef outputType) {
    return SurfaceNamer.NOT_IMPLEMENTED;
  }

  public String getGetResourceListCallName(Field resourcesField) {
    return methodName(Name.from("get", resourcesField.getSimpleName(), "list"));
  }

  public String getAndSavePagedResponseTypeName(ModelTypeTable typeTable, TypeRef resourceType) {
    return SurfaceNamer.NOT_IMPLEMENTED;
  }
}
