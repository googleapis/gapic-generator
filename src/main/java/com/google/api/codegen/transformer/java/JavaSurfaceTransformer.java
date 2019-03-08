/* Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProductServiceConfig;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.config.TransportProtocol;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.BatchingTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.RetryDefinitionsTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformer;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.PackageInfoView;
import com.google.api.codegen.viewmodel.PagedResponseIterateMethodView;
import com.google.api.codegen.viewmodel.ServiceDocView;
import com.google.api.codegen.viewmodel.SettingsDocView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangApiView;
import com.google.api.codegen.viewmodel.StaticLangCallableFactoryView;
import com.google.api.codegen.viewmodel.StaticLangFileView;
import com.google.api.codegen.viewmodel.StaticLangPagedResponseView;
import com.google.api.codegen.viewmodel.StaticLangRpcStubView;
import com.google.api.codegen.viewmodel.StaticLangSettingsView;
import com.google.api.codegen.viewmodel.StaticLangStubInterfaceView;
import com.google.api.codegen.viewmodel.StaticLangStubSettingsView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** A transformer to transform an ApiModel into the standard GAPIC surface in Java. */
public class JavaSurfaceTransformer {
  private final GapicCodePathMapper pathMapper;

  // TODO: Figure out a way to simplify the transformers in a way that reduces duplication and makes
  // it easy to follow the code.
  private final SurfaceTransformer surfaceTransformer;
  private final String rpcStubTemplateFilename;
  private final String callableFactoryTemplateFilename;

  private final ServiceTransformer serviceTransformer = new ServiceTransformer();
  private final PathTemplateTransformer pathTemplateTransformer = new PathTemplateTransformer();
  private final ApiCallableTransformer apiCallableTransformer = new ApiCallableTransformer();
  private final JavaMethodViewGenerator methodGenerator =
      new JavaMethodViewGenerator(SampleType.IN_CODE);
  private final PageStreamingTransformer pageStreamingTransformer = new PageStreamingTransformer();
  private final BatchingTransformer batchingTransformer = new BatchingTransformer();
  private final StandardImportSectionTransformer importSectionTransformer =
      new StandardImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final RetryDefinitionsTransformer retryDefinitionsTransformer =
      new RetryDefinitionsTransformer();
  private final ProductServiceConfig productServiceConfig = new ProductServiceConfig();

  private static final String API_TEMPLATE_FILENAME = "java/main.snip";
  private static final String SETTINGS_TEMPLATE_FILENAME = "java/settings.snip";
  private static final String STUB_SETTINGS_TEMPLATE_FILENAME = "java/stub_settings.snip";
  private static final String STUB_INTERFACE_TEMPLATE_FILENAME = "java/stub_interface.snip";

  private static final String PACKAGE_INFO_TEMPLATE_FILENAME = "java/package-info.snip";

  public JavaSurfaceTransformer(
      GapicCodePathMapper pathMapper,
      SurfaceTransformer surfaceTransformer,
      String rpcStubSnippetFileName,
      String callableFactoryTemplateFilename) {
    this.pathMapper = Preconditions.checkNotNull(pathMapper);
    this.surfaceTransformer = Preconditions.checkNotNull(surfaceTransformer);
    this.rpcStubTemplateFilename = Preconditions.checkNotNull(rpcStubSnippetFileName);
    this.callableFactoryTemplateFilename =
        Preconditions.checkNotNull(callableFactoryTemplateFilename);
  }

  public List<ViewModel> transform(ApiModel model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = surfaceTransformer.createSurfaceNamer(productConfig);

    List<ServiceDocView> serviceDocs = new ArrayList<>();
    for (InterfaceModel apiInterface : model.getInterfaces(productConfig)) {
      if (!productConfig.hasInterfaceConfig(apiInterface)) {
        continue;
      }

      ImportTypeTable typeTable =
          surfaceTransformer.createTypeTable(productConfig.getPackageName());
      InterfaceContext context =
          surfaceTransformer.createInterfaceContext(apiInterface, productConfig, namer, typeTable);
      StaticLangFileView<StaticLangApiView> apiFile = generateApiFile(context, productConfig);
      surfaceDocs.add(apiFile);

      serviceDocs.add(apiFile.classView().doc());

      StaticLangApiMethodView exampleApiMethod =
          getExampleApiMethod(apiFile.classView().apiMethods());

      context = context.withNewTypeTable(namer.getStubPackageName());
      StaticLangFileView<StaticLangStubSettingsView> stubSettingsFile =
          generateStubSettingsFile(context, productConfig, exampleApiMethod);

      context = context.withNewTypeTable(namer.getRootPackageName());
      StaticLangFileView<StaticLangSettingsView> settingsFile =
          generateSettingsFile(
              context, productConfig, exampleApiMethod, stubSettingsFile.classView());
      surfaceDocs.add(settingsFile);
      surfaceDocs.add(stubSettingsFile);

      context = context.withNewTypeTable(namer.getStubPackageName());
      StaticLangFileView<StaticLangStubInterfaceView> stubInterfaceFile =
          generateStubInterfaceFile(context, productConfig);
      surfaceDocs.add(stubInterfaceFile);

      context = context.withNewTypeTable(namer.getStubPackageName());
      StaticLangFileView<StaticLangRpcStubView> grpcStubFile =
          generateRpcStubClassFile(context, productConfig);
      surfaceDocs.add(grpcStubFile);
      surfaceDocs.add(generateCallableFactoryClassFile(context, productConfig));
    }

    PackageInfoView packageInfo = generatePackageInfo(model, productConfig, namer, serviceDocs);
    surfaceDocs.add(packageInfo);

    return surfaceDocs;
  }

