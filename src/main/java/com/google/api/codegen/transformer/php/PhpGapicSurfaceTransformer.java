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
package com.google.api.codegen.transformer.php;

import com.google.api.codegen.GeneratorVersionProvider;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProductServiceConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
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
import com.google.api.codegen.util.php.PhpPackageUtil;
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.DynamicLangXApiSubclassView;
import com.google.api.codegen.viewmodel.DynamicLangXApiView;
import com.google.api.codegen.viewmodel.GrpcStreamingDetailView;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.TypeRef;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The ModelToViewTransformer to transform a Model into the standard GAPIC surface in PHP. */
public class PhpGapicSurfaceTransformer implements ModelToViewTransformer {
  private GapicCodePathMapper pathMapper;
  private ServiceTransformer serviceTransformer;
  private PathTemplateTransformer pathTemplateTransformer;
  private PageStreamingTransformer pageStreamingTransformer;
  private DynamicLangApiMethodTransformer apiMethodTransformer;
  private GrpcStubTransformer grpcStubTransformer;
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new PhpImportSectionTransformer());

  private static final String API_TEMPLATE_FILENAME = "php/partial_veneer_client.snip";
  private static final String API_IMPL_TEMPLATE_FILENAME = "php/client_impl.snip";

  public PhpGapicSurfaceTransformer(
      GapicProductConfig productConfig, GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
    this.serviceTransformer = new ServiceTransformer();
    this.pathTemplateTransformer = new PathTemplateTransformer();
    this.pageStreamingTransformer = new PageStreamingTransformer();
    this.apiMethodTransformer =
        new DynamicLangApiMethodTransformer(new PhpApiMethodParamTransformer());
    this.grpcStubTransformer = new GrpcStubTransformer();
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(API_TEMPLATE_FILENAME, API_IMPL_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    ProtoApiModel apiModel = new ProtoApiModel(model);
    for (InterfaceModel apiInterface : apiModel.getInterfaces(productConfig)) {
      ModelTypeTable modelTypeTable =
          new ModelTypeTable(
              new PhpTypeTable(productConfig.getPackageName()),
              new PhpModelTypeNameConverter(productConfig.getPackageName()));
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface,
              productConfig,
              modelTypeTable,
              new PhpSurfaceNamer(productConfig.getPackageName()),
              new PhpFeatureConfig());
      surfaceDocs.addAll(transform(context));
    }
    return surfaceDocs;
  }

  public List<ViewModel> transform(GapicInterfaceContext context) {
    GapicInterfaceContext gapicImplContext =
        context.withNewTypeTable(context.getNamer().getGapicImplNamespace());

    List<ViewModel> surfaceData = new ArrayList<>();
    surfaceData.add(buildGapicClientViewModel(gapicImplContext));
    surfaceData.add(buildClientViewModel(context));
    return surfaceData;
  }

  private ViewModel buildGapicClientViewModel(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();

    addApiImports(context);

    List<ApiMethodView> methods = generateApiMethods(context);

    DynamicLangXApiView.Builder apiImplClass = DynamicLangXApiView.newBuilder();

    apiImplClass.doc(serviceTransformer.generateServiceDoc(context, methods.get(0)));

    apiImplClass.templateFileName(API_IMPL_TEMPLATE_FILENAME);
    apiImplClass.protoFilename(context.getInterface().getFile().getSimpleName());
    String implName = namer.getApiWrapperClassImplName(context.getInterfaceConfig());
    apiImplClass.name(implName);
    ProductServiceConfig productServiceConfig = new ProductServiceConfig();
    apiImplClass.serviceAddress(
        productServiceConfig.getServiceAddress(context.getInterface().getModel()));
    apiImplClass.servicePort(productServiceConfig.getServicePort());
    apiImplClass.serviceTitle(productServiceConfig.getTitle(context.getInterface().getModel()));
    apiImplClass.authScopes(productServiceConfig.getAuthScopes(context.getInterface().getModel()));

    apiImplClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    apiImplClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    apiImplClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    apiImplClass.pathTemplateGetterFunctions(
        pathTemplateTransformer.generatePathTemplateGetterFunctions(context));
    apiImplClass.pageStreamingDescriptors(pageStreamingTransformer.generateDescriptors(context));
    apiImplClass.hasPageStreamingMethods(context.getInterfaceConfig().hasPageStreamingMethods());
    apiImplClass.hasBatchingMethods(context.getInterfaceConfig().hasBatchingMethods());
    apiImplClass.longRunningDescriptors(createLongRunningDescriptors(context));
    apiImplClass.hasLongRunningOperations(context.getInterfaceConfig().hasLongRunningOperations());
    apiImplClass.grpcStreamingDescriptors(createGrpcStreamingDescriptors(context));

    apiImplClass.methodKeys(generateMethodKeys(context));
    apiImplClass.clientConfigPath(namer.getClientConfigPath(context.getInterfaceModel()));
    apiImplClass.interfaceKey(context.getInterface().getFullName());
    String grpcClientTypeName =
        namer.getAndSaveNicknameForGrpcClientTypeName(
            context.getModelTypeTable(), context.getInterfaceModel());
    apiImplClass.grpcClientTypeName(grpcClientTypeName);
    //>>>>>>> master

    apiImplClass.apiMethods(methods);

    apiImplClass.stubs(grpcStubTransformer.generateGrpcStubs(context));

    apiImplClass.hasDefaultServiceAddress(context.getInterfaceConfig().hasDefaultServiceAddress());
    apiImplClass.hasDefaultServiceScopes(context.getInterfaceConfig().hasDefaultServiceScopes());

    apiImplClass.toolkitVersion(GeneratorVersionProvider.getGeneratorVersion());

    // must be done as the last step to catch all imports
    apiImplClass.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    String outputPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    apiImplClass.outputPath(outputPath + "/" + implName + ".php");

    return apiImplClass.build();
  }

  private ViewModel buildClientViewModel(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    String name = namer.getApiWrapperClassName(context.getInterfaceConfig());

    addApiImports(context);

    context
        .getImportTypeTable()
        .getAndSaveNicknameFor(
            PhpPackageUtil.getFullyQualifiedName(
                namer.getGapicImplNamespace(),
                namer.getApiWrapperClassImplName(context.getInterfaceConfig())));

    DynamicLangXApiSubclassView.Builder apiClass = DynamicLangXApiSubclassView.newBuilder();
    apiClass.templateFileName(API_TEMPLATE_FILENAME);
    apiClass.protoFilename(context.getInterface().getFile().getSimpleName());
    apiClass.name(name);
    apiClass.parentName(namer.getApiWrapperClassImplName(context.getInterfaceConfig()));
    apiClass.fileHeader(fileHeaderTransformer.generateFileHeader(context));
    String outputPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), context.getProductConfig());
    apiClass.outputPath(outputPath + "/" + name + ".php");

    return apiClass.build();
  }

  private List<LongRunningOperationDetailView> createLongRunningDescriptors(
      GapicInterfaceContext context) {
    List<LongRunningOperationDetailView> result = new ArrayList<>();

    for (MethodModel method : context.getLongRunningMethods()) {
      GapicMethodContext methodContext = context.asDynamicMethodContext(method);
      LongRunningConfig lroConfig = methodContext.getMethodConfig().getLongRunningConfig();
      TypeRef returnType = lroConfig.getReturnType();
      TypeRef metadataType = lroConfig.getMetadataType();
      result.add(
          LongRunningOperationDetailView.newBuilder()
              .methodName(context.getNamer().getApiMethodName(method, VisibilityConfig.PUBLIC))
              .constructorName("")
              .clientReturnTypeName("")
              .operationPayloadTypeName(context.getImportTypeTable().getFullNameFor(returnType))
              .isEmptyOperation(ServiceMessages.s_isEmptyType(lroConfig.getReturnType()))
              .metadataTypeName(context.getImportTypeTable().getFullNameFor(metadataType))
              .implementsCancel(true)
              .implementsDelete(true)
              .initialPollDelay(lroConfig.getInitialPollDelay().toMillis())
              .pollDelayMultiplier(lroConfig.getPollDelayMultiplier())
              .maxPollDelay(lroConfig.getMaxPollDelay().toMillis())
              .totalPollTimeout(lroConfig.getTotalPollTimeout().toMillis())
              .build());
    }

    return result;
  }

  private List<GrpcStreamingDetailView> createGrpcStreamingDescriptors(
      GapicInterfaceContext context) {
    List<GrpcStreamingDetailView> result = new ArrayList<>();

    for (MethodModel method : context.getGrpcStreamingMethods()) {
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
              .build());
    }

    return result;
  }

  private void addApiImports(GapicInterfaceContext context) {
    ModelTypeTable typeTable = context.getImportTypeTable();
    typeTable.saveNicknameFor("\\Google\\GAX\\AgentHeaderDescriptor");
    typeTable.saveNicknameFor("\\Google\\GAX\\ApiCallable");
    typeTable.saveNicknameFor("\\Google\\GAX\\CallSettings");
    typeTable.saveNicknameFor("\\Google\\GAX\\GrpcConstants");
    typeTable.saveNicknameFor("\\Google\\GAX\\GrpcCredentialsHelper");
    typeTable.saveNicknameFor("\\Google\\GAX\\PathTemplate");
    typeTable.saveNicknameFor("\\Google\\GAX\\ValidationException");

    if (context.getInterfaceConfig().hasPageStreamingMethods()) {
      typeTable.saveNicknameFor("\\Google\\GAX\\PageStreamingDescriptor");
    }

    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      typeTable.saveNicknameFor("\\Google\\GAX\\LongRunning\\OperationsClient");
      typeTable.saveNicknameFor("\\Google\\GAX\\OperationResponse");
    }
  }

  private List<String> generateMethodKeys(GapicInterfaceContext context) {
    List<String> methodKeys = new ArrayList<>(context.getInterface().getMethods().size());

    for (MethodModel method : context.getSupportedMethods()) {
      methodKeys.add(context.getNamer().getMethodKey(method));
    }

    return methodKeys;
  }

  private List<ApiMethodView> generateApiMethods(GapicInterfaceContext context) {
    List<ApiMethodView> apiMethods = new ArrayList<>(context.getInterface().getMethods().size());

    for (MethodModel method : context.getSupportedMethods()) {
      apiMethods.add(apiMethodTransformer.generateMethod(context.asDynamicMethodContext(method)));
    }

    return apiMethods;
  }
}
