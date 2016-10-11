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

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.ServiceConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.BundlingTransformer;
import com.google.api.codegen.transformer.ImportTypeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.RetryDefinitionsTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.ResourceNameUtil;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiMethodType;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.PackageInfoView;
import com.google.api.codegen.viewmodel.PagedResponseIterateMethodView;
import com.google.api.codegen.viewmodel.ServiceDocView;
import com.google.api.codegen.viewmodel.SettingsDocView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangPagedResponseView;
import com.google.api.codegen.viewmodel.StaticLangPagedResponseWrappersView;
import com.google.api.codegen.viewmodel.StaticLangXApiView;
import com.google.api.codegen.viewmodel.StaticLangXSettingsView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The ModelToViewTransformer to transform a Model into the standard GAPIC surface in Java. */
public class JavaGapicSurfaceTransformer implements ModelToViewTransformer {
  private GapicCodePathMapper pathMapper;
  private ServiceTransformer serviceTransformer;
  private PathTemplateTransformer pathTemplateTransformer;
  private ApiCallableTransformer apiCallableTransformer;
  private ApiMethodTransformer apiMethodTransformer;
  private PageStreamingTransformer pageStreamingTransformer;
  private BundlingTransformer bundlingTransformer;
  private ImportTypeTransformer importTypeTransformer;
  private RetryDefinitionsTransformer retryDefinitionsTransformer;

  private static final String XAPI_TEMPLATE_FILENAME = "java/main.snip";
  private static final String XSETTINGS_TEMPLATE_FILENAME = "java/settings.snip";
  private static final String PACKAGE_INFO_TEMPLATE_FILENAME = "java/package-info.snip";
  private static final String PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME =
      "java/page_streaming_response.snip";

  public JavaGapicSurfaceTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
    this.serviceTransformer = new ServiceTransformer();
    this.pathTemplateTransformer = new PathTemplateTransformer();
    this.apiCallableTransformer = new ApiCallableTransformer();
    this.apiMethodTransformer = new ApiMethodTransformer();
    this.pageStreamingTransformer = new PageStreamingTransformer();
    this.bundlingTransformer = new BundlingTransformer();
    this.importTypeTransformer = new ImportTypeTransformer();
    this.retryDefinitionsTransformer = new RetryDefinitionsTransformer();
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(
        XAPI_TEMPLATE_FILENAME,
        XSETTINGS_TEMPLATE_FILENAME,
        PACKAGE_INFO_TEMPLATE_FILENAME,
        PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new JavaSurfaceNamer(apiConfig.getPackageName());

    List<ServiceDocView> serviceDocs = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service,
              apiConfig,
              createTypeTable(apiConfig.getPackageName()),
              namer,
              new JavaFeatureConfig());
      StaticLangXApiView xapi = generateXApi(context);
      surfaceDocs.add(xapi);

      serviceDocs.add(xapi.doc());

