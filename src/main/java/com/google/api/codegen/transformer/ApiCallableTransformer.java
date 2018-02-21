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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.DiscoveryMethodModel;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.TransportProtocol;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiCallableImplType;
import com.google.api.codegen.viewmodel.ApiCallableView;
import com.google.api.codegen.viewmodel.HttpMethodView;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;
import com.google.api.codegen.viewmodel.MethodDescriptorView;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.codegen.viewmodel.ServiceMethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiCallableTransformer {

  private final BatchingTransformer batchingTransformer;
  private final RetryDefinitionsTransformer retryDefinitionsTransformer;
  private final LongRunningTransformer lroTransformer;
  private final HeaderRequestParamTransformer headerRequestParamTransformer;

  public ApiCallableTransformer() {
    this.batchingTransformer = new BatchingTransformer();
    this.retryDefinitionsTransformer = new RetryDefinitionsTransformer();
    this.lroTransformer = new LongRunningTransformer();
    this.headerRequestParamTransformer = new HeaderRequestParamTransformer();
  }

  public List<ApiCallableView> generateStaticLangApiCallables(InterfaceContext context) {
    List<ApiCallableView> callableMembers = new ArrayList<>();
    boolean excludeMixins = !context.getFeatureConfig().enableMixins();

    for (MethodModel method : context.getSupportedMethods()) {
      if (excludeMixins && context.getMethodConfig(method).getRerouteToGrpcInterface() != null) {
        continue;
      }
      callableMembers.addAll(
          generateStaticLangApiCallables(context.asRequestMethodContext(method)));
    }

    return callableMembers;
  }

  public List<ApiCallSettingsView> generateCallSettings(InterfaceContext context) {
    List<ApiCallSettingsView> settingsMembers = new ArrayList<>();

    for (MethodModel method : context.getSupportedMethods()) {
      settingsMembers.addAll(generateApiCallableSettings(context.asRequestMethodContext(method)));
    }

    return settingsMembers;
  }

  private List<ApiCallableView> generateStaticLangApiCallables(MethodContext context) {
    List<ApiCallableView> apiCallables = new ArrayList<>();

    apiCallables.add(generateMainApiCallable(context));

    if (context.getMethodConfig().isPageStreaming()) {
      apiCallables.add(generatePagedApiCallable(context));
    }

    if (context.getMethodConfig().isLongRunningOperation()) {
      // Only Protobuf-based APIs have LongRunningOperations.
      apiCallables.add(generateOperationApiCallable((GapicMethodContext) context));
    }

    return apiCallables;
  }

  private ApiCallableView generateMainApiCallable(MethodContext context) {
    MethodConfig methodConfig = context.getMethodConfig();
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();

    ApiCallableView.Builder apiCallableBuilder = ApiCallableView.newBuilder();

    apiCallableBuilder.requestTypeName(
        method.getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer()));
    apiCallableBuilder.responseTypeName(
        method.getAndSaveResponseTypeName(context.getTypeTable(), context.getNamer()));
    apiCallableBuilder.name(namer.getCallableName(method));
    apiCallableBuilder.methodName(
        namer.getApiMethodName(method, context.getMethodConfig().getVisibility()));
    apiCallableBuilder.asyncMethodName(
        namer.getAsyncApiMethodName(method, VisibilityConfig.PUBLIC));
    apiCallableBuilder.memberName(namer.getSettingsMemberName(method));
    apiCallableBuilder.settingsFunctionName(namer.getSettingsFunctionName(method));
    apiCallableBuilder.grpcClientVarName(namer.getReroutedGrpcClientVarName(methodConfig));
    apiCallableBuilder.headerRequestParams(
        headerRequestParamTransformer.generateHeaderRequestParams(context));

    setCommonApiCallableFields(context, apiCallableBuilder);

    ApiCallableImplType callableImplType = ApiCallableImplType.SimpleApiCallable;
    if (methodConfig.isGrpcStreaming()) {
      callableImplType = ApiCallableImplType.of(methodConfig.getGrpcStreamingType());
      apiCallableBuilder.grpcStreamingType(methodConfig.getGrpcStreamingType());
    } else if (methodConfig.isBatching()) {
      callableImplType = ApiCallableImplType.BatchingApiCallable;
    }
    apiCallableBuilder.type(callableImplType);
    apiCallableBuilder.interfaceTypeName(
        namer.getApiCallableTypeName(callableImplType.serviceMethodType()));

    return apiCallableBuilder.build();
  }

  private ApiCallableView generatePagedApiCallable(MethodContext context) {
    MethodConfig methodConfig = context.getMethodConfig();
    SurfaceNamer namer = context.getNamer();
    MethodModel method = context.getMethodModel();

    PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

    ApiCallableView.Builder pagedApiCallableBuilder = ApiCallableView.newBuilder();
    pagedApiCallableBuilder.type(ApiCallableImplType.PagedApiCallable);
    pagedApiCallableBuilder.interfaceTypeName(
        namer.getApiCallableTypeName(ServiceMethodType.UnaryMethod));

    String pagedResponseTypeName =
        namer.getAndSavePagedResponseTypeName(context, pageStreaming.getResourcesFieldConfig());

    pagedApiCallableBuilder.requestTypeName(
        method.getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer()));
    pagedApiCallableBuilder.responseTypeName(pagedResponseTypeName);
    pagedApiCallableBuilder.name(namer.getPagedCallableName(method));
    pagedApiCallableBuilder.methodName(
        namer.getApiMethodName(method, context.getMethodConfig().getVisibility()));
    pagedApiCallableBuilder.asyncMethodName(
        namer.getAsyncApiMethodName(method, VisibilityConfig.PUBLIC));
    pagedApiCallableBuilder.memberName(namer.getSettingsMemberName(method));
    pagedApiCallableBuilder.settingsFunctionName(namer.getSettingsFunctionName(method));
    pagedApiCallableBuilder.grpcClientVarName(namer.getReroutedGrpcClientVarName(methodConfig));
    setCommonApiCallableFields(context, pagedApiCallableBuilder);

    return pagedApiCallableBuilder.build();
  }

  private ApiCallableView generateOperationApiCallable(GapicMethodContext context) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();

    ApiCallableView.Builder operationApiCallableBuilder = ApiCallableView.newBuilder();
    operationApiCallableBuilder.type(ApiCallableImplType.OperationApiCallable);
    operationApiCallableBuilder.interfaceTypeName(
        namer.getApiCallableTypeName(ServiceMethodType.LongRunningMethod));

    LongRunningOperationDetailView lroView = lroTransformer.generateDetailView(context);
    operationApiCallableBuilder.requestTypeName(
        method.getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer()));
    operationApiCallableBuilder.responseTypeName(lroView.operationPayloadTypeName());
    operationApiCallableBuilder.metadataTypeName(lroView.metadataTypeName());
    operationApiCallableBuilder.name(namer.getOperationCallableName(method));

    setCommonApiCallableFields(context, operationApiCallableBuilder);
    operationApiCallableBuilder.settingsFunctionName(
        namer.getOperationSettingsFunctionName(method));

    return operationApiCallableBuilder.build();
  }

  private void setCommonApiCallableFields(
      MethodContext context, ApiCallableView.Builder apiCallableBuilder) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    MethodConfig methodConfig = context.getMethodConfig();

    apiCallableBuilder.methodName(
        namer.getApiMethodName(method, context.getMethodConfig().getVisibility()));
    apiCallableBuilder.asyncMethodName(
        namer.getAsyncApiMethodName(method, VisibilityConfig.PUBLIC));
    apiCallableBuilder.memberName(namer.getSettingsMemberName(method));
    apiCallableBuilder.settingsFunctionName(namer.getSettingsFunctionName(method));
    apiCallableBuilder.grpcClientVarName(namer.getReroutedGrpcClientVarName(methodConfig));
    apiCallableBuilder.methodDescriptorName(namer.getMethodDescriptorName(method));
    apiCallableBuilder.transportSettingsVar(namer.getTransportSettingsVar(method));
    apiCallableBuilder.httpMethod(generateHttpFields(context));
  }

  private HttpMethodView generateHttpFields(MethodContext context) {
    if (context.getProductConfig().getTransportProtocol().equals(TransportProtocol.HTTP)) {
      Method method = ((DiscoveryMethodModel) context.getMethodModel()).getDiscoMethod();
      HttpMethodView.Builder httpMethodView = HttpMethodView.newBuilder();
      httpMethodView.fullMethodName(method.id());
      httpMethodView.httpMethod(method.httpMethod());
      List<String> pathParams = new ArrayList<>(method.pathParams().keySet());
      List<String> queryParams = new ArrayList<>(method.queryParams().keySet());
      Collections.sort(pathParams);
      Collections.sort(queryParams);
      httpMethodView.pathParams(pathParams);
      httpMethodView.queryParams(queryParams);
      httpMethodView.pathTemplate(method.path());
      return httpMethodView.build();
    } else {
      return null;
    }
  }

  public List<ApiCallSettingsView> generateApiCallableSettings(MethodContext context) {
    List<ApiCallSettingsView> settings = new ArrayList<>();

    MethodConfig methodConfig = context.getMethodConfig();

    if (methodConfig.isGrpcStreaming()) {
      settings.add(
          generateApiCallableSettings(
              context, ApiCallableImplType.of(methodConfig.getGrpcStreamingType())));
    } else if (methodConfig.isPageStreaming()) {
      settings.add(generateApiCallableSettings(context, ApiCallableImplType.PagedApiCallable));
    } else if (methodConfig.isBatching()) {
      settings.add(generateApiCallableSettings(context, ApiCallableImplType.BatchingApiCallable));
    } else if (methodConfig.isLongRunningOperation()) {
      if (context.getFeatureConfig().enableRawOperationCallSettings()) {
        settings.add(generateApiCallableSettings(context, ApiCallableImplType.SimpleApiCallable));
      }
      settings.add(generateApiCallableSettings(context, ApiCallableImplType.OperationApiCallable));
    } else {
      settings.add(generateApiCallableSettings(context, ApiCallableImplType.SimpleApiCallable));
    }

    return settings;
  }

  public ApiCallSettingsView generateApiCallableSettings(
      MethodContext context, ApiCallableImplType callableImplType) {
    SurfaceNamer namer = context.getNamer();
    ImportTypeTable typeTable = context.getTypeTable();
    MethodModel method = context.getMethodModel();
    MethodConfig methodConfig = context.getMethodConfig();

    Map<String, RetryCodesDefinitionView> retryCodesByKey = new HashMap<>();
    for (RetryCodesDefinitionView retryCodes :
        retryDefinitionsTransformer.generateRetryCodesDefinitions(
            context.getSurfaceInterfaceContext())) {
      retryCodesByKey.put(retryCodes.key(), retryCodes);
    }
    Map<String, RetryParamsDefinitionView> retryParamsByKey = new HashMap<>();
    for (RetryParamsDefinitionView retryParams :
        retryDefinitionsTransformer.generateRetryParamsDefinitions(
            context.getSurfaceInterfaceContext())) {
      retryParamsByKey.put(retryParams.key(), retryParams);
    }

    ApiCallSettingsView.Builder settings = ApiCallSettingsView.newBuilder();

    settings.methodName(namer.getApiMethodName(method, VisibilityConfig.PUBLIC));
    settings.asyncMethodName(namer.getAsyncApiMethodName(method, VisibilityConfig.PUBLIC));
    settings.requestTypeName(
        method.getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer()));
    settings.responseTypeName(
        method.getAndSaveResponseTypeName(context.getTypeTable(), context.getNamer()));

    settings.grpcTypeName(typeTable.getAndSaveNicknameFor(context.getGrpcContainerTypeName()));
    settings.grpcMethodConstant(namer.getGrpcMethodConstant(method));
    settings.retryCodesName(methodConfig.getRetryCodesConfigName());
    settings.retryCodesView(retryCodesByKey.get(methodConfig.getRetryCodesConfigName()));
    settings.retryParamsName(methodConfig.getRetrySettingsConfigName());
    settings.retryParamsView(retryParamsByKey.get(methodConfig.getRetrySettingsConfigName()));
    settings.visibility(namer.getVisiblityKeyword(methodConfig.getVisibility()));

    String notImplementedPrefix = "ApiCallableTransformer.generateApiCallableSettings - ";
    settings.resourceTypeName(
        namer.getNotImplementedString(notImplementedPrefix + "resourceTypeName"));
    settings.pagedListResponseTypeName(
        namer.getNotImplementedString(notImplementedPrefix + "pagedListResponseTypeName"));
    settings.pageStreamingDescriptorName(
        namer.getNotImplementedString(notImplementedPrefix + "pageStreamingDescriptorName"));
    settings.pagedListResponseFactoryName(
        namer.getNotImplementedString(notImplementedPrefix + "pagedListResponseFactoryName"));
    settings.batchingDescriptorName(
        namer.getNotImplementedString(notImplementedPrefix + "batchingDescriptorName"));

    settings.memberName(namer.getSettingsMemberName(method));
    settings.settingsGetFunction(namer.getSettingsFunctionName(method));
    settings.type(callableImplType);

    switch (callableImplType) {
      case BidiStreamingApiCallable:
      case ClientStreamingApiCallable:
      case ServerStreamingApiCallable:
        settings.type(ApiCallableImplType.of(methodConfig.getGrpcStreamingType()));
        if (methodConfig.getGrpcStreaming().hasResourceField()) {
          FieldModel resourceType = methodConfig.getGrpcStreaming().getResourcesField();
          settings.resourceTypeName(typeTable.getAndSaveNicknameForElementType(resourceType));
        }
        settings.grpcStreamingType(methodConfig.getGrpcStreaming().getType());
        break;
      case PagedApiCallable:
        settings.type(ApiCallableImplType.PagedApiCallable);
        FieldModel resourceField = methodConfig.getPageStreaming().getResourcesField();
        settings.resourceTypeName(typeTable.getAndSaveNicknameForElementType(resourceField));
        settings.pagedListResponseTypeName(
            namer.getAndSavePagedResponseTypeName(
                context, methodConfig.getPageStreaming().getResourcesFieldConfig()));
        settings.pageStreamingDescriptorName(namer.getPageStreamingDescriptorConstName(method));
        settings.pagedListResponseFactoryName(namer.getPagedListResponseFactoryConstName(method));
        break;
      case BatchingApiCallable:
        settings.type(ApiCallableImplType.BatchingApiCallable);
        settings.batchingDescriptorName(namer.getBatchingDescriptorConstName(method));
        settings.batchingConfig(batchingTransformer.generateBatchingConfig(context));
        break;
      case OperationApiCallable:
        settings.type(ApiCallableImplType.OperationApiCallable);
        settings.operationMethod(lroTransformer.generateDetailView(context));
        settings.memberName(namer.getOperationSettingsMemberName(method));
        settings.settingsGetFunction(namer.getOperationSettingsFunctionName(method));
        break;
      case SimpleApiCallable:
        break;
      default:
        throw new IllegalStateException(
            "Unhandled ApiCallableImplType enum value: " + callableImplType);
    }

    return settings.build();
  }

  public List<MethodDescriptorView> generateMethodDescriptors(InterfaceContext context) {
    List<MethodDescriptorView> callables = new ArrayList<>();
    boolean excludeMixins = !context.getFeatureConfig().enableMixins();

    for (MethodModel method : context.getSupportedMethods()) {
      if (excludeMixins && context.getMethodConfig(method).getRerouteToGrpcInterface() != null) {
        continue;
      }
      callables.add(generateMethodDescriptor(context.asRequestMethodContext(method)));
    }

    return callables;
  }

  private MethodDescriptorView generateMethodDescriptor(MethodContext context) {
    ImportTypeTable typeTable = context.getTypeTable();
    MethodModel method = context.getMethodModel();
    MethodConfig methodConfig = context.getMethodConfig();
    SurfaceNamer namer = context.getNamer();

    MethodDescriptorView.Builder methodDescriptorBuilder = MethodDescriptorView.newBuilder();

    if (methodConfig.isGrpcStreaming()) {
      methodDescriptorBuilder.grpcStreamingType(methodConfig.getGrpcStreaming().getType());
    }

    methodDescriptorBuilder.requestTypeName(method.getAndSaveRequestTypeName(typeTable, namer));
    methodDescriptorBuilder.responseTypeName(method.getAndSaveResponseTypeName(typeTable, namer));
    methodDescriptorBuilder.hasResponse(method.hasReturnValue());
    methodDescriptorBuilder.name(namer.getMethodDescriptorName(method));
    methodDescriptorBuilder.protoMethodName(method.getSimpleName());
    methodDescriptorBuilder.fullServiceName(context.getTargetInterface().getFullName());
    methodDescriptorBuilder.transportSettingsVar(namer.getTransportSettingsVar(method));

    methodDescriptorBuilder.headerRequestParams(
        headerRequestParamTransformer.generateHeaderRequestParams(context));

    methodDescriptorBuilder.httpMethod(generateHttpFields(context));

    return methodDescriptorBuilder.build();
  }
}
