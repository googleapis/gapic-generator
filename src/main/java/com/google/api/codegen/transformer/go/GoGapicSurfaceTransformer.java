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

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.ProductServiceConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.DefaultFeatureConfig;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.GrpcStubTransformer;
import com.google.api.codegen.transformer.IamResourceTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.go.GoTypeTable;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;
import com.google.api.codegen.viewmodel.PackageInfoView;
import com.google.api.codegen.viewmodel.PageStreamingDescriptorClassView;
import com.google.api.codegen.viewmodel.PathTemplateView;
import com.google.api.codegen.viewmodel.RetryConfigDefinitionView;
import com.google.api.codegen.viewmodel.ServiceDocView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangClientExampleFileView;
import com.google.api.codegen.viewmodel.StaticLangClientFileView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import io.grpc.Status.Code;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class GoGapicSurfaceTransformer implements ModelToViewTransformer {

  private static final String API_TEMPLATE_FILENAME = "go/main.snip";
  private static final String SAMPLE_TEMPLATE_FILENAME = "go/example.snip";
  private static final String DOC_TEMPLATE_FILENAME = "go/doc.snip";

  private static final int COMMENT_LINE_LENGTH = 75;

  private final ApiCallableTransformer apiCallableTransformer = new ApiCallableTransformer();
  private final StaticLangApiMethodTransformer apiMethodTransformer =
      new StaticLangApiMethodTransformer();
  private final FeatureConfig featureConfig = new DefaultFeatureConfig();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new GoImportSectionTransformer());
  private final GrpcStubTransformer grpcStubTransformer = new GrpcStubTransformer();
  private final IamResourceTransformer iamResourceTransformer = new IamResourceTransformer();
  private final PageStreamingTransformer pageStreamingTransformer = new PageStreamingTransformer();
  private final PathTemplateTransformer pathTemplateTransformer = new PathTemplateTransformer();
  private final ServiceMessages serviceMessages = new ServiceMessages();
  private final ServiceTransformer serviceTransformer = new ServiceTransformer();
  private final ProductServiceConfig productServiceConfig = new ProductServiceConfig();
  private final GapicCodePathMapper pathMapper;

  public GoGapicSurfaceTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(API_TEMPLATE_FILENAME, DOC_TEMPLATE_FILENAME, SAMPLE_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    List<ViewModel> models = new ArrayList<ViewModel>();
    GoSurfaceNamer namer = new GoSurfaceNamer(productConfig.getPackageName());
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, createTypeTable(), namer, featureConfig);
      models.add(generate(context));

      context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, createTypeTable(), namer, featureConfig);
      models.add(generateExample(context));
    }
    models.add(generatePackageInfo(model, productConfig, namer));
    return models;
  }

  private StaticLangClientFileView generate(GapicInterfaceContext context) {
    StaticLangClientFileView.Builder view = StaticLangClientFileView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    Model model = context.getModel();
    Interface apiInterface = context.getInterface();
    GapicProductConfig productConfig = context.getProductConfig();

    view.templateFileName(API_TEMPLATE_FILENAME);
    view.serviceDoc(serviceTransformer.generateServiceDoc(context, null));
    view.domainLayerLocation(productConfig.getDomainLayerLocation());
    view.clientTypeName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    view.clientConstructorName(namer.getApiWrapperClassConstructorName(apiInterface));
    view.defaultClientOptionFunctionName(namer.getDefaultApiSettingsFunctionName(apiInterface));
    view.defaultCallOptionFunctionName(namer.getDefaultCallSettingsFunctionName(apiInterface));
    view.callOptionsTypeName(namer.getCallSettingsTypeName(apiInterface));
    view.serviceOriginalName(model.getServiceConfig().getTitle());
    view.servicePhraseName(namer.getServicePhraseName(apiInterface));

    String outputPath = pathMapper.getOutputPath(apiInterface, productConfig);
    String fileName = namer.getServiceFileName(context.getInterfaceConfig());
    view.outputPath(outputPath + File.separator + fileName);

    List<RetryConfigDefinitionView> retryDef =
        generateRetryConfigDefinitions(context, context.getSupportedMethods());
    view.retryPairDefinitions(retryDef);

    view.pathTemplates(Collections.<PathTemplateView>emptyList());
    view.pathTemplateGetters(pathTemplateTransformer.generatePathTemplateGetterFunctions(context));
    view.callSettings(apiCallableTransformer.generateCallSettings(context));

    List<StaticLangApiMethodView> apiMethods =
        generateApiMethods(context, context.getSupportedMethods());
    view.apiMethods(apiMethods);

    view.iamResources(iamResourceTransformer.generateIamResources(context));
    if (!productConfig.getInterfaceConfig(apiInterface).getIamResources().isEmpty()) {
      context.getModelTypeTable().saveNicknameFor("cloud.google.com/go/iam;;;");
    }

    // In Go, multiple methods share the same iterator type, one iterator type per resource type.
    // We have to dedupe the iterators.
    Map<String, PageStreamingDescriptorClassView> iterators = new TreeMap<>();
    for (PageStreamingDescriptorClassView desc :
        pageStreamingTransformer.generateDescriptorClasses(context)) {
      iterators.put(desc.typeName(), desc);
    }
    view.pageStreamingDescriptorClasses(
        new ArrayList<PageStreamingDescriptorClassView>(iterators.values()));

    // Same with long running operations.
    Map<String, LongRunningOperationDetailView> lros = new TreeMap<>();
    for (StaticLangApiMethodView apiMethod : apiMethods) {
      LongRunningOperationDetailView lro = apiMethod.operationMethod();
      if (lro != null) {
        lros.put(lro.clientReturnTypeName(), lro);
      }
    }
    view.lroDetailViews(new ArrayList<LongRunningOperationDetailView>(lros.values()));

    view.serviceAddress(productServiceConfig.getServiceAddress(apiInterface.getModel()));
    view.servicePort(productServiceConfig.getServicePort());

    view.stubs(grpcStubTransformer.generateGrpcStubs(context));

    addXApiImports(context, context.getSupportedMethods());
    view.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return view.build();
  }

  private StaticLangClientExampleFileView generateExample(GapicInterfaceContext context) {
    StaticLangClientExampleFileView.Builder view = StaticLangClientExampleFileView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    Interface apiInterface = context.getInterface();
    GapicProductConfig productConfig = context.getProductConfig();

    view.templateFileName(SAMPLE_TEMPLATE_FILENAME);

    String outputPath = pathMapper.getOutputPath(apiInterface, productConfig);
    String fileName = namer.getExampleFileName(apiInterface);
    view.outputPath(outputPath + File.separator + fileName);

    view.clientTypeName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    view.clientConstructorName(namer.getApiWrapperClassConstructorName(apiInterface));
    view.clientConstructorExampleName(namer.getApiWrapperClassConstructorExampleName(apiInterface));
    view.apiMethods(generateApiMethods(context, context.getPublicMethods()));
    view.iamResources(iamResourceTransformer.generateIamResources(context));

    // Examples are different from the API. In particular, we use short declaration
    // and so we omit most type names. We only need
    //   - Context, to initialize the client
    //   - The VKit generated library, that's what the sample is for
    //   - The input types of the methods, to initialize the requests
    // So, we clear all imports; addXExampleImports will add back the ones we want.
    context.getModelTypeTable().getImports().clear();
    addXExampleImports(context, context.getPublicMethods());
    view.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return view.build();
  }

  private PackageInfoView generatePackageInfo(
      Model model, GapicProductConfig productConfig, SurfaceNamer namer) {
    String outputPath = productConfig.getPackageName();
    String fileName = "doc.go";
    PackageInfoView.Builder packageInfo = PackageInfoView.newBuilder();

    packageInfo.templateFileName(DOC_TEMPLATE_FILENAME);
    packageInfo.outputPath(outputPath + File.separator + fileName);
    packageInfo.serviceTitle(model.getServiceConfig().getTitle());
    packageInfo.importPath(productConfig.getPackageName());
    packageInfo.serviceDocs(Collections.<ServiceDocView>emptyList());
    packageInfo.packageDoc(
        CommonRenderingUtil.getDocLines(
            model.getServiceConfig().getDocumentation().getSummary(), COMMENT_LINE_LENGTH));
    packageInfo.domainLayerLocation(productConfig.getDomainLayerLocation());
    packageInfo.authScopes(productServiceConfig.getAuthScopes(model));

    packageInfo.fileHeader(
        fileHeaderTransformer.generateFileHeader(
            productConfig, ImportSectionView.newBuilder().build(), namer));

    return packageInfo.build();
  }

  static ModelTypeTable createTypeTable() {
    return new ModelTypeTable(new GoTypeTable(), new GoModelTypeNameConverter());
  }

  @VisibleForTesting
  List<StaticLangApiMethodView> generateApiMethods(
      GapicInterfaceContext context, List<Method> methods) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    for (Method method : methods) {
      GapicMethodConfig methodConfig = context.getMethodConfig(method);
      GapicMethodContext methodContext = context.asRequestMethodContext(method);

      if (method.getRequestStreaming() || method.getResponseStreaming()) {
        apiMethods.add(
            apiMethodTransformer.generateGrpcStreamingRequestObjectMethod(methodContext));
      } else if (methodConfig.isPageStreaming()) {
        apiMethods.add(apiMethodTransformer.generatePagedRequestObjectMethod(methodContext));
      } else if (methodConfig.isLongRunningOperation()) {
        apiMethods.add(apiMethodTransformer.generateOperationRequestObjectMethod(methodContext));
      } else {
        apiMethods.add(apiMethodTransformer.generateRequestObjectMethod(methodContext));
      }
    }
    return apiMethods;
  }

  @VisibleForTesting
  List<RetryConfigDefinitionView> generateRetryConfigDefinitions(
      GapicInterfaceContext context, List<Method> methods) {
    Set<RetryConfigDefinitionView.Name> retryNames = new HashSet<>();
    for (Method method : methods) {
      GapicMethodConfig conf = context.getMethodConfig(method);
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
      context.getModelTypeTable().saveNicknameFor("time;;;");
      context.getModelTypeTable().saveNicknameFor("google.golang.org/grpc/codes;;;");
    }
    return new ArrayList<>(retryDef.values());
  }

  private static final String EMPTY_PROTO_PKG = "github.com/golang/protobuf/ptypes/empty";

  @VisibleForTesting
  void addXApiImports(GapicInterfaceContext context, Collection<Method> methods) {
    ModelTypeTable typeTable = context.getModelTypeTable();
    typeTable.saveNicknameFor("cloud.google.com/go/internal/version;;;");
    typeTable.saveNicknameFor("golang.org/x/net/context;;;");
    typeTable.saveNicknameFor("google.golang.org/grpc;;;");
    typeTable.saveNicknameFor("github.com/googleapis/gax-go;gax;;");
    typeTable.saveNicknameFor("google.golang.org/api/option;;;");
    typeTable.saveNicknameFor("google.golang.org/api/transport;;;");
    typeTable.getImports().remove(EMPTY_PROTO_PKG);
    addContextImports(context, ImportContext.CLIENT, methods);
  }

  @VisibleForTesting
  void addXExampleImports(GapicInterfaceContext context, Collection<Method> methods) {
    ModelTypeTable typeTable = context.getModelTypeTable();
    typeTable.saveNicknameFor("golang.org/x/net/context;;;");
    typeTable.saveNicknameFor(context.getProductConfig().getPackageName() + ";;;");

    for (Method method : methods) {
      typeTable.getAndSaveNicknameFor(method.getInputType());
    }
    addContextImports(context, ImportContext.EXAMPLE, methods);
  }

  private void addContextImports(
      GapicInterfaceContext context, ImportContext importContext, Collection<Method> methods) {
    for (ImportKind kind : getImportKinds(context.getInterfaceConfig(), methods)) {
      ImmutableList<String> imps = CONTEXTUAL_IMPORTS.get(importContext, kind);
      if (imps != null) {
        for (String imp : imps) {
          context.getModelTypeTable().saveNicknameFor(imp);
        }
      }
    }
  }

  private Set<ImportKind> getImportKinds(
      GapicInterfaceConfig interfaceConfig, Collection<Method> methods) {
    EnumSet<ImportKind> kinds = EnumSet.noneOf(ImportKind.class);
    for (Method method : methods) {
      if (method.getResponseStreaming()) {
        kinds.add(ImportKind.SERVER_STREAM);
      }
      GapicMethodConfig methodConfig = interfaceConfig.getMethodConfig(method);
      if (methodConfig.isLongRunningOperation()) {
        kinds.add(ImportKind.LRO);
      }
      if (methodConfig.isPageStreaming()) {
        kinds.add(ImportKind.PAGE_STREAM);
      }
    }
    return kinds;
  }

  private enum ImportContext {
    CLIENT,
    EXAMPLE,
  }

  private enum ImportKind {
    PAGE_STREAM,
    LRO,
    SERVER_STREAM,
  }

  private static final ImmutableTable<ImportContext, ImportKind, ImmutableList<String>>
      CONTEXTUAL_IMPORTS =
          ImmutableTable.<ImportContext, ImportKind, ImmutableList<String>>builder()
              .put(
                  ImportContext.CLIENT,
                  ImportKind.PAGE_STREAM,
                  ImmutableList.<String>of("math;;;", "google.golang.org/api/iterator;;;"))
              .put(
                  ImportContext.EXAMPLE,
                  ImportKind.PAGE_STREAM,
                  ImmutableList.<String>of("google.golang.org/api/iterator;;;"))
              .put(
                  ImportContext.CLIENT,
                  ImportKind.LRO,
                  ImmutableList.<String>of(
                      "cloud.google.com/go/longrunning;;;",
                      "cloud.google.com/go/longrunning/autogen;lroauto;;"))
              .put(
                  ImportContext.EXAMPLE,
                  ImportKind.SERVER_STREAM,
                  ImmutableList.<String>of("io;;;"))
              .build();
}
