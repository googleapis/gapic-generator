/* Copyright 2016 Google LLC
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
package com.google.api.codegen.transformer.php;

import com.google.api.HttpRule;
import com.google.api.codegen.GeneratorVersionProvider;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProductServiceConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.TypeModel;
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
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.php.PhpPackageUtil;
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.DescriptorConfigView;
import com.google.api.codegen.viewmodel.DynamicLangXApiSubclassView;
import com.google.api.codegen.viewmodel.DynamicLangXApiView;
import com.google.api.codegen.viewmodel.GrpcStreamingDetailView;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;
import com.google.api.codegen.viewmodel.RestConfigView;
import com.google.api.codegen.viewmodel.RestMethodConfigView;
import com.google.api.codegen.viewmodel.RestPlaceholderConfigView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.pathtemplate.PathTemplate;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.lang.IllegalStateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  private static final String DESCRIPTOR_CONFIG_TEMPLATE_FILENAME = "php/descriptor_config.snip";
  private static final String REST_CONFIG_TEMPLATE_FILENAME = "php/rest_config.snip";

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
    return Arrays.asList(
        API_TEMPLATE_FILENAME,
        API_IMPL_TEMPLATE_FILENAME,
        DESCRIPTOR_CONFIG_TEMPLATE_FILENAME,
        REST_CONFIG_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    ProtoApiModel apiModel = new ProtoApiModel(model);
    for (InterfaceModel apiInterface : apiModel.getInterfaces()) {
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
    surfaceData.add(buildDescriptorConfigViewModel(gapicImplContext));
    surfaceData.add(buildRestConfigViewModel(gapicImplContext));
    return surfaceData;
  }

  private ViewModel buildGapicClientViewModel(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();

    addApiImports(context);

    List<ApiMethodView> methods = generateApiMethods(context);

    DynamicLangXApiView.Builder apiImplClass = DynamicLangXApiView.newBuilder();

    apiImplClass.doc(
        serviceTransformer.generateServiceDoc(context, methods.get(0), context.getProductConfig()));

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
    apiImplClass.clientConfigPath(namer.getClientConfigPath(context.getInterfaceConfig()));
    apiImplClass.clientConfigName(
        Name.upperCamel(context.getInterfaceConfig().getInterfaceModel().getSimpleName())
            .toLowerUnderscore());
    apiImplClass.interfaceKey(context.getInterface().getFullName());
    String grpcClientTypeName =
        namer.getAndSaveNicknameForGrpcClientTypeName(
            context.getImportTypeTable(), context.getInterfaceModel());
    apiImplClass.grpcClientTypeName(grpcClientTypeName);

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
      TypeModel returnType = lroConfig.getReturnType();
      TypeModel metadataType = lroConfig.getMetadataType();
      result.add(
          LongRunningOperationDetailView.newBuilder()
              .methodName(context.getNamer().getApiMethodName(method, VisibilityConfig.PUBLIC))
              .upperCamelMethodName(context.getNamer().getGrpcMethodName(method))
              .constructorName("")
              .clientReturnTypeName("")
              .operationPayloadTypeName(context.getImportTypeTable().getFullNameFor(returnType))
              .isEmptyOperation(lroConfig.getReturnType().isEmptyType())
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

  private ViewModel buildDescriptorConfigViewModel(GapicInterfaceContext context) {
    return DescriptorConfigView.newBuilder()
        .pageStreamingDescriptors(pageStreamingTransformer.generateDescriptors(context))
        .hasPageStreamingMethods(context.getInterfaceConfig().hasPageStreamingMethods())
        .hasBatchingMethods(context.getInterfaceConfig().hasBatchingMethods())
        .longRunningDescriptors(createLongRunningDescriptors(context))
        .hasLongRunningOperations(context.getInterfaceConfig().hasLongRunningOperations())
        .grpcStreamingDescriptors(createGrpcStreamingDescriptors(context))
        .interfaceKey(context.getInterface().getFullName())
        .templateFileName(DESCRIPTOR_CONFIG_TEMPLATE_FILENAME)
        .outputPath(generateConfigOutputPath(context, "descriptor_config"))
        .build();
  }

  private ViewModel buildRestConfigViewModel(GapicInterfaceContext context) {
    return RestConfigView.newBuilder()
        .templateFileName(REST_CONFIG_TEMPLATE_FILENAME)
        .outputPath(generateConfigOutputPath(context, "rest_client_config"))
        .interfaceKey(context.getInterface().getFullName())
        .apiMethods(generateRestMethodConfigViews(context))
        .build();
  }

  private List<RestMethodConfigView> generateRestMethodConfigViews(GapicInterfaceContext context) {
    List<RestMethodConfigView> configViews = new ArrayList<>();

    for (MethodModel methodModel : context.getSupportedMethods()) {
      GapicMethodContext methodContext = context.asDynamicMethodContext(methodModel);

      if (!methodContext.getMethodConfig().isGrpcStreaming()) {
        configViews.add(generateRestMethodConfigView(methodContext));
      }
    }

    return configViews;
  }

  private RestMethodConfigView generateRestMethodConfigView(GapicMethodContext context) {
    RestMethodConfigView.Builder restMethodConfig = RestMethodConfigView.newBuilder();
    Method method = context.getMethod();
    Map<String, String> httpMethodMap = new HashMap<>();
    HttpRule httpRule = getHttpRule(method.getOptionFields());
    String body = httpRule.getBody();

    httpMethodMap.put("get", httpRule.getGet());
    httpMethodMap.put("put", httpRule.getPut());
    httpMethodMap.put("delete", httpRule.getDelete());
    httpMethodMap.put("post", httpRule.getPost());
    httpMethodMap.put("patch", httpRule.getPatch());

    for (Map.Entry<String, String> entry : httpMethodMap.entrySet()) {
      String uriTemplate = entry.getValue();

      if (uriTemplate.isEmpty()) {
        continue;
      }

      Set<String> templateVars = PathTemplate.create(uriTemplate).vars();

      restMethodConfig.placeholders(generateRestPlaceholderConfigViews(context, templateVars));
      restMethodConfig.hasPlaceholders(templateVars.size() > 0);
      restMethodConfig.method(entry.getKey());
      restMethodConfig.uriTemplate(uriTemplate);

      break;
    }

    restMethodConfig.name(method.getSimpleName());
    restMethodConfig.hasBody(!body.isEmpty());
    restMethodConfig.body(body);

    return restMethodConfig.build();
  }

  private List<RestPlaceholderConfigView> generateRestPlaceholderConfigViews(
      GapicMethodContext context, Set<String> templateVars) {
    List<RestPlaceholderConfigView> placeholderViews = new ArrayList<>(templateVars.size());

    for (String var : templateVars) {
      placeholderViews.add(generateRestPlaceholderConfigView(context, var));
    }

    return placeholderViews;
  }

  private RestPlaceholderConfigView generateRestPlaceholderConfigView(
      GapicMethodContext context, String var) {
    SurfaceNamer namer = context.getNamer();
    MethodModel methodModel = context.getMethodConfig().getMethodModel();
    RestPlaceholderConfigView.Builder placeholderView = RestPlaceholderConfigView.newBuilder();
    String[] getters = var.split("\\.");
    String fullName = context.getSurfaceInterfaceContext().getInterface().getFullName();

    for (int i = 0; i < getters.length; i++) {
      getters[i] =
          namer.getFieldGetFunctionName(methodModel.getInputType(), Name.anyLower(getters[i]));
    }

    if (fullName.equals("google.longrunning.Operations")) {
      placeholderView.format(String.format("operations/{%s}", var));
      placeholderView.hasSpecialFormat(true);
    } else {
      placeholderView.hasSpecialFormat(false);
    }

    placeholderView.name(var);
    placeholderView.getters(Arrays.asList(getters));

    return placeholderView.build();
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
              .upperCamelMethodName(context.getNamer().getGrpcMethodName(method))
              .grpcStreamingType(grpcStreamingConfig.getType())
              .grpcResourcesField(resourcesFieldGetFunction)
              .build());
    }

    return result;
  }

  private void addApiImports(GapicInterfaceContext context) {
    ModelTypeTable typeTable = context.getImportTypeTable();
    GapicInterfaceConfig interfaceConfig = context.getInterfaceConfig();

    typeTable.saveNicknameFor("\\Google\\ApiCore\\ApiException");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\GapicClientTrait");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\PathTemplate");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\RequestParamsHeaderDescriptor");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\RetrySettings");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\Transport\\TransportInterface");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\ValidationException");
    typeTable.saveNicknameFor("\\Google\\Auth\\CredentialsLoader");
    typeTable.saveNicknameFor("\\Grpc\\Channel");
    typeTable.saveNicknameFor("\\Grpc\\ChannelCredentials");

    if (interfaceConfig.hasGrpcStreamingMethods()) {
      typeTable.saveNicknameFor("\\Google\\ApiCore\\Call");
    }

    if (interfaceConfig.hasLongRunningOperations()) {
      typeTable.saveNicknameFor("\\Google\\ApiCore\\LongRunning\\OperationsClient");
      typeTable.saveNicknameFor("\\Google\\ApiCore\\OperationResponse");
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

  private String generateConfigOutputPath(GapicInterfaceContext context, String name) {
    GapicInterfaceConfig interfaceConfig = context.getInterfaceConfig();
    String outputPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    return outputPath.substring(0, outputPath.length() - 5)
        + "/resources/"
        + Name.upperCamel(interfaceConfig.getInterfaceModel().getSimpleName())
            .join(name)
            .toLowerUnderscore()
        + ".php";
  }

  private HttpRule getHttpRule(Map<FieldDescriptor, Object> optionFields) {
    for (FieldDescriptor fieldDescriptor : optionFields.keySet()) {
      if (fieldDescriptor.getFullName().equals("google.api.http")) {
        return (HttpRule) optionFields.get(fieldDescriptor);
      }
    }

    throw new IllegalStateException("A HttpRule option must be defined.");
  }
}
