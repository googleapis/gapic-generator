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
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.GrpcStreamingConfig;
import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ServiceConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.nodejs.NodeJSUtils;
import com.google.api.codegen.transformer.BatchingTransformer;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GrpcStubTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
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
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    Iterable<Interface> services = new InterfaceView().getElementIterable(model);
    ImmutableList.Builder<ViewModel> models = ImmutableList.builder();
    NodeJSSurfaceNamer surfaceNamer =
        new NodeJSSurfaceNamer(apiConfig.getPackageName(), NodeJSUtils.isGcloud(apiConfig));
    models.addAll(generateIndexViews(services, surfaceNamer, apiConfig));
    models.addAll(generateApiClasses(model, apiConfig, surfaceNamer));
    return models.build();
  }

  private List<ViewModel> generateApiClasses(
      Model model, ApiConfig apiConfig, NodeJSSurfaceNamer surfaceNamer) {
    ImmutableList.Builder<ViewModel> models = ImmutableList.builder();
    FeatureConfig featureConfig = new NodeJSFeatureConfig();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      ModelTypeTable modelTypeTable =
          new ModelTypeTable(
              new JSTypeTable(apiConfig.getPackageName()),
              new NodeJSModelTypeNameConverter(apiConfig.getPackageName()));
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service, apiConfig, modelTypeTable, surfaceNamer, featureConfig);
      models.add(generateApiClass(context));
    }
    return models.build();
  }

  private ViewModel generateApiClass(SurfaceTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    String subPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    List<ApiMethodView> methods = generateApiMethods(context);

    DynamicLangXApiView.Builder xapiClass = DynamicLangXApiView.newBuilder();

    xapiClass.templateFileName(XAPI_TEMPLATE_FILENAME);
    xapiClass.outputPath(subPath + "/" + namer.getServiceFileName(context.getInterfaceConfig()));

    xapiClass.fileHeader(fileHeaderTransformer.generateFileHeader(context));
    xapiClass.protoFilename(context.getInterface().getFile().getSimpleName());

    xapiClass.name(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    xapiClass.doc(serviceTransformer.generateServiceDoc(context, methods.get(0)));
    xapiClass.stubs(grpcStubTransformer.generateGrpcStubs(context));

    ServiceConfig serviceConfig = new ServiceConfig();
    xapiClass.serviceAddress(serviceConfig.getServiceAddress(context.getInterface()));
    xapiClass.servicePort(serviceConfig.getServicePort());
    xapiClass.serviceTitle(serviceConfig.getTitle(context.getInterface()));
    xapiClass.authScopes(serviceConfig.getAuthScopes(context.getInterface()));
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
            context.getTypeTable(), context.getInterface()));

    xapiClass.apiMethods(methods);

    xapiClass.toolkitVersion(GeneratorVersionProvider.getGeneratorVersion());

    xapiClass.packageVersion(
        packageConfig.generatedPackageVersionBound(TargetLanguage.NODEJS).lower());

    xapiClass.validDescriptorsNames(generateValidDescriptorsNames(context));
    xapiClass.constructorName(namer.getApiWrapperClassConstructorName(context.getInterface()));
    xapiClass.isGcloud(NodeJSUtils.isGcloud(context.getApiConfig()));

    return xapiClass.build();
  }

  private List<String> generateValidDescriptorsNames(SurfaceTransformerContext context) {
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

  private List<ApiMethodView> generateApiMethods(SurfaceTransformerContext context) {
    ImmutableList.Builder<ApiMethodView> apiMethods = ImmutableList.builder();

    for (Method method : context.getSupportedMethods()) {
      apiMethods.add(apiMethodTransformer.generateMethod(context.asDynamicMethodContext(method)));
    }

    return apiMethods.build();
  }

  private List<GrpcStreamingDetailView> createGrpcStreamingDescriptors(
      SurfaceTransformerContext context) {
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
      SurfaceTransformerContext context) {
    List<LongRunningOperationDetailView> result = new ArrayList<>();

    for (Method method : context.getLongRunningMethods()) {
      MethodTransformerContext methodContext = context.asDynamicMethodContext(method);
      LongRunningConfig lroConfig = methodContext.getMethodConfig().getLongRunningConfig();
      TypeRef returnType = lroConfig.getReturnType();
      TypeRef metadataType = lroConfig.getMetadataType();
      result.add(
          LongRunningOperationDetailView.newBuilder()
              .methodName(context.getNamer().getApiMethodName(method, VisibilityConfig.PUBLIC))
              .constructorName("")
              .clientReturnTypeName("")
              .operationPayloadTypeName(context.getTypeTable().getFullNameFor(returnType))
              .isEmptyOperation(ServiceMessages.s_isEmptyType(lroConfig.getReturnType()))
              .metadataTypeName(context.getTypeTable().getFullNameFor(metadataType))
              .implementsCancel(true)
              .implementsDelete(true)
              .build());
    }

    return result;
  }

  private List<ViewModel> generateIndexViews(
      Iterable<Interface> services, NodeJSSurfaceNamer namer, ApiConfig apiConfig) {
    FileHeaderTransformer fileHeaderTransformer =
        new FileHeaderTransformer(new NodeJSImportSectionTransformer());
    ArrayList<ViewModel> indexViews = new ArrayList<>();

    ArrayList<VersionIndexRequireView> requireViews = new ArrayList<>();
    for (Interface service : services) {
      requireViews.add(
          VersionIndexRequireView.newBuilder()
              .clientName(namer.getApiWrapperVariableName(apiConfig.getInterfaceConfig(service)))
              .fileName(namer.getClientFileName(service))
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
            .fileHeader(
                fileHeaderTransformer.generateFileHeader(
                    apiConfig, ImportSectionView.newBuilder().build(), namer));
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
              .fileHeader(
                  fileHeaderTransformer.generateFileHeader(
                      apiConfig, ImportSectionView.newBuilder().build(), namer));
      indexViews.add(versionIndexViewBuilder.build());
    }
    return indexViews;
  }
}
