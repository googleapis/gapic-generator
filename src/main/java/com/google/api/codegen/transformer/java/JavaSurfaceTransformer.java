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
import com.google.api.codegen.SampleValueSet;
import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.config.TransportProtocol;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.BatchingTransformer;
import com.google.api.codegen.transformer.DiscoGapicInterfaceContext;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.RetryDefinitionsTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformer;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.PackageInfoView;
import com.google.api.codegen.viewmodel.PagedResponseIterateMethodView;
import com.google.api.codegen.viewmodel.SampleValueSetsModel;
import com.google.api.codegen.viewmodel.ServiceDocView;
import com.google.api.codegen.viewmodel.SettingsDocView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView.Builder;
import com.google.api.codegen.viewmodel.StaticLangApiView;
import com.google.api.codegen.viewmodel.StaticLangFileView;
import com.google.api.codegen.viewmodel.StaticLangPagedResponseView;
import com.google.api.codegen.viewmodel.StaticLangRpcStubView;
import com.google.api.codegen.viewmodel.StaticLangSettingsView;
import com.google.api.codegen.viewmodel.StaticLangStubInterfaceView;
import com.google.api.codegen.viewmodel.StaticLangStubSettingsView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** A transformer to transform an ApiModel into the standard GAPIC surface in Java. */
public class JavaSurfaceTransformer {
  private final GapicCodePathMapper pathMapper;
  private final PackageMetadataConfig packageMetadataConfig;

  // TODO: Simplify by making JavaSurfaceTransformer abstract and making
  // JavaDiscoGapicSurfaceTransformer and JavaGapicSurfaceTransformer derive from it and override
  // the methods that are used below.
  private final SurfaceTransformer surfaceTransformer;
  private final String rpcStubTemplateFilename;

  private final ServiceTransformer serviceTransformer = new ServiceTransformer();
  private final PathTemplateTransformer pathTemplateTransformer = new PathTemplateTransformer();
  private final ApiCallableTransformer apiCallableTransformer = new ApiCallableTransformer();
  private final StaticLangApiMethodTransformer apiMethodTransformer =
      new StaticLangApiMethodTransformer();
  private final PageStreamingTransformer pageStreamingTransformer = new PageStreamingTransformer();
  private final BatchingTransformer batchingTransformer = new BatchingTransformer();
  private final StandardImportSectionTransformer importSectionTransformer =
      new StandardImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final RetryDefinitionsTransformer retryDefinitionsTransformer =
      new RetryDefinitionsTransformer();

  private static final String API_TEMPLATE_FILENAME = "java/main.snip";
  private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME = "java/standalone_sample.snip";
  private static final String SETTINGS_TEMPLATE_FILENAME = "java/settings.snip";
  private static final String STUB_SETTINGS_TEMPLATE_FILENAME = "java/stub_settings.snip";
  private static final String STUB_INTERFACE_TEMPLATE_FILENAME = "java/stub_interface.snip";

  private static final String PACKAGE_INFO_TEMPLATE_FILENAME = "java/package-info.snip";

  public JavaSurfaceTransformer(
      GapicCodePathMapper pathMapper,
      PackageMetadataConfig packageMetadataConfig,
      SurfaceTransformer surfaceTransformer,
      String rpcStubSnippetFileName) {
    this.pathMapper = pathMapper;
    this.packageMetadataConfig = packageMetadataConfig;
    this.surfaceTransformer = surfaceTransformer;
    this.rpcStubTemplateFilename = rpcStubSnippetFileName;
  }

  public List<ViewModel> transform(ApiModel model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = surfaceTransformer.createSurfaceNamer(productConfig);

    List<ServiceDocView> serviceDocs = new ArrayList<>();
    for (InterfaceModel apiInterface : model.getInterfaces()) {
      boolean enableStringFormatFunctions = productConfig.getResourceNameMessageConfigs().isEmpty();
      ImportTypeTable typeTable =
          surfaceTransformer.createTypeTable(productConfig.getPackageName());
      InterfaceContext context =
          surfaceTransformer.createInterfaceContext(
              apiInterface, productConfig, namer, typeTable, enableStringFormatFunctions);
      StaticLangFileView<StaticLangApiView> apiFile = generateApiFile(context, productConfig);
      surfaceDocs.add(apiFile);
      surfaceDocs.addAll(generateSampleFilesForApi(context, apiFile));

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
    }

    PackageInfoView packageInfo = generatePackageInfo(model, productConfig, namer, serviceDocs);
    surfaceDocs.add(packageInfo);

    return surfaceDocs;
  }

