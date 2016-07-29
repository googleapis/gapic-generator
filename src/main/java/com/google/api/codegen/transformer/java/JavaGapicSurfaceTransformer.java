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
import com.google.api.codegen.ServiceConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.BundlingTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ApiMethodType;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.PackageInfoView;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.codegen.viewmodel.ServiceDocView;
import com.google.api.codegen.viewmodel.SettingsDocView;
import com.google.api.codegen.viewmodel.StaticApiMethodView;
import com.google.api.codegen.viewmodel.StaticXApiView;
import com.google.api.codegen.viewmodel.StaticXSettingsView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.gax.core.RetrySettings;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import io.grpc.Status.Code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

/**
 * The ModelToViewTransformer to transform a Model into the standard GAPIC surface in Java.
 */
public class JavaGapicSurfaceTransformer implements ModelToViewTransformer {
  private GapicCodePathMapper pathMapper;
  private ServiceTransformer serviceTransformer;
  private PathTemplateTransformer pathTemplateTransformer;
  private ApiCallableTransformer apiCallableTransformer;
  private ApiMethodTransformer apiMethodTransformer;
  private PageStreamingTransformer pageStreamingTransformer;
  private BundlingTransformer bundlingTransformer;

  private static final String XAPI_TEMPLATE_FILENAME = "java/main.snip";
  private static final String XSETTINGS_TEMPLATE_FILENAME = "java/settings.snip";
  private static final String PACKAGE_INFO_TEMPLATE_FILENAME = "java/package-info.snip";

  /**
   * Standard constructor.
   */
  public JavaGapicSurfaceTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
    this.serviceTransformer = new ServiceTransformer();
    this.pathTemplateTransformer = new PathTemplateTransformer();
    this.apiCallableTransformer = new ApiCallableTransformer();
    this.apiMethodTransformer = new ApiMethodTransformer();
    this.pageStreamingTransformer = new PageStreamingTransformer();
    this.bundlingTransformer = new BundlingTransformer();
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(
        XAPI_TEMPLATE_FILENAME, XSETTINGS_TEMPLATE_FILENAME, PACKAGE_INFO_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new JavaSurfaceNamer();

    List<ServiceDocView> serviceDocs = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(service, apiConfig, createTypeTable(), namer);
      StaticXApiView xapi = generateXApi(context);
      surfaceDocs.add(xapi);

      serviceDocs.add(xapi.doc());

      context = SurfaceTransformerContext.create(service, apiConfig, createTypeTable(), namer);
      StaticApiMethodView exampleApiMethod = getExampleApiMethod(xapi.apiMethods());
      StaticXSettingsView xsettings = generateXSettings(context, exampleApiMethod);
      surfaceDocs.add(xsettings);
    }

    PackageInfoView packageInfo =
        generatePackageInfo(model, apiConfig, createTypeTable(), namer, serviceDocs);
    surfaceDocs.add(packageInfo);

