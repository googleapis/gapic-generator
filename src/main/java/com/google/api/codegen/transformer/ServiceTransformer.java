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

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.ServiceDocView;
import com.google.common.collect.ImmutableList;

public class ServiceTransformer {

  public ServiceDocView generateServiceDoc(
      InterfaceContext context, ApiMethodView exampleApiMethod, GapicProductConfig productConfig) {
    SurfaceNamer namer = context.getNamer();
    ServiceDocView.Builder serviceDoc = ServiceDocView.newBuilder();

    ImmutableList.Builder<String> docLines = ImmutableList.builder();
    docLines.addAll(namer.getDocLines(context.getInterfaceDescription()));
    InterfaceConfig conf = context.getInterfaceConfig();
    if (!conf.getManualDoc().isEmpty()) {
      docLines.add("");
      docLines.addAll(namer.getDocLines(conf.getManualDoc()));
    }
    serviceDoc.lines(docLines.build());

    serviceDoc.exampleApiMethod(exampleApiMethod);
    serviceDoc.apiVarName(namer.getApiWrapperVariableName(context.getInterfaceConfig()));
    serviceDoc.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    serviceDoc.settingsVarName(namer.getApiSettingsVariableName(context.getInterfaceConfig()));
    serviceDoc.settingsClassName(namer.getApiSettingsClassName(context.getInterfaceConfig()));
    serviceDoc.hasDefaultInstance(context.getInterfaceConfig().hasDefaultInstance());
    serviceDoc.serviceTitle(context.serviceTitle());
    serviceDoc.defaultTransportProviderBuilder(
        namer.getDefaultTransportProviderBuilder(productConfig.getTransportProtocol()));
    serviceDoc.defaultChannelProviderBuilder(
        namer.getDefaultChannelProviderBuilder(productConfig.getTransportProtocol()));
    return serviceDoc.build();
  }
}