  /**
   * Generates a list of standalone sample view models for the the API, at most one for each calling
   * form for each method, as specified in each method view model's sampleValueSetsModel (which
   * ultimately derives from user-provided configuration). If no samples are configured, no sample
   * files are produced.
   *
   * @param context the interface for whose methods the sample files will be generated
   * @param apiFile the previously generated API file view model for whose methods sample files will
   *     be generated. The view models returned for each method are modified clones of the view
   *     models for the methods in apiFile.
   * @return A list of view models, each one corresponding to a distinct method sample
   */
  private List<ViewModel> generateSampleFilesForApi(
      InterfaceContext context, StaticLangFileView<StaticLangApiView> apiFile) {
    List<ViewModel> sampleDocs = new ArrayList<>();

    StaticLangApiView classView = apiFile.classView();

    final StaticLangFileView.Builder<StaticLangApiView> sampleFileBuilder =
        StaticLangFileView.<StaticLangApiView>newBuilder();
    sampleFileBuilder.templateFileName(STANDALONE_SAMPLE_TEMPLATE_FILENAME);

    for (StaticLangApiMethodView methodView : classView.apiMethods()) {
      SampleValueSetsModel valueSetsModel = methodView.sampleValueSetsModel();
      final Set<SampleValueSet> matchingValueSets =
          valueSetsModel.forSampleType(SampleType.STANDALONE);

      Builder methodViewBuilder = methodView.toBuilder();

      for (SampleValueSet values : matchingValueSets) {

        methodViewBuilder.sampleValueSet(values);
        final StaticLangApiMethodView sampleMethodView = methodViewBuilder.build();

        sampleFileBuilder.classView(generateSampleClass(classView, sampleMethodView));
        String outputPath =
            pathMapper.getSamplesOutputPath(
                context.getInterfaceModel().getFullName(),
                context.getProductConfig(),
                sampleMethodView.name());

        SurfaceNamer namer = context.getNamer();
        String className = namer.getApiSampleClassName(sampleMethodView, values.getId());
        // TODO(vchudnov-g): Capture the sample class name in the View Model
        sampleFileBuilder.outputPath(
            outputPath + File.separator + namer.getApiSampleFileName(className));

        // must be done as the last step to catch all imports
        // TODO(vchudnov-g): Generate only the headers needed for the sample.
        sampleFileBuilder.fileHeader(fileHeaderTransformer.generateFileHeader(context, className));

        sampleDocs.add(sampleFileBuilder.build());
      }
    }

    return sampleDocs;
  }

  /**
   * Makes the specified API method the only method in a near-clone of apiView. This allows us to
   * generate a class that creates a sample for just this one method.
   *
   * @param apiView the class view that we're cloning before paring down its methods
   * @param method the single method that will be present in the clone of apiView
   * @return a near-clone of apiView but with only one method, the one specified
   */
  private StaticLangApiView generateSampleClass(
      StaticLangApiView apiView, StaticLangApiMethodView method) {
    StaticLangApiView.Builder sampleViewBuilder = apiView.toBuilder();
    List<StaticLangApiMethodView> methods = new ArrayList<>();
    methods.add(method);
    sampleViewBuilder.apiMethods(methods);
    return sampleViewBuilder.build();
  }

  private StaticLangFileView<StaticLangApiView> generateApiFile(
      InterfaceContext context, GapicProductConfig productConfig) {
    StaticLangFileView.Builder<StaticLangApiView> apiFile =
        StaticLangFileView.<StaticLangApiView>newBuilder();

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

    List<StaticLangApiMethodView> methods = generateApiMethods(context);

    StaticLangApiView.Builder xapiClass = StaticLangApiView.newBuilder();

    ApiMethodView exampleApiMethod = getExampleApiMethod(methods);
    xapiClass.doc(serviceTransformer.generateServiceDoc(context, exampleApiMethod, productConfig));

    String name = context.getNamer().getApiWrapperClassName(context.getInterfaceConfig());
    xapiClass.releaseLevelAnnotation(
        namer.getReleaseAnnotation(packageMetadataConfig.releaseLevel(TargetLanguage.JAVA)));
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
    xapiClass.pagedResponseViews(
        generatePagedResponseWrappers(
            context, productConfig, packageMetadataConfig.releaseLevel(TargetLanguage.JAVA)));
    return xapiClass.build();
  }

