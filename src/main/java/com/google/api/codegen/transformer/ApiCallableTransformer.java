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

import com.google.api.codegen.config.FieldType;
import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.ProtoField;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiCallableImplType;
import com.google.api.codegen.viewmodel.ApiCallableView;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.codegen.viewmodel.ServiceMethodType;
import com.google.api.tools.framework.model.TypeRef;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.Duration;

public class ApiCallableTransformer {
  private final BatchingTransformer batchingTransformer;
  private final RetryDefinitionsTransformer retryDefinitionsTransformer;

  public ApiCallableTransformer() {
    this.batchingTransformer = new BatchingTransformer();
    this.retryDefinitionsTransformer = new RetryDefinitionsTransformer();
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
    SurfaceNamer namer = context.getNamer();

    ApiCallableView.Builder apiCallableBuilder = ApiCallableView.newBuilder();

    apiCallableBuilder.requestTypeName(
        context
            .getMethodModel()
            .getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer()));
    apiCallableBuilder.responseTypeName(
        context
            .getMethodModel()
            .getAndSaveResponseTypeName(context.getTypeTable(), context.getNamer()));
    apiCallableBuilder.name(context.getCallableName());
    apiCallableBuilder.methodName(
        context.getApiMethodName(context.getMethodConfig().getVisibility()));
    apiCallableBuilder.asyncMethodName(context.getAsyncApiMethodName(VisibilityConfig.PUBLIC));
    apiCallableBuilder.memberName(context.getSettingsMemberName());
    apiCallableBuilder.settingsFunctionName(context.getSettingsFunctionName());
    apiCallableBuilder.grpcClientVarName(namer.getReroutedGrpcClientVarName(methodConfig));

    ApiCallableImplType callableImplType = ApiCallableImplType.SimpleApiCallable;
    if (methodConfig.isGrpcStreaming()) {
      callableImplType = ApiCallableImplType.StreamingApiCallable;
      apiCallableBuilder.grpcStreamingType(methodConfig.getGrpcStreaming().getType());
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

  private ApiCallableView generatePagedApiCallable(MethodContext context) {
    MethodConfig methodConfig = context.getMethodConfig();
    SurfaceNamer namer = context.getNamer();

    PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

    ApiCallableView.Builder pagedApiCallableBuilder = ApiCallableView.newBuilder();
    pagedApiCallableBuilder.type(ApiCallableImplType.PagedApiCallable);
    pagedApiCallableBuilder.interfaceTypeName(
        namer.getApiCallableTypeName(ServiceMethodType.UnaryMethod));

    String pagedResponseTypeName =
        context.getAndSavePagedResponseTypeName(pageStreaming.getResourcesFieldConfig());

    pagedApiCallableBuilder.requestTypeName(
        context
            .getMethodModel()
            .getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer()));
    pagedApiCallableBuilder.responseTypeName(pagedResponseTypeName);
    pagedApiCallableBuilder.name(context.getPagedCallableName());
    pagedApiCallableBuilder.methodName(
        context.getApiMethodName(context.getMethodConfig().getVisibility()));
    pagedApiCallableBuilder.asyncMethodName(context.getAsyncApiMethodName(VisibilityConfig.PUBLIC));
    pagedApiCallableBuilder.memberName(context.getSettingsMemberName());
    pagedApiCallableBuilder.settingsFunctionName(context.getSettingsFunctionName());
    pagedApiCallableBuilder.grpcClientVarName(namer.getReroutedGrpcClientVarName(methodConfig));

    return pagedApiCallableBuilder.build();
  }

