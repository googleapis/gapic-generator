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
import com.google.api.codegen.LanguageUtil;
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
import com.google.api.codegen.viewmodel.ApiCallableView;
import com.google.api.codegen.viewmodel.BundlingApiCallableView;
import com.google.api.codegen.viewmodel.PagedApiCallableView;
import com.google.api.codegen.viewmodel.SimpleApiCallableView;
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

public class ModelToJavaSurfaceTransformer implements ModelToViewTransformer {
  private ApiConfig cachedApiConfig;
  private GapicCodePathMapper pathMapper;
  private PathTemplateTransformer pathTemplateTransformer;
  private ApiMethodTransformer apiMethodTransformer;

  private static final String XAPI_TEMPLATE_FILENAME = "java/xapi.snip";
  private static final String XSETTINGS_TEMPLATE_FILENAME = "java/xsettings.snip";

  public ModelToJavaSurfaceTransformer(ApiConfig apiConfig, GapicCodePathMapper pathMapper) {
    this.cachedApiConfig = apiConfig;
    this.pathMapper = pathMapper;
    this.pathTemplateTransformer = new PathTemplateTransformer();
    this.apiMethodTransformer = new ApiMethodTransformer();
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(XAPI_TEMPLATE_FILENAME, XSETTINGS_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      surfaceDocs.addAll(transform(service));
    }
    return surfaceDocs;
  }

  public List<ViewModel> transform(Interface service) {
    SurfaceTransformerContext context =
        SurfaceTransformerContext.create(
            service, cachedApiConfig, new ModelToJavaTypeTable(), new JavaSurfaceNamer());

    List<ViewModel> surfaceData = new ArrayList<>();

    surfaceData.add(generateXApi(context));

    context = context.withNewTypeTable();
    surfaceData.add(generateXSettings(context));

    return surfaceData;
  }

  private StaticXApiView generateXApi(SurfaceTransformerContext context) {
    addXApiImports(context);

    StaticXApiView xapiClass = new StaticXApiView();
    xapiClass.templateFileName = XAPI_TEMPLATE_FILENAME;
    xapiClass.packageName = context.getApiConfig().getPackageName();
    xapiClass.name = context.getNamer().getApiWrapperClassName(context.getInterface());
    xapiClass.settingsClassName =
        context.getNamer().getApiSettingsClassName(context.getInterface());
    xapiClass.apiCallableMembers = generateApiCallables(context);
    xapiClass.pathTemplates = pathTemplateTransformer.generatePathTemplates(context);
    xapiClass.formatResourceFunctions =
        pathTemplateTransformer.generateFormatResourceFunctions(context);
    xapiClass.parseResourceFunctions =
        pathTemplateTransformer.generateParseResourceFunctions(context);
    xapiClass.apiMethods = generateApiMethods(context);

    // must be done as the last step to catch all imports
    xapiClass.imports = context.getTypeTable().getImports();

    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    xapiClass.outputPath = outputPath + "/" + xapiClass.name + ".java";

    return xapiClass;
  }

  private StaticXSettingsView generateXSettings(SurfaceTransformerContext context) {
    addXSettingsImports(context);

    StaticXSettingsView xsettingsClass = new StaticXSettingsView();
    xsettingsClass.templateFileName = XSETTINGS_TEMPLATE_FILENAME;
    xsettingsClass.packageName = context.getApiConfig().getPackageName();
    xsettingsClass.name = context.getNamer().getApiSettingsClassName(context.getInterface());
    ServiceConfig serviceConfig = new ServiceConfig();
    xsettingsClass.serviceAddress = serviceConfig.getServiceAddress(context.getInterface());
    xsettingsClass.servicePort = serviceConfig.getServicePort();
    xsettingsClass.authScopes = serviceConfig.getAuthScopes(context.getInterface());

    // must be done as the last step to catch all imports
    xsettingsClass.imports = context.getTypeTable().getImports();

    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    xsettingsClass.outputPath = outputPath + "/" + xsettingsClass.name + ".java";

    return xsettingsClass;
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
    String methodNameLowCml = LanguageUtil.upperCamelToLowerCamel(method.getSimpleName());
    ModelTypeTable typeTable = context.getTypeTable();

    List<ApiCallableView> apiCallables = new ArrayList<>();

    if (methodConfig.isBundling()) {
      BundlingApiCallableView apiCallable = new BundlingApiCallableView();

      apiCallable.requestTypeName = typeTable.getAndSaveNicknameFor(method.getInputType());
      apiCallable.responseTypeName = typeTable.getAndSaveNicknameFor(method.getOutputType());
      apiCallable.name = methodNameLowCml + "Callable";
      apiCallable.settingsFunctionName = methodNameLowCml + "Settings";

      apiCallables.add(apiCallable);

    } else {
      SimpleApiCallableView apiCallable = new SimpleApiCallableView();

      apiCallable.requestTypeName = typeTable.getAndSaveNicknameFor(method.getInputType());
      apiCallable.responseTypeName = typeTable.getAndSaveNicknameFor(method.getOutputType());
      apiCallable.name = methodNameLowCml + "Callable";
      apiCallable.settingsFunctionName = methodNameLowCml + "Settings";

      apiCallables.add(apiCallable);

      if (methodConfig.isPageStreaming()) {
        PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

        PagedApiCallableView pagedApiCallable = new PagedApiCallableView();

        pagedApiCallable.requestTypeName = apiCallable.requestTypeName;
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
