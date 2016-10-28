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

import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiCallableType;
import com.google.api.codegen.viewmodel.ApiCallableView;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    MethodConfig methodConfig = context.getMethodConfig();

    List<ApiCallableView> apiCallables = new ArrayList<>();

    ApiCallableView.Builder apiCallableBuilder = ApiCallableView.newBuilder();

    apiCallableBuilder.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    apiCallableBuilder.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));
    apiCallableBuilder.name(context.getNamer().getCallableName(method));
    apiCallableBuilder.methodName(
        context.getNamer().getApiMethodName(method, context.getMethodConfig().getVisibility()));
    apiCallableBuilder.asyncMethodName(context.getNamer().getAsyncApiMethodName(method));
    apiCallableBuilder.memberName(context.getNamer().getSettingsMemberName(method));
    apiCallableBuilder.settingsFunctionName(context.getNamer().getSettingsFunctionName(method));
    apiCallableBuilder.grpcClientVarName(
        context.getNamer().getReroutedGrpcClientVarName(methodConfig));

    if (methodConfig.isGrpcStreaming()) {
      apiCallableBuilder.type(ApiCallableType.StreamingApiCallable);
      apiCallableBuilder.grpcStreamingType(methodConfig.getGrpcStreaming().getType());
    } else if (methodConfig.isBundling()) {
      apiCallableBuilder.type(ApiCallableType.BundlingApiCallable);
    } else {
      apiCallableBuilder.type(ApiCallableType.SimpleApiCallable);
    }

    apiCallables.add(apiCallableBuilder.build());

    if (methodConfig.isPageStreaming()) {
      PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

      ApiCallableView.Builder pagedApiCallableBuilder = ApiCallableView.newBuilder();
      pagedApiCallableBuilder.type(ApiCallableType.PagedApiCallable);

      String pagedResponseTypeName =
          context
              .getNamer()
              .getAndSavePagedResponseTypeName(
                  method, typeTable, pageStreaming.getResourcesField());

      pagedApiCallableBuilder.requestTypeName(
          typeTable.getAndSaveNicknameFor(method.getInputType()));
      pagedApiCallableBuilder.responseTypeName(pagedResponseTypeName);
      pagedApiCallableBuilder.name(context.getNamer().getPagedCallableName(method));
      pagedApiCallableBuilder.methodName(
          context.getNamer().getApiMethodName(method, context.getMethodConfig().getVisibility()));
      pagedApiCallableBuilder.asyncMethodName(context.getNamer().getAsyncApiMethodName(method));
      pagedApiCallableBuilder.memberName(context.getNamer().getSettingsMemberName(method));
      pagedApiCallableBuilder.settingsFunctionName(
          context.getNamer().getSettingsFunctionName(method));
      pagedApiCallableBuilder.grpcClientVarName(
          context.getNamer().getReroutedGrpcClientVarName(methodConfig));

      apiCallables.add(pagedApiCallableBuilder.build());
    }

    return apiCallables;
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
    settings.asyncMethodName(namer.getAsyncApiMethodName(method));
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

    if (methodConfig.isGrpcStreaming()) {
      settings.type(ApiCallableType.StreamingApiCallable);
      if (methodConfig.getGrpcStreaming().hasResourceField()) {
        TypeRef resourceType = methodConfig.getGrpcStreaming().getResourcesField().getType();
        settings.resourceTypeName(typeTable.getAndSaveNicknameForElementType(resourceType));
      }
      settings.grpcStreamingType(methodConfig.getGrpcStreaming().getType());
    } else if (methodConfig.isPageStreaming()) {
      namer.addPageStreamingCallSettingsImports(typeTable);
      settings.type(ApiCallableType.PagedApiCallable);
      Field resourceField = methodConfig.getPageStreaming().getResourcesField();
      settings.resourceTypeName(
          typeTable.getAndSaveNicknameForElementType(resourceField.getType()));
      settings.pagedListResponseTypeName(
          namer.getAndSavePagedResponseTypeName(
              context.getMethod(), context.getTypeTable(), resourceField));
      settings.pageStreamingDescriptorName(namer.getPageStreamingDescriptorConstName(method));
      settings.pagedListResponseFactoryName(namer.getPagedListResponseFactoryConstName(method));
    } else if (methodConfig.isBundling()) {
      namer.addBundlingCallSettingsImports(typeTable);
      settings.type(ApiCallableType.BundlingApiCallable);
      settings.bundlingDescriptorName(namer.getBundlingDescriptorConstName(method));
      settings.bundlingConfig(bundlingTransformer.generateBundlingConfig(context));
    } else {
      settings.type(ApiCallableType.SimpleApiCallable);
    }

    settings.memberName(namer.getSettingsMemberName(method));
    settings.settingsGetFunction(namer.getSettingsFunctionName(method));

    return Arrays.asList(settings.build());
  }
}
