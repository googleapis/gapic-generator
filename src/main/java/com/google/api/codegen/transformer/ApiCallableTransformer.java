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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiCallableImplType;
import com.google.api.codegen.viewmodel.ApiCallableView;
import com.google.api.codegen.viewmodel.DirectCallableView;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.codegen.viewmodel.ServiceMethodType;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiCallableTransformer {

  private final BatchingTransformer batchingTransformer;
  private final RetryDefinitionsTransformer retryDefinitionsTransformer;
  private final LongRunningTransformer lroTransformer;

  public ApiCallableTransformer() {
    this.batchingTransformer = new BatchingTransformer();
    this.retryDefinitionsTransformer = new RetryDefinitionsTransformer();
    this.lroTransformer = new LongRunningTransformer();
  }

  public List<ApiCallableView> generateStaticLangApiCallables(GapicInterfaceContext context) {
    List<ApiCallableView> callableMembers = new ArrayList<>();
    boolean excludeMixins = !context.getFeatureConfig().enableMixins();

    for (Method method : context.getSupportedMethods()) {
      if (excludeMixins && context.getMethodConfig(method).getRerouteToGrpcInterface() != null) {
        continue;
      }
      callableMembers.addAll(
          generateStaticLangApiCallables(context.asRequestMethodContext(method)));
    }

    return callableMembers;
  }

  public List<ApiCallSettingsView> generateCallSettings(GapicInterfaceContext context) {
    List<ApiCallSettingsView> settingsMembers = new ArrayList<>();

    for (Method method : context.getSupportedMethods()) {
      settingsMembers.addAll(generateApiCallableSettings(context.asRequestMethodContext(method)));
    }

    return settingsMembers;
  }

  private List<ApiCallableView> generateStaticLangApiCallables(GapicMethodContext context) {
    List<ApiCallableView> apiCallables = new ArrayList<>();

    apiCallables.add(generateMainApiCallable(context));

    if (context.getMethodConfig().isPageStreaming()) {
      apiCallables.add(generatePagedApiCallable(context));
    }

    if (context.getMethodConfig().isLongRunningOperation()) {
      apiCallables.add(generateOperationApiCallable(context));
    }

    return apiCallables;
  }

  private ApiCallableView generateMainApiCallable(GapicMethodContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    GapicMethodConfig methodConfig = context.getMethodConfig();
    SurfaceNamer namer = context.getNamer();

    ApiCallableView.Builder apiCallableBuilder = ApiCallableView.newBuilder();

    apiCallableBuilder.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    apiCallableBuilder.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));
    apiCallableBuilder.name(namer.getCallableName(method));

    setCommonApiCallableFields(context, apiCallableBuilder);

    ApiCallableImplType callableImplType = ApiCallableImplType.SimpleApiCallable;
    if (methodConfig.isGrpcStreaming()) {
      callableImplType = ApiCallableImplType.of(methodConfig.getGrpcStreamingType());
      apiCallableBuilder.grpcStreamingType(methodConfig.getGrpcStreamingType());
    } else if (methodConfig.isBatching()) {
      callableImplType = ApiCallableImplType.BatchingApiCallable;
    } else if (methodConfig.isLongRunningOperation()) {
      callableImplType = ApiCallableImplType.InitialOperationApiCallable;
    }
    apiCallableBuilder.type(callableImplType);
    apiCallableBuilder.interfaceTypeName(
        namer.getApiCallableTypeName(callableImplType.serviceMethodType()));

    return apiCallableBuilder.build();
  }

  private ApiCallableView generatePagedApiCallable(GapicMethodContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    GapicMethodConfig methodConfig = context.getMethodConfig();
    SurfaceNamer namer = context.getNamer();

    PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

    ApiCallableView.Builder pagedApiCallableBuilder = ApiCallableView.newBuilder();
    pagedApiCallableBuilder.type(ApiCallableImplType.PagedApiCallable);
    pagedApiCallableBuilder.interfaceTypeName(
        namer.getApiCallableTypeName(ServiceMethodType.UnaryMethod));

    String pagedResponseTypeName =
        namer.getAndSavePagedResponseTypeName(
            method, typeTable, pageStreaming.getResourcesFieldConfig());

    pagedApiCallableBuilder.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    pagedApiCallableBuilder.responseTypeName(pagedResponseTypeName);
    pagedApiCallableBuilder.name(namer.getPagedCallableName(method));

    setCommonApiCallableFields(context, pagedApiCallableBuilder);

    return pagedApiCallableBuilder.build();
  }

  private ApiCallableView generateOperationApiCallable(GapicMethodContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    GapicMethodConfig methodConfig = context.getMethodConfig();
    SurfaceNamer namer = context.getNamer();

    ApiCallableView.Builder operationApiCallableBuilder = ApiCallableView.newBuilder();
    operationApiCallableBuilder.type(ApiCallableImplType.OperationApiCallable);
    operationApiCallableBuilder.interfaceTypeName(
        namer.getApiCallableTypeName(ServiceMethodType.LongRunningMethod));

    LongRunningOperationDetailView lroView = lroTransformer.generateDetailView(context);

    operationApiCallableBuilder.requestTypeName(
        typeTable.getAndSaveNicknameFor(method.getInputType()));
    operationApiCallableBuilder.responseTypeName(lroView.operationPayloadTypeName());
    operationApiCallableBuilder.metadataTypeName(lroView.metadataTypeName());
    operationApiCallableBuilder.name(namer.getOperationCallableName(method));

    setCommonApiCallableFields(context, operationApiCallableBuilder);

    return operationApiCallableBuilder.build();
  }

  private void setCommonApiCallableFields(
      GapicMethodContext context, ApiCallableView.Builder apiCallableBuilder) {
    Method method = context.getMethod();
    SurfaceNamer namer = context.getNamer();
    GapicMethodConfig methodConfig = context.getMethodConfig();

    apiCallableBuilder.methodName(
        namer.getApiMethodName(method, context.getMethodConfig().getVisibility()));
    apiCallableBuilder.asyncMethodName(
        namer.getAsyncApiMethodName(method, VisibilityConfig.PUBLIC));
    apiCallableBuilder.memberName(namer.getSettingsMemberName(method));
    apiCallableBuilder.settingsFunctionName(namer.getSettingsFunctionName(method));
    apiCallableBuilder.grpcClientVarName(namer.getReroutedGrpcClientVarName(methodConfig));
    apiCallableBuilder.grpcDirectCallableName(namer.getDirectCallableName(method));
  }

  public List<ApiCallSettingsView> generateApiCallableSettings(GapicMethodContext context) {
    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    GapicMethodConfig methodConfig = context.getMethodConfig();
    Map<String, RetryCodesDefinitionView> retryCodesByKey = new HashMap<>();
    for (RetryCodesDefinitionView retryCodes :
        retryDefinitionsTransformer.generateRetryCodesDefinitions(
            context.getSurfaceTransformerContext())) {
      retryCodesByKey.put(retryCodes.key(), retryCodes);
    }
    Map<String, RetryParamsDefinitionView> retryParamsByKey = new HashMap<>();
    for (RetryParamsDefinitionView retryParams :
        retryDefinitionsTransformer.generateRetryParamsDefinitions(
            context.getSurfaceTransformerContext())) {
      retryParamsByKey.put(retryParams.key(), retryParams);
    }

    ApiCallSettingsView.Builder settings = ApiCallSettingsView.newBuilder();

    settings.methodName(namer.getApiMethodName(method, VisibilityConfig.PUBLIC));
    settings.asyncMethodName(namer.getAsyncApiMethodName(method, VisibilityConfig.PUBLIC));
    settings.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    settings.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));

    settings.grpcTypeName(
        typeTable.getAndSaveNicknameFor(
            namer.getGrpcContainerTypeName(context.getTargetInterface())));
    settings.grpcMethodConstant(namer.getGrpcMethodConstant(method));
    settings.retryCodesName(methodConfig.getRetryCodesConfigName());
    settings.retryCodesView(retryCodesByKey.get(methodConfig.getRetryCodesConfigName()));
    settings.retryParamsName(methodConfig.getRetrySettingsConfigName());
    settings.retryParamsView(retryParamsByKey.get(methodConfig.getRetrySettingsConfigName()));

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

    if (methodConfig.isGrpcStreaming()) {
      settings.type(ApiCallableImplType.of(methodConfig.getGrpcStreamingType()));
      if (methodConfig.getGrpcStreaming().hasResourceField()) {
        TypeRef resourceType = methodConfig.getGrpcStreaming().getResourcesField().getType();
        settings.resourceTypeName(typeTable.getAndSaveNicknameForElementType(resourceType));
      }
      settings.grpcStreamingType(methodConfig.getGrpcStreaming().getType());
    } else if (methodConfig.isPageStreaming()) {
      settings.type(ApiCallableImplType.PagedApiCallable);
      Field resourceField = methodConfig.getPageStreaming().getResourcesField();
      settings.resourceTypeName(
          typeTable.getAndSaveNicknameForElementType(resourceField.getType()));
      settings.pagedListResponseTypeName(
          namer.getAndSavePagedResponseTypeName(
              context.getMethod(),
              context.getTypeTable(),
              methodConfig.getPageStreaming().getResourcesFieldConfig()));
      settings.pageStreamingDescriptorName(namer.getPageStreamingDescriptorConstName(method));
      settings.pagedListResponseFactoryName(namer.getPagedListResponseFactoryConstName(method));
    } else if (methodConfig.isBatching()) {
      settings.type(ApiCallableImplType.BatchingApiCallable);
      settings.batchingDescriptorName(namer.getBatchingDescriptorConstName(method));
      settings.batchingConfig(batchingTransformer.generateBatchingConfig(context));
    } else if (methodConfig.isLongRunningOperation()) {
      settings.type(ApiCallableImplType.OperationApiCallable);
      settings.operationMethod(lroTransformer.generateDetailView(context));
    } else {
      settings.type(ApiCallableImplType.SimpleApiCallable);
    }

    settings.memberName(namer.getSettingsMemberName(method));
    settings.settingsGetFunction(namer.getSettingsFunctionName(method));

    return Arrays.asList(settings.build());
  }

  public List<DirectCallableView> generateStaticLangDirectCallables(GapicInterfaceContext context) {
    List<DirectCallableView> callables = new ArrayList<>();
    boolean excludeMixins = !context.getFeatureConfig().enableMixins();

    for (Method method : context.getSupportedMethods()) {
      if (excludeMixins && context.getMethodConfig(method).getRerouteToGrpcInterface() != null) {
        continue;
      }
      callables.add(generateDirectCallable(context.asRequestMethodContext(method)));
    }

    return callables;
  }

  private DirectCallableView generateDirectCallable(GapicMethodContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    GapicMethodConfig methodConfig = context.getMethodConfig();
    SurfaceNamer namer = context.getNamer();

    DirectCallableView.Builder callableBuilder = DirectCallableView.newBuilder();

    ServiceMethodType callableInterfaceType = ServiceMethodType.UnaryMethod;
    if (methodConfig.isGrpcStreaming()) {
      callableInterfaceType =
          ApiCallableImplType.of(methodConfig.getGrpcStreamingType()).serviceMethodType();
      callableBuilder.grpcStreamingType(methodConfig.getGrpcStreaming().getType());
    }

    callableBuilder.interfaceTypeName(namer.getDirectCallableTypeName(callableInterfaceType));
    callableBuilder.createCallableFunctionName(
        namer.getCreateCallableFunctionName(callableInterfaceType));
    callableBuilder.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    callableBuilder.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));
    callableBuilder.name(namer.getDirectCallableName(method));
    callableBuilder.protoMethodName(method.getSimpleName());
    callableBuilder.fullServiceName(context.getTargetInterface().getFullName());

    return callableBuilder.build();
  }
}
