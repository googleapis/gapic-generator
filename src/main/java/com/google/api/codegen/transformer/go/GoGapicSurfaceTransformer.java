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

import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProductConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.ProtoInterfaceModel;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.DefaultFeatureConfig;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GrpcStubTransformer;
import com.google.api.codegen.transformer.IamResourceTransformer;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.MethodContext;
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
import com.google.api.tools.framework.model.Model;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
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
    ApiModel apiModel = new ProtoApiModel(model);
    GoSurfaceNamer namer = new GoSurfaceNamer(productConfig.getPackageName());
    for (InterfaceModel apiInterface : apiModel.getInterfaces(productConfig)) {
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, createTypeTable(), namer, featureConfig);
      models.add(generate(context));

      context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, createTypeTable(), namer, featureConfig);
      models.add(generateExample(context));
    }
    models.add(generatePackageInfo(apiModel, productConfig, namer));
    return models;
  }

  private StaticLangClientFileView generate(GapicInterfaceContext context) {
    StaticLangClientFileView.Builder view = StaticLangClientFileView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    ApiModel model = context.getApiModel();
    ProtoInterfaceModel apiInterface = context.getInterfaceModel();
    GapicProductConfig productConfig = context.getProductConfig();
    GapicInterfaceConfig interfaceConfig = context.getInterfaceConfig();

    view.templateFileName(API_TEMPLATE_FILENAME);
    view.serviceDoc(serviceTransformer.generateServiceDoc(context, null));
    view.domainLayerLocation(productConfig.getDomainLayerLocation());
    view.clientTypeName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    view.clientConstructorName(namer.getApiWrapperClassConstructorName(interfaceConfig));
    view.defaultClientOptionFunctionName(namer.getDefaultApiSettingsFunctionName(interfaceConfig));
    view.defaultCallOptionFunctionName(namer.getDefaultCallSettingsFunctionName(interfaceConfig));
    view.callOptionsTypeName(namer.getCallSettingsTypeName(interfaceConfig));
    view.serviceOriginalName(model.getTitle());
    view.servicePhraseName(namer.getServicePhraseName(interfaceConfig));

    String outputPath = pathMapper.getOutputPath(apiInterface.getFullName(), productConfig);
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
    if (!((GapicInterfaceConfig) productConfig.getInterfaceConfig(apiInterface.getFullName()))
        .getIamResources()
        .isEmpty()) {
      context.getImportTypeTable().saveNicknameFor("cloud.google.com/go/iam;;;");
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

    view.serviceAddress(context.getApiModel().getServiceAddress());
    view.servicePort(model.getServicePort());

    view.stubs(grpcStubTransformer.generateGrpcStubs(context));

    addXApiImports(context, context.getSupportedMethods());
    view.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return view.build();
  }

  private StaticLangClientExampleFileView generateExample(InterfaceContext context) {
    StaticLangClientExampleFileView.Builder view = StaticLangClientExampleFileView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    InterfaceModel apiInterface = context.getInterfaceModel();
    ProductConfig productConfig = context.getProductConfig();
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();

    view.templateFileName(SAMPLE_TEMPLATE_FILENAME);

    String outputPath = pathMapper.getOutputPath(apiInterface.getFullName(), productConfig);
    String fileName = namer.getExampleFileName(context.getInterfaceConfig());
    view.outputPath(outputPath + File.separator + fileName);

    view.clientTypeName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    view.clientConstructorName(namer.getApiWrapperClassConstructorName(interfaceConfig));
    view.clientConstructorExampleName(
        namer.getApiWrapperClassConstructorExampleName(interfaceConfig));
    view.apiMethods(generateApiMethods(context, context.getPublicMethods()));
    view.iamResources(iamResourceTransformer.generateIamResources(context));

    // Examples are different from the API. In particular, we use short declaration
    // and so we omit most type names. We only need
    //   - Context, to initialize the client
    //   - The VKit generated library, that's what the sample is for
    //   - The input types of the methods, to initialize the requests
    // So, we clear all imports; addXExampleImports will add back the ones we want.
    context.getImportTypeTable().getImports().clear();
    addXExampleImports(context, context.getPublicMethods());
    view.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return view.build();
  }

  private PackageInfoView generatePackageInfo(
      ApiModel model, GapicProductConfig productConfig, SurfaceNamer namer) {
    String outputPath = productConfig.getPackageName();
    String fileName = "doc.go";
    PackageInfoView.Builder packageInfo = PackageInfoView.newBuilder();

    packageInfo.templateFileName(DOC_TEMPLATE_FILENAME);
    packageInfo.outputPath(outputPath + File.separator + fileName);
    packageInfo.serviceTitle(model.getTitle());
    packageInfo.importPath(productConfig.getPackageName());
    packageInfo.serviceDocs(Collections.<ServiceDocView>emptyList());
    packageInfo.packageDoc(
        CommonRenderingUtil.getDocLines(model.getDocumentationSummary(), COMMENT_LINE_LENGTH));
    packageInfo.domainLayerLocation(productConfig.getDomainLayerLocation());
    packageInfo.authScopes(model.getAuthScopes());

    packageInfo.fileHeader(
        fileHeaderTransformer.generateFileHeader(
            productConfig, ImportSectionView.newBuilder().build(), namer));
    packageInfo.releaseLevel(productConfig.getReleaseLevel());

    return packageInfo.build();
  }

  static ModelTypeTable createTypeTable() {
    return new ModelTypeTable(new GoTypeTable(), new GoModelTypeNameConverter());
  }

  @VisibleForTesting
  List<StaticLangApiMethodView> generateApiMethods(
      InterfaceContext context, Iterable<MethodModel> methods) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    for (MethodModel method : methods) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      MethodContext methodContext = context.asRequestMethodContext(method);

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
      InterfaceContext context, List<MethodModel> methods) {
    Set<RetryConfigDefinitionView.Name> retryNames = new HashSet<>();
    for (MethodModel method : methods) {
      MethodConfig conf = context.getMethodConfig(method);
      retryNames.add(
          RetryConfigDefinitionView.Name.create(
              conf.getRetrySettingsConfigName(), conf.getRetryCodesConfigName()));
    }

    TreeMap<RetryConfigDefinitionView.Name, RetryConfigDefinitionView> retryDef = new TreeMap<>();
    Map<String, ImmutableSet<String>> retryCodesDef =
        context.getInterfaceConfig().getRetryCodesDefinition();
    Map<String, RetrySettings> retryParamsDef =
        context.getInterfaceConfig().getRetrySettingsDefinition();
    for (RetryConfigDefinitionView.Name name : retryNames) {
      ImmutableSet<String> codes = retryCodesDef.get(name.retryCodesConfigName());
      if (codes.isEmpty()) {
        continue;
      }
      List<String> retryCodeNames = new ArrayList<>();
      for (String code : codes) {
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
      context.getImportTypeTable().saveNicknameFor("time;;;");
      context.getImportTypeTable().saveNicknameFor("google.golang.org/grpc/codes;;;");
    }
    return new ArrayList<>(retryDef.values());
  }

  private static final String EMPTY_PROTO_PKG = "github.com/golang/protobuf/ptypes/empty";

  @VisibleForTesting
  void addXApiImports(InterfaceContext context, Collection<MethodModel> methods) {
    ImportTypeTable typeTable = context.getImportTypeTable();
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
  void addXExampleImports(InterfaceContext context, Iterable<MethodModel> methods) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    typeTable.saveNicknameFor("golang.org/x/net/context;;;");
    typeTable.saveNicknameFor(context.getProductConfig().getPackageName() + ";;;");

    for (MethodModel method : methods) {
      method.getAndSaveRequestTypeName(context.getImportTypeTable(), context.getNamer());
    }
    addContextImports(context, ImportContext.EXAMPLE, methods);
  }

  private void addContextImports(
      InterfaceContext context, ImportContext importContext, Iterable<MethodModel> methods) {
    for (ImportKind kind : getImportKinds(context.getInterfaceConfig(), methods)) {
      ImmutableList<String> imps = CONTEXTUAL_IMPORTS.get(importContext, kind);
      if (imps != null) {
        for (String imp : imps) {
          context.getImportTypeTable().saveNicknameFor(imp);
        }
      }
    }
  }

  private Set<ImportKind> getImportKinds(
      InterfaceConfig interfaceConfig, Iterable<MethodModel> methods) {
    EnumSet<ImportKind> kinds = EnumSet.noneOf(ImportKind.class);
    for (MethodModel method : methods) {
      if (method.getResponseStreaming()) {
        kinds.add(ImportKind.SERVER_STREAM);
      }
      MethodConfig methodConfig = interfaceConfig.getMethodConfig(method);
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
