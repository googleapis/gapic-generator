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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.GeneratorVersionProvider;
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig;
import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProductServiceConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.nodejs.NodeJSUtils;
import com.google.api.codegen.transformer.BatchingTransformer;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.GrpcStubTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.js.JSTypeTable;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.DynamicLangXApiView;
import com.google.api.codegen.viewmodel.GrpcStreamingDetailView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;
import com.google.api.codegen.viewmodel.PathTemplateGetterFunctionView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.VersionIndexRequireView;
import com.google.api.codegen.viewmodel.metadata.VersionIndexView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/** Responsible for producing GAPIC surface views for NodeJS */
public class NodeJSGapicSurfaceTransformer implements ModelToViewTransformer {
  private static final String INDEX_TEMPLATE_FILE = "nodejs/index.snip";
  private static final String VERSION_INDEX_TEMPLATE_FILE = "nodejs/version_index.snip";
  private static final String XAPI_TEMPLATE_FILENAME = "nodejs/main.snip";

  private final GapicCodePathMapper pathMapper;
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new NodeJSImportSectionTransformer());
  private final DynamicLangApiMethodTransformer apiMethodTransformer =
      new DynamicLangApiMethodTransformer(new NodeJSApiMethodParamTransformer());
  private final ServiceTransformer serviceTransformer = new ServiceTransformer();
  private final GrpcStubTransformer grpcStubTransformer = new GrpcStubTransformer();
  private final PageStreamingTransformer pageStreamingTransformer = new PageStreamingTransformer();
  private final BatchingTransformer batchingTransformer = new BatchingTransformer();
  private final PathTemplateTransformer pathTemplateTransformer = new PathTemplateTransformer();
  private final PackageMetadataConfig packageConfig;

  public NodeJSGapicSurfaceTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageConfig) {
    this.pathMapper = pathMapper;
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(
        INDEX_TEMPLATE_FILE, VERSION_INDEX_TEMPLATE_FILE, XAPI_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    Iterable<Interface> apiInterfaces = new InterfaceView().getElementIterable(model);
    ImmutableList.Builder<ViewModel> models = ImmutableList.builder();
    NodeJSSurfaceNamer surfaceNamer =
        new NodeJSSurfaceNamer(productConfig.getPackageName(), NodeJSUtils.isGcloud(productConfig));
    models.addAll(generateIndexViews(apiInterfaces, surfaceNamer, productConfig));
    models.addAll(generateApiClasses(model, productConfig, surfaceNamer));
    return models.build();
  }

  private List<ViewModel> generateApiClasses(
      Model model, GapicProductConfig productConfig, NodeJSSurfaceNamer surfaceNamer) {
    ImmutableList.Builder<ViewModel> models = ImmutableList.builder();
    FeatureConfig featureConfig = new NodeJSFeatureConfig();
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      ModelTypeTable modelTypeTable =
          new ModelTypeTable(
              new JSTypeTable(productConfig.getPackageName()),
              new NodeJSModelTypeNameConverter(productConfig.getPackageName()));
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, modelTypeTable, surfaceNamer, featureConfig);
      models.add(generateApiClass(context));
    }
    return models.build();
  }

  private ViewModel generateApiClass(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    String subPath = pathMapper.getOutputPath(context.getInterface(), context.getProductConfig());
    List<ApiMethodView> methods = generateApiMethods(context);

    DynamicLangXApiView.Builder xapiClass = DynamicLangXApiView.newBuilder();

    xapiClass.templateFileName(XAPI_TEMPLATE_FILENAME);
    xapiClass.outputPath(subPath + "/" + namer.getServiceFileName(context.getInterfaceConfig()));

    xapiClass.fileHeader(fileHeaderTransformer.generateFileHeader(context));
    xapiClass.protoFilename(context.getInterface().getFile().getSimpleName());

    xapiClass.name(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    xapiClass.doc(serviceTransformer.generateServiceDoc(context, methods.get(0)));
    xapiClass.stubs(grpcStubTransformer.generateGrpcStubs(context));

    ProductServiceConfig productServiceConfig = new ProductServiceConfig();
    xapiClass.serviceAddress(productServiceConfig.getServiceAddress(context.getInterface()));
    xapiClass.servicePort(productServiceConfig.getServicePort());
    xapiClass.serviceTitle(productServiceConfig.getTitle(context.getInterface()));
    xapiClass.authScopes(productServiceConfig.getAuthScopes(context.getInterface()));
    xapiClass.hasDefaultServiceAddress(context.getInterfaceConfig().hasDefaultServiceAddress());
    xapiClass.hasDefaultServiceScopes(context.getInterfaceConfig().hasDefaultServiceScopes());

    xapiClass.pageStreamingDescriptors(pageStreamingTransformer.generateDescriptors(context));
    xapiClass.batchingDescriptors(batchingTransformer.generateDescriptors(context));
    xapiClass.longRunningDescriptors(createLongRunningDescriptors(context));
    xapiClass.grpcStreamingDescriptors(createGrpcStreamingDescriptors(context));
    xapiClass.hasPageStreamingMethods(context.getInterfaceConfig().hasPageStreamingMethods());
    xapiClass.hasBatchingMethods(context.getInterfaceConfig().hasBatchingMethods());
    xapiClass.hasLongRunningOperations(context.getInterfaceConfig().hasLongRunningOperations());

    xapiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    xapiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    xapiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    xapiClass.pathTemplateGetterFunctions(ImmutableList.<PathTemplateGetterFunctionView>of());

    xapiClass.methodKeys(ImmutableList.<String>of());
    xapiClass.interfaceKey(context.getInterface().getFullName());
    xapiClass.clientConfigPath(namer.getClientConfigPath(context.getInterface()));
    xapiClass.grpcClientTypeName(
        namer.getAndSaveNicknameForGrpcClientTypeName(
            context.getModelTypeTable(), context.getInterface()));

    xapiClass.apiMethods(methods);

    xapiClass.toolkitVersion(GeneratorVersionProvider.getGeneratorVersion());

    xapiClass.packageVersion(
        packageConfig.generatedPackageVersionBound(TargetLanguage.NODEJS).lower());

    xapiClass.validDescriptorsNames(generateValidDescriptorsNames(context));
    xapiClass.constructorName(namer.getApiWrapperClassConstructorName(context.getInterface()));
    xapiClass.isGcloud(NodeJSUtils.isGcloud(context.getProductConfig()));

    return xapiClass.build();
  }

  private List<String> generateValidDescriptorsNames(GapicInterfaceContext context) {
    ImmutableList.Builder<String> validDescriptorsNames = ImmutableList.builder();
    if (context.getInterfaceConfig().hasPageStreamingMethods()) {
      validDescriptorsNames.add("PAGE_DESCRIPTORS");
    }
    if (context.getInterfaceConfig().hasBatchingMethods()) {
      validDescriptorsNames.add("bundleDescriptors");
    }
    if (context.getInterfaceConfig().hasGrpcStreamingMethods()) {
      validDescriptorsNames.add("STREAM_DESCRIPTORS");
    }
    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      validDescriptorsNames.add("self.longrunningDescriptors");
    }
    return validDescriptorsNames.build();
  }

  private List<ApiMethodView> generateApiMethods(GapicInterfaceContext context) {
    ImmutableList.Builder<ApiMethodView> apiMethods = ImmutableList.builder();

    for (Method method : context.getSupportedMethods()) {
      apiMethods.add(apiMethodTransformer.generateMethod(context.asDynamicMethodContext(method)));
    }

    return apiMethods.build();
  }

  private List<GrpcStreamingDetailView> createGrpcStreamingDescriptors(
      GapicInterfaceContext context) {
    List<GrpcStreamingDetailView> result = new ArrayList<>();

    for (Method method : context.getGrpcStreamingMethods()) {
      GrpcStreamingConfig grpcStreamingConfig =
          context.asDynamicMethodContext(method).getMethodConfig().getGrpcStreaming();
      String resourcesFieldGetFunction = null;
      if (grpcStreamingConfig.hasResourceField()) {
        resourcesFieldGetFunction =
            context.getNamer().getFieldGetFunctionName(grpcStreamingConfig.getResourcesField());
      }

      result.add(
          GrpcStreamingDetailView.newBuilder()
              .methodName(context.getNamer().getApiMethodName(method, VisibilityConfig.PUBLIC))
              .grpcStreamingType(grpcStreamingConfig.getType())
              .grpcResourcesField(resourcesFieldGetFunction)
              .streamTypeName(context.getNamer().getStreamTypeName(grpcStreamingConfig.getType()))
              .build());
    }

    return result;
  }

  private List<LongRunningOperationDetailView> createLongRunningDescriptors(
      GapicInterfaceContext context) {
    List<LongRunningOperationDetailView> result = new ArrayList<>();

    for (Method method : context.getLongRunningMethods()) {
      GapicMethodContext methodContext = context.asDynamicMethodContext(method);
      LongRunningConfig lroConfig = methodContext.getMethodConfig().getLongRunningConfig();
      TypeRef returnType = lroConfig.getReturnType();
      TypeRef metadataType = lroConfig.getMetadataType();
      result.add(
          LongRunningOperationDetailView.newBuilder()
              .methodName(context.getNamer().getApiMethodName(method, VisibilityConfig.PUBLIC))
              .constructorName("")
              .clientReturnTypeName("")
              .operationPayloadTypeName(context.getModelTypeTable().getFullNameFor(returnType))
              .isEmptyOperation(ServiceMessages.s_isEmptyType(lroConfig.getReturnType()))
              .metadataTypeName(context.getModelTypeTable().getFullNameFor(metadataType))
              .implementsCancel(true)
              .implementsDelete(true)
              .build());
    }

    return result;
  }

  private List<ViewModel> generateIndexViews(
      Iterable<Interface> apiInterfaces,
      NodeJSSurfaceNamer namer,
      GapicProductConfig productConfig) {
    FileHeaderTransformer fileHeaderTransformer =
        new FileHeaderTransformer(new NodeJSImportSectionTransformer());
    ArrayList<ViewModel> indexViews = new ArrayList<>();

    ArrayList<VersionIndexRequireView> requireViews = new ArrayList<>();
    for (Interface apiInterface : apiInterfaces) {
      requireViews.add(
          VersionIndexRequireView.newBuilder()
              .clientName(
                  namer.getApiWrapperVariableName(productConfig.getInterfaceConfig(apiInterface)))
              .fileName(namer.getClientFileName(apiInterface))
              .build());
    }
    String version = namer.getApiWrapperModuleVersion();
    boolean hasVersion = version != null && !version.isEmpty();
    String indexOutputPath = hasVersion ? "src/" + version + "/index.js" : "src/index.js";
    VersionIndexView.Builder indexViewbuilder =
        VersionIndexView.newBuilder()
            .templateFileName(INDEX_TEMPLATE_FILE)
            .outputPath(indexOutputPath)
            .requireViews(requireViews)
            .primaryService(requireViews.get(0))
            .packageVersion(
                packageConfig.generatedPackageVersionBound(TargetLanguage.NODEJS).lower())
            .fileHeader(
                fileHeaderTransformer.generateFileHeader(
                    productConfig, ImportSectionView.newBuilder().build(), namer));
    if (hasVersion) {
      indexViewbuilder.apiVersion(version);
    }
    indexViews.add(indexViewbuilder.build());

    if (hasVersion) {
      String versionIndexOutputPath = "src/index.js";
      VersionIndexView.Builder versionIndexViewBuilder =
          VersionIndexView.newBuilder()
              .templateFileName(VERSION_INDEX_TEMPLATE_FILE)
              .outputPath(versionIndexOutputPath)
              .requireViews(new ArrayList<VersionIndexRequireView>())
              .apiVersion(version)
              .packageVersion(
                  packageConfig.generatedPackageVersionBound(TargetLanguage.NODEJS).lower())
              .fileHeader(
                  fileHeaderTransformer.generateFileHeader(
                      productConfig, ImportSectionView.newBuilder().build(), namer));
      indexViews.add(versionIndexViewBuilder.build());
    }
    return indexViews;
  }
}
