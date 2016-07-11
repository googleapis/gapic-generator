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
import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.PageStreamingConfig;
import com.google.api.codegen.ServiceConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.surface.SurfaceApiCallable;
import com.google.api.codegen.surface.SurfaceApiMethod;
import com.google.api.codegen.surface.SurfaceBundlingApiCallable;
import com.google.api.codegen.surface.SurfaceCallableMethod;
import com.google.api.codegen.surface.SurfaceDoc;
import com.google.api.codegen.surface.SurfaceFlattenedMethod;
import com.google.api.codegen.surface.SurfacePagedApiCallable;
import com.google.api.codegen.surface.SurfacePagedCallableMethod;
import com.google.api.codegen.surface.SurfaceRequestObjectMethod;
import com.google.api.codegen.surface.SurfaceSimpleApiCallable;
import com.google.api.codegen.surface.SurfaceStaticXApi;
import com.google.api.codegen.surface.SurfaceStaticXSettings;
import com.google.api.codegen.surface.SurfaceUnpagedListCallableMethod;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class ModelToJavaSurfaceTransformer implements ModelToSurfaceTransformer {
  private ApiConfig cachedApiConfig;
  private GapicCodePathMapper pathMapper;
  private PathTemplateTransformer pathTemplateTransformer;
  private ApiMethodTransformer apiMethodTransformer;

  public ModelToJavaSurfaceTransformer(ApiConfig apiConfig, GapicCodePathMapper pathMapper) {
    this.cachedApiConfig = apiConfig;
    this.pathMapper = pathMapper;
    this.pathTemplateTransformer = new PathTemplateTransformer();
    this.apiMethodTransformer = new ApiMethodTransformer();
  }

  @Override
  public List<String> getTemplateFileNames() {
    List<String> fileNames = new ArrayList<>();

    fileNames.add(new SurfaceStaticXApi().getTemplateFileName());
    fileNames.add(new SurfaceStaticXSettings().getTemplateFileName());

    return fileNames;
  }

  @Override
  public List<SurfaceDoc> transform(Interface service) {
    ModelToSurfaceContext context =
        ModelToSurfaceContext.create(
            service, cachedApiConfig, new ModelToJavaTypeTable(), new JavaIdentifierNamer());

    String outputPath = pathMapper.getOutputPath(service, context.getApiConfig());

    List<SurfaceDoc> surfaceData = new ArrayList<>();

    addXApiImports(context);

    SurfaceStaticXApi xapiClass = new SurfaceStaticXApi();
    xapiClass.packageName = context.getApiConfig().getPackageName();
    xapiClass.name = context.getNamer().getApiWrapperClassName(context.getInterface());
    xapiClass.settingsClassName = getSettingsClassName(context);
    xapiClass.apiCallableMembers = generateApiCallables(context);
    xapiClass.pathTemplates = pathTemplateTransformer.generatePathTemplates(context);
    xapiClass.formatResourceFunctions =
        pathTemplateTransformer.generateFormatResourceFunctions(context);
    xapiClass.parseResourceFunctions =
        pathTemplateTransformer.generateParseResourceFunctions(context);
    xapiClass.apiMethods = generateApiMethods(context);

    // must be done as the last step to catch all imports
    xapiClass.imports = context.getTypeTable().getImports();

    xapiClass.outputPath = outputPath + "/" + xapiClass.name + ".java";

    surfaceData.add(xapiClass);

    context = context.withNewTypeTable();
    addXSettingsImports(context);

    SurfaceStaticXSettings xsettingsClass = new SurfaceStaticXSettings();
    xsettingsClass.packageName = context.getApiConfig().getPackageName();
    xsettingsClass.name = getSettingsClassName(context);
    ServiceConfig serviceConfig = new ServiceConfig();
    xsettingsClass.serviceAddress = serviceConfig.getServiceAddress(service);
    xsettingsClass.servicePort = serviceConfig.getServicePort();
    xsettingsClass.authScopes = serviceConfig.getAuthScopes(service);

    // must be done as the last step to catch all imports
    xsettingsClass.imports = context.getTypeTable().getImports();

    xsettingsClass.outputPath = outputPath + "/" + xsettingsClass.name + ".java";

    surfaceData.add(xsettingsClass);

    return surfaceData;
  }

  private void addXApiImports(ModelToSurfaceContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("com.google.api.gax.grpc.ApiCallable");
    typeTable.saveNicknameFor("com.google.api.gax.protobuf.PathTemplate");
    typeTable.saveNicknameFor("io.grpc.ManagedChannel");
    typeTable.saveNicknameFor("java.io.Closeable");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.util.ArrayList");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.concurrent.ScheduledExecutorService");
  }

  private void addXSettingsImports(ModelToSurfaceContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("com.google.api.gax.core.ConnectionSettings");
    typeTable.saveNicknameFor("com.google.api.gax.core.RetrySettings");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.ApiCallSettings");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.SimpleCallSettings");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.ServiceApiSettings");
    typeTable.saveNicknameFor("com.google.auth.Credentials");
    typeTable.saveNicknameFor("com.google.common.collect.ImmutableList");
    typeTable.saveNicknameFor("com.google.common.collect.ImmutableMap");
    typeTable.saveNicknameFor("com.google.common.collect.ImmutableSet");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("com.google.common.collect.Sets");
    typeTable.saveNicknameFor("io.grpc.ManagedChannel");
    typeTable.saveNicknameFor("io.grpc.Status");
    typeTable.saveNicknameFor("org.joda.time.Duration");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.concurrent.ScheduledExecutorService");
  }

  private List<SurfaceApiCallable> generateApiCallables(ModelToSurfaceContext context) {
    List<SurfaceApiCallable> callableMembers = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      callableMembers.addAll(generateApiCallables(context, method, methodConfig));
    }

    return callableMembers;
  }

  private List<SurfaceApiCallable> generateApiCallables(
      ModelToSurfaceContext context, Method method, MethodConfig methodConfig) {
    String methodNameLowCml = LanguageUtil.upperCamelToLowerCamel(method.getSimpleName());
    ModelTypeTable typeTable = context.getTypeTable();

    List<SurfaceApiCallable> apiCallables = new ArrayList<>();

    if (methodConfig.isBundling()) {
      SurfaceBundlingApiCallable apiCallable = new SurfaceBundlingApiCallable();

      apiCallable.inTypeName = typeTable.getAndSaveNicknameFor(method.getInputType());
      apiCallable.outTypeName = typeTable.getAndSaveNicknameFor(method.getOutputType());
      apiCallable.name = methodNameLowCml + "Callable";
      apiCallable.settingsFunctionName = methodNameLowCml + "Settings";

      apiCallables.add(apiCallable);

    } else {
      SurfaceSimpleApiCallable apiCallable = new SurfaceSimpleApiCallable();

      apiCallable.inTypeName = typeTable.getAndSaveNicknameFor(method.getInputType());
      apiCallable.outTypeName = typeTable.getAndSaveNicknameFor(method.getOutputType());
      apiCallable.name = methodNameLowCml + "Callable";
      apiCallable.settingsFunctionName = methodNameLowCml + "Settings";

      apiCallables.add(apiCallable);

      if (methodConfig.isPageStreaming()) {
        PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

        SurfacePagedApiCallable pagedApiCallable = new SurfacePagedApiCallable();

        pagedApiCallable.inTypeName = apiCallable.inTypeName;
        pagedApiCallable.pageAccessorTypeName =
            typeTable.getAndSaveNicknameFor("com.google.api.gax.core.PageAccessor");
        pagedApiCallable.resourceTypeName =
            typeTable.getAndSaveNicknameForElementType(pageStreaming.getResourcesField().getType());
        pagedApiCallable.name = methodNameLowCml + "PagedCallable";
        pagedApiCallable.settingsFunctionName = methodNameLowCml + "Settings";
        apiCallables.add(pagedApiCallable);
      }
    }

    return apiCallables;
  }

  private List<SurfaceApiMethod> generateApiMethods(ModelToSurfaceContext context) {
    List<SurfaceApiMethod> apiMethods = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);

      if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            apiMethods.add(
                apiMethodTransformer.generatePagedFlattenedMethod(
                    context, method, methodConfig, fields));
          }
        }
        apiMethods.add(
            apiMethodTransformer.generatePagedRequestObjectMethod(context, method, methodConfig));
        apiMethods.add(new SurfacePagedCallableMethod());
        apiMethods.add(new SurfaceUnpagedListCallableMethod());
      } else {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            SurfaceFlattenedMethod apiMethod = new SurfaceFlattenedMethod();
            apiMethod.fields = fields;
            apiMethods.add(apiMethod);
          }
        }
        apiMethods.add(new SurfaceRequestObjectMethod());
        apiMethods.add(new SurfaceCallableMethod());
      }
    }

    return apiMethods;
  }

  private String getSettingsClassName(ModelToSurfaceContext context) {
    return context.getInterface().getSimpleName() + "Settings";
  }
}
