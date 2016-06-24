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
package com.google.api.codegen.proto3;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.PageStreamingConfig;
import com.google.api.codegen.java.surface.JavaApiCallable;
import com.google.api.codegen.java.surface.JavaFormatResourceFunction;
import com.google.api.codegen.java.surface.JavaPagedApiCallable;
import com.google.api.codegen.java.surface.JavaPathTemplate;
import com.google.api.codegen.java.surface.JavaResourceParam;
import com.google.api.codegen.java.surface.JavaSimpleApiCallable;
import com.google.api.codegen.java.surface.JavaSurface;
import com.google.api.codegen.java.surface.JavaXApi;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;

import java.util.ArrayList;
import java.util.List;

public class Proto3ToJavaSurfaceTransformer {
  private Interface service;
  private ApiConfig apiConfig;
  private Proto3JavaTypeTable typeTable = new Proto3JavaTypeTable();

  public static JavaSurface defaultTransform(Interface service, ApiConfig apiConfig) {
    return new Proto3ToJavaSurfaceTransformer(service, apiConfig).transform();
  }

  public Proto3ToJavaSurfaceTransformer(Interface service, ApiConfig apiConfig) {
    this.service = service;
    this.apiConfig = apiConfig;
  }

  public JavaSurface transform() {
    JavaSurface surface = new JavaSurface();

    JavaXApi xapiClass = new JavaXApi();
    xapiClass.name = getApiWrapperClassName();
    xapiClass.settingsClassName = getSettingsClassName();
    xapiClass.apiCallableMembers = generateApiCallables();
    xapiClass.pathTemplates = generatePathTemplates();
    xapiClass.formatResourceFunctions = generateFormatResourceFunctions();

    surface.xapiClass = xapiClass;

    return surface;
  }

  public List<JavaApiCallable> generateApiCallables() {
    List<JavaApiCallable> callableMembers = new ArrayList<>();

    for (Method method : service.getMethods()) {
      MethodConfig methodConfig = apiConfig.getInterfaceConfig(service).getMethodConfig(method);
      callableMembers.addAll(generateApiCallables(method, methodConfig));
    }

    return callableMembers;
  }

  public List<JavaApiCallable> generateApiCallables(Method method, MethodConfig methodConfig) {
    String methodNameLowCml = LanguageUtil.upperCamelToLowerCamel(method.getSimpleName());

    List<JavaApiCallable> apiCallables = new ArrayList<>();

    JavaSimpleApiCallable apiCallable = new JavaSimpleApiCallable();

    apiCallable.inTypeName = typeTable.importAndGetShortestName(method.getInputType());
    apiCallable.outTypeName = typeTable.importAndGetShortestName(method.getOutputType());
    apiCallable.name = methodNameLowCml + "Callable";

    apiCallables.add(apiCallable);

    if (methodConfig.isPageStreaming()) {
      PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

      JavaPagedApiCallable pagedApiCallable = new JavaPagedApiCallable();

      pagedApiCallable.inTypeName = apiCallable.inTypeName;
      pagedApiCallable.pageAccessorTypeName =
          typeTable.importAndGetShortestName("com.google.api.gax.core.PageAccessor");
      pagedApiCallable.resourceTypeName =
          typeTable.importAndGetShortestNameForElementType(
              pageStreaming.getResourcesField().getType());
      pagedApiCallable.name = methodNameLowCml + "PagedCallable";
      apiCallables.add(pagedApiCallable);
    }

    return apiCallables;
  }

  public List<JavaPathTemplate> generatePathTemplates() {
    List<JavaPathTemplate> pathTemplates = new ArrayList<>();

    for (CollectionConfig collectionConfig :
        apiConfig.getInterfaceConfig(service).getCollectionConfigs()) {
      JavaPathTemplate pathTemplate = new JavaPathTemplate();
      pathTemplate.name = getPathTemplateName(collectionConfig);
      pathTemplate.pattern = collectionConfig.getNamePattern();
      pathTemplates.add(pathTemplate);
    }

    return pathTemplates;
  }

  public List<JavaFormatResourceFunction> generateFormatResourceFunctions() {
    List<JavaFormatResourceFunction> functions = new ArrayList<>();

    for (CollectionConfig collectionConfig :
        apiConfig.getInterfaceConfig(service).getCollectionConfigs()) {
      JavaFormatResourceFunction function = new JavaFormatResourceFunction();
      function.entityName = collectionConfig.getEntityName();
      function.name =
          "format"
              + LanguageUtil.lowerUnderscoreToUpperCamel(collectionConfig.getEntityName())
              + "Name";
      function.pathTemplateName = getPathTemplateName(collectionConfig);
      List<JavaResourceParam> resourceParams = new ArrayList<>();
      for (String var : collectionConfig.getNameTemplate().vars()) {
        JavaResourceParam param = new JavaResourceParam();
        param.name = LanguageUtil.lowerUnderscoreToLowerCamel(var);
        param.templateKey = var;
        resourceParams.add(param);
      }
      function.resourceParams = resourceParams;

      functions.add(function);
    }

    return functions;
  }

  public String getPathTemplateName(CollectionConfig collectionConfig) {
    return LanguageUtil.lowerUnderscoreToUpperUnderscore(collectionConfig.getEntityName())
        + "_PATH_TEMPLATE";
  }

  public String getApiWrapperClassName() {
    return service.getSimpleName() + "Api";
  }

  public String getSettingsClassName() {
    return service.getSimpleName() + "Settings";
  }
}
