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

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.ServiceConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.GrpcStubTransformer;
import com.google.api.codegen.transformer.ImportTypeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.DynamicLangXApiView;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
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
  private ApiMethodTransformer apiMethodTransformer;
  private GrpcStubTransformer grpcStubTransformer;

  private static final String XAPI_TEMPLATE_FILENAME = "php/main.snip";

  public PhpGapicSurfaceTransformer(ApiConfig apiConfig, GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
    this.serviceTransformer = new ServiceTransformer();
    this.pathTemplateTransformer = new PathTemplateTransformer();
    this.pageStreamingTransformer = new PageStreamingTransformer();
    this.apiMethodTransformer = new ApiMethodTransformer();
    this.grpcStubTransformer = new GrpcStubTransformer();
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(XAPI_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      ModelTypeTable modelTypeTable =
          new ModelTypeTable(
              new PhpTypeTable(apiConfig.getPackageName()),
              new PhpModelTypeNameConverter(apiConfig.getPackageName()));
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service,
              apiConfig,
              modelTypeTable,
              new PhpSurfaceNamer(apiConfig.getPackageName()),
              new PhpFeatureConfig());
      surfaceDocs.addAll(transform(context));
    }
    return surfaceDocs;
  }

  public List<ViewModel> transform(SurfaceTransformerContext context) {
    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    SurfaceNamer namer = context.getNamer();

    List<ViewModel> surfaceData = new ArrayList<>();

    addApiImports(context);

    List<ApiMethodView> methods = generateApiMethods(context);

    ImportTypeTransformer importTypeTransformer = new PhpImportTypeTransformer();

    DynamicLangXApiView.Builder xapiClass = DynamicLangXApiView.newBuilder();

    xapiClass.doc(serviceTransformer.generateServiceDoc(context, methods.get(0)));

    xapiClass.templateFileName(XAPI_TEMPLATE_FILENAME);
    xapiClass.protoFilename(context.getInterface().getFile().getSimpleName());
    xapiClass.packageName(context.getApiConfig().getPackageName());
    String name = namer.getApiWrapperClassName(context.getInterface());
    xapiClass.name(name);
    ServiceConfig serviceConfig = new ServiceConfig();
    xapiClass.serviceAddress(serviceConfig.getServiceAddress(context.getInterface()));
    xapiClass.servicePort(serviceConfig.getServicePort());
    xapiClass.serviceTitle(serviceConfig.getTitle(context.getInterface()));
    xapiClass.authScopes(serviceConfig.getAuthScopes(context.getInterface()));

    xapiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    xapiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    xapiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    xapiClass.pathTemplateGetterFunctions(
        pathTemplateTransformer.generatePathTemplateGetterFunctions(context));
    xapiClass.pageStreamingDescriptors(pageStreamingTransformer.generateDescriptors(context));
    xapiClass.longRunningDescriptors(createLongRunningDescriptors(context));
    xapiClass.hasLongRunningOperations(context.getInterfaceConfig().hasLongRunningOperations());

    xapiClass.methodKeys(generateMethodKeys(context));
    xapiClass.clientConfigPath(namer.getClientConfigPath(context.getInterface()));
    xapiClass.interfaceKey(context.getInterface().getFullName());
    String grpcClientTypeName =
        namer.getAndSaveNicknameForGrpcClientTypeName(
            context.getTypeTable(), context.getInterface());
    xapiClass.grpcClientTypeName(grpcClientTypeName);

    xapiClass.apiMethods(methods);

    xapiClass.stubs(grpcStubTransformer.generateGrpcStubs(context));

    xapiClass.hasDefaultServiceAddress(context.getInterfaceConfig().hasDefaultServiceAddress());
    xapiClass.hasDefaultServiceScopes(context.getInterfaceConfig().hasDefaultServiceScopes());

    // must be done as the last step to catch all imports
    xapiClass.imports(importTypeTransformer.generateImports(context.getTypeTable().getImports()));

    xapiClass.outputPath(outputPath + "/" + name + ".php");

    surfaceData.add(xapiClass.build());

    return surfaceData;
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

  private void addApiImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
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
      typeTable.saveNicknameFor("Google\\Longrunning\\OperationsClient");
    }
  }

  private List<String> generateMethodKeys(SurfaceTransformerContext context) {
    List<String> methodKeys = new ArrayList<>(context.getInterface().getMethods().size());

    for (Method method : context.getSupportedMethods()) {
      methodKeys.add(context.getNamer().getMethodKey(method));
    }

    return methodKeys;
  }

  private List<ApiMethodView> generateApiMethods(SurfaceTransformerContext context) {
    List<ApiMethodView> apiMethods = new ArrayList<>(context.getInterface().getMethods().size());

    for (Method method : context.getSupportedMethods()) {
      apiMethods.add(
          apiMethodTransformer.generateDynamicLangApiMethod(
              context.asDynamicMethodContext(method)));
    }

    return apiMethods;
  }
}