  private List<StaticLangPagedResponseView> generatePagedResponseWrappers(
      InterfaceContext context, GapicProductConfig productConfig, ReleaseLevel releaseLevel) {
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
    xsettingsClass.serviceAddress(stubSettingsView.serviceAddress());
    xsettingsClass.servicePort(stubSettingsView.servicePort());
    xsettingsClass.authScopes(stubSettingsView.authScopes());

    xsettingsClass.callSettings(apiCallableTransformer.generateCallSettings(context));
    xsettingsClass.pageStreamingDescriptors(stubSettingsView.pageStreamingDescriptors());
    xsettingsClass.pagedListResponseFactories(stubSettingsView.pagedListResponseFactories());
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
        context
            .getNamer()
            .getReleaseAnnotation(packageMetadataConfig.releaseLevel(TargetLanguage.JAVA)));
    xsettingsClass.doc(
        generateSettingsDoc(
            context,
            exampleApiMethod,
            productConfig,
            context.getNamer().getApiStubSettingsClassName(interfaceConfig),
            namer.getApiStubInterfaceName(context.getInterfaceConfig())));
    String name = namer.getApiStubSettingsClassName(context.getInterfaceConfig());
    xsettingsClass.name(name);
    xsettingsClass.serviceAddress(model.getServiceAddress());
    xsettingsClass.servicePort(model.getServicePort());
    xsettingsClass.authScopes(model.getAuthScopes());
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
        StaticLangFileView.<StaticLangStubInterfaceView>newBuilder();

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
    List<StaticLangApiMethodView> methods = generateApiMethods(apiMethodsContext);
    for (TypeAlias alias :
        apiMethodsContext.getImportTypeTable().getTypeTable().getAllImports().values()) {
      context.getImportTypeTable().getAndSaveNicknameFor(alias);
    }

    StaticLangStubInterfaceView.Builder stubInterface = StaticLangStubInterfaceView.newBuilder();

    stubInterface.doc(serviceTransformer.generateServiceDoc(context, null, productConfig));

    String name = context.getNamer().getApiStubInterfaceName(context.getInterfaceConfig());
    stubInterface.releaseLevelAnnotation(
        context
            .getNamer()
            .getReleaseAnnotation(packageMetadataConfig.releaseLevel(TargetLanguage.JAVA)));
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
    List<StaticLangApiMethodView> methods = generateApiMethods(apiMethodsContext);

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

    // TODO(andrealin): Move the baseUrl to gapic.yaml and pull value from GapicProductConfig.
    if (productConfig.getTransportProtocol().equals(TransportProtocol.HTTP)) {
      stubClass.baseUrl(((DiscoGapicInterfaceContext) context).getDocument().baseUrl());
    }

    return stubClass.build();
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
    packageInfo.authScopes(model.getAuthScopes());

    packageInfo.fileHeader(
        fileHeaderTransformer.generateFileHeader(
            productConfig, ImportSectionView.newBuilder().build(), namer));

    InterfaceModel firstInterface = model.getInterfaces().iterator().next();
    String outputPath = pathMapper.getOutputPath(firstInterface.getFullName(), productConfig);
    packageInfo.outputPath(outputPath + File.separator + "package-info.java");
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

    switch (context.getApiModel().getApiSource()) {
      case DISCOVERY:
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
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.ApiMethodDescriptor");
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.HttpJsonCallSettings");
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.HttpJsonCallableFactory");
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.ApiMessageHttpRequestFormatter");
        typeTable.saveNicknameFor("com.google.common.collect.Sets");
        typeTable.saveNicknameFor("java.lang.Void");
        typeTable.saveNicknameFor("java.util.HashSet");
        typeTable.saveNicknameFor("java.util.Arrays");
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
    typeTable.saveNicknameFor("com.google.api.gax.paging.PagedListResponse");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.ApiExceptions");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.PageContext");
    typeTable.saveNicknameFor("com.google.common.base.Function");
    typeTable.saveNicknameFor("com.google.common.collect.Iterables");
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
    ApiModel model = context.getApiModel();
    settingsDoc.serviceAddress(model.getServiceAddress());
    settingsDoc.servicePort(model.getServicePort());
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

