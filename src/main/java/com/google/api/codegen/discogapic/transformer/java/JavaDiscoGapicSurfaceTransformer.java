/* Copyright 2017 Google Inc
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
package com.google.api.codegen.discogapic.transformer.java;

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.DiscoGapicInterfaceConfig;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldType;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProductServiceConfig;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discogapic.transformer.DocumentToViewTransformer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.BatchingTransformer;
import com.google.api.codegen.transformer.DiscoGapicInterfaceContext;
import com.google.api.codegen.transformer.DiscoGapicMethodContext;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.RetryDefinitionsTransformer;
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.java.JavaFeatureConfig;
import com.google.api.codegen.transformer.java.JavaSchemaTypeNameConverter;
import com.google.api.codegen.transformer.java.JavaSurfaceNamer;
import com.google.api.codegen.util.java.JavaNameFormatter;
import com.google.api.codegen.util.java.JavaTypeTable;
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
import com.google.api.codegen.viewmodel.StaticLangFileView;
import com.google.api.codegen.viewmodel.StaticLangPagedResponseView;
import com.google.api.codegen.viewmodel.StaticLangPagedResponseWrappersView;
import com.google.api.codegen.viewmodel.StaticLangSettingsView;
import com.google.api.codegen.viewmodel.StaticLangStubInterfaceView;
import com.google.api.codegen.viewmodel.ViewModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** The ModelToViewTransformer to transform a Document into the standard GAPIC surface in Java. */
public class JavaDiscoGapicSurfaceTransformer implements DocumentToViewTransformer {
  private final GapicCodePathMapper pathMapper;
  private final PackageMetadataConfig packageMetadataConfig;
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
  private final ProductServiceConfig productServiceConfig = new ProductServiceConfig();

  private final JavaNameFormatter nameFormatter = new JavaNameFormatter();

  // TODO(andrealin) Create the service, page streaming, batching, etc transformers.

  private static final String API_TEMPLATE_FILENAME = "java/main.snip";
  private static final String SETTINGS_TEMPLATE_FILENAME = "java/settings.snip";
  private static final String STUB_INTERFACE_TEMPLATE_FILENAME = "java/stub_interface.snip";
  private static final String PACKAGE_INFO_TEMPLATE_FILENAME = "java/package-info.snip";
  private static final String PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME =
      "java/page_streaming_response.snip";

  public JavaDiscoGapicSurfaceTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageMetadataConfig) {
    this.pathMapper = pathMapper;
    this.packageMetadataConfig = packageMetadataConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(
        API_TEMPLATE_FILENAME,
        SETTINGS_TEMPLATE_FILENAME,
        STUB_INTERFACE_TEMPLATE_FILENAME,
        PACKAGE_INFO_TEMPLATE_FILENAME,
        PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Document document, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    JavaNameFormatter javaNameFormatter = new JavaNameFormatter();
    String packageName = productConfig.getPackageName();
    SurfaceNamer namer = new JavaSurfaceNamer(packageName, packageName, javaNameFormatter);
    DiscoGapicNamer discoGapicNamer = new DiscoGapicNamer(namer);

    List<ServiceDocView> serviceDocs = new ArrayList<>();
    for (String interfaceName : productConfig.getInterfaceConfigMap().keySet()) {
      boolean enableStringFormatFunctions = productConfig.getResourceNameMessageConfigs().isEmpty();
      DiscoGapicInterfaceContext context =
          DiscoGapicInterfaceContext.createWithInterface(
              document,
              interfaceName,
              productConfig,
              createTypeTable(productConfig.getPackageName()),
              discoGapicNamer,
              JavaFeatureConfig.newBuilder()
                  .enableStringFormatFunctions(enableStringFormatFunctions)
                  .build());
      StaticLangFileView<StaticLangApiView> apiFile = generateApiFile(context);
      surfaceDocs.add(apiFile);

      serviceDocs.add(apiFile.classView().doc());

      context = context.withNewTypeTable();
      StaticLangApiMethodView exampleApiMethod =
          getExampleApiMethod(apiFile.classView().apiMethods());
      StaticLangFileView<StaticLangSettingsView> settingsFile =
          generateSettingsFile(context, exampleApiMethod);
      surfaceDocs.add(settingsFile);
      context = context.withNewTypeTable(namer.getStubPackageName());
      StaticLangFileView<StaticLangStubInterfaceView> stubInterfaceFile =
          generateStubInterfaceFile(context);
      surfaceDocs.add(stubInterfaceFile);
    }

    StaticLangPagedResponseWrappersView pagedResponseWrappers =
        generatePagedResponseWrappers(
            document, productConfig, packageMetadataConfig.releaseLevel(TargetLanguage.JAVA));
    if (pagedResponseWrappers != null) {
      surfaceDocs.add(pagedResponseWrappers);
    }

    PackageInfoView packageInfo = generatePackageInfo(document, productConfig, namer, serviceDocs);
    surfaceDocs.add(packageInfo);

    return surfaceDocs;
  }

  private SchemaTypeTable createTypeTable(String implicitPackageName) {
    return new SchemaTypeTable(
        new JavaTypeTable(implicitPackageName),
        new JavaSchemaTypeNameConverter(implicitPackageName, nameFormatter));
  }

  private StaticLangFileView generateApiFile(DiscoGapicInterfaceContext context) {
    StaticLangFileView.Builder apiFile = StaticLangFileView.newBuilder();

    apiFile.templateFileName(API_TEMPLATE_FILENAME);

    apiFile.classView(generateApiClass(context));

    String outputPath = pathMapper.getOutputPath(null, context.getProductConfig());
    String className = context.getNamer().getApiWrapperClassName(context.getInterfaceConfig());
    apiFile.outputPath(outputPath + File.separator + className + ".java");

    // must be done as the last step to catch all imports
    apiFile.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return apiFile.build();
  }

  private StaticLangApiView generateApiClass(DiscoGapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    DiscoGapicInterfaceConfig interfaceConfig = context.getInterfaceConfig();
    addApiImports(context);

    List<StaticLangApiMethodView> methods = generateApiMethods(context);

    StaticLangApiView.Builder xapiClass = StaticLangApiView.newBuilder();

    ApiMethodView exampleApiMethod = getExampleApiMethod(methods);
    xapiClass.doc(serviceTransformer.generateServiceDoc(context, exampleApiMethod));

    String name = context.getNamer().getApiWrapperClassName(context.getInterfaceConfig());
    xapiClass.releaseLevelAnnotation(
        context
            .getNamer()
            .getReleaseAnnotation(packageMetadataConfig.releaseLevel(TargetLanguage.JAVA)));
    xapiClass.name(name);
    xapiClass.settingsClassName(namer.getApiSettingsClassName(context.getInterfaceConfig()));
    xapiClass.stubInterfaceName(namer.getApiStubInterfaceName(interfaceConfig));
    xapiClass.stubInterfaceName(
        getAndSaveNicknameForStubType(context, namer.getApiStubInterfaceName(interfaceConfig)));
    xapiClass.apiCallableMembers(apiCallableTransformer.generateStaticLangApiCallables(context));
    xapiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    xapiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    xapiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    xapiClass.apiMethods(methods);
    xapiClass.hasDefaultInstance(context.getInterfaceConfig().hasDefaultInstance());
    xapiClass.hasLongRunningOperations(false);

    return xapiClass.build();
  }

  private StaticLangPagedResponseWrappersView generatePagedResponseWrappers(
      Document model, GapicProductConfig productConfig, ReleaseLevel releaseLevel) {
    String packageName = productConfig.getPackageName();

    SurfaceNamer namer = new JavaSurfaceNamer(packageName, packageName, nameFormatter);
    DiscoGapicNamer discoGapicNamer = new DiscoGapicNamer(namer);
    SchemaTypeTable typeTable = createTypeTable(productConfig.getPackageName());

    addPagedResponseWrapperImports(typeTable);

    StaticLangPagedResponseWrappersView.Builder pagedResponseWrappers =
        StaticLangPagedResponseWrappersView.newBuilder();

    pagedResponseWrappers.releaseLevelAnnotation(namer.getReleaseAnnotation(releaseLevel));
    pagedResponseWrappers.templateFileName(PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME);

    String name = namer.getPagedResponseWrappersClassName();
    pagedResponseWrappers.name(name);

    List<StaticLangPagedResponseView> pagedResponseWrappersList = new ArrayList<>();
    for (String interfaceName : productConfig.getInterfaceConfigMap().keySet()) {
      DiscoGapicInterfaceContext context =
          DiscoGapicInterfaceContext.createWithInterface(
              model,
              interfaceName,
              productConfig,
              typeTable,
              discoGapicNamer,
              JavaFeatureConfig.newBuilder()
                  .enableStringFormatFunctions(
                      productConfig.getResourceNameMessageConfigs().isEmpty())
                  .build());
      for (MethodModel method : context.getSupportedMethods()) {
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
    ImportSectionView importSection =
        importSectionTransformer.generateImportSection(typeTable.getImports());
    pagedResponseWrappers.fileHeader(
        fileHeaderTransformer.generateFileHeader(productConfig, importSection, namer));

    String outputPath = pathMapper.getOutputPath(null, productConfig);
    pagedResponseWrappers.outputPath(outputPath + File.separator + name + ".java");

    return pagedResponseWrappers.build();
  }

  private StaticLangPagedResponseView generatePagedResponseWrapper(
      MethodContext context, SchemaTypeTable typeTable) {
    MethodModel method = context.getMethodModel();
    FieldType resourceField = context.getMethodConfig().getPageStreaming().getResourcesField();

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
        typeTable.getAndSaveNicknameFor(
            context
                .getMethodModel()
                .getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer())));
    pagedResponseWrapper.responseTypeName(
        context
            .getMethodModel()
            .getAndSaveResponseTypeName(context.getTypeTable(), context.getNamer()));
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

  private StaticLangFileView<StaticLangSettingsView> generateSettingsFile(
      DiscoGapicInterfaceContext context, StaticLangApiMethodView exampleApiMethod) {
    StaticLangFileView.Builder<StaticLangSettingsView> settingsFile =
        StaticLangFileView.<StaticLangSettingsView>newBuilder();

    settingsFile.classView(generateSettingsClass(context, exampleApiMethod));
    settingsFile.templateFileName(SETTINGS_TEMPLATE_FILENAME);

    String outputPath =
        pathMapper.getOutputPath(context.getInterfaceFullName(), context.getProductConfig());
    String className = context.getNamer().getApiSettingsClassName(context.getInterfaceConfig());
    settingsFile.outputPath(outputPath + File.separator + className + ".java");

    // must be done as the last step to catch all imports
    settingsFile.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return settingsFile.build();
  }

  private StaticLangSettingsView generateSettingsClass(
      DiscoGapicInterfaceContext context, StaticLangApiMethodView exampleApiMethod) {
    addSettingsImports(context);

    SurfaceNamer namer = context.getNamer();
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();

    StaticLangSettingsView.Builder xsettingsClass = StaticLangSettingsView.newBuilder();
    xsettingsClass.releaseLevelAnnotation(
        context
            .getNamer()
            .getReleaseAnnotation(packageMetadataConfig.releaseLevel(TargetLanguage.JAVA)));
    xsettingsClass.doc(generateSettingsDoc(context, exampleApiMethod));
    String name = namer.getApiSettingsClassName(context.getInterfaceConfig());
    xsettingsClass.name(name);

    // TODO(andrealin): Service address.
    xsettingsClass.serviceAddress("Service address.");
    xsettingsClass.servicePort(productServiceConfig.getServicePort());
    xsettingsClass.authScopes(productServiceConfig.getAuthScopes(context.getDocument()));

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

    xsettingsClass.hasDefaultServiceAddress(interfaceConfig.hasDefaultServiceAddress());
    xsettingsClass.hasDefaultServiceScopes(interfaceConfig.hasDefaultServiceScopes());
    xsettingsClass.hasDefaultInstance(interfaceConfig.hasDefaultInstance());
    xsettingsClass.packagePath(namer.getPackagePath());
    xsettingsClass.stubInterfaceName(
        getAndSaveNicknameForStubType(context, namer.getApiStubInterfaceName(interfaceConfig)));
    xsettingsClass.grpcStubClassName(
        getAndSaveNicknameForStubType(context, namer.getApiGrpcStubClassName(interfaceConfig)));

    return xsettingsClass.build();
  }

  private StaticLangFileView<StaticLangStubInterfaceView> generateStubInterfaceFile(
      DiscoGapicInterfaceContext context) {
    StaticLangFileView.Builder<StaticLangStubInterfaceView> fileView =
        StaticLangFileView.<StaticLangStubInterfaceView>newBuilder();

    fileView.classView(generateStubInterface(context));
    fileView.templateFileName(STUB_INTERFACE_TEMPLATE_FILENAME);

    String outputPath =
        pathMapper.getOutputPath(context.getInterfaceFullName(), context.getProductConfig());
    String className = context.getNamer().getApiStubInterfaceName(context.getInterfaceConfig());
    fileView.outputPath(
        outputPath + File.separator + "stub" + File.separator + className + ".java");

    // must be done as the last step to catch all imports
    fileView.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return fileView.build();
  }

  private StaticLangStubInterfaceView generateStubInterface(DiscoGapicInterfaceContext context) {
    DiscoGapicInterfaceConfig interfaceConfig = context.getInterfaceConfig();

    addStubInterfaceImports(context);

    List<StaticLangApiMethodView> methods = generateApiMethods(context);

    StaticLangStubInterfaceView.Builder stubInterface = StaticLangStubInterfaceView.newBuilder();

    stubInterface.doc(serviceTransformer.generateServiceDoc(context, null));

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
      Document model,
      GapicProductConfig productConfig,
      SurfaceNamer namer,
      List<ServiceDocView> serviceDocs) {
    PackageInfoView.Builder packageInfo = PackageInfoView.newBuilder();

    packageInfo.templateFileName(PACKAGE_INFO_TEMPLATE_FILENAME);

    // TODO(andrealin): Service title.
    packageInfo.serviceTitle("Service title.");
    packageInfo.serviceDocs(serviceDocs);
    packageInfo.domainLayerLocation(productConfig.getDomainLayerLocation());
    packageInfo.authScopes(productServiceConfig.getAuthScopes(model));

    packageInfo.fileHeader(
        fileHeaderTransformer.generateFileHeader(
            productConfig, ImportSectionView.newBuilder().build(), namer));

    String outputPath = pathMapper.getOutputPath(null, productConfig);
    packageInfo.outputPath(outputPath + "/package-info.java");

    return packageInfo.build();
  }

  private void addApiImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    // TODO several of these can be deleted (e.g. DiscoGapic doesn't use grpc)
    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("com.google.api.gax.core.BackgroundResource");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.UnaryCallable");
    typeTable.saveNicknameFor("com.google.api.pathtemplate.PathTemplate");
    typeTable.saveNicknameFor("java.util.concurrent.TimeUnit");
    typeTable.saveNicknameFor("java.io.Closeable");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.util.ArrayList");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.concurrent.ScheduledExecutorService");
    typeTable.saveNicknameFor("javax.annotation.Generated");
  }

  private void addSettingsImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    typeTable.saveNicknameFor("com.google.api.core.ApiFunction");
    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("com.google.api.gax.core.CredentialsProvider");
    typeTable.saveNicknameFor("com.google.api.gax.core.ExecutorProvider");
    typeTable.saveNicknameFor("com.google.api.gax.core.GoogleCredentialsProvider");
    typeTable.saveNicknameFor("com.google.api.gax.core.InstantiatingExecutorProvider");
    typeTable.saveNicknameFor("com.google.api.gax.core.PropertiesProvider");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.ChannelProvider");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.GrpcStatusCode");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.GrpcTransport");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.GrpcTransportProvider");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.InstantiatingChannelProvider");
    typeTable.saveNicknameFor("com.google.api.gax.retrying.RetrySettings");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.ClientContext");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.ClientSettings");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.StatusCode");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.SimpleCallSettings");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.TransportProvider");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.UnaryCallSettings");
    typeTable.saveNicknameFor("com.google.auth.Credentials");
    typeTable.saveNicknameFor("com.google.common.collect.ImmutableList");
    typeTable.saveNicknameFor("com.google.common.collect.ImmutableMap");
    typeTable.saveNicknameFor("com.google.common.collect.ImmutableSet");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("com.google.common.collect.Sets");
    typeTable.saveNicknameFor("io.grpc.ManagedChannel");
    typeTable.saveNicknameFor("io.grpc.Status");
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
    if (interfaceConfig.hasGrpcStreamingMethods()) {
      typeTable.saveNicknameFor("com.google.api.gax.rpc.StreamingCallSettings");
    }
    if (interfaceConfig.hasLongRunningOperations()) {
      typeTable.saveNicknameFor("com.google.api.gax.rpc.OperationCallSettings");
      typeTable.saveNicknameFor("com.google.longrunning.Operation");
      typeTable.saveNicknameFor("com.google.api.gax.grpc.OperationTimedPollAlgorithm");
    }
  }

  private void addStubInterfaceImports(DiscoGapicInterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();

    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("com.google.api.gax.core.BackgroundResource");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.UnaryCallable");
    typeTable.saveNicknameFor("javax.annotation.Generated");

    DiscoGapicInterfaceConfig interfaceConfig = context.getInterfaceConfig();
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

  public SettingsDocView generateSettingsDoc(
      DiscoGapicInterfaceContext context, StaticLangApiMethodView exampleApiMethod) {
    SurfaceNamer namer = context.getNamer();
    SettingsDocView.Builder settingsDoc = SettingsDocView.newBuilder();
    ProductServiceConfig productServiceConfig = new ProductServiceConfig();

    // TODO(andrealin): Service address.
    settingsDoc.serviceAddress("Service address.");
    settingsDoc.servicePort(productServiceConfig.getServicePort());
    settingsDoc.exampleApiMethodName(exampleApiMethod.name());
    settingsDoc.exampleApiMethodSettingsGetter(exampleApiMethod.settingsGetterName());
    settingsDoc.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    settingsDoc.settingsVarName(namer.getApiSettingsVariableName(context.getInterfaceConfig()));
    settingsDoc.settingsClassName(namer.getApiSettingsClassName(context.getInterfaceConfig()));
    settingsDoc.settingsBuilderVarName(
        namer.getApiSettingsBuilderVarName(context.getInterfaceConfig()));
    settingsDoc.hasDefaultInstance(context.getInterfaceConfig().hasDefaultInstance());
    return settingsDoc.build();
  }

  private List<StaticLangApiMethodView> generateApiMethods(DiscoGapicInterfaceContext context) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();

    for (MethodModel method : context.getSupportedMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      DiscoGapicMethodContext requestMethodContext = context.asRequestMethodContext(method);

      if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            DiscoGapicMethodContext flattenedMethodContext =
                context.asFlattenedMethodContext(
                    method, flatteningGroup, context.getInterfaceName());
            apiMethods.add(
                apiMethodTransformer.generatePagedFlattenedMethod(flattenedMethodContext));
          }
        }
        apiMethods.add(apiMethodTransformer.generatePagedRequestObjectMethod(requestMethodContext));
        apiMethods.add(apiMethodTransformer.generatePagedCallableMethod(requestMethodContext));
        apiMethods.add(
            apiMethodTransformer.generateUnpagedListCallableMethod(requestMethodContext));
      } else {
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            DiscoGapicMethodContext flattenedMethodContext =
                context.asFlattenedMethodContext(
                    method, flatteningGroup, context.getInterfaceName());
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
