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
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.ServiceConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.BundlingTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.RetryDefinitionsTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.StandardImportTypeTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.PackageInfoView;
import com.google.api.codegen.viewmodel.PagedResponseIterateMethodView;
import com.google.api.codegen.viewmodel.ServiceDocView;
import com.google.api.codegen.viewmodel.SettingsDocView;
import com.google.api.codegen.viewmodel.StaticLangApiFileView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangApiView;
import com.google.api.codegen.viewmodel.StaticLangPagedResponseView;
import com.google.api.codegen.viewmodel.StaticLangPagedResponseWrappersView;
import com.google.api.codegen.viewmodel.StaticLangSettingsFileView;
import com.google.api.codegen.viewmodel.StaticLangSettingsView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The ModelToViewTransformer to transform a Model into the standard GAPIC surface in Java. */
public class JavaGapicSurfaceTransformer implements ModelToViewTransformer {
  private final GapicCodePathMapper pathMapper;
  private final ServiceTransformer serviceTransformer = new ServiceTransformer();
  private final PathTemplateTransformer pathTemplateTransformer = new PathTemplateTransformer();
  private final ApiCallableTransformer apiCallableTransformer = new ApiCallableTransformer();
  private final ApiMethodTransformer apiMethodTransformer = new ApiMethodTransformer();
  private final PageStreamingTransformer pageStreamingTransformer = new PageStreamingTransformer();
  private final BundlingTransformer bundlingTransformer = new BundlingTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportTypeTransformer());
  private final RetryDefinitionsTransformer retryDefinitionsTransformer =
      new RetryDefinitionsTransformer();

  private static final String XAPI_TEMPLATE_FILENAME = "java/main.snip";
  private static final String XSETTINGS_TEMPLATE_FILENAME = "java/settings.snip";
  private static final String PACKAGE_INFO_TEMPLATE_FILENAME = "java/package-info.snip";
  private static final String PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME =
      "java/page_streaming_response.snip";

  public JavaGapicSurfaceTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
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
      boolean enableStringFormatFunctions = apiConfig.getResourceNameMessageConfigs().isEmpty();
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service,
              apiConfig,
              createTypeTable(apiConfig.getPackageName()),
              namer,
              JavaFeatureConfig.newBuilder()
                  .enableStringFormatFunctions(enableStringFormatFunctions)
                  .build());
      StaticLangApiFileView apiFile = generateApiFile(context);
      surfaceDocs.add(apiFile);

      serviceDocs.add(apiFile.api().doc());

      context =
          SurfaceTransformerContext.create(
              service,
              apiConfig,
              createTypeTable(apiConfig.getPackageName()),
              namer,
              JavaFeatureConfig.newBuilder()
                  .enableStringFormatFunctions(enableStringFormatFunctions)
                  .build());
      StaticLangApiMethodView exampleApiMethod = getExampleApiMethod(apiFile.api().apiMethods());
      StaticLangSettingsFileView settingsFile = generateSettingsFile(context, exampleApiMethod);
      surfaceDocs.add(settingsFile);
    }

    StaticLangPagedResponseWrappersView pagedResponseWrappers =
        generatePagedResponseWrappers(model, apiConfig);
    if (pagedResponseWrappers != null) {
      surfaceDocs.add(pagedResponseWrappers);
    }

    PackageInfoView packageInfo = generatePackageInfo(model, apiConfig, namer, serviceDocs);
    surfaceDocs.add(packageInfo);

    return surfaceDocs;
  }

  private ModelTypeTable createTypeTable(String implicitPackageName) {
    return new ModelTypeTable(
        new JavaTypeTable(implicitPackageName),
        new JavaModelTypeNameConverter(implicitPackageName));
  }

  private StaticLangApiFileView generateApiFile(SurfaceTransformerContext context) {
    StaticLangApiFileView.Builder apiFile = StaticLangApiFileView.newBuilder();

    apiFile.templateFileName(XAPI_TEMPLATE_FILENAME);

    apiFile.api(generateApiClass(context));

    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    String className = context.getNamer().getApiWrapperClassName(context.getInterface());
    apiFile.outputPath(outputPath + File.separator + className + ".java");

    // must be done as the last step to catch all imports
    apiFile.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return apiFile.build();
  }

  private StaticLangApiView generateApiClass(SurfaceTransformerContext context) {
    addApiImports(context);

    List<StaticLangApiMethodView> methods = generateApiMethods(context);

    StaticLangApiView.Builder xapiClass = StaticLangApiView.newBuilder();

    ApiMethodView exampleApiMethod = getExampleApiMethod(methods);
    xapiClass.doc(serviceTransformer.generateServiceDoc(context, exampleApiMethod));

    String name = context.getNamer().getApiWrapperClassName(context.getInterface());
    xapiClass.releaseAnnotation(
        context.getNamer().getReleaseAnnotation(context.getApiConfig().getReleaseLevel()));
    xapiClass.name(name);
    xapiClass.settingsClassName(context.getNamer().getApiSettingsClassName(context.getInterface()));
    xapiClass.apiCallableMembers(apiCallableTransformer.generateStaticLangApiCallables(context));
    xapiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    xapiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    xapiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    xapiClass.apiMethods(methods);
    xapiClass.hasDefaultInstance(context.getInterfaceConfig().hasDefaultInstance());
    xapiClass.hasLongRunningOperations(context.getInterfaceConfig().hasLongRunningOperations());

    return xapiClass.build();
  }

  private StaticLangPagedResponseWrappersView generatePagedResponseWrappers(
      Model model, ApiConfig apiConfig) {

    SurfaceNamer namer = new JavaSurfaceNamer(apiConfig.getPackageName());
    ModelTypeTable typeTable = createTypeTable(apiConfig.getPackageName());

    addPagedResponseWrapperImports(typeTable);

    StaticLangPagedResponseWrappersView.Builder pagedResponseWrappers =
        StaticLangPagedResponseWrappersView.newBuilder();

    pagedResponseWrappers.templateFileName(PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME);

    String name = namer.getPagedResponseWrappersClassName();
    pagedResponseWrappers.name(name);

    List<StaticLangPagedResponseView> pagedResponseWrappersList = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service,
              apiConfig,
              typeTable,
              namer,
              JavaFeatureConfig.newBuilder()
                  .enableStringFormatFunctions(apiConfig.getResourceNameMessageConfigs().isEmpty())
                  .build());
      for (Method method : context.getSupportedMethods()) {
        if (context.getMethodConfig(method).isPageStreaming()) {
          pagedResponseWrappersList.add(
              generatePagedResponseWrapper(context.asRequestMethodContext(method), typeTable));
        }
      }
    }

    if (pagedResponseWrappersList.size() == 0) {
      return null;
    }

    pagedResponseWrappers.pagedResponseWrapperList(pagedResponseWrappersList);

    // must be done as the last step to catch all imports
    pagedResponseWrappers.fileHeader(
        fileHeaderTransformer.generateFileHeader(apiConfig, typeTable.getImports(), namer));

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

    SurfaceNamer namer = context.getNamer();

    List<PagedResponseIterateMethodView> iterateMethods = new ArrayList<>();

    FieldConfig resourceFieldConfig =
        context.getMethodConfig().getPageStreaming().getResourcesFieldConfig();

    if (context.getFeatureConfig().useResourceNameFormatOption(resourceFieldConfig)) {

      String resourceTypeName =
          namer.getAndSaveElementResourceTypeName(context.getTypeTable(), resourceFieldConfig);
      String resourceTypeIterateMethodName =
          namer.getPagedResponseIterateMethod(context.getFeatureConfig(), resourceFieldConfig);
      String parseMethodName =
          namer.getResourceTypeParseMethodName(context.getTypeTable(), resourceFieldConfig);

      PagedResponseIterateMethodView.Builder iterateMethod =
          PagedResponseIterateMethodView.newBuilder()
              .overloadResourceTypeName(resourceTypeName)
              .overloadResourceTypeParseFunctionName(parseMethodName)
              .overloadResourceTypeIterateMethodName(resourceTypeIterateMethodName)
              .iterateMethodName(namer.getPagedResponseIterateMethod());

      iterateMethods.add(iterateMethod.build());
    }

    return iterateMethods;
  }

  private StaticLangApiMethodView getExampleApiMethod(List<StaticLangApiMethodView> methods) {
    StaticLangApiMethodView exampleApiMethod =
        searchExampleMethod(methods, ClientMethodType.FlattenedMethod);
    if (exampleApiMethod == null) {
      exampleApiMethod = searchExampleMethod(methods, ClientMethodType.PagedFlattenedMethod);
    }
    if (exampleApiMethod == null) {
      exampleApiMethod = searchExampleMethod(methods, ClientMethodType.RequestObjectMethod);
    }
    if (exampleApiMethod == null) {
      throw new RuntimeException("Could not find method to use as an example method");
    }
    return exampleApiMethod;
  }

  private StaticLangApiMethodView searchExampleMethod(
      List<StaticLangApiMethodView> methods, ClientMethodType methodType) {
    for (StaticLangApiMethodView method : methods) {
      if (method.type().equals(methodType)) {
        return method;
      }
    }
    return null;
  }

  private StaticLangSettingsFileView generateSettingsFile(
      SurfaceTransformerContext context, StaticLangApiMethodView exampleApiMethod) {
    StaticLangSettingsFileView.Builder settingsFile = StaticLangSettingsFileView.newBuilder();

    settingsFile.settings(generateSettingsClass(context, exampleApiMethod));
    settingsFile.templateFileName(XSETTINGS_TEMPLATE_FILENAME);

    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    String className = context.getNamer().getApiSettingsClassName(context.getInterface());
    settingsFile.outputPath(outputPath + "/" + className + ".java");

    // must be done as the last step to catch all imports
    settingsFile.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return settingsFile.build();
  }

  private StaticLangSettingsView generateSettingsClass(
      SurfaceTransformerContext context, StaticLangApiMethodView exampleApiMethod) {
    addSettingsImports(context);

    StaticLangSettingsView.Builder xsettingsClass = StaticLangSettingsView.newBuilder();
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
    xsettingsClass.pageStreamingDescriptors(
        pageStreamingTransformer.generateDescriptorClasses(context));
    xsettingsClass.pagedListResponseFactories(
        pageStreamingTransformer.generateFactoryClasses(context));
    xsettingsClass.bundlingDescriptors(bundlingTransformer.generateDescriptorClasses(context));
    xsettingsClass.retryCodesDefinitions(
        retryDefinitionsTransformer.generateRetryCodesDefinitions(context));
    xsettingsClass.retryParamsDefinitions(
        retryDefinitionsTransformer.generateRetryParamsDefinitions(context));
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    xsettingsClass.hasDefaultServiceAddress(interfaceConfig.hasDefaultServiceAddress());
    xsettingsClass.hasDefaultServiceScopes(interfaceConfig.hasDefaultServiceScopes());
    xsettingsClass.hasDefaultInstance(interfaceConfig.hasDefaultInstance());

    return xsettingsClass.build();
  }

  private PackageInfoView generatePackageInfo(
      Model model, ApiConfig apiConfig, SurfaceNamer namer, List<ServiceDocView> serviceDocs) {
    PackageInfoView.Builder packageInfo = PackageInfoView.newBuilder();

    packageInfo.templateFileName(PACKAGE_INFO_TEMPLATE_FILENAME);

    packageInfo.serviceTitle(model.getServiceConfig().getTitle());
    packageInfo.serviceDocs(serviceDocs);
    packageInfo.domainLayerLocation(apiConfig.getDomainLayerLocation());

    packageInfo.fileHeader(
        fileHeaderTransformer.generateFileHeader(
            apiConfig, Collections.<String, TypeAlias>emptyMap(), namer));

    Interface firstInterface = new InterfaceView().getElementIterable(model).iterator().next();
    String outputPath = pathMapper.getOutputPath(firstInterface, apiConfig);
    packageInfo.outputPath(outputPath + "/package-info.java");

    return packageInfo.build();
  }

  private void addApiImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("com.google.api.gax.grpc.ChannelAndExecutor");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.UnaryCallable");
    typeTable.saveNicknameFor("com.google.api.gax.protobuf.PathTemplate");
    typeTable.saveNicknameFor("com.google.protobuf.ExperimentalApi");
    typeTable.saveNicknameFor("io.grpc.ManagedChannel");
    typeTable.saveNicknameFor("java.io.Closeable");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.util.ArrayList");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.concurrent.ScheduledExecutorService");
    typeTable.saveNicknameFor("javax.annotation.Generated");

    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      typeTable.saveNicknameFor("com.google.api.gax.grpc.OperationFuture");
      typeTable.saveNicknameFor("com.google.api.gax.grpc.FixedChannelProvider");
      typeTable.saveNicknameFor("com.google.api.gax.grpc.FixedExecutorProvider");
      typeTable.saveNicknameFor("com.google.longrunning.OperationsClient");
      typeTable.saveNicknameFor("com.google.longrunning.OperationsSettings");
    }
  }

  private void addSettingsImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("com.google.api.gax.core.CredentialsProvider");
    typeTable.saveNicknameFor("com.google.api.gax.core.GoogleCredentialsProvider");
    typeTable.saveNicknameFor("com.google.api.gax.core.RetrySettings");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.ChannelProvider");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.ClientSettings");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.ExecutorProvider");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.InstantiatingChannelProvider");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.InstantiatingExecutorProvider");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.SimpleCallSettings");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.UnaryCallSettings");
    typeTable.saveNicknameFor("com.google.auth.Credentials");
    typeTable.saveNicknameFor("com.google.common.collect.ImmutableList");
    typeTable.saveNicknameFor("com.google.common.collect.ImmutableMap");
    typeTable.saveNicknameFor("com.google.common.collect.ImmutableSet");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("com.google.common.collect.Sets");
    typeTable.saveNicknameFor("com.google.protobuf.ExperimentalApi");
    typeTable.saveNicknameFor("io.grpc.ManagedChannel");
    typeTable.saveNicknameFor("io.grpc.Status");
    typeTable.saveNicknameFor("org.joda.time.Duration");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.concurrent.ScheduledExecutorService");
    typeTable.saveNicknameFor("javax.annotation.Generated");

    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    if (interfaceConfig.hasPageStreamingMethods()) {
      typeTable.saveNicknameFor("com.google.api.gax.grpc.CallContext");
      typeTable.saveNicknameFor("com.google.api.gax.grpc.PagedCallSettings");
      typeTable.saveNicknameFor("com.google.api.gax.grpc.PagedListDescriptor");
      typeTable.saveNicknameFor("com.google.api.gax.grpc.PagedListResponseFactory");
      typeTable.saveNicknameFor("com.google.api.gax.grpc.UnaryCallable");
    }
    if (interfaceConfig.hasBundlingMethods()) {
      typeTable.saveNicknameFor("com.google.api.gax.grpc.BundlingCallSettings");
      typeTable.saveNicknameFor("com.google.api.gax.grpc.BundlingDescriptor");
      typeTable.saveNicknameFor("com.google.api.gax.grpc.BundlingSettings");
      typeTable.saveNicknameFor("com.google.api.gax.grpc.RequestIssuer");
      typeTable.saveNicknameFor("java.util.ArrayList");
      typeTable.saveNicknameFor("java.util.Collection");
    }
    if (interfaceConfig.hasGrpcStreamingMethods()) {
      typeTable.saveNicknameFor("com.google.api.gax.grpc.StreamingCallSettings");
    }
    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      typeTable.saveNicknameFor("com.google.api.gax.grpc.OperationCallSettings");
    }
  }

  private void addPagedResponseWrapperImports(ModelTypeTable typeTable) {
    typeTable.saveNicknameFor("com.google.api.gax.grpc.CallContext");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.PagedListDescriptor");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.PagedListResponseImpl");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.UnaryCallable");
    typeTable.saveNicknameFor("com.google.common.base.Function");
    typeTable.saveNicknameFor("com.google.common.collect.Iterables");
    typeTable.saveNicknameFor("com.google.protobuf.ExperimentalApi");
    typeTable.saveNicknameFor("javax.annotation.Generated");
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
    settingsDoc.hasDefaultInstance(context.getInterfaceConfig().hasDefaultInstance());
    return settingsDoc.build();
  }

  private List<StaticLangApiMethodView> generateApiMethods(SurfaceTransformerContext context) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();

    for (Method method : context.getSupportedMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      MethodTransformerContext requestMethodContext = context.asRequestMethodContext(method);

      if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            MethodTransformerContext flattenedMethodContext =
                context.asFlattenedMethodContext(method, flatteningGroup);
            apiMethods.add(
                apiMethodTransformer.generatePagedFlattenedMethod(flattenedMethodContext));
          }
        }
        apiMethods.add(apiMethodTransformer.generatePagedRequestObjectMethod(requestMethodContext));
        apiMethods.add(apiMethodTransformer.generatePagedCallableMethod(requestMethodContext));
        apiMethods.add(
            apiMethodTransformer.generateUnpagedListCallableMethod(requestMethodContext));
      } else if (methodConfig.isGrpcStreaming()) {
        context.getTypeTable().saveNicknameFor("com.google.api.gax.grpc.StreamingCallable");
        apiMethods.add(apiMethodTransformer.generateCallableMethod(requestMethodContext));
      } else if (methodConfig.isLongRunningOperation()) {
        context.getTypeTable().saveNicknameFor("com.google.api.gax.grpc.OperationCallable");
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            MethodTransformerContext flattenedMethodContext =
                context.asFlattenedMethodContext(method, flatteningGroup);
            apiMethods.add(
                apiMethodTransformer.generateAsyncOperationFlattenedMethod(flattenedMethodContext));
          }
        }
        apiMethods.add(
            apiMethodTransformer.generateAsyncOperationRequestObjectMethod(requestMethodContext));
        apiMethods.add(apiMethodTransformer.generateOperationCallableMethod(requestMethodContext));
        apiMethods.add(apiMethodTransformer.generateCallableMethod(requestMethodContext));
      } else {
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            MethodTransformerContext flattenedMethodContext =
                context.asFlattenedMethodContext(method, flatteningGroup);
            apiMethods.add(apiMethodTransformer.generateFlattenedMethod(flattenedMethodContext));
          }
        }
        apiMethods.add(apiMethodTransformer.generateRequestObjectMethod(requestMethodContext));
        apiMethods.add(apiMethodTransformer.generateCallableMethod(requestMethodContext));
      }
    }

    return apiMethods;
  }
}
