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
import com.google.api.codegen.java.surface.JavaApiMethod;
import com.google.api.codegen.java.surface.JavaRequestObjectParam;
import com.google.api.codegen.java.surface.JavaBundlingApiCallable;
import com.google.api.codegen.java.surface.JavaCallableMethod;
import com.google.api.codegen.java.surface.JavaFlattenedMethod;
import com.google.api.codegen.java.surface.JavaFormatResourceFunction;
import com.google.api.codegen.java.surface.JavaPagedApiCallable;
import com.google.api.codegen.java.surface.JavaPagedCallableMethod;
import com.google.api.codegen.java.surface.JavaPagedFlattenedMethod;
import com.google.api.codegen.java.surface.JavaPagedRequestObjectMethod;
import com.google.api.codegen.java.surface.JavaParseResourceFunction;
import com.google.api.codegen.java.surface.JavaPathTemplate;
import com.google.api.codegen.java.surface.JavaPathTemplateCheck;
import com.google.api.codegen.java.surface.JavaRequestObjectMethod;
import com.google.api.codegen.java.surface.JavaResourceIdParam;
import com.google.api.codegen.java.surface.JavaSimpleApiCallable;
import com.google.api.codegen.java.surface.JavaSurface;
import com.google.api.codegen.java.surface.JavaUnpagedListCallableMethod;
import com.google.api.codegen.java.surface.JavaXApi;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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
    xapiClass.apiMethods = generateApiMethods();

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

    if (methodConfig.isBundling()) {
      JavaBundlingApiCallable apiCallable = new JavaBundlingApiCallable();

      apiCallable.inTypeName = typeTable.importAndGetShortestName(method.getInputType());
      apiCallable.outTypeName = typeTable.importAndGetShortestName(method.getOutputType());
      apiCallable.name = methodNameLowCml + "Callable";
      apiCallable.settingsFunctionName = methodNameLowCml + "Settings";

      apiCallables.add(apiCallable);

    } else {
      JavaSimpleApiCallable apiCallable = new JavaSimpleApiCallable();

      apiCallable.inTypeName = typeTable.importAndGetShortestName(method.getInputType());
      apiCallable.outTypeName = typeTable.importAndGetShortestName(method.getOutputType());
      apiCallable.name = methodNameLowCml + "Callable";
      apiCallable.settingsFunctionName = methodNameLowCml + "Settings";

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
        pagedApiCallable.settingsFunctionName = methodNameLowCml + "Settings";
        apiCallables.add(pagedApiCallable);
      }
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

  private List<JavaApiMethod> generateApiMethods() {
    List<JavaApiMethod> apiMethods = new ArrayList<>();

    for (Method method : service.getMethods()) {
      MethodConfig methodConfig = apiConfig.getInterfaceConfig(service).getMethodConfig(method);

      if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            apiMethods.add(generatePagedFlattenedMethod(method, methodConfig, fields));
          }
        }
        apiMethods.add(new JavaPagedRequestObjectMethod());
        apiMethods.add(new JavaPagedCallableMethod());
        apiMethods.add(new JavaUnpagedListCallableMethod());
      } else {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            JavaFlattenedMethod apiMethod = new JavaFlattenedMethod();
            apiMethod.fields = fields;
            apiMethods.add(apiMethod);
          }
        }
        apiMethods.add(new JavaRequestObjectMethod());
        apiMethods.add(new JavaCallableMethod());
      }
    }

    return apiMethods;
  }

  private JavaPagedFlattenedMethod generatePagedFlattenedMethod(
      Method method, MethodConfig methodConfig, ImmutableList<Field> fields) {
    PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

    JavaPagedFlattenedMethod apiMethod = new JavaPagedFlattenedMethod();
    apiMethod.resourceTypeName =
        typeTable.importAndGetShortestNameForElementType(
            pageStreaming.getResourcesField().getType());
    apiMethod.name = getApiMethodName(method);

    List<JavaRequestObjectParam> requestObjectParams = new ArrayList<>();
    for (Field field : fields) {
      requestObjectParams.add(generateRequestObjectParam(field));
    }
    apiMethod.requestObjectParams = requestObjectParams;

    List<JavaPathTemplateCheck> pathTemplateChecks = new ArrayList<>();
    for (Field field : fields) {
      ImmutableMap<String, String> fieldNamePatterns = methodConfig.getFieldNamePatterns();
      String entityName = fieldNamePatterns.get(field.getSimpleName());
      if (entityName != null) {
        CollectionConfig collectionConfig =
            apiConfig.getInterfaceConfig(service).getCollectionConfig(entityName);
        JavaPathTemplateCheck check = new JavaPathTemplateCheck();
        check.pathTemplateName = getPathTemplateName(collectionConfig);
        check.paramName = getVariableNameForField(field);

        pathTemplateChecks.add(check);
      }
    }
    apiMethod.pathTemplateChecks = pathTemplateChecks;

    apiMethod.requestTypeName = typeTable.importAndGetShortestName(method.getInputType());

    return apiMethod;
  }

  private JavaRequestObjectParam generateRequestObjectParam(Field field) {
    JavaRequestObjectParam param = new JavaRequestObjectParam();
    param.name = getVariableNameForField(field);
    param.typeName = typeTable.importAndGetShortestName(field.getType());

    TypeRef fieldType = field.getType();
    String fieldSuffix = LanguageUtil.lowerUnderscoreToUpperCamel(field.getSimpleName());
    if (fieldType.isMap()) {
      param.setCallName = "putAll" + fieldSuffix;
    } else if (fieldType.isRepeated()) {
      param.setCallName = "addAll" + fieldSuffix;
    } else {
      param.setCallName = "set" + fieldSuffix;
    }

    return param;
  }

  private String getVariableNameForField(Field field) {
    return LanguageUtil.lowerUnderscoreToLowerCamel(field.getSimpleName());
  }

  private String getApiMethodName(Method method) {
    return LanguageUtil.upperCamelToLowerCamel(method.getSimpleName());
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
