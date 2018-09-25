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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.ResourceNameConfig;
import com.google.api.codegen.config.ResourceNameMessageConfigs;
import com.google.api.codegen.config.ResourceNameType;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.BatchingTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.ParamWithSimpleDoc;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.RetryDefinitionsTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.csharp.CSharpAliasMode;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiCallableImplType;
import com.google.api.codegen.viewmodel.ApiCallableView;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.ModifyMethodView;
import com.google.api.codegen.viewmodel.ResourceNameSingleView;
import com.google.api.codegen.viewmodel.ResourceNameView;
import com.google.api.codegen.viewmodel.ResourceProtoFieldView;
import com.google.api.codegen.viewmodel.ResourceProtoView;
import com.google.api.codegen.viewmodel.SettingsDocView;
import com.google.api.codegen.viewmodel.StaticLangApiAndSettingsFileView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangApiView;
import com.google.api.codegen.viewmodel.StaticLangResourceNamesView;
import com.google.api.codegen.viewmodel.StaticLangSettingsView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/* Transforms a ProtoApiModel into the standard GAPIC library for C#. */
public class CSharpGapicClientTransformer implements ModelToViewTransformer<ProtoApiModel> {

  private static final String API_TEMPLATE_FILENAME = "csharp/gapic_client.snip";
  private static final String RESOURCENAMES_TEMPLATE_FILENAME = "csharp/gapic_resourcenames.snip";

  private static final CSharpAliasMode ALIAS_MODE = CSharpAliasMode.Global;

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
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new CSharpSurfaceNamer(productConfig.getPackageName(), ALIAS_MODE);
    CSharpFeatureConfig featureConfig = new CSharpFeatureConfig();

    InterfaceModel lastApiInterface = null;
    for (InterfaceModel apiInterface : model.getInterfaces()) {
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface,
              productConfig,
              csharpCommonTransformer.createTypeTable(productConfig.getPackageName(), ALIAS_MODE),
              namer,
              featureConfig);

