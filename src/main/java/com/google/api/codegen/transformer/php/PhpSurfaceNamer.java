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

import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.php.PhpNameFormatter;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;

/**
 * The SurfaceNamer for PHP.
 */
public class PhpSurfaceNamer extends SurfaceNamer {
  public PhpSurfaceNamer() {
    super(new PhpNameFormatter(), new ModelTypeFormatterImpl(new PhpModelTypeNameConverter()));
  }

  @Override
  public String getSetFunctionCallName(TypeRef type, Name identifier) {
    if (type.isMap() || type.isRepeated()) {
      return methodName(Name.from("add").join(identifier));
    } else {
      return methodName(Name.from("set").join(identifier));
    }
  }

  @Override
  public String getPathTemplateName(CollectionConfig collectionConfig) {
    return inittedConstantName(Name.from(collectionConfig.getEntityName(), "name", "template"));
  }

  @Override
  public void addPageStreamingDescriptorImports(ModelTypeTable typeTable) {
    typeTable.saveNicknameFor("Google\\GAX\\PageStreamingDescriptor");
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
  public String getRetrySettingsTypeName() {
    return "Google\\GAX\\RetrySettings";
  }

  @Override
  public String getOptionalArrayTypeName() {
    return "array";
  }

  @Override
  public String getDynamicReturnTypeName(Method method, MethodConfig methodConfig) {
    if (new ServiceMessages().isEmptyType(method.getOutputType())) {
      return "";
    }
    if (methodConfig.isPageStreaming()) {
      return "Google\\GAX\\PageAccessor";
    }
    return getModelTypeFormatter().getFullNameFor(method.getOutputType());
  }
}
