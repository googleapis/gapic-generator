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
package com.google.api.codegen.configgen.mergers;

import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.configgen.InterfaceTransformer;
import com.google.api.codegen.configgen.ProtoInterfaceTransformer;
import com.google.api.codegen.configgen.ProtoMethodTransformer;
import com.google.api.codegen.configgen.ProtoPageStreamingTransformer;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.protobuf.Api;

/** Merges the gapic config from a proto Model into a ConfigNode. */
public class ProtoConfigMerger {
  public ConfigNode mergeConfig(Model model) {
    CollectionMerger collectionMerger = new CollectionMerger();
    RetryMerger retryMerger = new RetryMerger();
    PageStreamingMerger pageStreamingMerger =
        new PageStreamingMerger(new ProtoPageStreamingTransformer(), model.getDiagCollector());
    MethodMerger methodMerger =
        new MethodMerger(retryMerger, pageStreamingMerger, new ProtoMethodTransformer());
    LanguageSettingsMerger languageSettingsMerger = new LanguageSettingsMerger();
    InterfaceTransformer interfaceTranformer = new ProtoInterfaceTransformer();
    InterfaceMerger interfaceMerger =
        new InterfaceMerger(collectionMerger, retryMerger, methodMerger, interfaceTranformer);
    String packageName = getPackageName(model);
    if (packageName == null) {
      return null;
    }

    ConfigMerger configMerger =
        new ConfigMerger(languageSettingsMerger, interfaceMerger, packageName);
    return configMerger.mergeConfig(new ProtoApiModel(model));
  }

  private String getPackageName(Model model) {
    if (model.getServiceConfig().getApisCount() > 0) {
      Api api = model.getServiceConfig().getApis(0);
      Interface apiInterface = model.getSymbolTable().lookupInterface(api.getName());
      if (apiInterface != null) {
        return apiInterface.getFile().getFullName();
      }
    }

    model.getDiagCollector().addDiag(Diag.error(model.getLocation(), "No interface found"));
    return null;
  }
}