      surfaceDocs.add(generateApiAndSettingsView(context));
      lastApiInterface = apiInterface;
    }

    GapicInterfaceContext context =
        GapicInterfaceContext.create(
            lastApiInterface,
            productConfig,
            csharpCommonTransformer.createTypeTable(productConfig.getPackageName(), ALIAS_MODE),
            namer,
            featureConfig);
    surfaceDocs.add(generateResourceNamesView(context));

    return surfaceDocs;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(API_TEMPLATE_FILENAME, RESOURCENAMES_TEMPLATE_FILENAME);
  }

  private StaticLangResourceNamesView generateResourceNamesView(GapicInterfaceContext context) {
    StaticLangResourceNamesView.Builder view = StaticLangResourceNamesView.newBuilder();
    view.templateFileName(RESOURCENAMES_TEMPLATE_FILENAME);
    String outputPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), context.getProductConfig());
    view.outputPath(outputPath + File.separator + "ResourceNames.cs");
    List<ResourceNameView> resourceNames =
        pathTemplateTransformer
            .generateResourceNames(context)
            .stream()
            .filter(
                x ->
                    x.type() != com.google.api.codegen.viewmodel.ResourceNameType.SINGLE
                        || ((ResourceNameSingleView) x).commonResourceName() == null)
            .collect(Collectors.toList());
    view.resourceNames(resourceNames);
    view.resourceProtos(generateResourceProtos(context));
    context.getImportTypeTable().saveNicknameFor("Google.Api.Gax.GaxPreconditions");
    context.getImportTypeTable().saveNicknameFor("System.Linq.Enumerable");
    context.getImportTypeTable().saveNicknameFor("System.InvalidOperationException");
    view.fileHeader(fileHeaderTransformer.generateFileHeader(context));
    return view.build();
  }

  private List<ResourceProtoView> generateResourceProtos(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    ResourceNameMessageConfigs resourceConfigs =
        context.getProductConfig().getResourceNameMessageConfigs();
    String csharpDefaultNamespace = context.getProductConfig().getPackageName();
    ListMultimap<String, FieldModel> fieldsByMessage =
        resourceConfigs.getFieldsWithResourceNamesByMessage();
    Map<String, FieldConfig> fieldConfigMap =
        context.getProductConfig().getDefaultResourceNameFieldConfigMap();
    List<ResourceProtoView> protos = new ArrayList<>();
    // Get the default proto package name for this interface.
    String defaultPackageName = context.getInterfaceModel().getParentFullName();
    for (Entry<String, Collection<FieldModel>> entry : fieldsByMessage.asMap().entrySet()) {
      String msgName = entry.getKey();
      if (!msgName.startsWith(defaultPackageName)) {
        // If the proto is not in this package, don't generate a partial class for it.
        continue;
      }
      // msgName is the "<proto package name>.<C# proto class name>".
      // Remove the proto package name, and prepend the C# namespace
      msgName = csharpDefaultNamespace + msgName.substring(msgName.lastIndexOf('.'));
      Collection<FieldModel> fields = new ArrayList<>(entry.getValue());
      ResourceProtoView.Builder protoBuilder = ResourceProtoView.newBuilder();
      protoBuilder.protoClassName(namer.getTypeNameConverter().getTypeName(msgName).getNickname());
      List<ResourceProtoFieldView> fieldViews = new ArrayList<>();
      for (FieldModel field : fields) {
        FieldConfig fieldConfig = fieldConfigMap.get(field.getFullName());
        ResourceNameConfig resourceNameConfig = fieldConfig.getResourceNameConfig();
        String fieldTypeSimpleName = namer.getResourceTypeName(resourceNameConfig);
        boolean isAny = false;
        if (fieldTypeSimpleName.equals("IResourceName")) {
          fieldTypeSimpleName = CSharpTypeTable.ALIAS_GAX + "::IResourceName";
          isAny = true;
        }
        String fieldTypeName =
            context
                .getImportTypeTable()
                .getAndSaveNicknameForTypedResourceName(fieldConfig, fieldTypeSimpleName);
        if (field.isRepeated()) {
          fieldTypeName =
              fieldTypeName.replaceFirst(
                  CSharpTypeTable.ALIAS_SYSTEM_COLLECTIONS_GENERIC + "::IEnumerable",
                  CSharpTypeTable.ALIAS_GAX + "::ResourceNameList");
        } else if (resourceNameConfig.getCommonResourceName() == null && !isAny) {
          // Needs to be fully qualifed because the 'fieldTypeName' class name will be
          // the same as a property name on this proto message.
          fieldTypeName = namer.getPackageName() + "." + fieldTypeName;
        }

        String fieldDocTypeName = fieldTypeName.replace('<', '{').replace('>', '}');
        String fieldElementTypeName =
            context
                .getImportTypeTable()
                .getAndSaveNicknameForResourceNameElementType(fieldConfig, fieldTypeSimpleName);
        ResourceProtoFieldView fieldView =
            ResourceProtoFieldView.newBuilder()
                .typeName(fieldTypeName)
                .parseMethodTypeName(fieldTypeName)
                .docTypeName(fieldDocTypeName)
                .elementTypeName(fieldElementTypeName)
                .isAny(fieldConfig.getResourceNameType() == ResourceNameType.ANY)
                .isRepeated(field.isRepeated())
                .isOneof(fieldConfig.getResourceNameType() == ResourceNameType.ONEOF)
                .propertyName(namer.getResourceNameFieldGetFunctionName(fieldConfig))
                .underlyingPropertyName(namer.publicMethodName(Name.from(field.getSimpleName())))
                .build();
        fieldViews.add(fieldView);
      }
      protoBuilder.fields(fieldViews);
      protos.add(protoBuilder.build());
    }
    // Sort alphabetically, to make generated code deterministic.
    Collections.sort(
        protos,
        new Comparator<ResourceProtoView>() {
          @Override
          public int compare(ResourceProtoView a, ResourceProtoView b) {
            return a.protoClassName().compareTo(b.protoClassName());
          }
        });
    return protos;
  }

  private StaticLangApiAndSettingsFileView generateApiAndSettingsView(
      GapicInterfaceContext context) {
    StaticLangApiAndSettingsFileView.Builder fileView =
        StaticLangApiAndSettingsFileView.newBuilder();

    fileView.templateFileName(API_TEMPLATE_FILENAME);

    fileView.api(generateApiClass(context));
    fileView.settings(generateSettingsClass(context));

    String outputPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), context.getProductConfig());
    String name = context.getNamer().getApiWrapperClassName(context.getInterfaceConfig());
    fileView.outputPath(outputPath + File.separator + name + ".cs");

    // must be done as the last step to catch all imports
    csharpCommonTransformer.addCommonImports(context);
    context.getImportTypeTable().saveNicknameFor("Google.Protobuf.SomeKindOfProtobuf");
    fileView.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return fileView.build();
  }

  private StaticLangApiView generateApiClass(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiView.Builder apiClass = StaticLangApiView.newBuilder();
    List<StaticLangApiMethodView> methods = generateApiMethods(context);

    apiClass.doc(serviceTransformer.generateServiceDoc(context, null, context.getProductConfig()));

    apiClass.name(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    apiClass.implName(namer.getApiWrapperClassImplName(context.getInterfaceConfig()));
    apiClass.grpcServiceName(namer.getGrpcContainerTypeName(context.getInterfaceModel()));
    String grpcTypeName = namer.getGrpcServiceClassName(context.getInterfaceModel());
    int dotIndex = grpcTypeName.indexOf('.');
    apiClass.grpcTypeNameOuter(grpcTypeName.substring(0, dotIndex));
    apiClass.grpcTypeNameInner(grpcTypeName.substring(dotIndex + 1, grpcTypeName.length()));
    apiClass.settingsClassName(
        context.getNamer().getApiSettingsClassName(context.getInterfaceConfig()));
    List<ApiCallableView> callables = new ArrayList<>();
    for (ApiCallableView call : apiCallableTransformer.generateStaticLangApiCallables(context)) {
      if (call.type() == ApiCallableImplType.SimpleApiCallable
          || call.type() == ApiCallableImplType.BatchingApiCallable
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
    apiClass.reroutedGrpcClients(csharpCommonTransformer.generateReroutedGrpcView(context));
    apiClass.modifyMethods(generateModifyMethods(context));
    apiClass.apiHasUnaryMethod(
        methods.stream().anyMatch(m -> m.grpcStreamingType() == GrpcStreamingType.NonStreaming));
    apiClass.apiHasServerStreamingMethod(
        methods.stream().anyMatch(m -> m.grpcStreamingType() == GrpcStreamingType.ServerStreaming));
    apiClass.apiHasClientStreamingMethod(
        methods.stream().anyMatch(m -> m.grpcStreamingType() == GrpcStreamingType.ClientStreaming));
    apiClass.apiHasBidiStreamingMethod(
        methods.stream().anyMatch(m -> m.grpcStreamingType() == GrpcStreamingType.BidiStreaming));

    return apiClass.build();
  }

  private boolean methodTypeHasImpl(ClientMethodType type) {
    switch (type) {
      case RequestObjectMethod:
      case AsyncRequestObjectCallSettingsMethod:
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
    ApiModel model = context.getApiModel();
    settingsClass.serviceAddress(context.getServiceAddress());
    settingsClass.servicePort(model.getServicePort());
    settingsClass.authScopes(model.getAuthScopes());
    settingsClass.callSettings(generateCallSettings(context));
    settingsClass.pageStreamingDescriptors(
        pageStreamingTransformer.generateDescriptorClasses(context));
    settingsClass.batchingDescriptors(batchingTransformer.generateDescriptorClasses(context));
    settingsClass.retryCodesDefinitions(
        retryDefinitionsTransformer.generateRetryCodesDefinitions(context));
    settingsClass.retryParamsDefinitions(
        retryDefinitionsTransformer.generateRetryParamsDefinitions(context));
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    settingsClass.hasDefaultServiceAddress(interfaceConfig.hasDefaultServiceAddress());
    settingsClass.hasDefaultServiceScopes(interfaceConfig.hasDefaultServiceScopes());
    settingsClass.hasDefaultInstance(interfaceConfig.hasDefaultInstance());

    return settingsClass.build();
  }

  public List<ApiCallSettingsView> generateCallSettings(GapicInterfaceContext context) {
    // This method can be removed once mixins are supported in C#
    List<ApiCallSettingsView> settingsMembers = new ArrayList<>();
    for (MethodModel method : csharpCommonTransformer.getSupportedMethods(context)) {
      List<ApiCallSettingsView> calls =
          apiCallableTransformer.generateApiCallableSettings(
              context.asRequestMethodContext(method));
      settingsMembers.addAll(calls);
    }
    return settingsMembers;
  }

  private List<ModifyMethodView> generateModifyMethods(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    List<ModifyMethodView> modifyMethods = new ArrayList<>();
    Set<String> modifyTypeNames = new HashSet<>();
    for (MethodModel method : csharpCommonTransformer.getSupportedMethods(context)) {
      MethodContext methodContext = context.asRequestMethodContext(method);
      String inputTypeFullName = methodContext.getMethodModel().getInputFullName();
      if (modifyTypeNames.contains(inputTypeFullName)) {
        continue;
      }
      modifyTypeNames.add(inputTypeFullName);
      MethodConfig methodConfig = methodContext.getMethodConfig();
      ModifyMethodView.Builder builder = ModifyMethodView.builder();
      builder.name(namer.getModifyMethodName(methodContext));
      builder.requestTypeName(
          method.getAndSaveRequestTypeName(context.getImportTypeTable(), context.getNamer()));
      builder.grpcStreamingType(methodConfig.getGrpcStreamingType());
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
    for (MethodModel method : csharpCommonTransformer.getSupportedMethods(context)) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      MethodContext requestMethodContext = context.asRequestMethodContext(method);
      if (methodConfig.isGrpcStreaming()) {
        // Only for protobuf-based APIs.
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            GapicMethodContext methodContext =
                context.asFlattenedMethodContext(method, flatteningGroup);
            apiMethods.add(
                apiMethodTransformer.generateGrpcStreamingFlattenedMethod(
                    methodContext, csharpCommonTransformer.callSettingsParam()));
          }
        }
        apiMethods.add(
            apiMethodTransformer.generateGrpcStreamingRequestObjectMethod(requestMethodContext));
      } else if (methodConfig.isLongRunningOperation()) {
        // Only for protobuf-based APIs.
        GapicMethodContext gapicMethodContext = (GapicMethodContext) requestMethodContext;
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            GapicMethodContext methodContext =
                context.asFlattenedMethodContext(
                    requestMethodContext.getMethodModel(), flatteningGroup);
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
                gapicMethodContext, csharpCommonTransformer.callSettingsParam()));
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
                requestMethodContext,
                csharpCommonTransformer.callSettingsParam(),
                ClientMethodType.AsyncRequestObjectCallSettingsMethod));
        apiMethods.add(
            apiMethodTransformer.generateRequestObjectAsyncMethod(
                requestMethodContext,
                csharpCommonTransformer.cancellationTokenParam(),
                ClientMethodType.AsyncRequestObjectCancellationMethod));
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
    ApiModel model = context.getApiModel();
    settingsDoc.serviceAddress(context.getServiceAddress());
    settingsDoc.servicePort(model.getServicePort());
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