  private StaticLangFileView<StaticLangApiView> generateApiFile(
      InterfaceContext context, GapicProductConfig productConfig) {
    StaticLangFileView.Builder<StaticLangApiView> apiFile = StaticLangFileView.newBuilder();

    apiFile.templateFileName(API_TEMPLATE_FILENAME);

    apiFile.classView(generateApiClass(context, productConfig));

    String outputPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    String className = context.getNamer().getApiWrapperClassName(context.getInterfaceConfig());
    apiFile.outputPath(outputPath + File.separator + className + ".java");

    // must be done as the last step to catch all imports
    apiFile.fileHeader(fileHeaderTransformer.generateFileHeader(context, className));

    return apiFile.build();
  }

  private StaticLangApiView generateApiClass(
      InterfaceContext context, GapicProductConfig productConfig) {
    SurfaceNamer namer = context.getNamer();
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();

    addApiImports(context);

    List<StaticLangApiMethodView> methods = methodGenerator.generateApiMethods(context);

    StaticLangApiView.Builder xapiClass = StaticLangApiView.newBuilder();

    ApiMethodView exampleApiMethod = getExampleApiMethod(methods);
    xapiClass.doc(serviceTransformer.generateServiceDoc(context, exampleApiMethod, productConfig));

    String name = context.getNamer().getApiWrapperClassName(context.getInterfaceConfig());
    xapiClass.releaseLevelAnnotation(namer.getReleaseAnnotation(productConfig.getReleaseLevel()));
    xapiClass.name(name);
    xapiClass.settingsClassName(namer.getApiSettingsClassName(interfaceConfig));
    xapiClass.stubInterfaceName(
        getAndSaveNicknameForStubType(context, namer.getApiStubInterfaceName(interfaceConfig)));
    xapiClass.stubSettingsClassName(
        getAndSaveNicknameForStubType(context, namer.getApiStubSettingsClassName(interfaceConfig)));
    xapiClass.apiCallableMembers(apiCallableTransformer.generateStaticLangApiCallables(context));
    xapiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    xapiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    xapiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    xapiClass.apiMethods(methods);
    xapiClass.hasDefaultInstance(interfaceConfig.hasDefaultInstance());
    xapiClass.hasLongRunningOperations(interfaceConfig.hasLongRunningOperations());
    xapiClass.pagedResponseViews(generatePagedResponseWrappers(context));
    return xapiClass.build();
  }

  private List<StaticLangPagedResponseView> generatePagedResponseWrappers(
      InterfaceContext context) {
    addPagedResponseWrapperImports(context.getImportTypeTable());

    ImmutableList.Builder<StaticLangPagedResponseView> pagedResponseWrappersList =
        ImmutableList.builder();

    for (MethodModel method : context.getSupportedMethods()) {
      if (context.getMethodConfig(method).isPageStreaming()) {
        pagedResponseWrappersList.add(
            generatePagedResponseWrapper(
                context.asRequestMethodContext(method), context.getImportTypeTable()));
      }
    }

    return pagedResponseWrappersList.build();
  }

  private StaticLangPagedResponseView generatePagedResponseWrapper(
      MethodContext context, ImportTypeTable typeTable) {
    MethodModel method = context.getMethodModel();
    FieldModel resourceField = context.getMethodConfig().getPageStreaming().getResourcesField();

    StaticLangPagedResponseView.Builder pagedResponseWrapper =
        StaticLangPagedResponseView.newBuilder();

    String pagedResponseTypeName =
        context.getNamer().getPagedResponseTypeInnerName(method, typeTable, resourceField);
    pagedResponseWrapper.pagedResponseTypeName(pagedResponseTypeName);
    pagedResponseWrapper.pageTypeName(
        context.getNamer().getPageTypeInnerName(method, typeTable, resourceField));
    pagedResponseWrapper.fixedSizeCollectionTypeName(
        context.getNamer().getFixedSizeCollectionTypeInnerName(method, typeTable, resourceField));
    pagedResponseWrapper.requestTypeName(
        method.getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer()));
    pagedResponseWrapper.responseTypeName(
        method.getAndSaveResponseTypeName(context.getTypeTable(), context.getNamer()));
    pagedResponseWrapper.resourceTypeName(
        typeTable.getAndSaveNicknameForElementType(resourceField));
    pagedResponseWrapper.iterateMethods(getIterateMethods(context));

