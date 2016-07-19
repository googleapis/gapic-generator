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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.PageStreamingConfig;
import com.google.api.codegen.ServiceConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.viewmodel.ApiCallableType;
import com.google.api.codegen.viewmodel.ApiCallableView;
import com.google.api.codegen.viewmodel.StaticApiMethodView;
import com.google.api.codegen.viewmodel.StaticXApiView;
import com.google.api.codegen.viewmodel.StaticXSettingsView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The ModelToViewTransformer to transform a Model into the standard GAPIC surface in Java.
 */
public class JavaGapicSurfaceTransformer implements ModelToViewTransformer {
  private GapicCodePathMapper pathMapper;
  private PathTemplateTransformer pathTemplateTransformer;
  private ApiMethodTransformer apiMethodTransformer;

  private static final String XAPI_TEMPLATE_FILENAME = "java/xapi.snip";
  private static final String XSETTINGS_TEMPLATE_FILENAME = "java/xsettings.snip";

  /**
   * Standard constructor.
   */
  public JavaGapicSurfaceTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
    this.pathTemplateTransformer = new PathTemplateTransformer();
    this.apiMethodTransformer = new ApiMethodTransformer();
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(XAPI_TEMPLATE_FILENAME, XSETTINGS_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service, apiConfig, new JavaModelTypeTable(), new JavaSurfaceNamer());
      surfaceDocs.addAll(transform(context));
    }
    return surfaceDocs;
  }

  public List<ViewModel> transform(SurfaceTransformerContext context) {
    List<ViewModel> surfaceData = new ArrayList<>();

    surfaceData.add(generateXApi(context));

    context = context.withNewTypeTable();
    surfaceData.add(generateXSettings(context));

    return surfaceData;
  }

  private StaticXApiView generateXApi(SurfaceTransformerContext context) {
    addXApiImports(context);

    StaticXApiView.Builder xapiClass = StaticXApiView.newBuilder();
    xapiClass.templateFileName(XAPI_TEMPLATE_FILENAME);
    xapiClass.packageName(context.getApiConfig().getPackageName());
    String name = context.getNamer().getApiWrapperClassName(context.getInterface());
    xapiClass.name(name);
    xapiClass.settingsClassName(context.getNamer().getApiSettingsClassName(context.getInterface()));
    xapiClass.apiCallableMembers(generateApiCallables(context));
    xapiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    xapiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    xapiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    xapiClass.apiMethods(generateApiMethods(context));

    // must be done as the last step to catch all imports
    xapiClass.imports(context.getTypeTable().getImports());

    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    xapiClass.outputPath(outputPath + "/" + name + ".java");

    return xapiClass.build();
  }

  private StaticXSettingsView generateXSettings(SurfaceTransformerContext context) {
    addXSettingsImports(context);

    StaticXSettingsView.Builder xsettingsClass = StaticXSettingsView.newBuilder();
    xsettingsClass.templateFileName(XSETTINGS_TEMPLATE_FILENAME);
    xsettingsClass.packageName(context.getApiConfig().getPackageName());
    String name = context.getNamer().getApiSettingsClassName(context.getInterface());
    xsettingsClass.name(name);
    ServiceConfig serviceConfig = new ServiceConfig();
    xsettingsClass.serviceAddress(serviceConfig.getServiceAddress(context.getInterface()));
    xsettingsClass.servicePort(serviceConfig.getServicePort());
    xsettingsClass.authScopes(serviceConfig.getAuthScopes(context.getInterface()));

    // must be done as the last step to catch all imports
    xsettingsClass.imports(context.getTypeTable().getImports());

    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    xsettingsClass.outputPath(outputPath + "/" + name + ".java");

    return xsettingsClass.build();
  }

  private void addXApiImports(SurfaceTransformerContext context) {
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

  private void addXSettingsImports(SurfaceTransformerContext context) {
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

  private List<ApiCallableView> generateApiCallables(SurfaceTransformerContext context) {
    List<ApiCallableView> callableMembers = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      callableMembers.addAll(generateApiCallables(context, method, methodConfig));
    }

    return callableMembers;
  }

  private List<ApiCallableView> generateApiCallables(
      SurfaceTransformerContext context, Method method, MethodConfig methodConfig) {
    ModelTypeTable typeTable = context.getTypeTable();

    List<ApiCallableView> apiCallables = new ArrayList<>();

    ApiCallableView.Builder apiCallableBuilder = ApiCallableView.newBuilder();

    apiCallableBuilder.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    apiCallableBuilder.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));
    apiCallableBuilder.name(context.getNamer().getCallableName(method));
    apiCallableBuilder.settingsFunctionName(context.getNamer().getSettingsFunctionName(method));

    if (methodConfig.isBundling()) {
      apiCallableBuilder.type(ApiCallableType.BundlingApiCallable);
    } else {
      apiCallableBuilder.type(ApiCallableType.SimpleApiCallable);
    }

    apiCallables.add(apiCallableBuilder.build());

    if (methodConfig.isPageStreaming()) {
      PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

      ApiCallableView.Builder pagedApiCallableBuilder = ApiCallableView.newBuilder();

      String pagedResponseTypeName =
          context
              .getNamer()
              .getAndSavePagedResponseTypeName(
                  typeTable, pageStreaming.getResourcesField().getType());

      pagedApiCallableBuilder.requestTypeName(
          typeTable.getAndSaveNicknameFor(method.getInputType()));
      pagedApiCallableBuilder.responseTypeName(pagedResponseTypeName);
      pagedApiCallableBuilder.name(context.getNamer().getPagedCallableName(method));
      pagedApiCallableBuilder.settingsFunctionName(
          context.getNamer().getSettingsFunctionName(method));

      apiCallables.add(pagedApiCallableBuilder.type(ApiCallableType.PagedApiCallable).build());
    }

    return apiCallables;
  }

  private List<StaticApiMethodView> generateApiMethods(SurfaceTransformerContext context) {
    List<StaticApiMethodView> apiMethods = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      MethodTransformerContext methodContext = context.asMethodContext(method);

      if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            apiMethods.add(
                apiMethodTransformer.generatePagedFlattenedMethod(methodContext, fields));
          }
        }
        apiMethods.add(apiMethodTransformer.generatePagedRequestObjectMethod(methodContext));
        apiMethods.add(apiMethodTransformer.generatePagedCallableMethod(methodContext));
        apiMethods.add(apiMethodTransformer.generateUnpagedListCallableMethod(methodContext));
      } else {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            apiMethods.add(apiMethodTransformer.generateFlattenedMethod(methodContext, fields));
          }
        }
        apiMethods.add(apiMethodTransformer.generateRequestObjectMethod(methodContext));
        apiMethods.add(apiMethodTransformer.generateCallableMethod(methodContext));
      }
    }

    return apiMethods;
  }
}
