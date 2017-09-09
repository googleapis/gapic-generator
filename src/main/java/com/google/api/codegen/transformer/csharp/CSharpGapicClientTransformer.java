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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.ProductServiceConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.BatchingTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.ParamWithSimpleDoc;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.RetryDefinitionsTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiCallableImplType;
import com.google.api.codegen.viewmodel.ApiCallableView;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.ModifyMethodView;
import com.google.api.codegen.viewmodel.ReroutedGrpcView;
import com.google.api.codegen.viewmodel.SettingsDocView;
import com.google.api.codegen.viewmodel.StaticLangApiAndSettingsFileView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangApiView;
import com.google.api.codegen.viewmodel.StaticLangResourceNamesView;
import com.google.api.codegen.viewmodel.StaticLangSettingsView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CSharpGapicClientTransformer implements ModelToViewTransformer {

  private static final String XAPI_TEMPLATE_FILENAME = "csharp/gapic_client.snip";
  private static final String RESOURCENAMES_TEMPLATE_FILENAME = "csharp/gapic_resourcenames.snip";

  private final GapicCodePathMapper pathMapper;
  private final StaticLangApiMethodTransformer apiMethodTransformer =
      new CSharpApiMethodTransformer();
  private final ServiceTransformer serviceTransformer = new ServiceTransformer();
  private final PathTemplateTransformer pathTemplateTransformer = new PathTemplateTransformer();
  private final ApiCallableTransformer apiCallableTransformer = new ApiCallableTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());
  private final PageStreamingTransformer pageStreamingTransformer = new PageStreamingTransformer();
  private final BatchingTransformer batchingTransformer = new BatchingTransformer();
  private final RetryDefinitionsTransformer retryDefinitionsTransformer =
      new RetryDefinitionsTransformer();
  private final CSharpCommonTransformer csharpCommonTransformer = new CSharpCommonTransformer();

  public CSharpGapicClientTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new CSharpSurfaceNamer(productConfig.getPackageName());
    CSharpFeatureConfig featureConfig = new CSharpFeatureConfig();

    Interface lastApiInterface = null;
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface,
              productConfig,
              createTypeTable(productConfig.getPackageName()),
              namer,
              featureConfig);

      surfaceDocs.add(generateApiAndSettingsView(context));
      lastApiInterface = apiInterface;
    }

    GapicInterfaceContext context =
        GapicInterfaceContext.create(
            lastApiInterface,
            productConfig,
            createTypeTable(productConfig.getPackageName()),
            namer,
            featureConfig);
    surfaceDocs.add(generateResourceNamesView(context));

    return surfaceDocs;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(XAPI_TEMPLATE_FILENAME, RESOURCENAMES_TEMPLATE_FILENAME);
  }

  private ModelTypeTable createTypeTable(String implicitPackageName) {
    return new ModelTypeTable(
        new CSharpTypeTable(implicitPackageName),
        new CSharpModelTypeNameConverter(implicitPackageName));
  }

  private StaticLangResourceNamesView generateResourceNamesView(GapicInterfaceContext context) {
    StaticLangResourceNamesView.Builder view = StaticLangResourceNamesView.newBuilder();
    view.templateFileName(RESOURCENAMES_TEMPLATE_FILENAME);
    String outputPath =
        pathMapper.getOutputPath(context.getInterface(), context.getProductConfig());
    view.outputPath(outputPath + File.separator + "ResourceNames.cs");
    view.resourceNames(pathTemplateTransformer.generateResourceNames(context));
    view.resourceProtos(pathTemplateTransformer.generateResourceProtos(context));
    context.getModelTypeTable().saveNicknameFor("Google.Api.Gax.GaxPreconditions");
    context.getModelTypeTable().saveNicknameFor("System.Linq.Enumerable");
    context.getModelTypeTable().saveNicknameFor("System.InvalidOperationException");
    view.fileHeader(fileHeaderTransformer.generateFileHeader(context));
    return view.build();
  }

  private StaticLangApiAndSettingsFileView generateApiAndSettingsView(
      GapicInterfaceContext context) {
    StaticLangApiAndSettingsFileView.Builder fileView =
        StaticLangApiAndSettingsFileView.newBuilder();

    fileView.templateFileName(XAPI_TEMPLATE_FILENAME);

    fileView.api(generateApiClass(context));
    fileView.settings(generateSettingsClass(context));

    String outputPath =
        pathMapper.getOutputPath(context.getInterface(), context.getProductConfig());
    String name = context.getNamer().getApiWrapperClassName(context.getInterfaceConfig());
    fileView.outputPath(outputPath + File.separator + name + ".cs");

    // must be done as the last step to catch all imports
    csharpCommonTransformer.addCommonImports(context);
    fileView.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return fileView.build();
  }

  private StaticLangApiView generateApiClass(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiView.Builder apiClass = StaticLangApiView.newBuilder();
    List<StaticLangApiMethodView> methods = generateApiMethods(context);

    apiClass.doc(serviceTransformer.generateServiceDoc(context, null));

    apiClass.name(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    apiClass.implName(namer.getApiWrapperClassImplName(context.getInterfaceConfig()));
    apiClass.grpcServiceName(namer.getGrpcContainerTypeName(context.getInterface()));
    String grpcTypeName = namer.getGrpcServiceClassName(context.getInterface());
    int dotIndex = grpcTypeName.indexOf('.');
    apiClass.grpcTypeNameOuter(grpcTypeName.substring(0, dotIndex));
    apiClass.grpcTypeNameInner(grpcTypeName.substring(dotIndex + 1, grpcTypeName.length()));
    apiClass.settingsClassName(
        context.getNamer().getApiSettingsClassName(context.getInterfaceConfig()));
    List<ApiCallableView> callables = new ArrayList<>();
    for (ApiCallableView call : apiCallableTransformer.generateStaticLangApiCallables(context)) {
      if (call.type() == ApiCallableImplType.SimpleApiCallable
          || call.type() == ApiCallableImplType.BatchingApiCallable
          || call.type() == ApiCallableImplType.InitialOperationApiCallable
          || call.type() == ApiCallableImplType.BidiStreamingApiCallable
          || call.type() == ApiCallableImplType.ServerStreamingApiCallable) {
        callables.add(call);
      }
    }
    apiClass.apiCallableMembers(callables);
    apiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    apiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    apiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    apiClass.apiMethods(methods);
    List<StaticLangApiMethodView> methodsImpl = new ArrayList<>();
    for (StaticLangApiMethodView method : methods) {
      if (methodTypeHasImpl(method.type())) {
        methodsImpl.add(method);
      }
    }
    apiClass.apiMethodsImpl(methodsImpl);
    apiClass.hasDefaultInstance(context.getInterfaceConfig().hasDefaultInstance());
    apiClass.hasLongRunningOperations(context.getInterfaceConfig().hasLongRunningOperations());
    apiClass.reroutedGrpcClients(generateReroutedGrpcView(context));
    apiClass.modifyMethods(generateModifyMethods(context));

    return apiClass.build();
  }

  private boolean methodTypeHasImpl(ClientMethodType type) {
    switch (type) {
      case RequestObjectMethod:
      case AsyncRequestObjectMethod:
      case PagedRequestObjectMethod:
      case AsyncPagedRequestObjectMethod:
      case OperationRequestObjectMethod:
      case AsyncOperationRequestObjectMethod:
        return true;
      default:
        return false;
    }
  }

  private StaticLangSettingsView generateSettingsClass(GapicInterfaceContext context) {
    StaticLangSettingsView.Builder settingsClass = StaticLangSettingsView.newBuilder();
    settingsClass.doc(generateSettingsDoc(context));
    String name = context.getNamer().getApiSettingsClassName(context.getInterfaceConfig());
    settingsClass.name(name);
    ProductServiceConfig productServiceConfig = new ProductServiceConfig();
    settingsClass.serviceAddress(
        productServiceConfig.getServiceAddress(context.getInterface().getModel()));
    settingsClass.servicePort(productServiceConfig.getServicePort());
    settingsClass.authScopes(productServiceConfig.getAuthScopes(context.getInterface().getModel()));
    settingsClass.callSettings(generateCallSettings(context));
    settingsClass.pageStreamingDescriptors(
        pageStreamingTransformer.generateDescriptorClasses(context));
    settingsClass.pagedListResponseFactories(
        pageStreamingTransformer.generateFactoryClasses(context));
    settingsClass.batchingDescriptors(batchingTransformer.generateDescriptorClasses(context));
    settingsClass.retryCodesDefinitions(
        retryDefinitionsTransformer.generateRetryCodesDefinitions(context));
    settingsClass.retryParamsDefinitions(
        retryDefinitionsTransformer.generateRetryParamsDefinitions(context));
    GapicInterfaceConfig interfaceConfig = context.getInterfaceConfig();
    settingsClass.hasDefaultServiceAddress(interfaceConfig.hasDefaultServiceAddress());
    settingsClass.hasDefaultServiceScopes(interfaceConfig.hasDefaultServiceScopes());
    settingsClass.hasDefaultInstance(interfaceConfig.hasDefaultInstance());

    return settingsClass.build();
  }

  public List<ApiCallSettingsView> generateCallSettings(GapicInterfaceContext context) {
    // This method can be removed once mixins are supported in C#
    List<ApiCallSettingsView> settingsMembers = new ArrayList<>();
    for (Method method : csharpCommonTransformer.getSupportedMethods(context)) {
      List<ApiCallSettingsView> calls =
          apiCallableTransformer.generateApiCallableSettings(
              context.asRequestMethodContext(method));
      settingsMembers.addAll(calls);
    }
    return settingsMembers;
  }

  private List<ReroutedGrpcView> generateReroutedGrpcView(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    Set<ReroutedGrpcView> reroutedViews = new LinkedHashSet<>();
    for (Method method : csharpCommonTransformer.getSupportedMethods(context)) {
      GapicMethodConfig methodConfig = context.getMethodConfig(method);
      String reroute = methodConfig.getRerouteToGrpcInterface();
      if (reroute != null) {
        ReroutedGrpcView rerouted =
            ReroutedGrpcView.newBuilder()
                .grpcClientVarName(namer.getReroutedGrpcClientVarName(methodConfig))
                .typeName(namer.getReroutedGrpcTypeName(context.getModelTypeTable(), methodConfig))
                .getMethodName(namer.getReroutedGrpcMethodName(methodConfig))
                .build();
        reroutedViews.add(rerouted);
      }
    }
    return new ArrayList<ReroutedGrpcView>(reroutedViews);
  }

  private List<ModifyMethodView> generateModifyMethods(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getModelTypeTable();
    List<ModifyMethodView> modifyMethods = new ArrayList<>();
    Set<TypeRef> modifyTypes = new HashSet<TypeRef>();
    for (Method method : csharpCommonTransformer.getSupportedMethods(context)) {
      TypeRef inputType = method.getInputType();
      if (modifyTypes.contains(inputType)) {
        continue;
      }
      modifyTypes.add(inputType);
      GapicMethodConfig methodContext = context.asRequestMethodContext(method).getMethodConfig();
      ModifyMethodView.Builder builder = ModifyMethodView.builder();
      builder.name(namer.getModifyMethodName(method));
      builder.requestTypeName(typeTable.getAndSaveNicknameFor(inputType));
      builder.grpcStreamingType(methodContext.getGrpcStreamingType());
      modifyMethods.add(builder.build());
    }
    return modifyMethods;
  }

  private List<StaticLangApiMethodView> generateApiMethods(GapicInterfaceContext context) {
    List<ParamWithSimpleDoc> pagedMethodAdditionalParams =
        new ImmutableList.Builder<ParamWithSimpleDoc>()
            .addAll(csharpCommonTransformer.pagedMethodAdditionalParams())
            .addAll(csharpCommonTransformer.callSettingsParam())
            .build();

    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    for (Method method : csharpCommonTransformer.getSupportedMethods(context)) {
      GapicMethodConfig methodConfig = context.getMethodConfig(method);
      GapicMethodContext requestMethodContext = context.asRequestMethodContext(method);
      if (methodConfig.isGrpcStreaming()) {
        apiMethods.add(
            apiMethodTransformer.generateGrpcStreamingRequestObjectMethod(requestMethodContext));
      } else if (methodConfig.isLongRunningOperation()) {
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            GapicMethodContext methodContext =
                context.asFlattenedMethodContext(method, flatteningGroup);
            apiMethods.add(
                apiMethodTransformer.generateAsyncOperationFlattenedMethod(
                    methodContext,
                    csharpCommonTransformer.callSettingsParam(),
                    ClientMethodType.AsyncOperationFlattenedCallSettingsMethod,
                    true));
            apiMethods.add(
                apiMethodTransformer.generateAsyncOperationFlattenedMethod(
                    methodContext,
                    csharpCommonTransformer.cancellationTokenParam(),
                    ClientMethodType.AsyncOperationFlattenedCancellationMethod,
                    true));
            apiMethods.add(
                apiMethodTransformer.generateOperationFlattenedMethod(
                    methodContext, csharpCommonTransformer.callSettingsParam()));
          }
        }
        apiMethods.add(
            apiMethodTransformer.generateAsyncOperationRequestObjectMethod(
                requestMethodContext, csharpCommonTransformer.callSettingsParam(), true));
        apiMethods.add(
            apiMethodTransformer.generateOperationRequestObjectMethod(
                requestMethodContext, csharpCommonTransformer.callSettingsParam()));
      } else if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            GapicMethodContext methodContext =
                context.asFlattenedMethodContext(method, flatteningGroup);
            apiMethods.add(
                apiMethodTransformer.generatePagedFlattenedAsyncMethod(
                    methodContext, pagedMethodAdditionalParams));
            apiMethods.add(
                apiMethodTransformer.generatePagedFlattenedMethod(
                    methodContext, pagedMethodAdditionalParams));
          }
        }
        apiMethods.add(
            apiMethodTransformer.generatePagedRequestObjectAsyncMethod(
                requestMethodContext, csharpCommonTransformer.callSettingsParam()));
        apiMethods.add(
            apiMethodTransformer.generatePagedRequestObjectMethod(
                requestMethodContext, csharpCommonTransformer.callSettingsParam()));
      } else {
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            GapicMethodContext methodContext =
                context.asFlattenedMethodContext(method, flatteningGroup);
            apiMethods.add(
                apiMethodTransformer.generateFlattenedAsyncMethod(
                    methodContext,
                    csharpCommonTransformer.callSettingsParam(),
                    ClientMethodType.FlattenedAsyncCallSettingsMethod));
            apiMethods.add(
                apiMethodTransformer.generateFlattenedAsyncMethod(
                    methodContext,
                    csharpCommonTransformer.cancellationTokenParam(),
                    ClientMethodType.FlattenedAsyncCancellationTokenMethod));
            apiMethods.add(
                apiMethodTransformer.generateFlattenedMethod(
                    methodContext, csharpCommonTransformer.callSettingsParam()));
          }
        }
        apiMethods.add(
            apiMethodTransformer.generateRequestObjectAsyncMethod(
                requestMethodContext, csharpCommonTransformer.callSettingsParam()));
        apiMethods.add(
            apiMethodTransformer.generateRequestObjectMethod(
                requestMethodContext, csharpCommonTransformer.callSettingsParam()));
      }
    }

    return apiMethods;
  }

  public SettingsDocView generateSettingsDoc(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    SettingsDocView.Builder settingsDoc = SettingsDocView.newBuilder();
    ProductServiceConfig productServiceConfig = new ProductServiceConfig();
    settingsDoc.serviceAddress(
        productServiceConfig.getServiceAddress(context.getInterface().getModel()));
    settingsDoc.servicePort(productServiceConfig.getServicePort());
    settingsDoc.exampleApiMethodName(""); // Unused in C#
    settingsDoc.exampleApiMethodSettingsGetter(""); // Unused in C#
    settingsDoc.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    settingsDoc.settingsVarName(namer.getApiSettingsVariableName(context.getInterfaceConfig()));
    settingsDoc.settingsClassName(namer.getApiSettingsClassName(context.getInterfaceConfig()));
    settingsDoc.settingsBuilderVarName(
        namer.getApiSettingsBuilderVarName(context.getInterfaceConfig()));
    settingsDoc.hasDefaultInstance(context.getInterfaceConfig().hasDefaultInstance());
    return settingsDoc.build();
  }
}