  private List<StaticLangApiMethodView> generateApiMethods(InterfaceContext context) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();

    for (MethodModel method : context.getSupportedMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      MethodContext requestMethodContext = context.asRequestMethodContext(method);

      if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            MethodContext flattenedMethodContext =
                context.asFlattenedMethodContext(method, flatteningGroup);
            apiMethods.add(
                apiMethodTransformer.generatePagedFlattenedMethod(flattenedMethodContext));
            if (hasAnyResourceNameParameter(flatteningGroup)) {
              apiMethods.add(
                  apiMethodTransformer.generatePagedFlattenedMethod(
                      flattenedMethodContext.withResourceNamesInSamplesOnly()));
            }
          }
        }
        apiMethods.add(apiMethodTransformer.generatePagedRequestObjectMethod(requestMethodContext));
        apiMethods.add(apiMethodTransformer.generatePagedCallableMethod(requestMethodContext));
        apiMethods.add(
            apiMethodTransformer.generateUnpagedListCallableMethod(requestMethodContext));
      } else if (methodConfig.isGrpcStreaming()) {
        ImportTypeTable typeTable = context.getImportTypeTable();
        switch (methodConfig.getGrpcStreamingType()) {
          case BidiStreaming:
            typeTable.saveNicknameFor("com.google.api.gax.rpc.BidiStreamingCallable");
            break;
          case ClientStreaming:
            typeTable.saveNicknameFor("com.google.api.gax.rpc.ClientStreamingCallable");
            break;
          case ServerStreaming:
            typeTable.saveNicknameFor("com.google.api.gax.rpc.ServerStreamingCallable");
            break;
          default:
            throw new IllegalArgumentException(
                "Invalid streaming type: " + methodConfig.getGrpcStreamingType());
        }
        apiMethods.add(apiMethodTransformer.generateCallableMethod(requestMethodContext));
      } else if (methodConfig.isLongRunningOperation()) {
        context.getImportTypeTable().saveNicknameFor("com.google.api.gax.rpc.OperationCallable");
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            MethodContext flattenedMethodContext =
                context.asFlattenedMethodContext(method, flatteningGroup);
            apiMethods.add(
                apiMethodTransformer.generateAsyncOperationFlattenedMethod(flattenedMethodContext));
            if (hasAnyResourceNameParameter(flatteningGroup)) {
              apiMethods.add(
                  apiMethodTransformer.generateAsyncOperationFlattenedMethod(
                      flattenedMethodContext.withResourceNamesInSamplesOnly()));
            }
          }
        }
        apiMethods.add(
            apiMethodTransformer.generateAsyncOperationRequestObjectMethod(requestMethodContext));
        apiMethods.add(apiMethodTransformer.generateOperationCallableMethod(requestMethodContext));
        apiMethods.add(apiMethodTransformer.generateCallableMethod(requestMethodContext));
      } else {
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            MethodContext flattenedMethodContext =
                context.asFlattenedMethodContext(method, flatteningGroup);
            apiMethods.add(apiMethodTransformer.generateFlattenedMethod(flattenedMethodContext));
            if (hasAnyResourceNameParameter(flatteningGroup)) {
              apiMethods.add(
                  apiMethodTransformer.generateFlattenedMethod(
                      flattenedMethodContext.withResourceNamesInSamplesOnly()));
            }
          }
        }
        apiMethods.add(apiMethodTransformer.generateRequestObjectMethod(requestMethodContext));
        apiMethods.add(apiMethodTransformer.generateCallableMethod(requestMethodContext));
      }
    }

    return apiMethods;
  }

  private boolean hasAnyResourceNameParameter(FlatteningConfig flatteningGroup) {
    return flatteningGroup
        .getFlattenedFieldConfigs()
        .values()
        .stream()
        .anyMatch(FieldConfig::useResourceNameType);
  }
}