    return pagedResponseWrapper.build();
  }

  private List<PagedResponseIterateMethodView> getIterateMethods(MethodContext context) {

    SurfaceNamer namer = context.getNamer();

    List<PagedResponseIterateMethodView> iterateMethods = new ArrayList<>();

    FieldConfig resourceFieldConfig =
        context.getMethodConfig().getPageStreaming().getResourcesFieldConfig();

    if (context.getFeatureConfig().useResourceNameFormatOption(resourceFieldConfig)) {

      String resourceTypeName =
          namer.getAndSaveElementResourceTypeName(context.getTypeTable(), resourceFieldConfig);
      String resourceTypeIterateMethodName =
          namer.getPagedResponseIterateMethod(context.getFeatureConfig(), resourceFieldConfig);
      String resourceTypeGetValuesMethodName =
          namer.getPageGetValuesMethod(context.getFeatureConfig(), resourceFieldConfig);
      String parseMethodName =
          namer.getResourceTypeParseMethodName(context.getTypeTable(), resourceFieldConfig);

      PagedResponseIterateMethodView.Builder iterateMethod =
          PagedResponseIterateMethodView.newBuilder()
              .overloadResourceTypeName(resourceTypeName)
              .overloadResourceTypeParseFunctionName(parseMethodName)
              .overloadResourceTypeIterateMethodName(resourceTypeIterateMethodName)
              .overloadResourceTypeGetValuesMethodName(resourceTypeGetValuesMethodName)
              .iterateMethodName(namer.getPagedResponseIterateMethod())
              .getValuesMethodName(namer.getPageGetValuesMethod());

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
      exampleApiMethod =
          searchExampleMethod(methods, ClientMethodType.AsyncOperationFlattenedMethod);
    }
    if (exampleApiMethod == null) {
      exampleApiMethod = searchExampleMethod(methods, ClientMethodType.CallableMethod);
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

  private StaticLangFileView<StaticLangStubSettingsView> generateStubSettingsFile(
      InterfaceContext context,
      GapicProductConfig productConfig,
      StaticLangApiMethodView exampleApiMethod) {
    StaticLangFileView.Builder<StaticLangStubSettingsView> settingsFile =
        StaticLangFileView.newBuilder();

    settingsFile.classView(generateStubSettingsClass(context, productConfig, exampleApiMethod));
    settingsFile.templateFileName(STUB_SETTINGS_TEMPLATE_FILENAME);

    String outputPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    String className = context.getNamer().getApiStubSettingsClassName(context.getInterfaceConfig());
    settingsFile.outputPath(outputPath + File.separator + className + ".java");

    // must be done as the last step to catch all imports
    settingsFile.fileHeader(fileHeaderTransformer.generateFileHeader(context, className));

    return settingsFile.build();
  }

  private StaticLangFileView<StaticLangSettingsView> generateSettingsFile(
      InterfaceContext context,
      GapicProductConfig productConfig,
      StaticLangApiMethodView exampleApiMethod,
      StaticLangStubSettingsView stubSettingsView) {
    StaticLangFileView.Builder<StaticLangSettingsView> settingsFile =
        StaticLangFileView.newBuilder();

    settingsFile.classView(
        generateSettingsClass(context, productConfig, exampleApiMethod, stubSettingsView));
    settingsFile.templateFileName(SETTINGS_TEMPLATE_FILENAME);

    String outputPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    String className = context.getNamer().getApiSettingsClassName(context.getInterfaceConfig());
    settingsFile.outputPath(outputPath + File.separator + className + ".java");

    // must be done as the last step to catch all imports
    settingsFile.fileHeader(fileHeaderTransformer.generateFileHeader(context, className));

    return settingsFile.build();
  }

  private StaticLangSettingsView generateSettingsClass(
      InterfaceContext context,
      GapicProductConfig productConfig,
      StaticLangApiMethodView exampleApiMethod,
      StaticLangStubSettingsView stubSettingsView) {
    addSettingsImports(context);

    SurfaceNamer namer = context.getNamer();
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();

    StaticLangSettingsView.Builder xsettingsClass = StaticLangSettingsView.newBuilder();
    String name = namer.getApiSettingsClassName(context.getInterfaceConfig());
    xsettingsClass.doc(
        generateSettingsDoc(
            context,
            exampleApiMethod,
            productConfig,
            name,
            namer.getApiWrapperClassName(context.getInterfaceConfig())));

    xsettingsClass.name(name);
    xsettingsClass.releaseLevelAnnotation(stubSettingsView.releaseLevelAnnotation());
    xsettingsClass.serviceHostname(stubSettingsView.serviceHostname());
    xsettingsClass.servicePort(stubSettingsView.servicePort());
    xsettingsClass.authScopes(stubSettingsView.authScopes());

    xsettingsClass.callSettings(apiCallableTransformer.generateCallSettings(context));
    xsettingsClass.pageStreamingDescriptors(stubSettingsView.pageStreamingDescriptors());
    xsettingsClass.batchingDescriptors(stubSettingsView.batchingDescriptors());
    xsettingsClass.retryCodesDefinitions(stubSettingsView.retryCodesDefinitions());
    xsettingsClass.hasDefaultServiceAddress(stubSettingsView.hasDefaultServiceAddress());
    xsettingsClass.hasDefaultServiceScopes(stubSettingsView.hasDefaultServiceScopes());
    xsettingsClass.hasDefaultInstance(stubSettingsView.hasDefaultInstance());
    xsettingsClass.retryParamsDefinitions(stubSettingsView.retryParamsDefinitions());
    xsettingsClass.instantiatingChannelProvider(stubSettingsView.instantiatingChannelProvider());
    xsettingsClass.transportProtocol(stubSettingsView.transportProtocol());
    xsettingsClass.useDefaultServicePortInEndpoint(
        stubSettingsView.useDefaultServicePortInEndpoint());
    xsettingsClass.defaultTransportProviderBuilder(
        stubSettingsView.defaultTransportProviderBuilder());
    xsettingsClass.stubSettingsName(
        getAndSaveNicknameForStubType(context, namer.getApiStubSettingsClassName(interfaceConfig)));

    return xsettingsClass.build();
  }

  private StaticLangStubSettingsView generateStubSettingsClass(
      InterfaceContext context,
      GapicProductConfig productConfig,
      StaticLangApiMethodView exampleApiMethod) {
    addSettingsImports(context);

    SurfaceNamer namer = context.getNamer();
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    ApiModel model = context.getApiModel();

    StaticLangStubSettingsView.Builder xsettingsClass = StaticLangStubSettingsView.newBuilder();
    xsettingsClass.releaseLevelAnnotation(
        context.getNamer().getReleaseAnnotation(productConfig.getReleaseLevel()));
    xsettingsClass.doc(
        generateSettingsDoc(
            context,
            exampleApiMethod,
            productConfig,
            context.getNamer().getApiStubSettingsClassName(interfaceConfig),
            namer.getApiStubInterfaceName(context.getInterfaceConfig())));
    String name = namer.getApiStubSettingsClassName(context.getInterfaceConfig());
    xsettingsClass.name(name);
    xsettingsClass.serviceHostname(
        productServiceConfig.getServiceHostname(context.getServiceAddress()));
    xsettingsClass.servicePort(productServiceConfig.getServicePort(context.getServiceAddress()));
    xsettingsClass.authScopes(model.getAuthScopes(productConfig));
    if (productConfig.getTransportProtocol().equals(TransportProtocol.HTTP)) {
      xsettingsClass.useDefaultServicePortInEndpoint(false);
    }

    xsettingsClass.transportProtocol(productConfig.getTransportProtocol());
    xsettingsClass.rpcTransportName(
        namer.getTransportClassName(productConfig.getTransportProtocol()));
    xsettingsClass.transportNameGetter(
        namer.getTransporNameGetMethod(productConfig.getTransportProtocol()));
    xsettingsClass.defaultTransportProviderBuilder(
        namer.getDefaultTransportProviderBuilder(productConfig.getTransportProtocol()));
    xsettingsClass.transportProvider(
        namer.getTransportProvider(productConfig.getTransportProtocol()));
    xsettingsClass.instantiatingChannelProvider(
        namer.getInstantiatingChannelProvider(productConfig.getTransportProtocol()));

    List<ApiCallSettingsView> apiCallSettings =
        apiCallableTransformer.generateCallSettings(context);
    xsettingsClass.callSettings(apiCallSettings);
    xsettingsClass.pageStreamingDescriptors(
        pageStreamingTransformer.generateDescriptorClasses(context));
    xsettingsClass.pagedListResponseFactories(
        pageStreamingTransformer.generateFactoryClasses(context));
    xsettingsClass.batchingDescriptors(batchingTransformer.generateDescriptorClasses(context));
    xsettingsClass.retryCodesDefinitions(
        retryDefinitionsTransformer.generateRetryCodesDefinitions(context));
    xsettingsClass.retryParamsDefinitions(
        retryDefinitionsTransformer.generateRetryParamsDefinitions(context));
    xsettingsClass.transportProtocol(productConfig.getTransportProtocol());

    xsettingsClass.hasDefaultServiceAddress(interfaceConfig.hasDefaultServiceAddress());
    xsettingsClass.hasDefaultServiceScopes(interfaceConfig.hasDefaultServiceScopes());
    xsettingsClass.hasDefaultInstance(interfaceConfig.hasDefaultInstance());
    xsettingsClass.stubInterfaceName(
        getAndSaveNicknameForStubType(context, namer.getApiStubInterfaceName(interfaceConfig)));
    xsettingsClass.rpcStubClassName(
        getAndSaveNicknameForStubType(
            context,
            namer.getApiRpcStubClassName(
                interfaceConfig.getInterfaceModel(), productConfig.getTransportProtocol())));

    return xsettingsClass.build();
  }

  private StaticLangFileView<StaticLangStubInterfaceView> generateStubInterfaceFile(
      InterfaceContext context, GapicProductConfig productConfig) {
    StaticLangFileView.Builder<StaticLangStubInterfaceView> fileView =
        StaticLangFileView.newBuilder();

    fileView.classView(generateStubInterface(context, productConfig));
    fileView.templateFileName(STUB_INTERFACE_TEMPLATE_FILENAME);

    String outputPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    String className = context.getNamer().getApiStubInterfaceName(context.getInterfaceConfig());
    fileView.outputPath(outputPath + File.separator + className + ".java");

    // must be done as the last step to catch all imports
    fileView.fileHeader(fileHeaderTransformer.generateFileHeader(context, className));

    return fileView.build();
  }

  private StaticLangStubInterfaceView generateStubInterface(
      InterfaceContext context, GapicProductConfig productConfig) {
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();

    addStubInterfaceImports(context);

    // Stub class has different default package name from methods classes.
    InterfaceContext apiMethodsContext =
        context.withNewTypeTable(context.getNamer().getRootPackageName());
    List<StaticLangApiMethodView> methods = methodGenerator.generateApiMethods(apiMethodsContext);
    for (TypeAlias alias :
        apiMethodsContext.getImportTypeTable().getTypeTable().getAllImports().values()) {
      context.getImportTypeTable().getAndSaveNicknameFor(alias);
    }

    StaticLangStubInterfaceView.Builder stubInterface = StaticLangStubInterfaceView.newBuilder();

    stubInterface.doc(serviceTransformer.generateServiceDoc(context, null, productConfig));

    String name = context.getNamer().getApiStubInterfaceName(context.getInterfaceConfig());
    stubInterface.releaseLevelAnnotation(
        context.getNamer().getReleaseAnnotation(productConfig.getReleaseLevel()));
    stubInterface.name(name);
    stubInterface.callableMethods(filterIncludeCallableMethods(methods));
    stubInterface.hasLongRunningOperations(interfaceConfig.hasLongRunningOperations());

    return stubInterface.build();
  }

  private StaticLangFileView<StaticLangRpcStubView> generateRpcStubClassFile(
      InterfaceContext context, GapicProductConfig productConfig) {
    StaticLangFileView.Builder<StaticLangRpcStubView> fileView = StaticLangFileView.newBuilder();

    fileView.classView(generateRpcStubClass(context, productConfig));
    fileView.templateFileName(rpcStubTemplateFilename);

    String outputPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    String className =
        context
            .getNamer()
            .getApiRpcStubClassName(
                context.getInterfaceConfig().getInterfaceModel(),
                productConfig.getTransportProtocol());
    fileView.outputPath(outputPath + File.separator + className + ".java");

    // must be done as the last step to catch all imports
    fileView.fileHeader(fileHeaderTransformer.generateFileHeader(context, className));

    return fileView.build();
  }

  private StaticLangRpcStubView generateRpcStubClass(
      InterfaceContext context, GapicProductConfig productConfig) {
    SurfaceNamer namer = context.getNamer();
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();

    addRpcStubImports(context);

    // Stub class has different default package name from method, request, and resource classes.
    InterfaceContext apiMethodsContext =
        context.withNewTypeTable(context.getNamer().getRootPackageName());
    List<StaticLangApiMethodView> methods = methodGenerator.generateApiMethods(apiMethodsContext);

    StaticLangRpcStubView.Builder stubClass = StaticLangRpcStubView.newBuilder();

    stubClass.doc(serviceTransformer.generateServiceDoc(context, null, productConfig));

    String name =
        namer.getApiRpcStubClassName(
            interfaceConfig.getInterfaceModel(), productConfig.getTransportProtocol());
    stubClass.releaseLevelAnnotation(namer.getReleaseAnnotation(ReleaseLevel.BETA));
    stubClass.name(name);
    stubClass.parentName(namer.getApiStubInterfaceName(interfaceConfig));
    stubClass.settingsClassName(
        getAndSaveNicknameForRootType(
            apiMethodsContext, namer.getApiSettingsClassName(interfaceConfig)));
    stubClass.stubSettingsClassName(
        getAndSaveNicknameForStubType(
            apiMethodsContext, namer.getApiStubSettingsClassName(interfaceConfig)));
    stubClass.callableFactoryClassName(
        getAndSaveNicknameForStubType(
            apiMethodsContext,
            namer.getCallableFactoryClassName(
                interfaceConfig, productConfig.getTransportProtocol())));
    stubClass.methodDescriptors(
        apiCallableTransformer.generateMethodDescriptors(apiMethodsContext));
    stubClass.apiCallables(
        apiCallableTransformer.generateStaticLangApiCallables(apiMethodsContext));
    stubClass.callableMethods(filterIncludeCallableMethods(methods));
    stubClass.hasDefaultInstance(interfaceConfig.hasDefaultInstance());
    stubClass.hasLongRunningOperations(interfaceConfig.hasLongRunningOperations());

    for (TypeAlias alias :
        apiMethodsContext.getImportTypeTable().getTypeTable().getAllImports().values()) {
      context.getImportTypeTable().getAndSaveNicknameFor(alias);
    }

    return stubClass.build();
  }

  private StaticLangFileView<StaticLangCallableFactoryView> generateCallableFactoryClassFile(
      InterfaceContext context, GapicProductConfig productConfig) {
    StaticLangFileView.Builder<StaticLangCallableFactoryView> fileView =
        StaticLangFileView.newBuilder();

    fileView.classView(generateCallableFactoryClass(context, productConfig));
    fileView.templateFileName(callableFactoryTemplateFilename);

    String outputPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    String className =
        context
            .getNamer()
            .getCallableFactoryClassName(
                context.getInterfaceConfig(), productConfig.getTransportProtocol());
    fileView.outputPath(outputPath + File.separator + className + ".java");

    // must be done as the last step to catch all imports
    fileView.fileHeader(fileHeaderTransformer.generateFileHeader(context, className));

    return fileView.build();
  }

  private StaticLangCallableFactoryView generateCallableFactoryClass(
      InterfaceContext context, GapicProductConfig productConfig) {
    SurfaceNamer namer = context.getNamer();
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();

    addCallableFactoryImports(context);

    StaticLangCallableFactoryView.Builder callableFactory =
        StaticLangCallableFactoryView.newBuilder();

    callableFactory.doc(serviceTransformer.generateServiceDoc(context, null, productConfig));

    callableFactory.releaseLevelAnnotation(namer.getReleaseAnnotation(ReleaseLevel.BETA));
    callableFactory.name(
        namer.getCallableFactoryClassName(interfaceConfig, productConfig.getTransportProtocol()));
    if (productConfig.getTransportProtocol().equals(TransportProtocol.HTTP)) {
      callableFactory.operationStubType(
          context
              .getImportTypeTable()
              .getAndSaveNicknameFor("com.google.api.gax.core.BackgroundResource"));
      callableFactory.operationMessage(
          context
              .getImportTypeTable()
              .getAndSaveNicknameFor("com.google.api.gax.httpjson.ApiMessage"));
    }
    return callableFactory.build();
  }

  private String getAndSaveNicknameForRootType(InterfaceContext context, String nickname) {
    SurfaceNamer namer = context.getNamer();
    ImportTypeTable typeTable = context.getImportTypeTable();

    String fullyQualifiedTypeName = namer.getRootPackageName() + "." + nickname;
    return typeTable.getAndSaveNicknameFor(fullyQualifiedTypeName);
  }

  private String getAndSaveNicknameForStubType(InterfaceContext context, String nickname) {
    SurfaceNamer namer = context.getNamer();
    ImportTypeTable typeTable = context.getImportTypeTable();

    String fullyQualifiedTypeName = namer.getStubPackageName() + "." + nickname;
    return typeTable.getAndSaveNicknameFor(fullyQualifiedTypeName);
  }

  private List<StaticLangApiMethodView> filterIncludeCallableMethods(
      List<StaticLangApiMethodView> inMethods) {
    List<StaticLangApiMethodView> outMethods = new ArrayList<>();

    for (StaticLangApiMethodView methodView : inMethods) {
      switch (methodView.type()) {
        case PagedCallableMethod:
        case UnpagedListCallableMethod:
        case CallableMethod:
        case OperationCallableMethod:
          outMethods.add(methodView);
        default:
          // don't include
      }
    }

    return outMethods;
  }

  private PackageInfoView generatePackageInfo(
      ApiModel model,
      GapicProductConfig productConfig,
      SurfaceNamer namer,
      List<ServiceDocView> serviceDocs) {
    PackageInfoView.Builder packageInfo = PackageInfoView.newBuilder();

    packageInfo.templateFileName(PACKAGE_INFO_TEMPLATE_FILENAME);

    packageInfo.serviceTitle(model.getTitle());
    packageInfo.serviceDocs(serviceDocs);
    packageInfo.domainLayerLocation(productConfig.getDomainLayerLocation());
    packageInfo.authScopes(model.getAuthScopes(productConfig));

    packageInfo.fileHeader(
        fileHeaderTransformer.generateFileHeader(
            productConfig, ImportSectionView.newBuilder().build(), namer));

    model
        .getInterfaces(productConfig)
        .stream()
        .filter(productConfig::hasInterfaceConfig)
        .map(InterfaceModel::getFullName)
        .findFirst()
        .map(name -> pathMapper.getOutputPath(name, productConfig))
        .ifPresent(path -> packageInfo.outputPath(path + File.separator + "package-info.java"));
    packageInfo.releaseLevel(productConfig.getReleaseLevel());

    return packageInfo.build();
  }

  private void addApiImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("com.google.api.gax.core.BackgroundResource");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.UnaryCallable");
    typeTable.saveNicknameFor("com.google.api.pathtemplate.PathTemplate");
    typeTable.saveNicknameFor("java.io.Closeable");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.util.concurrent.TimeUnit");
    typeTable.saveNicknameFor("javax.annotation.Generated");

    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      typeTable.saveNicknameFor("com.google.api.gax.longrunning.OperationFuture");
      typeTable.saveNicknameFor("com.google.longrunning.Operation");
      typeTable.saveNicknameFor("com.google.longrunning.OperationsClient");
    }

    switch (context.getProductConfig().getTransportProtocol()) {
      case HTTP:
        typeTable.saveNicknameFor("java.util.List");
        typeTable.saveNicknameFor("java.util.ArrayList");
        typeTable.saveNicknameFor("java.util.concurrent.ScheduledExecutorService");
    }
  }

  private void addSettingsImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    typeTable.saveNicknameFor("com.google.api.core.ApiFunction");
    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("com.google.api.gax.core.CredentialsProvider");
    typeTable.saveNicknameFor("com.google.api.gax.core.ExecutorProvider");
    typeTable.saveNicknameFor("com.google.api.gax.core.GaxProperties");
    typeTable.saveNicknameFor("com.google.api.gax.core.GoogleCredentialsProvider");
    typeTable.saveNicknameFor("com.google.api.gax.core.InstantiatingExecutorProvider");
    typeTable.saveNicknameFor("com.google.api.gax.retrying.RetrySettings");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.ApiClientHeaderProvider");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.ClientContext");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.ClientSettings");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.StubSettings");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.HeaderProvider");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.StatusCode");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.TransportChannelProvider");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.UnaryCallSettings");
    typeTable.saveNicknameFor("com.google.auth.Credentials");
    typeTable.saveNicknameFor("com.google.common.collect.ImmutableList");
    typeTable.saveNicknameFor("com.google.common.collect.ImmutableMap");
    typeTable.saveNicknameFor("com.google.common.collect.ImmutableSet");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("com.google.common.collect.Sets");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.concurrent.ScheduledExecutorService");
    typeTable.saveNicknameFor("javax.annotation.Generated");
    typeTable.saveNicknameFor("org.threeten.bp.Duration");

    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    if (interfaceConfig.hasPageStreamingMethods()) {
      typeTable.saveNicknameFor("com.google.api.core.ApiFuture");
      typeTable.saveNicknameFor("com.google.api.gax.rpc.ApiCallContext");
      typeTable.saveNicknameFor("com.google.api.gax.rpc.PageContext");
      typeTable.saveNicknameFor("com.google.api.gax.rpc.PagedCallSettings");
      typeTable.saveNicknameFor("com.google.api.gax.rpc.PagedListDescriptor");
      typeTable.saveNicknameFor("com.google.api.gax.rpc.PagedListResponseFactory");
      typeTable.saveNicknameFor("com.google.api.gax.rpc.UnaryCallable");
    }
    if (interfaceConfig.hasBatchingMethods()) {
      typeTable.saveNicknameFor("com.google.api.gax.batching.BatchingSettings");
      typeTable.saveNicknameFor("com.google.api.gax.batching.FlowController");
      typeTable.saveNicknameFor("com.google.api.gax.batching.FlowController.LimitExceededBehavior");
      typeTable.saveNicknameFor("com.google.api.gax.batching.FlowControlSettings");
      typeTable.saveNicknameFor("com.google.api.gax.batching.PartitionKey");
      typeTable.saveNicknameFor("com.google.api.gax.batching.RequestBuilder");
      typeTable.saveNicknameFor("com.google.api.gax.rpc.BatchedRequestIssuer");
      typeTable.saveNicknameFor("com.google.api.gax.rpc.BatchingCallSettings");
      typeTable.saveNicknameFor("com.google.api.gax.rpc.BatchingDescriptor");
      typeTable.saveNicknameFor("java.util.ArrayList");
      typeTable.saveNicknameFor("java.util.Collection");
    }

    if (interfaceConfig.hasGrpcStreamingMethods(GrpcStreamingType.ServerStreaming)) {
      typeTable.saveNicknameFor("com.google.api.gax.rpc.ServerStreamingCallSettings");
    }
    if (interfaceConfig.hasGrpcStreamingMethods(GrpcStreamingType.ClientStreaming)
        || interfaceConfig.hasGrpcStreamingMethods(GrpcStreamingType.BidiStreaming)) {
      typeTable.saveNicknameFor("com.google.api.gax.rpc.StreamingCallSettings");
    }
    if (interfaceConfig.hasLongRunningOperations()) {
      typeTable.saveNicknameFor("com.google.api.gax.longrunning.OperationSnapshot");
      typeTable.saveNicknameFor("com.google.api.gax.rpc.OperationCallSettings");
      typeTable.saveNicknameFor("com.google.longrunning.Operation");
      typeTable.saveNicknameFor("com.google.api.gax.longrunning.OperationTimedPollAlgorithm");
    }
    switch (context.getProductConfig().getTransportProtocol()) {
      case GRPC:
        typeTable.saveNicknameFor("com.google.api.gax.grpc.GrpcTransportChannel");
        typeTable.saveNicknameFor("com.google.api.gax.grpc.InstantiatingGrpcChannelProvider");
        if (interfaceConfig.hasLongRunningOperations()) {
          typeTable.saveNicknameFor("com.google.api.gax.grpc.ProtoOperationTransformers");
        }
        typeTable.saveNicknameFor("com.google.api.gax.grpc.GaxGrpcProperties");
        break;
      case HTTP:
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.HttpJsonTransportChannel");
        typeTable.saveNicknameFor(
            "com.google.api.gax.httpjson.InstantiatingHttpJsonChannelProvider");
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.GaxHttpJsonProperties");
        typeTable.saveNicknameFor("java.lang.Void");
        break;
    }
  }

  private void addRpcStubImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();

    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("com.google.api.gax.core.BackgroundResource");
    typeTable.saveNicknameFor("com.google.api.gax.core.BackgroundResourceAggregation");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.ClientContext");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.UnaryCallable");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.util.ArrayList");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.Map");
    typeTable.saveNicknameFor("java.util.concurrent.TimeUnit");
    typeTable.saveNicknameFor("javax.annotation.Generated");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.RequestParamsExtractor");
    typeTable.saveNicknameFor("com.google.common.collect.ImmutableMap");

    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    if (interfaceConfig.hasGrpcStreamingMethods(
        GrpcStreamingConfig.GrpcStreamingType.BidiStreaming)) {
      typeTable.saveNicknameFor("com.google.api.gax.rpc.BidiStreamingCallable");
    }
    if (interfaceConfig.hasGrpcStreamingMethods(
        GrpcStreamingConfig.GrpcStreamingType.ServerStreaming)) {
      typeTable.saveNicknameFor("com.google.api.gax.rpc.ServerStreamingCallable");
    }
    if (interfaceConfig.hasGrpcStreamingMethods(
        GrpcStreamingConfig.GrpcStreamingType.ClientStreaming)) {
      typeTable.saveNicknameFor("com.google.api.gax.rpc.ClientStreamingCallable");
    }
    if (interfaceConfig.hasLongRunningOperations()) {
      typeTable.saveNicknameFor("com.google.api.gax.longrunning.OperationSnapshot");
    }
    switch (context.getProductConfig().getTransportProtocol()) {
      case GRPC:
        typeTable.saveNicknameFor("com.google.api.gax.grpc.GrpcStubCallableFactory");
        typeTable.saveNicknameFor("com.google.api.gax.grpc.GrpcCallableFactory");
        typeTable.saveNicknameFor("com.google.api.gax.grpc.GrpcCallSettings");
        typeTable.saveNicknameFor("io.grpc.MethodDescriptor");
        typeTable.saveNicknameFor("io.grpc.protobuf.ProtoUtils");
        if (interfaceConfig.hasLongRunningOperations()) {
          typeTable.saveNicknameFor("com.google.longrunning.Operation");
          typeTable.saveNicknameFor("com.google.longrunning.stub.GrpcOperationsStub");
        }
        break;
      case HTTP:
        typeTable.saveNicknameFor("com.google.api.client.http.HttpMethods");
        typeTable.saveNicknameFor("com.google.api.core.InternalApi");
        typeTable.saveNicknameFor("com.google.api.pathtemplate.PathTemplate");
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.ApiMessageHttpRequestFormatter");
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.ApiMessageHttpResponseParser");
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.ApiMethodDescriptor");
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.HttpJsonCallSettings");
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.HttpJsonStubCallableFactory");
        typeTable.saveNicknameFor("com.google.common.collect.Sets");
        typeTable.saveNicknameFor("java.lang.Void");
        break;
    }
  }

  private void addCallableFactoryImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    typeTable.saveNicknameFor("com.google.api.gax.rpc.OperationCallable");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.OperationCallSettings");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.BidiStreamingCallable");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.StreamingCallSettings");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.ServerStreamingCallSettings");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.ServerStreamingCallable");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.ClientStreamingCallable");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.ClientContext");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.UnaryCallable");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.UnaryCallSettings");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.PagedCallSettings");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.BatchingCallSettings");

    switch (context.getProductConfig().getTransportProtocol()) {
      case GRPC:
        typeTable.saveNicknameFor("com.google.api.gax.grpc.GrpcCallableFactory");
        typeTable.saveNicknameFor("com.google.api.gax.grpc.GrpcStubCallableFactory");
        typeTable.saveNicknameFor("com.google.longrunning.Operation");
        typeTable.saveNicknameFor("com.google.longrunning.stub.OperationsStub");
        break;
      case HTTP:
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.HttpJsonCallableFactory");
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.HttpJsonStubCallableFactory");
        typeTable.saveNicknameFor("javax.annotation.Nullable");
        break;
    }
  }

  private void addStubInterfaceImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();

    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("com.google.api.gax.core.BackgroundResource");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.UnaryCallable");
    typeTable.saveNicknameFor("javax.annotation.Generated");
    typeTable.saveNicknameFor("java.lang.Void");

    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    if (interfaceConfig.hasLongRunningOperations()) {
      typeTable.saveNicknameFor("com.google.longrunning.Operation");
      typeTable.saveNicknameFor("com.google.longrunning.stub.OperationsStub");
    }
  }

  private void addPagedResponseWrapperImports(ImportTypeTable typeTable) {
    typeTable.saveNicknameFor("com.google.api.core.ApiFunction");
    typeTable.saveNicknameFor("com.google.api.core.ApiFuture");
    typeTable.saveNicknameFor("com.google.api.core.ApiFutures");
    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("com.google.api.gax.paging.AbstractPage");
    typeTable.saveNicknameFor("com.google.api.gax.paging.AbstractPagedListResponse");
    typeTable.saveNicknameFor("com.google.api.gax.paging.AbstractFixedSizeCollection");
    typeTable.saveNicknameFor("com.google.api.gax.paging.FixedSizeCollection");
    typeTable.saveNicknameFor("com.google.api.gax.paging.Page");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.ApiExceptions");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.PageContext");
    typeTable.saveNicknameFor("com.google.common.base.Function");
    typeTable.saveNicknameFor("com.google.common.collect.Iterables");
    typeTable.saveNicknameFor("com.google.common.util.concurrent.MoreExecutors");
    typeTable.saveNicknameFor("javax.annotation.Generated");
    typeTable.saveNicknameFor("java.util.Iterator");
    typeTable.saveNicknameFor("java.util.List");
  }

  private SettingsDocView generateSettingsDoc(
      InterfaceContext context,
      StaticLangApiMethodView exampleApiMethod,
      GapicProductConfig productConfig,
      String settingsClassName,
      String apiClassName) {
    SurfaceNamer namer = context.getNamer();
    SettingsDocView.Builder settingsDoc = SettingsDocView.newBuilder();
    settingsDoc.serviceHostname(
        productServiceConfig.getServiceHostname(context.getServiceAddress()));
    settingsDoc.servicePort(productServiceConfig.getServicePort(context.getServiceAddress()));
    settingsDoc.transportProtocol(productConfig.getTransportProtocol());
    settingsDoc.exampleApiMethodName(exampleApiMethod.name());
    settingsDoc.exampleApiMethodSettingsGetter(exampleApiMethod.settingsGetterName());
    settingsDoc.apiClassName(apiClassName);
    settingsDoc.settingsVarName(namer.getApiSettingsVariableName(context.getInterfaceConfig()));
    settingsDoc.settingsClassName(settingsClassName);
    settingsDoc.settingsBuilderVarName(
        namer.getApiSettingsBuilderVarName(context.getInterfaceConfig()));
    settingsDoc.hasDefaultInstance(context.getInterfaceConfig().hasDefaultInstance());
    return settingsDoc.build();
  }
}
