/* Copyright 2017 Google LLC
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
package com.google.api.codegen.clientconfig.transformer;

import com.google.api.codegen.clientconfig.viewmodel.ClientConfigView;
import com.google.api.codegen.clientconfig.viewmodel.MethodView;
import com.google.api.codegen.clientconfig.viewmodel.RetryCodeDefView;
import com.google.api.codegen.clientconfig.viewmodel.RetryParamDefView;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.gax.retrying.RetrySettings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;

/** Constructs a partial ViewModel for producing client config views. */
public class ClientConfigTransformer {
  public static final String TEMPLATE_FILENAME = "clientconfig/main.snip";

  private final MethodTransformer methodTransformer;

  public ClientConfigTransformer(MethodTransformer methodTransformer) {
    this.methodTransformer = methodTransformer;
  }

  public ClientConfigView.Builder generateClientConfig(InterfaceConfig interfaceConfig) {
    ClientConfigView.Builder view = ClientConfigView.newBuilder();
    view.templateFileName(TEMPLATE_FILENAME);
    view.name(interfaceConfig.getInterfaceModel().getFullName());
    view.hasVariable(false);
    view.retryCodesDef(generateRetryCodesDef(interfaceConfig.getRetryCodesDefinition()));
    view.retryParamsDef(generateRetryParamsDef(interfaceConfig.getRetrySettingsDefinition()));
    view.methods(generateMethods(interfaceConfig.getMethodConfigs()));
    return view;
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
      methods.add(methodTransformer.generateMethod(methodConfig));
    }
    return methods.build();
  }
}
