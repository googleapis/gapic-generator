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
import com.google.api.HttpRule.Builder;
import com.google.api.Service;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProductServiceConfig;
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
import com.google.api.codegen.viewmodel.BatchingDescriptorView;
import com.google.api.codegen.viewmodel.DescriptorConfigView;
import com.google.api.codegen.viewmodel.DynamicLangXApiSubclassView;
import com.google.api.codegen.viewmodel.DynamicLangXApiView;
import com.google.api.codegen.viewmodel.GrpcStreamingDetailView;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;
import com.google.api.codegen.viewmodel.RestConfigView;
import com.google.api.codegen.viewmodel.RestInterfaceConfigView;
import com.google.api.codegen.viewmodel.RestMethodConfigView;
import com.google.api.codegen.viewmodel.RestPlaceholderConfigView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.pathtemplate.PathTemplate;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.lang.IllegalStateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** The ModelToViewTransformer to transform a Model into the standard GAPIC surface in PHP. */
public class PhpGapicSurfaceTransformer implements ModelToViewTransformer {
  private Model serviceModel;
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
      GapicProductConfig productConfig, GapicCodePathMapper pathMapper, Model serviceModel) {
    this.serviceModel = serviceModel;
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
  public List<ViewModel> transform(ApiModel model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    for (InterfaceModel apiInterface : model.getInterfaces()) {
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
    surfaceData.add(buildDescriptorConfigViewModel(context));
    surfaceData.add(buildRestConfigViewModel(context));
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
    apiImplClass.batchingDescriptors(ImmutableList.<BatchingDescriptorView>of());
    apiImplClass.hasBatchingMethods(context.getInterfaceConfig().hasBatchingMethods());
    apiImplClass.longRunningDescriptors(createLongRunningDescriptors(context));
    apiImplClass.hasLongRunningOperations(context.getInterfaceConfig().hasLongRunningOperations());
    apiImplClass.grpcStreamingDescriptors(createGrpcStreamingDescriptors(context));

    apiImplClass.methodKeys(generateMethodKeys(context));
    apiImplClass.clientConfigPath(namer.getClientConfigPath(context.getInterfaceConfig()));
    apiImplClass.clientConfigName(namer.getClientConfigName(context.getInterfaceConfig()));
    apiImplClass.interfaceKey(context.getInterface().getFullName());
    String grpcClientTypeName =
        namer.getAndSaveNicknameForGrpcClientTypeName(
            context.getImportTypeTable(), context.getInterfaceModel());
    apiImplClass.grpcClientTypeName(grpcClientTypeName);

    apiImplClass.apiMethods(methods);

    apiImplClass.stubs(grpcStubTransformer.generateGrpcStubs(context));

    apiImplClass.hasDefaultServiceAddress(context.getInterfaceConfig().hasDefaultServiceAddress());
    apiImplClass.hasDefaultServiceScopes(context.getInterfaceConfig().hasDefaultServiceScopes());

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
              .transportMethodName(context.getNamer().getGrpcMethodName(method))
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
    String outputPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), context.getProductConfig());
    SurfaceNamer namer = context.getNamer();

    return DescriptorConfigView.newBuilder()
        .batchingDescriptors(ImmutableList.<BatchingDescriptorView>of())
        .pageStreamingDescriptors(pageStreamingTransformer.generateDescriptors(context))
        .longRunningDescriptors(createLongRunningDescriptors(context))
        .grpcStreamingDescriptors(createGrpcStreamingDescriptors(context))
        .interfaceKey(context.getInterface().getFullName())
        .templateFileName(DESCRIPTOR_CONFIG_TEMPLATE_FILENAME)
        .outputPath(
            outputPath + namer.getConfigPath(context.getInterfaceConfig(), "descriptor_config"))
        .build();
  }

  private ViewModel buildRestConfigViewModel(GapicInterfaceContext context) {
    String outputPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), context.getProductConfig());
    SurfaceNamer namer = context.getNamer();

    return RestConfigView.newBuilder()
        .templateFileName(REST_CONFIG_TEMPLATE_FILENAME)
        .outputPath(
            outputPath + namer.getConfigPath(context.getInterfaceConfig(), "rest_client_config"))
        .interfaceConfigs(generateRestInterfaceConfigViews(context))
        .build();
  }

  private List<RestInterfaceConfigView> generateRestInterfaceConfigViews(
      GapicInterfaceContext context) {
    List<RestInterfaceConfigView> configViews = new ArrayList<>();
    GapicInterfaceConfig interfaceConfig = context.getInterfaceConfig();
    SurfaceNamer namer = context.getNamer();
    Map<String, List<HttpRule>> interfaces = new TreeMap<>();
    Service serviceConfig = serviceModel.getServiceConfig();

    for (MethodModel methodModel : context.getSupportedMethods()) {
      GapicMethodContext methodContext = context.asDynamicMethodContext(methodModel);
      MethodConfig methodConfig = methodContext.getMethodConfig();
      Method method = methodContext.getMethod();

      // REST does not support streaming methods
      if (methodConfig.isGrpcStreaming()) {
        continue;
      }

      String interfaceName =
          methodConfig.getRerouteToGrpcInterface() == null
              ? context.getInterface().getFullName()
              : methodConfig.getRerouteToGrpcInterface();
      HttpRule httpRule =
          getHttpRule(method.getOptionFields())
              .toBuilder()
              .setSelector(String.format("%s.%s", interfaceName, method.getSimpleName()))
              .build();

      addHttpRuleToMap(interfaces, interfaceName, httpRule);
    }

    for (HttpRule httpRule : serviceConfig.getHttp().getRulesList()) {
      String selector = httpRule.getSelector();
      String interfaceName = selector.substring(0, selector.lastIndexOf("."));

      addHttpRuleToMap(interfaces, interfaceName, httpRule);
    }

    for (Map.Entry<String, List<HttpRule>> entry : interfaces.entrySet()) {
      configViews.add(generateRestInterfaceConfigView(entry.getKey(), entry.getValue(), namer));
    }

    return configViews;
  }

  private RestInterfaceConfigView generateRestInterfaceConfigView(
      String key, List<HttpRule> httpRules, SurfaceNamer namer) {
    return RestInterfaceConfigView.newBuilder()
        .key(key)
        .apiMethods(generateRestMethodConfigViews(httpRules, namer))
        .build();
  }

  private List<RestMethodConfigView> generateRestMethodConfigViews(
      List<HttpRule> httpRules, SurfaceNamer namer) {
    List<RestMethodConfigView> configViews = new ArrayList<>();

    for (HttpRule httpRule : httpRules) {
      configViews.add(generateRestMethodConfigView(httpRule, namer));
    }

    return configViews;
  }

  private RestMethodConfigView generateRestMethodConfigView(HttpRule httpRule, SurfaceNamer namer) {
    String body = httpRule.getBody();
    String selector = httpRule.getSelector();
    List<String> additionalBindings = new ArrayList<>();
    Map.Entry<String, String> entry = getHttpMethodEntry(httpRule);
    String uriTemplate = entry.getValue();
    Set<String> templateVars = PathTemplate.create(uriTemplate).vars();

    if (httpRule.getAdditionalBindingsCount() > 0) {
      for (HttpRule additionalBindingHttpRule : httpRule.getAdditionalBindingsList()) {
        additionalBindings.add(getHttpMethodEntry(additionalBindingHttpRule).getValue());
      }
    }

    return RestMethodConfigView.newBuilder()
        .additionalBindings(additionalBindings)
        .hasAdditionalBindings(additionalBindings.size() > 0)
        .placeholders(generateRestPlaceholderConfigViews(namer, templateVars))
        .hasPlaceholders(templateVars.size() > 0)
        .method(entry.getKey())
        .uriTemplate(uriTemplate)
        .name(selector.substring(selector.lastIndexOf(".") + 1))
        .hasBody(!body.isEmpty())
        .body(body)
        .build();
  }

  private List<RestPlaceholderConfigView> generateRestPlaceholderConfigViews(
      SurfaceNamer namer, Set<String> templateVars) {
    List<RestPlaceholderConfigView> placeholderViews = new ArrayList<>(templateVars.size());

    for (String var : templateVars) {
      placeholderViews.add(generateRestPlaceholderConfigView(namer, var));
    }

    return placeholderViews;
  }

  private RestPlaceholderConfigView generateRestPlaceholderConfigView(
      SurfaceNamer namer, String var) {
    RestPlaceholderConfigView.Builder placeholderView = RestPlaceholderConfigView.newBuilder();
    ImmutableList.Builder<String> getters = ImmutableList.builder();

    for (String getter : var.split("\\.")) {
      getters.add(
          namer.getFieldGetFunctionName(
              Name.anyLower(getter),
              SurfaceNamer.MapType.NOT_MAP,
              SurfaceNamer.Cardinality.NOT_REPEATED));
    }

    placeholderView.name(var);
    placeholderView.getters(getters.build());

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
              .transportMethodName(context.getNamer().getGrpcMethodName(method))
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
    typeTable.saveNicknameFor("\\Google\\ApiCore\\Call");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\GapicClientTrait");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\PathTemplate");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\RequestParamsHeaderDescriptor");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\RetrySettings");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\Transport\\TransportInterface");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\ValidationException");
    typeTable.saveNicknameFor("\\Google\\Auth\\CredentialsLoader");
    typeTable.saveNicknameFor("\\Grpc\\Channel");
    typeTable.saveNicknameFor("\\Grpc\\ChannelCredentials");

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

  private HttpRule getHttpRule(Map<FieldDescriptor, Object> optionFields) {
    for (FieldDescriptor fieldDescriptor : optionFields.keySet()) {
      if (fieldDescriptor.getFullName().equals("google.api.http")) {
        return (HttpRule) optionFields.get(fieldDescriptor);
      }
    }

    throw new IllegalStateException("A HttpRule option must be defined.");
  }

  private void addHttpRuleToMap(
      Map<String, List<HttpRule>> interfaces, String interfaceName, HttpRule httpRule) {
    if (interfaces.containsKey(interfaceName)) {
      List<HttpRule> httpRules = interfaces.get(interfaceName);

      for (int i = 0; i < httpRules.size(); i++) {
        // If a rule is already set, override it. This exists to allow rules
        // defined in the service configuration to take precedence as they are
        // added last.
        if (httpRules.get(i).getSelector().equals(httpRule.getSelector())) {
          httpRules.set(i, httpRule);
          return;
        }
      }

      httpRules.add(httpRule);
    } else {
      interfaces.put(interfaceName, new ArrayList<HttpRule>(Arrays.asList(httpRule)));
    }
  }

  private Map.Entry<String, String> getHttpMethodEntry(HttpRule httpRule) {
    Map<String, String> httpMethodMap = new TreeMap<>();

    httpMethodMap.put("get", httpRule.getGet());
    httpMethodMap.put("put", httpRule.getPut());
    httpMethodMap.put("delete", httpRule.getDelete());
    httpMethodMap.put("post", httpRule.getPost());
    httpMethodMap.put("patch", httpRule.getPatch());

    for (Map.Entry<String, String> entry : httpMethodMap.entrySet()) {
      if (!entry.getValue().isEmpty()) {
        return entry;
      }
    }

    throw new IllegalStateException("A HTTP method must be defined.");
  }
}
