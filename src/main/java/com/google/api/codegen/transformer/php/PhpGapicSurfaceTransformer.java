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
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.MethodModel;
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
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.api.codegen.viewmodel.ApiMethodView;
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

  private static final String XAPI_TEMPLATE_FILENAME = "php/main.snip";

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
    return Arrays.asList(XAPI_TEMPLATE_FILENAME);
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
    String outputPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    SurfaceNamer namer = context.getNamer();

    List<ViewModel> surfaceData = new ArrayList<>();

    addApiImports(context);

    List<ApiMethodView> methods = generateApiMethods(context);

    DynamicLangXApiView.Builder xapiClass = DynamicLangXApiView.newBuilder();

    xapiClass.doc(serviceTransformer.generateServiceDoc(context, methods.get(0)));

    xapiClass.templateFileName(XAPI_TEMPLATE_FILENAME);
    xapiClass.protoFilename(context.getInterface().getFile().getSimpleName());
    String name = namer.getApiWrapperClassName(context.getInterfaceConfig());
    xapiClass.name(name);
    ApiModel model = context.getApiModel();
    xapiClass.serviceAddress(model.getServiceAddress());
    xapiClass.servicePort(model.getServicePort());
    xapiClass.serviceTitle(model.getTitle());
    xapiClass.authScopes(model.getAuthScopes());

    xapiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    xapiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    xapiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    xapiClass.pathTemplateGetterFunctions(
        pathTemplateTransformer.generatePathTemplateGetterFunctions(context));
    xapiClass.pageStreamingDescriptors(pageStreamingTransformer.generateDescriptors(context));
    xapiClass.hasPageStreamingMethods(context.getInterfaceConfig().hasPageStreamingMethods());
    xapiClass.hasBatchingMethods(context.getInterfaceConfig().hasBatchingMethods());
    xapiClass.longRunningDescriptors(createLongRunningDescriptors(context));
    xapiClass.hasLongRunningOperations(context.getInterfaceConfig().hasLongRunningOperations());
    xapiClass.grpcStreamingDescriptors(createGrpcStreamingDescriptors(context));

    xapiClass.methodKeys(generateMethodKeys(context));
    xapiClass.clientConfigPath(namer.getClientConfigPath(context.getInterfaceModel()));
    xapiClass.interfaceKey(context.getInterface().getFullName());
    String grpcClientTypeName =
        namer.getAndSaveNicknameForGrpcClientTypeName(
            context.getImportTypeTable(), context.getInterfaceModel());
    xapiClass.grpcClientTypeName(grpcClientTypeName);

    xapiClass.apiMethods(methods);

    xapiClass.stubs(grpcStubTransformer.generateGrpcStubs(context));

    xapiClass.hasDefaultServiceAddress(context.getInterfaceConfig().hasDefaultServiceAddress());
    xapiClass.hasDefaultServiceScopes(context.getInterfaceConfig().hasDefaultServiceScopes());

    xapiClass.toolkitVersion(GeneratorVersionProvider.getGeneratorVersion());

    // must be done as the last step to catch all imports
    xapiClass.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    xapiClass.outputPath(outputPath + "/" + name + ".php");

    surfaceData.add(xapiClass.build());

    return surfaceData;
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
              .initialPollDelay(lroConfig.getInitialPollDelay().getMillis())
              .pollDelayMultiplier(lroConfig.getPollDelayMultiplier())
              .maxPollDelay(lroConfig.getMaxPollDelay().getMillis())
              .totalPollTimeout(lroConfig.getTotalPollTimeout().getMillis())
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
