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

import com.google.api.codegen.BundlingConfig;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.PageStreamingConfig;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiCallableType;
import com.google.api.codegen.viewmodel.ApiCallableView;
import com.google.api.codegen.viewmodel.BundlingConfigView;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApiCallableTransformer {

  public List<ApiCallableView> generateStaticApiCallables(SurfaceTransformerContext context) {
    List<ApiCallableView> callableMembers = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      callableMembers.addAll(generateStaticApiCallables(context.asMethodContext(method)));
    }

    return callableMembers;
  }

  public List<ApiCallSettingsView> generateCallSettings(SurfaceTransformerContext context) {
    List<ApiCallSettingsView> settingsMembers = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      settingsMembers.addAll(generateApiCallableSettings(context.asMethodContext(method)));
    }

    return settingsMembers;
  }

  private List<ApiCallableView> generateStaticApiCallables(MethodTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    MethodConfig methodConfig = context.getMethodConfig();

    List<ApiCallableView> apiCallables = new ArrayList<>();

    ApiCallableView.Builder apiCallableBuilder = ApiCallableView.newBuilder();

    apiCallableBuilder.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    apiCallableBuilder.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));
    apiCallableBuilder.name(context.getNamer().getCallableName(method));
    apiCallableBuilder.settingsFunctionName(context.getNamer().getSettingsFunctionName(method));

    if (methodConfig.isBundling()) {
      apiCallableBuilder.type(ApiCallableType.BundlingApiCallable);
    } else {
      apiCallableBuilder.type(ApiCallableType.SimpleApiCallable);
    }

    apiCallables.add(apiCallableBuilder.build());

    if (methodConfig.isPageStreaming()) {
      PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

      ApiCallableView.Builder pagedApiCallableBuilder = ApiCallableView.newBuilder();

      String pagedResponseTypeName =
          context
              .getNamer()
              .getAndSavePagedResponseTypeName(
                  typeTable, pageStreaming.getResourcesField().getType());

      pagedApiCallableBuilder.requestTypeName(
          typeTable.getAndSaveNicknameFor(method.getInputType()));
      pagedApiCallableBuilder.responseTypeName(pagedResponseTypeName);
      pagedApiCallableBuilder.name(context.getNamer().getPagedCallableName(method));
      pagedApiCallableBuilder.settingsFunctionName(
          context.getNamer().getSettingsFunctionName(method));

      apiCallables.add(pagedApiCallableBuilder.type(ApiCallableType.PagedApiCallable).build());
    }

    return apiCallables;
  }

  private List<ApiCallSettingsView> generateApiCallableSettings(MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    MethodConfig methodConfig = context.getMethodConfig();

    ApiCallSettingsView.Builder settings = ApiCallSettingsView.newBuilder();

    settings.methodName(namer.getApiMethodName(method));
    settings.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    settings.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));
    settings.grpcTypeName(
        typeTable.getAndSaveNicknameFor(namer.getGrpcContainerTypeName(context.getInterface())));
    settings.grpcMethodConstant(namer.getGrpcMethodConstant(method));
    settings.retryCodesName(methodConfig.getRetryCodesConfigName());
    settings.retryParamsName(methodConfig.getRetrySettingsConfigName());

    String notImplementedPrefix = "ApiCallableTransformer.generateApiCallableSettings - ";
    settings.resourceTypeName(
        namer.getNotImplementedString(notImplementedPrefix + "resourceTypeName"));
    settings.pageStreamingDescriptorName(
        namer.getNotImplementedString(notImplementedPrefix + "pageStreamingDescriptorName"));
    settings.bundlingDescriptorName(
        namer.getNotImplementedString(notImplementedPrefix + "bundlingDescriptorName"));

    if (methodConfig.isPageStreaming()) {
      namer.addPageStreamingCallSettingsImports(typeTable);
      settings.type(ApiCallableType.PagedApiCallable);
      TypeRef resourceType = methodConfig.getPageStreaming().getResourcesField().getType();
      settings.resourceTypeName(typeTable.getAndSaveNicknameForElementType(resourceType));
      settings.pageStreamingDescriptorName(namer.getPageStreamingDescriptorConstName(method));
    } else {
      if (methodConfig.isBundling()) {
        namer.addBundlingCallSettingsImports(typeTable);
        settings.type(ApiCallableType.BundlingApiCallable);
        settings.bundlingDescriptorName(namer.getBundlingDescriptorConstName(method));
        settings.bundlingConfig(generateBundlingConfig(context));
      } else {
        settings.type(ApiCallableType.SimpleApiCallable);
      }
    }

    settings.memberName(namer.getSettingsMemberName(method));
    settings.fnGetterName(namer.getSettingsMemberName(method));

    return Arrays.asList(settings.build());
  }

  private BundlingConfigView generateBundlingConfig(MethodTransformerContext context) {
    BundlingConfig bundlingConfig = context.getMethodConfig().getBundling();
    BundlingConfigView.Builder bundlingConfigView = BundlingConfigView.newBuilder();

    bundlingConfigView.elementCountThreshold(bundlingConfig.getElementCountThreshold());
    bundlingConfigView.elementCountLimit(bundlingConfig.getElementCountLimit());
    bundlingConfigView.requestByteThreshold(bundlingConfig.getRequestByteThreshold());
    bundlingConfigView.requestByteLimit(bundlingConfig.getRequestByteLimit());
    bundlingConfigView.delayThresholdMillis(bundlingConfig.getDelayThresholdMillis());

    return bundlingConfigView.build();
  }
}