      context =
          SurfaceTransformerContext.create(
              service,
              apiConfig,
              createTypeTable(apiConfig.getPackageName()),
              namer,
              new JavaFeatureConfig());
      StaticLangApiMethodView exampleApiMethod = getExampleApiMethod(xapi.apiMethods());
      StaticLangXSettingsView xsettings = generateXSettings(context, exampleApiMethod);
      surfaceDocs.add(xsettings);
    }

    surfaceDocs.add(generatePagedResponseWrappers(model, apiConfig));

    PackageInfoView packageInfo = generatePackageInfo(model, apiConfig, serviceDocs);
    surfaceDocs.add(packageInfo);

    return surfaceDocs;
  }

  private ModelTypeTable createTypeTable(String implicitPackageName) {
    return new ModelTypeTable(
        new JavaTypeTable(implicitPackageName),
        new JavaModelTypeNameConverter(implicitPackageName));
  }

  private StaticLangXApiView generateXApi(SurfaceTransformerContext context) {
    addXApiImports(context);

    List<StaticLangApiMethodView> methods = generateApiMethods(context);

    StaticLangXApiView.Builder xapiClass = StaticLangXApiView.newBuilder();

    ApiMethodView exampleApiMethod = getExampleApiMethod(methods);
    xapiClass.doc(serviceTransformer.generateServiceDoc(context, exampleApiMethod));

    xapiClass.templateFileName(XAPI_TEMPLATE_FILENAME);
    xapiClass.packageName(context.getApiConfig().getPackageName());
    String name = context.getNamer().getApiWrapperClassName(context.getInterface());
    xapiClass.name(name);
    xapiClass.settingsClassName(context.getNamer().getApiSettingsClassName(context.getInterface()));
    xapiClass.apiCallableMembers(apiCallableTransformer.generateStaticLangApiCallables(context));
    xapiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    xapiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    xapiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    xapiClass.apiMethods(methods);

    // must be done as the last step to catch all imports
    xapiClass.imports(importTypeTransformer.generateImports(context.getTypeTable().getImports()));

    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    xapiClass.outputPath(outputPath + File.separator + name + ".java");

    return xapiClass.build();
  }

  private StaticLangPagedResponseWrappersView generatePagedResponseWrappers(
      Model model, ApiConfig apiConfig) {

    SurfaceNamer namer = new JavaSurfaceNamer(apiConfig.getPackageName());
    ModelTypeTable typeTable = createTypeTable(apiConfig.getPackageName());

    StaticLangPagedResponseWrappersView.Builder pagedResponseWrappers =
        StaticLangPagedResponseWrappersView.newBuilder();

    namer.addPagedListResponseImports(typeTable);
    namer.addPageStreamingDescriptorImports(typeTable);

    pagedResponseWrappers.templateFileName(PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME);
    pagedResponseWrappers.packageName(apiConfig.getPackageName());

    String name = namer.getPagedResponseWrappersClassName();
    pagedResponseWrappers.name(name);

    List<StaticLangPagedResponseView> pagedResponseWrappersList = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service, apiConfig, typeTable, namer, new JavaFeatureConfig());
      for (Method method : context.getSupportedMethods()) {
        if (context.getMethodConfig(method).isPageStreaming()) {
          pagedResponseWrappersList.add(
              generatePagedResponseWrapper(context.asMethodContext(method), typeTable));
        }
      }
    }
    pagedResponseWrappers.pagedResponseWrapperList(pagedResponseWrappersList);

    pagedResponseWrappers.imports(importTypeTransformer.generateImports(typeTable.getImports()));

    Interface firstInterface = new InterfaceView().getElementIterable(model).iterator().next();
    String outputPath = pathMapper.getOutputPath(firstInterface, apiConfig);
    pagedResponseWrappers.outputPath(outputPath + File.separator + name + ".java");

    return pagedResponseWrappers.build();
  }

  private StaticLangPagedResponseView generatePagedResponseWrapper(
      MethodTransformerContext context, ModelTypeTable typeTable) {
    Method method = context.getMethod();
    Field resourceField = context.getMethodConfig().getPageStreaming().getResourcesField();

    StaticLangPagedResponseView.Builder pagedResponseWrapper =
        StaticLangPagedResponseView.newBuilder();

    String pagedResponseTypeName =
        context.getNamer().getPagedResponseTypeInnerName(method, typeTable, resourceField);
    pagedResponseWrapper.name(pagedResponseTypeName);
    pagedResponseWrapper.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    pagedResponseWrapper.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));
    pagedResponseWrapper.resourceTypeName(
        typeTable.getAndSaveNicknameForElementType(resourceField.getType()));
    pagedResponseWrapper.iterateMethods(getIterateMethods(context));

    return pagedResponseWrapper.build();
  }

  private List<PagedResponseIterateMethodView> getIterateMethods(MethodTransformerContext context) {

    List<PagedResponseIterateMethodView> iterateMethods = new ArrayList<>();

    Field resourceField = context.getMethodConfig().getPageStreaming().getResourcesField();

    if (context.getFeatureConfig().useResourceNameFormatOption(resourceField)) {
      PagedResponseIterateMethodView.Builder iterateMethod =
          PagedResponseIterateMethodView.newBuilder();

      String resourceShortName = ResourceNameUtil.getResourceName(resourceField);
      String resourceTypeName =
          context
              .getTypeTable()
              .getAndSaveNicknameForTypedResourceName(
                  resourceField, resourceField.getType().makeOptional(), resourceShortName);
      iterateMethod.overloadResourceTypeName(resourceTypeName);
      iterateMethod.overloadResourceTypeParseFunctionName(
          context.getNamer().publicMethodName(Name.from("parse")));
      iterateMethod.overloadResourceTypeIterateMethodName(
          context
              .getNamer()
              .getPagedResponseIterateMethod(context.getFeatureConfig(), resourceField));
      iterateMethod.iterateMethodName(context.getNamer().getPagedResponseIterateMethod());

      iterateMethods.add(iterateMethod.build());
    }

    return iterateMethods;
  }

  private StaticLangApiMethodView getExampleApiMethod(List<StaticLangApiMethodView> methods) {
    StaticLangApiMethodView exampleApiMethod = null;
    for (StaticLangApiMethodView method : methods) {
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

  private StaticLangXSettingsView generateXSettings(
      SurfaceTransformerContext context, StaticLangApiMethodView exampleApiMethod) {
    addXSettingsImports(context);

    StaticLangXSettingsView.Builder xsettingsClass = StaticLangXSettingsView.newBuilder();
    xsettingsClass.templateFileName(XSETTINGS_TEMPLATE_FILENAME);
    xsettingsClass.packageName(context.getApiConfig().getPackageName());
    xsettingsClass.doc(generateSettingsDoc(context, exampleApiMethod));
    String name = context.getNamer().getApiSettingsClassName(context.getInterface());
    xsettingsClass.name(name);
    ServiceConfig serviceConfig = new ServiceConfig();
    xsettingsClass.serviceAddress(serviceConfig.getServiceAddress(context.getInterface()));
    xsettingsClass.servicePort(serviceConfig.getServicePort());
    xsettingsClass.authScopes(serviceConfig.getAuthScopes(context.getInterface()));

    List<ApiCallSettingsView> apiCallSettings =
        apiCallableTransformer.generateCallSettings(context);
    xsettingsClass.callSettings(apiCallSettings);
    xsettingsClass.unaryCallSettings(unaryCallSettings(apiCallSettings));
    xsettingsClass.pageStreamingDescriptors(
        pageStreamingTransformer.generateDescriptorClasses(context));
    xsettingsClass.pagedListResponseFactories(
        pageStreamingTransformer.generateFactoryClasses(context));
    xsettingsClass.bundlingDescriptors(bundlingTransformer.generateDescriptorClasses(context));
    xsettingsClass.retryCodesDefinitions(
        retryDefinitionsTransformer.generateRetryCodesDefinitions(context));
    xsettingsClass.retryParamsDefinitions(
        retryDefinitionsTransformer.generateRetryParamsDefinitions(context));

    // must be done as the last step to catch all imports
    xsettingsClass.imports(
        importTypeTransformer.generateImports(context.getTypeTable().getImports()));

    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    xsettingsClass.outputPath(outputPath + "/" + name + ".java");

    return xsettingsClass.build();
  }

  private List<ApiCallSettingsView> unaryCallSettings(List<ApiCallSettingsView> callSettings) {
    ArrayList<ApiCallSettingsView> unaryCallSettings = new ArrayList<>();
    for (ApiCallSettingsView settingsView : callSettings) {
      if (!settingsView.isStreaming()) {
        unaryCallSettings.add(settingsView);
      }
    }
    return unaryCallSettings;
  }

  private PackageInfoView generatePackageInfo(
      Model model, ApiConfig apiConfig, List<ServiceDocView> serviceDocs) {
    PackageInfoView.Builder packageInfo = PackageInfoView.newBuilder();

    packageInfo.templateFileName(PACKAGE_INFO_TEMPLATE_FILENAME);

    packageInfo.serviceTitle(model.getServiceConfig().getTitle());
    packageInfo.serviceDocs(serviceDocs);
    packageInfo.packageName(apiConfig.getPackageName());
    packageInfo.handWrittenLocation(apiConfig.getHandWrittenLocation());

    Interface firstInterface = new InterfaceView().getElementIterable(model).iterator().next();
    String outputPath = pathMapper.getOutputPath(firstInterface, apiConfig);
    packageInfo.outputPath(outputPath + "/package-info.java");

    return packageInfo.build();
  }

  private void addXApiImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("com.google.api.gax.grpc.UnaryApiCallable");
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
    typeTable.saveNicknameFor("com.google.api.gax.grpc.UnaryApiCallSettings");
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
    addStreamingSettingsImportIfNecessary(context);
  }

  private void addStreamingSettingsImportIfNecessary(SurfaceTransformerContext context) {
    for (Method method : context.getSupportedMethods()) {
      if (MethodConfig.isGrpcStreamingMethod(method)) {
        context.getTypeTable().saveNicknameFor("com.google.api.gax.grpc.StreamingCallSettings");
        break;
      }
    }
  }

  public SettingsDocView generateSettingsDoc(
      SurfaceTransformerContext context, StaticLangApiMethodView exampleApiMethod) {
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

  private List<StaticLangApiMethodView> generateApiMethods(SurfaceTransformerContext context) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();

    for (Method method : context.getSupportedMethods()) {
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
      } else if (methodConfig.isGrpcStreaming()) {
        context.getTypeTable().saveNicknameFor("com.google.api.gax.grpc.StreamingApiCallable");
        apiMethods.add(apiMethodTransformer.generateCallableMethod(methodContext));
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