  private ApiCallableView generateOperationApiCallable(GapicMethodContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    MethodModel method = context.getMethodModel();
    MethodConfig methodConfig = context.getMethodConfig();
    SurfaceNamer namer = context.getNamer();

    LongRunningConfig longRunning = methodConfig.getLongRunningConfig();

    ApiCallableView.Builder operationApiCallableBuilder = ApiCallableView.newBuilder();
    operationApiCallableBuilder.type(ApiCallableImplType.OperationApiCallable);
    operationApiCallableBuilder.interfaceTypeName(
        namer.getApiCallableTypeName(ServiceMethodType.LongRunningMethod));

    String operationResponseTypeName = typeTable.getAndSaveNicknameFor(longRunning.getReturnType());

    operationApiCallableBuilder.requestTypeName(
        context
            .getMethodModel()
            .getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer()));
    operationApiCallableBuilder.responseTypeName(operationResponseTypeName);
    operationApiCallableBuilder.name(namer.getOperationCallableName(method));
    operationApiCallableBuilder.methodName(
        namer.getApiMethodName(method, context.getMethodConfig().getVisibility()));
    operationApiCallableBuilder.asyncMethodName(
        namer.getAsyncApiMethodName(method, VisibilityConfig.PUBLIC));
    operationApiCallableBuilder.memberName(namer.getSettingsMemberName(method));
    operationApiCallableBuilder.settingsFunctionName(namer.getSettingsFunctionName(method));
    operationApiCallableBuilder.grpcClientVarName(namer.getReroutedGrpcClientVarName(methodConfig));

    return operationApiCallableBuilder.build();
  }

  public List<ApiCallSettingsView> generateApiCallableSettings(MethodContext context) {
    SurfaceNamer namer = context.getNamer();
    ImportTypeTable typeTable = context.getTypeTable();
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

    settings.methodName(context.getApiMethodName(VisibilityConfig.PUBLIC));
    settings.protoMethodName(context.getMethodModel().getProtoMethodName());
    settings.fullServiceName(context.getTargetInterfaceFullName());
    settings.asyncMethodName(context.getAsyncApiMethodName(VisibilityConfig.PUBLIC));
    settings.requestTypeName(
        context
            .getMethodModel()
            .getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer()));
    settings.responseTypeName(
        context
            .getMethodModel()
            .getAndSaveResponseTypeName(context.getTypeTable(), context.getNamer()));

    settings.grpcTypeName(typeTable.getAndSaveNicknameFor(context.getGrpcContainerTypeName()));
    settings.grpcMethodConstant(context.getGrpcMethodConstant());
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
    settings.operationResultTypeName(
        namer.getNotImplementedString(notImplementedPrefix + "operationResultTypeName"));

    if (methodConfig.isGrpcStreaming()) {
      // GrpcStreaming is only applicable to protobuf-based APIs.
      settings.type(ApiCallableImplType.StreamingApiCallable);
      if (methodConfig.getGrpcStreaming().hasResourceField()) {
        FieldType resourceType =
            new ProtoField(methodConfig.getGrpcStreaming().getResourcesField());
        settings.resourceTypeName(typeTable.getAndSaveNicknameForElementType(resourceType));
      }
      settings.grpcStreamingType(methodConfig.getGrpcStreaming().getType());
    } else if (methodConfig.isPageStreaming()) {
      settings.type(ApiCallableImplType.PagedApiCallable);
      settings.resourceTypeName(
          typeTable.getAndSaveNicknameForElementType(
              methodConfig.getPageStreaming().getResourcesField()));
      settings.pagedListResponseTypeName(
          context.getAndSavePagedResponseTypeName(
              methodConfig.getPageStreaming().getResourcesFieldConfig()));
      settings.pageStreamingDescriptorName(context.getPageStreamingDescriptorConstName());
      settings.pagedListResponseFactoryName(context.getPagedListResponseFactoryConstName());
    } else if (methodConfig.isBatching()) {
      settings.type(ApiCallableImplType.BatchingApiCallable);
      settings.batchingDescriptorName(
          context.getNamer().getBatchingDescriptorConstName(context.getMethodModel()));
      settings.batchingConfig(batchingTransformer.generateBatchingConfig(context));
    } else if (methodConfig.isLongRunningOperation()) {
      // LongRunningOperations are only applicable to protobuf-based APIs.
      settings.type(ApiCallableImplType.OperationApiCallable);
      TypeRef operationResultType = methodConfig.getLongRunningConfig().getReturnType();
      settings.operationResultTypeName(
          ((ModelTypeTable) typeTable).getAndSaveNicknameForElementType(operationResultType));
      Duration pollingInterval = methodConfig.getLongRunningConfig().getPollingInterval();
      if (pollingInterval != null) {
        settings.operationPollingIntervalMillis(Long.toString(pollingInterval.getMillis()));
      }
    } else {
      settings.type(ApiCallableImplType.SimpleApiCallable);
    }

    settings.memberName(context.getSettingsMemberName());
    settings.settingsGetFunction(context.getSettingsFunctionName());

    return Arrays.asList(settings.build());
  }
}
