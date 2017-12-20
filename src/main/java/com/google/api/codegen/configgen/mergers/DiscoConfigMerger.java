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
package com.google.api.codegen.configgen.mergers;

import com.google.api.codegen.config.DiscoApiModel;
import com.google.api.codegen.configgen.ConfigHelper;
import com.google.api.codegen.configgen.DiscoInterfaceTransformer;
import com.google.api.codegen.configgen.DiscoMethodTransformer;
import com.google.api.codegen.configgen.DiscoPageStreamingTransformer;
import com.google.api.codegen.configgen.InterfaceTransformer;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.discovery.Document;
import com.google.api.tools.framework.model.SimpleDiagCollector;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import java.io.File;

/** Merges the gapic config from a discovery Document into a ConfigNode. */
public class DiscoConfigMerger {
  public ConfigNode mergeConfig(Document model, String fileName) {
    return createMerger(model, fileName).mergeConfig(new DiscoApiModel(model));
  }

  public ConfigNode mergeConfig(Document model, File file) {
    return createMerger(model, file.getName()).mergeConfig(new DiscoApiModel(model), file);
  }

  private ConfigMerger createMerger(Document model, String fileName) {
    CollectionMerger collectionMerger = new CollectionMerger();
    RetryMerger retryMerger = new RetryMerger();
    ConfigHelper helper = new ConfigHelper(new SimpleDiagCollector(), fileName);
    PageStreamingMerger pageStreamingMerger =
        new PageStreamingMerger(new DiscoPageStreamingTransformer(), helper);
    MethodMerger methodMerger =
        new MethodMerger(retryMerger, pageStreamingMerger, new DiscoMethodTransformer());
    LanguageSettingsMerger languageSettingsMerger = new LanguageSettingsMerger();
    InterfaceTransformer interfaceTranformer = new DiscoInterfaceTransformer();
    InterfaceMerger interfaceMerger =
        new InterfaceMerger(collectionMerger, retryMerger, methodMerger, interfaceTranformer);
    String packageName = getPackageName(model);
    return new ConfigMerger(languageSettingsMerger, interfaceMerger, packageName, helper);
  }

  private String getPackageName(Document model) {
    String reverseDomain =
        Joiner.on(".").join(Lists.reverse(Splitter.on(".").splitToList(model.ownerDomain())));
    return String.format("%s.%s.%s", reverseDomain, model.name(), model.version());
  }
}
