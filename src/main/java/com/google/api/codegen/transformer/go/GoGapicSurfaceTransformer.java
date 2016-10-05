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
package com.google.api.codegen.transformer.go;

import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.go.GoContextCommon;
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.ServiceConfig;
import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.GrpcStubTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.go.GoTypeTable;
import com.google.api.codegen.viewmodel.GrpcStubView;
import com.google.api.codegen.viewmodel.PackageInfoView;
import com.google.api.codegen.viewmodel.PageStreamingDescriptorClassView;
import com.google.api.codegen.viewmodel.RetryConfigDefinitionView;
import com.google.api.codegen.viewmodel.ServiceDocView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangXCombinedSurfaceView;
import com.google.api.codegen.viewmodel.StaticLangXExampleView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.gax.core.RetrySettings;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.aspects.documentation.model.ElementDocumentationAttribute;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import io.grpc.Status.Code;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class GoGapicSurfaceTransformer implements ModelToViewTransformer {

  private static final String XAPI_TEMPLATE_FILENAME = "go/main.snip";
  private static final String SAMPLE_TEMPLATE_FILENAME = "go/example.snip";
  private static final String DOC_TEMPLATE_FILENAME = "go/doc.snip";

  private final ApiCallableTransformer apiCallableTransformer = new ApiCallableTransformer();
  private final ApiMethodTransformer apiMethodTransformer = new ApiMethodTransformer();
  private final PageStreamingTransformer pageStreamingTransformer = new PageStreamingTransformer();
  private final PathTemplateTransformer pathTemplateTransformer = new PathTemplateTransformer();
  private final GrpcStubTransformer grpcStubTransformer = new GrpcStubTransformer();
  private final GapicCodePathMapper pathMapper;

  public GoGapicSurfaceTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(XAPI_TEMPLATE_FILENAME, DOC_TEMPLATE_FILENAME, SAMPLE_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> models = new ArrayList<ViewModel>();
    GoSurfaceNamer namer = new GoSurfaceNamer(apiConfig.getPackageName());
    List<ServiceDocView> serviceDocs = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service, apiConfig, createTypeTable(), namer, new FeatureConfig());
      models.add(generate(context));

      context =
          SurfaceTransformerContext.create(
              service, apiConfig, createTypeTable(), namer, new FeatureConfig());
      models.add(generateExample(context));
    }
    models.add(generatePackageInfo(model, apiConfig, namer));
    return models;
  }

  private static final ImmutableList<String> PAGE_STREAM_IMPORTS =
      ImmutableList.<String>of("math;;;", "google.golang.org/api/iterator;;;");

  private StaticLangXCombinedSurfaceView generate(SurfaceTransformerContext context) {
    StaticLangXCombinedSurfaceView.Builder view = StaticLangXCombinedSurfaceView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    Model model = context.getModel();
    Interface service = context.getInterface();
    ApiConfig apiConfig = context.getApiConfig();

    view.templateFileName(XAPI_TEMPLATE_FILENAME);
    view.serviceDoc(doc(service));
    view.localPackageName(namer.getLocalPackageName());
    view.clientTypeName(namer.getApiWrapperClassName(service));
    view.clientConstructorName(namer.getApiWrapperClassConstructorName(service));
    view.defaultClientOptionFunctionName(namer.getDefaultApiSettingsFunctionName(service));
    view.defaultCallOptionFunctionName(namer.getDefaultCallSettingsFunctionName(service));
    view.callOptionsTypeName(namer.getCallSettingsTypeName(service));
    view.serviceOriginalName(model.getServiceConfig().getTitle());
    view.servicePhraseName(namer.getServicePhraseName(service));

    String outputPath = pathMapper.getOutputPath(service, apiConfig);
    String fileName = namer.getServiceFileName(service, apiConfig.getPackageName());
    view.outputPath(outputPath + File.separator + fileName);

    List<RetryConfigDefinitionView> retryDef =
        generateRetryConfigDefinitions(context, context.getSupportedMethods());
    view.retryPairDefinitions(retryDef);

    view.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    view.pathTemplateGetters(pathTemplateTransformer.generatePathTemplateGetterFunctions(context));
    view.callSettings(apiCallableTransformer.generateCallSettings(context));
    view.apiMethods(
        generateApiMethods(context, context.getSupportedMethods(), PAGE_STREAM_IMPORTS));

    // In Go, multiple methods share the same iterator type, one iterator type per resource type.
    // We have to dedupe the iterators.
    Map<String, PageStreamingDescriptorClassView> iterators = new TreeMap<>();
    for (PageStreamingDescriptorClassView desc :
        pageStreamingTransformer.generateDescriptorClasses(context)) {
      iterators.put(desc.typeName(), desc);
    }
    view.pageStreamingDescriptorClasses(
        new ArrayList<PageStreamingDescriptorClassView>(iterators.values()));

    ServiceConfig serviceConfig = new ServiceConfig();
    view.serviceAddress(serviceConfig.getServiceAddress(service));
    view.servicePort(serviceConfig.getServicePort());
    view.authScopes(serviceConfig.getAuthScopes(service));

    view.stubs(grpcStubTransformer.generateGrpcStubs(context));

    addXApiImports(context);
    view.imports(GoTypeTable.formatImports(context.getTypeTable().getImports()));

    return view.build();
  }

  private StaticLangXExampleView generateExample(SurfaceTransformerContext context) {
    StaticLangXExampleView.Builder view = StaticLangXExampleView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    Model model = context.getModel();
    Interface service = context.getInterface();
    ApiConfig apiConfig = context.getApiConfig();

    view.templateFileName(SAMPLE_TEMPLATE_FILENAME);

    String outputPath = pathMapper.getOutputPath(service, apiConfig);
    String fileName = namer.getExampleFileName(service, apiConfig.getPackageName());
    view.outputPath(outputPath + File.separator + fileName);

    view.exampleLocalPackageName(namer.getExamplePackageName());
    view.libLocalPackageName(namer.getLocalPackageName());
    view.clientTypeName(namer.getApiWrapperClassName(service));
    view.clientConstructorName(namer.getApiWrapperClassConstructorName(service));
    view.clientConstructorExampleName(namer.getApiWrapperClassConstructorExampleName(service));
    view.apiMethods(
        generateApiMethods(
            context, context.getSupportedMethods(), Collections.<String>emptyList()));

    addXExampleImports(context);
    view.imports(GoTypeTable.formatImports(context.getTypeTable().getImports()));

    return view.build();
  }

  private PackageInfoView generatePackageInfo(
      Model model, ApiConfig apiConfig, SurfaceNamer namer) {
    String outputPath = apiConfig.getPackageName();
    String fileName = "doc.go";
    String doc = model.getServiceConfig().getDocumentation().getSummary();
    return PackageInfoView.newBuilder()
        .templateFileName(DOC_TEMPLATE_FILENAME)
        .outputPath(outputPath + File.separator + fileName)
        .serviceTitle(model.getServiceConfig().getTitle())
        .importPath(apiConfig.getPackageName())
        .packageName(namer.getLocalPackageName())
        .serviceDocs(Collections.<ServiceDocView>emptyList())
        // TODO(pongad): GoContextCommon should be deleted after we convert Go discovery to MVMM.
        .packageDoc(new GoContextCommon().getWrappedCommentLines(doc))
        .build();
  }

  @VisibleForTesting
  static ModelTypeTable createTypeTable() {
    return new ModelTypeTable(new GoTypeTable(), new GoModelTypeNameConverter());
  }

  /**
   * Returns the doc comments of Go for the proto elements.
   */
  public static List<String> doc(ProtoElement element) {
    if (!element.hasAttribute(ElementDocumentationAttribute.KEY)) {
      return ImmutableList.<String>of("");
    }

    // TODO(pongad): GoContextCommon should be deleted after we convert Go discovery to MVMM.
    return new GoContextCommon().getCommentLines(DocumentationUtil.getScopedDescription(element));
  }

  @VisibleForTesting
  List<StaticLangApiMethodView> generateApiMethods(
      SurfaceTransformerContext context, List<Method> methods, List<String> pageStreamImports) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    for (Method method : methods) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      MethodTransformerContext methodContext = context.asMethodContext(method);

      if (methodConfig.isPageStreaming()) {
        for (String imp : pageStreamImports) {
          context.getTypeTable().saveNicknameFor(imp);
        }
        apiMethods.add(apiMethodTransformer.generatePagedRequestObjectMethod(methodContext));
      } else {
        apiMethods.add(apiMethodTransformer.generateRequestObjectMethod(methodContext));
      }
    }
    return apiMethods;
  }

  @VisibleForTesting
  List<RetryConfigDefinitionView> generateRetryConfigDefinitions(
      SurfaceTransformerContext context, List<Method> methods) {
    Set<RetryConfigDefinitionView.Name> retryNames = new HashSet<>();
    for (Method method : methods) {
      MethodConfig conf = context.getMethodConfig(method);
      retryNames.add(
          RetryConfigDefinitionView.Name.create(
              conf.getRetrySettingsConfigName(), conf.getRetryCodesConfigName()));
    }

    TreeMap<RetryConfigDefinitionView.Name, RetryConfigDefinitionView> retryDef = new TreeMap<>();
    Map<String, ImmutableSet<Code>> retryCodesDef =
        context.getInterfaceConfig().getRetryCodesDefinition();
    Map<String, RetrySettings> retryParamsDef =
        context.getInterfaceConfig().getRetrySettingsDefinition();
    for (RetryConfigDefinitionView.Name name : retryNames) {
      ImmutableSet<Code> codes = retryCodesDef.get(name.retryCodesConfigName());
      if (codes.isEmpty()) {
        continue;
      }
      List<String> retryCodeNames = new ArrayList<>();
      for (Code code : codes) {
        retryCodeNames.add(context.getNamer().getStatusCodeName(code));
      }
      retryDef.put(
          name,
          RetryConfigDefinitionView.newBuilder()
              .name(name)
              .retryCodes(retryCodeNames)
              .params(retryParamsDef.get(name.retrySettingsConfigName()))
              .build());
    }
    if (!retryDef.isEmpty()) {
      context.getTypeTable().saveNicknameFor("time;;;");
      context.getTypeTable().saveNicknameFor("google.golang.org/grpc/codes;;;");
    }
    return new ArrayList<>(retryDef.values());
  }

  private Set<RetryConfigDefinitionView.Name> getRetryNames(SurfaceTransformerContext context) {
    Set<RetryConfigDefinitionView.Name> retryNames = new HashSet<>();
    for (Method method : context.getSupportedMethods()) {
      MethodConfig conf = context.getMethodConfig(method);
      retryNames.add(
          RetryConfigDefinitionView.Name.create(
              conf.getRetrySettingsConfigName(), conf.getRetryCodesConfigName()));
    }
    return retryNames;
  }

  private static final String EMPTY_PROTO_PKG = "github.com/golang/protobuf/ptypes/empty";

  private void addXApiImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("fmt;;;");
    typeTable.saveNicknameFor("runtime;;;");
    typeTable.saveNicknameFor("golang.org/x/net/context;;;");
    typeTable.saveNicknameFor("google.golang.org/grpc;;;");
    typeTable.saveNicknameFor("google.golang.org/grpc/metadata;;;");
    typeTable.saveNicknameFor("github.com/googleapis/gax-go;gax;;");
    typeTable.saveNicknameFor("google.golang.org/api/option;;;");
    typeTable.saveNicknameFor("google.golang.org/api/transport;;;");
    typeTable.getImports().remove(EMPTY_PROTO_PKG);
  }

  @VisibleForTesting
  void addXExampleImports(SurfaceTransformerContext context) {
    // Examples are different from the API. In particular, we use short declaration
    // and so we omit most type names. We only need
    //   context, to initialize the client
    //   the VKit generated library, that's what the sample is for
    //   the input types of the methods, to initialize the requests
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.getImports().clear();
    typeTable.saveNicknameFor("golang.org/x/net/context;;;");
    typeTable.saveNicknameFor(context.getApiConfig().getPackageName() + ";;;");
    for (Method method : context.getSupportedMethods()) {
      typeTable.getAndSaveNicknameFor(method.getInputType());
    }
  }
}
