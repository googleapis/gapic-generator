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

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.PageStreamingConfig;
import com.google.api.codegen.java.surface.JavaApiCallable;
import com.google.api.codegen.java.surface.JavaFormatResourceFunction;
import com.google.api.codegen.java.surface.JavaPagedApiCallable;
import com.google.api.codegen.java.surface.JavaParseResourceFunction;
import com.google.api.codegen.java.surface.JavaPathTemplate;
import com.google.api.codegen.java.surface.JavaResourceIdParam;
import com.google.api.codegen.java.surface.JavaSimpleApiCallable;
import com.google.api.codegen.java.surface.JavaSurface;
import com.google.api.codegen.java.surface.JavaXApi;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;

import java.util.ArrayList;
import java.util.List;

public class ModelToJavaSurfaceTransformer {
  private Interface service;
  private ApiConfig apiConfig;
  private ModelToJavaTypeTable typeTable;

  public static JavaSurface defaultTransform(Interface service, ApiConfig apiConfig) {
    return new ModelToJavaSurfaceTransformer(service, apiConfig).transform();
  }

  public ModelToJavaSurfaceTransformer(Interface service, ApiConfig apiConfig) {
    this.service = service;
    this.apiConfig = apiConfig;
  }

  public JavaSurface transform() {
    JavaSurface surface = new JavaSurface();

    typeTable = new ModelToJavaTypeTable();
    addAlwaysImports();

    JavaXApi xapiClass = new JavaXApi();
    xapiClass.packageName = apiConfig.getPackageName();
    xapiClass.name = getApiWrapperClassName();
    xapiClass.settingsClassName = getSettingsClassName();
    xapiClass.apiCallableMembers = generateApiCallables();
    xapiClass.pathTemplates = generatePathTemplates();
    xapiClass.formatResourceFunctions = generateFormatResourceFunctions();
    xapiClass.parseResourceFunctions = generateParseResourceFunctions();

    // must be done as the last step to catch all imports
    xapiClass.imports = typeTable.getImports();

    surface.xapiClass = xapiClass;

    typeTable = new ModelToJavaTypeTable();

    return surface;
  }

  private void addAlwaysImports() {
    typeTable.addImport("com.google.api.gax.grpc.ApiCallable");
    typeTable.addImport("com.google.api.gax.protobuf.PathTemplate");
    typeTable.addImport("io.grpc.ManagedChannel");
    typeTable.addImport("java.io.Closeable");
    typeTable.addImport("java.io.IOException");
    typeTable.addImport("java.util.ArrayList");
    typeTable.addImport("java.util.List");
    typeTable.addImport("java.util.concurrent.ScheduledExecutorService");
  }

  private List<JavaApiCallable> generateApiCallables() {
    List<JavaApiCallable> callableMembers = new ArrayList<>();

    for (Method method : service.getMethods()) {
      MethodConfig methodConfig = apiConfig.getInterfaceConfig(service).getMethodConfig(method);
      callableMembers.addAll(generateApiCallables(method, methodConfig));
    }

    return callableMembers;
  }

  private List<JavaApiCallable> generateApiCallables(Method method, MethodConfig methodConfig) {
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

  private List<JavaPathTemplate> generatePathTemplates() {
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

  private List<JavaFormatResourceFunction> generateFormatResourceFunctions() {
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
      List<JavaResourceIdParam> resourceIdParams = new ArrayList<>();
      for (String var : collectionConfig.getNameTemplate().vars()) {
        JavaResourceIdParam param = new JavaResourceIdParam();
        param.name = LanguageUtil.lowerUnderscoreToLowerCamel(var);
        param.templateKey = var;
        resourceIdParams.add(param);
      }
      function.resourceIdParams = resourceIdParams;

      functions.add(function);
    }

    return functions;
  }

  private List<JavaParseResourceFunction> generateParseResourceFunctions() {
    List<JavaParseResourceFunction> functions = new ArrayList<>();

    for (CollectionConfig collectionConfig :
        apiConfig.getInterfaceConfig(service).getCollectionConfigs()) {
      for (String var : collectionConfig.getNameTemplate().vars()) {
        JavaParseResourceFunction function = new JavaParseResourceFunction();
        function.entityName =
            LanguageUtil.lowerUnderscoreToLowerCamel(collectionConfig.getEntityName());
        function.name =
            "parse"
                + LanguageUtil.lowerUnderscoreToUpperCamel(var)
                + "From"
                + LanguageUtil.lowerUnderscoreToUpperCamel(collectionConfig.getEntityName())
                + "Name";
        function.pathTemplateName = getPathTemplateName(collectionConfig);
        function.entityNameParamName = function.entityName + "Name";
        function.outputResourceId = var;

        functions.add(function);
      }
    }

    return functions;
  }

  private String getPathTemplateName(CollectionConfig collectionConfig) {
    return LanguageUtil.lowerUnderscoreToUpperUnderscore(collectionConfig.getEntityName())
        + "_PATH_TEMPLATE";
  }

  private String getApiWrapperClassName() {
    return service.getSimpleName() + "Api";
  }

  private String getSettingsClassName() {
    return service.getSimpleName() + "Settings";
  }
}
