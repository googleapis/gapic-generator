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

import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiCallableImplType;
import com.google.api.codegen.viewmodel.ApiCallableView;
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
import org.joda.time.Duration;

public class ApiCallableTransformer {
  private final BundlingTransformer bundlingTransformer;
  private final RetryDefinitionsTransformer retryDefinitionsTransformer;

  public ApiCallableTransformer() {
    this.bundlingTransformer = new BundlingTransformer();
    this.retryDefinitionsTransformer = new RetryDefinitionsTransformer();
  }

  public List<ApiCallableView> generateStaticLangApiCallables(SurfaceTransformerContext context) {
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

  public List<ApiCallSettingsView> generateCallSettings(SurfaceTransformerContext context) {
    List<ApiCallSettingsView> settingsMembers = new ArrayList<>();

    for (Method method : context.getSupportedMethods()) {
      settingsMembers.addAll(generateApiCallableSettings(context.asRequestMethodContext(method)));
    }

    return settingsMembers;
  }

  private List<ApiCallableView> generateStaticLangApiCallables(MethodTransformerContext context) {
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

  private ApiCallableView generateMainApiCallable(MethodTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    MethodConfig methodConfig = context.getMethodConfig();
    SurfaceNamer namer = context.getNamer();

    ApiCallableView.Builder apiCallableBuilder = ApiCallableView.newBuilder();

    apiCallableBuilder.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    apiCallableBuilder.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));
    apiCallableBuilder.name(namer.getCallableName(method));
    apiCallableBuilder.methodName(
        namer.getApiMethodName(method, context.getMethodConfig().getVisibility()));
    apiCallableBuilder.asyncMethodName(
        namer.getAsyncApiMethodName(method, VisibilityConfig.PUBLIC));
    apiCallableBuilder.memberName(namer.getSettingsMemberName(method));
    apiCallableBuilder.settingsFunctionName(context.getNamer().getSettingsFunctionName(method));
    apiCallableBuilder.grpcClientVarName(namer.getReroutedGrpcClientVarName(methodConfig));

    ApiCallableImplType callableImplType = ApiCallableImplType.SimpleApiCallable;
    if (methodConfig.isGrpcStreaming()) {
      callableImplType = ApiCallableImplType.StreamingApiCallable;
      apiCallableBuilder.grpcStreamingType(methodConfig.getGrpcStreaming().getType());
    } else if (methodConfig.isBundling()) {
      callableImplType = ApiCallableImplType.BundlingApiCallable;
    } else if (methodConfig.isLongRunningOperation()) {
      callableImplType = ApiCallableImplType.InitialOperationApiCallable;
    }
    apiCallableBuilder.type(callableImplType);
    apiCallableBuilder.interfaceTypeName(
        namer.getApiCallableTypeName(callableImplType.serviceMethodType()));

    return apiCallableBuilder.build();
  }

  private ApiCallableView generatePagedApiCallable(MethodTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    MethodConfig methodConfig = context.getMethodConfig();
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
    pagedApiCallableBuilder.methodName(
        namer.getApiMethodName(method, context.getMethodConfig().getVisibility()));
    pagedApiCallableBuilder.asyncMethodName(
        namer.getAsyncApiMethodName(method, VisibilityConfig.PUBLIC));
    pagedApiCallableBuilder.memberName(namer.getSettingsMemberName(method));
    pagedApiCallableBuilder.settingsFunctionName(namer.getSettingsFunctionName(method));
    pagedApiCallableBuilder.grpcClientVarName(namer.getReroutedGrpcClientVarName(methodConfig));

    return pagedApiCallableBuilder.build();
  }

  private ApiCallableView generateOperationApiCallable(MethodTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    MethodConfig methodConfig = context.getMethodConfig();
    SurfaceNamer namer = context.getNamer();

    LongRunningConfig longRunning = methodConfig.getLongRunningConfig();

    ApiCallableView.Builder operationApiCallableBuilder = ApiCallableView.newBuilder();
    operationApiCallableBuilder.type(ApiCallableImplType.OperationApiCallable);
    operationApiCallableBuilder.interfaceTypeName(
        namer.getApiCallableTypeName(ServiceMethodType.LongRunningMethod));

    String operationResponseTypeName = typeTable.getAndSaveNicknameFor(longRunning.getReturnType());

    operationApiCallableBuilder.requestTypeName(
        typeTable.getAndSaveNicknameFor(method.getInputType()));
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

  public List<ApiCallSettingsView> generateApiCallableSettings(MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    MethodConfig methodConfig = context.getMethodConfig();
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
    settings.bundlingDescriptorName(
        namer.getNotImplementedString(notImplementedPrefix + "bundlingDescriptorName"));
    settings.operationResultTypeName(
        namer.getNotImplementedString(notImplementedPrefix + "operationResultTypeName"));

    if (methodConfig.isGrpcStreaming()) {
      settings.type(ApiCallableImplType.StreamingApiCallable);
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
    } else if (methodConfig.isBundling()) {
      settings.type(ApiCallableImplType.BundlingApiCallable);
      settings.bundlingDescriptorName(namer.getBundlingDescriptorConstName(method));
      settings.bundlingConfig(bundlingTransformer.generateBundlingConfig(context));
    } else if (methodConfig.isLongRunningOperation()) {
      settings.type(ApiCallableImplType.OperationApiCallable);
      TypeRef operationResultType = methodConfig.getLongRunningConfig().getReturnType();
      settings.operationResultTypeName(
          typeTable.getAndSaveNicknameForElementType(operationResultType));
      Duration pollingInterval = methodConfig.getLongRunningConfig().getPollingInterval();
      if (pollingInterval != null) {
        settings.operationPollingIntervalMillis(Long.toString(pollingInterval.getMillis()));
      }
    } else {
      settings.type(ApiCallableImplType.SimpleApiCallable);
    }

    settings.memberName(namer.getSettingsMemberName(method));
    settings.settingsGetFunction(namer.getSettingsFunctionName(method));

    return Arrays.asList(settings.build());
  }
}
