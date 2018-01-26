/* Copyright 2017 Google LLC
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
package com.google.api.codegen.transformer.clientconfig;

import com.google.api.codegen.config.BatchingConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.ProductConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.clientconfig.ClientConfigView;
import com.google.api.codegen.viewmodel.clientconfig.MethodView;
import com.google.api.codegen.viewmodel.clientconfig.PairView;
import com.google.api.codegen.viewmodel.clientconfig.RetryCodeDefView;
import com.google.api.codegen.viewmodel.clientconfig.RetryParamDefView;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.List;
import java.util.Map;

/** The base ModelToViewTransformer to transform a Model into the client config. */
public abstract class AbstractClientConfigTransformer implements ModelToViewTransformer {
  private static final String TEMPLATE_FILENAME = "clientconfig/main.snip";

  private final GapicCodePathMapper pathMapper;

  public AbstractClientConfigTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  protected String getExtension() {
    return ".json";
  }

  protected boolean isRetryingSupported(MethodConfig methodConfig) {
    return !methodConfig.getRetryCodesConfigName().isEmpty()
        && !methodConfig.getRetrySettingsConfigName().isEmpty();
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    for (InterfaceModel apiInterface : new ProtoApiModel(model).getInterfaces()) {
      views.add(generateClientConfig(apiInterface, productConfig));
    }
    return views.build();
  }

  private ClientConfigView generateClientConfig(
      InterfaceModel apiInterface, ProductConfig productConfig) {
    InterfaceConfig interfaceConfig = productConfig.getInterfaceConfig(apiInterface);
    ClientConfigView.Builder view = ClientConfigView.newBuilder();
    view.templateFileName(TEMPLATE_FILENAME);
    view.name(interfaceConfig.getInterfaceModel().getFullName());
    view.hasVariable(false);
    view.retryCodesDef(generateRetryCodesDef(interfaceConfig.getRetryCodesDefinition()));
    view.retryParamsDef(generateRetryParamsDef(interfaceConfig.getRetrySettingsDefinition()));
    view.methods(generateMethods(interfaceConfig.getMethodConfigs()));
    String subPath = pathMapper.getOutputPath(apiInterface.getFullName(), productConfig);
    String fileName =
        Name.upperCamel(apiInterface.getSimpleName()).join("client_config").toLowerUnderscore()
            + getExtension();
    view.outputPath(subPath.isEmpty() ? fileName : subPath + File.separator + fileName);
    view.hasVariable(!getExtension().equals(".json"));
    return view.build();
  }

  private List<RetryCodeDefView> generateRetryCodesDef(
      Map<String, ImmutableSet<String>> retryCodesDefConfig) {
    ImmutableList.Builder<RetryCodeDefView> retryCodesDef = ImmutableList.builder();
    for (Map.Entry<String, ImmutableSet<String>> entry : retryCodesDefConfig.entrySet()) {
      RetryCodeDefView.Builder retryCodeDef = RetryCodeDefView.newBuilder();
      retryCodeDef.name(entry.getKey());
      retryCodeDef.codes(ImmutableList.copyOf(entry.getValue()));
      retryCodesDef.add(retryCodeDef.build());
    }
    return retryCodesDef.build();
  }

  private List<RetryParamDefView> generateRetryParamsDef(
      Map<String, RetrySettings> retryParamsDefConfig) {
    ImmutableList.Builder<RetryParamDefView> retryParamsDef = ImmutableList.builder();
    for (Map.Entry<String, RetrySettings> entry : retryParamsDefConfig.entrySet()) {
      RetryParamDefView.Builder retryParamDef = RetryParamDefView.newBuilder();
      retryParamDef.name(entry.getKey());
      RetrySettings settings = entry.getValue();
      retryParamDef.initialRetryDelayMillis(
          String.valueOf(settings.getInitialRetryDelay().toMillis()));
      retryParamDef.retryDelayMultiplier(String.valueOf(settings.getRetryDelayMultiplier()));
      retryParamDef.maxRetryDelayMillis(String.valueOf(settings.getMaxRetryDelay().toMillis()));
      retryParamDef.initialRpcTimeoutMillis(
          String.valueOf(settings.getInitialRpcTimeout().toMillis()));
      retryParamDef.rpcTimeoutMultiplier(String.valueOf(settings.getRpcTimeoutMultiplier()));
      retryParamDef.maxRpcTimeoutMillis(String.valueOf(settings.getMaxRpcTimeout().toMillis()));
      retryParamDef.totalTimeoutMillis(String.valueOf(settings.getTotalTimeout().toMillis()));
      retryParamsDef.add(retryParamDef.build());
    }
    return retryParamsDef.build();
  }

  private List<MethodView> generateMethods(List<? extends MethodConfig> methodConfigs) {
    ImmutableList.Builder<MethodView> methods = ImmutableList.builder();
    for (MethodConfig methodConfig : methodConfigs) {
      MethodView.Builder method = MethodView.newBuilder();
      method.name(methodConfig.getMethodModel().getSimpleName());
      method.timeoutMillis(String.valueOf(methodConfig.getTimeout().toMillis()));
      method.isRetryingSupported(isRetryingSupported(methodConfig));
      method.retryCodesName(methodConfig.getRetryCodesConfigName());
      method.retryParamsName(methodConfig.getRetrySettingsConfigName());
      method.batching(generateBatching(methodConfig));
      methods.add(method.build());
    }
    return methods.build();
  }

  private List<PairView> generateBatching(MethodConfig methodConfig) {
    if (!methodConfig.isBatching()) {
      return ImmutableList.of();
    }

    BatchingConfig batchingConfig = methodConfig.getBatching();
    ImmutableList.Builder<PairView> builder = ImmutableList.builder();
    if (batchingConfig.getElementCountThreshold() > 0) {
      builder.add(
          PairView.newBuilder()
              .key("element_count_threshold")
              .value(String.valueOf(batchingConfig.getElementCountThreshold()))
              .build());
    }

    if (batchingConfig.getElementCountLimit() > 0) {
      builder.add(
          PairView.newBuilder()
              .key("element_count_limit")
              .value(String.valueOf(batchingConfig.getElementCountLimit()))
              .build());
    }

    if (batchingConfig.getRequestByteThreshold() > 0) {
      builder.add(
          PairView.newBuilder()
              .key("request_byte_threshold")
              .value(String.valueOf(batchingConfig.getRequestByteThreshold()))
              .build());
    }

    if (batchingConfig.getRequestByteLimit() > 0) {
      builder.add(
          PairView.newBuilder()
              .key("request_byte_limit")
              .value(String.valueOf(batchingConfig.getRequestByteLimit()))
              .build());
    }

    if (batchingConfig.getDelayThresholdMillis() > 0) {
      builder.add(
          PairView.newBuilder()
              .key("delay_threshold_millis")
              .value(String.valueOf(batchingConfig.getDelayThresholdMillis()))
              .build());
    }

    return builder.build();
  }
}