    return surfaceDocs;
  }

  private ModelTypeTable createTypeTable() {
    return new ModelTypeTable(new JavaTypeTable(), new JavaModelTypeNameConverter());
  }

  private StaticXApiView generateXApi(SurfaceTransformerContext context) {
    addXApiImports(context);

    List<StaticApiMethodView> methods = generateApiMethods(context);

    StaticXApiView.Builder xapiClass = StaticXApiView.newBuilder();

    ApiMethodView exampleApiMethod = getExampleApiMethod(methods);
    xapiClass.doc(serviceTransformer.generateServiceDoc(context, exampleApiMethod));

    xapiClass.templateFileName(XAPI_TEMPLATE_FILENAME);
    xapiClass.packageName(context.getApiConfig().getPackageName());
    String name = context.getNamer().getApiWrapperClassName(context.getInterface());
    xapiClass.name(name);
    xapiClass.settingsClassName(context.getNamer().getApiSettingsClassName(context.getInterface()));
    xapiClass.apiCallableMembers(apiCallableTransformer.generateStaticApiCallables(context));
    xapiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    xapiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    xapiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    xapiClass.apiMethods(methods);

    // must be done as the last step to catch all imports
    xapiClass.imports(context.getTypeTable().getImports());

    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    xapiClass.outputPath(outputPath + "/" + name + ".java");

    return xapiClass.build();
  }

  private StaticApiMethodView getExampleApiMethod(List<StaticApiMethodView> methods) {
    StaticApiMethodView exampleApiMethod = null;
    for (StaticApiMethodView method : methods) {
      if (method.type().equals(ApiMethodType.FlattenedMethod)) {
        exampleApiMethod = method;
        break;
      }
    }
    if (exampleApiMethod == null) {
      throw new RuntimeException("Could not find flattened method to use as an example method");
    }
    return exampleApiMethod;
  }

  private StaticXSettingsView generateXSettings(
      SurfaceTransformerContext context, StaticApiMethodView exampleApiMethod) {
    addXSettingsImports(context);

    StaticXSettingsView.Builder xsettingsClass = StaticXSettingsView.newBuilder();
    xsettingsClass.templateFileName(XSETTINGS_TEMPLATE_FILENAME);
    xsettingsClass.packageName(context.getApiConfig().getPackageName());
    xsettingsClass.doc(generateSettingsDoc(context, exampleApiMethod));
    String name = context.getNamer().getApiSettingsClassName(context.getInterface());
    xsettingsClass.name(name);
    ServiceConfig serviceConfig = new ServiceConfig();
    xsettingsClass.serviceAddress(serviceConfig.getServiceAddress(context.getInterface()));
    xsettingsClass.servicePort(serviceConfig.getServicePort());
    xsettingsClass.authScopes(serviceConfig.getAuthScopes(context.getInterface()));
    xsettingsClass.callSettings(apiCallableTransformer.generateCallSettings(context));
    xsettingsClass.pageStreamingDescriptors(
        pageStreamingTransformer.generateDescriptorClasses(context));
    xsettingsClass.bundlingDescriptors(bundlingTransformer.generateDescriptorClasses(context));
    xsettingsClass.retryCodesDefinitions(generateRetryCodesDefinitions(context));
    xsettingsClass.retryParamsDefinitions(generateRetryParamsDefinitions(context));

    // must be done as the last step to catch all imports
    xsettingsClass.imports(context.getTypeTable().getImports());

    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    xsettingsClass.outputPath(outputPath + "/" + name + ".java");

    return xsettingsClass.build();
  }

  private PackageInfoView generatePackageInfo(
      Model model,
      ApiConfig apiConfig,
      ModelTypeTable createTypeTable,
      SurfaceNamer namer,
      List<ServiceDocView> serviceDocs) {
    PackageInfoView.Builder packageInfo = PackageInfoView.newBuilder();

    packageInfo.templateFileName(PACKAGE_INFO_TEMPLATE_FILENAME);

    packageInfo.serviceTitle(model.getServiceConfig().getTitle());
    packageInfo.serviceDocs(serviceDocs);
    packageInfo.packageName(apiConfig.getPackageName());

    Interface firstInterface = new InterfaceView().getElementIterable(model).iterator().next();
    String outputPath = pathMapper.getOutputPath(firstInterface, apiConfig);
    packageInfo.outputPath(outputPath + "/package-info.java");

    return packageInfo.build();
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

  public SettingsDocView generateSettingsDoc(
      SurfaceTransformerContext context, StaticApiMethodView exampleApiMethod) {
    SurfaceNamer namer = context.getNamer();
    SettingsDocView.Builder settingsDoc = SettingsDocView.newBuilder();
    ServiceConfig serviceConfig = new ServiceConfig();
    settingsDoc.serviceAddress(serviceConfig.getServiceAddress(context.getInterface()));
    settingsDoc.servicePort(serviceConfig.getServicePort());
    settingsDoc.exampleApiMethodName(exampleApiMethod.name());
    settingsDoc.exampleApiMethodSettingsGetter(exampleApiMethod.settingsGetterName());
    settingsDoc.apiClassName(namer.getApiWrapperClassName(context.getInterface()));
    settingsDoc.settingsVarName(namer.getApiSettingsVariableName(context.getInterface()));
    settingsDoc.settingsClassName(namer.getApiSettingsClassName(context.getInterface()));
    settingsDoc.settingsBuilderVarName(namer.getApiSettingsBuilderVarName(context.getInterface()));
    return settingsDoc.build();
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

  private List<RetryCodesDefinitionView> generateRetryCodesDefinitions(
      SurfaceTransformerContext context) {
    List<RetryCodesDefinitionView> definitions = new ArrayList<>();

    for (Entry<String, ImmutableSet<Code>> retryCodesDef :
        context.getInterfaceConfig().getRetryCodesDefinition().entrySet()) {
      definitions.add(
          RetryCodesDefinitionView.newBuilder()
              .key(retryCodesDef.getKey())
              .codes(retryCodesDef.getValue())
              .build());
    }

    return definitions;
  }

  private List<RetryParamsDefinitionView> generateRetryParamsDefinitions(
      SurfaceTransformerContext context) {
    List<RetryParamsDefinitionView> definitions = new ArrayList<>();

    for (Entry<String, RetrySettings> retryCodesDef :
        context.getInterfaceConfig().getRetrySettingsDefinition().entrySet()) {
      RetrySettings settings = retryCodesDef.getValue();
      RetryParamsDefinitionView.Builder params = RetryParamsDefinitionView.newBuilder();
      params.key(retryCodesDef.getKey());
      params.initialRetryDelay(settings.getInitialRetryDelay());
      params.retryDelayMultiplier(settings.getRetryDelayMultiplier());
      params.maxRetryDelay(settings.getMaxRetryDelay());
      params.initialRpcTimeout(settings.getInitialRpcTimeout());
      params.rpcTimeoutMultiplier(settings.getRpcTimeoutMultiplier());
      params.maxRpcTimeout(settings.getMaxRpcTimeout());
      params.totalTimeout(settings.getTotalTimeout());
      definitions.add(params.build());
    }

    return definitions;
  }
}
