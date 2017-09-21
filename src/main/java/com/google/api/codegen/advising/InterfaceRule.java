/* Copyright 2017 Google Inc
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
package com.google.api.codegen.advising;

import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.configgen.transformer.ConfigTransformer;
import com.google.api.codegen.configgen.transformer.MethodTransformer;
import com.google.api.codegen.configgen.viewmodel.MethodView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class InterfaceRule implements AdviserRule {
  private MethodTransformer methodTransformer = new MethodTransformer();
  private MethodSnippetGenerator methodSnippetGenerator = new MethodSnippetGenerator(4);

  @Override
  public String getName() {
    return "interface";
  }

  @Override
  public List<String> collectAdvice(
      Model model, ConfigProto configProto, List<String> suppressedElements) {
    ImmutableList.Builder<String> messages = ImmutableList.builder();
    Collection<Interface> apiInterfaces = new HashSet<>();
    for (ProtoFile file : model.reachable(model.getFiles())) {
      if (!file.isSource()) {
        continue;
      }

      Iterables.addAll(apiInterfaces, file.getReachableInterfaces());
    }
    Map<String, Integer> foundApis = new HashMap<>();
    Collection<String> extraApis = new HashSet<>();
    int missingNameCount = 0;
    for (InterfaceConfigProto interfaceProto : configProto.getInterfacesList()) {
      String apiName = interfaceProto.getName();
      if (apiName.isEmpty()) {
        ++missingNameCount;
        continue;
      }

      if (suppressedElements.contains(apiName)) {
        continue;
      }

      Interface apiInterface = model.getSymbolTable().lookupInterface(apiName);
      if (apiInterface == null) {
        extraApis.add(apiName);
      } else if (foundApis.containsKey(apiName)) {
        foundApis.put(apiName, foundApis.get(apiName) + 1);
      } else {
        foundApis.put(apiName, 1);
      }
    }

    Collection<Interface> missingApis = new HashSet<>();
    for (Interface apiInterface : apiInterfaces) {
      if (!foundApis.containsKey(apiInterface.getFullName())
          && !suppressedElements.contains(apiInterface.getFullName())) {
        missingApis.add(apiInterface);
      }
    }

    if (missingNameCount > 0) {
      messages.add(
          String.format(
              "Missing name in %d API %s.%n%n",
              missingNameCount, missingNameCount == 1 ? "interface" : "interfaces"));
    }

    for (Interface apiInterface : missingApis) {
      Map<String, String> collectionNameMap =
          ConfigTransformer.getResourceToEntityNameMap(apiInterface.getMethods());
      String collectionsYaml = getCollectionsYaml(collectionNameMap);
      String methodsYaml = getMethodsYaml(apiInterface, collectionNameMap);
      messages.add(
          String.format(
              "Missing API interface %s.%n"
                  + "Did you mean:%n"
                  + "  interfaces:%n"
                  + "    # ...%n"
                  + "  - name: %s%n"
                  + "    collections:%s%n"
                  + "    retry_codes_def:%n"
                  + "    - name: idempotent%n"
                  + "      retry_codes:%n"
                  + "      - UNAVAILABLE%n"
                  + "      - DEADLINE_EXCEEDED%n"
                  + "    - name: non_idempotent%n"
                  + "      retry_codes: []%n"
                  + "    retry_params_def:%n"
                  + "    - name: default%n"
                  + "      initial_retry_delay_millis: 100%n"
                  + "      retry_delay_multiplier: 1.3%n"
                  + "      max_retry_delay_millis: 60000%n"
                  + "      initial_rpc_timeout_millis: 20000%n"
                  + "      rpc_timeout_multiplier: 1%n"
                  + "      max_rpc_timeout_millis: 20000%n"
                  + "      total_timeout_millis: 600000%n"
                  + "    methods:%s%n%n",
              apiInterface.getFullName(),
              apiInterface.getFullName(),
              collectionsYaml,
              methodsYaml));
    }

    if (!extraApis.isEmpty()) {
      messages.add(
          String.format(
              "API interfaces not found in protos:%n%s%n",
              Joiner.on(String.format("%n")).join(extraApis)));
    }

    for (Map.Entry<String, Integer> entry : foundApis.entrySet()) {
      if (entry.getValue() > 1) {
        messages.add(
            String.format(
                "API interface name %s duplicated %d times.%n%n",
                entry.getKey(), entry.getValue()));
      }
    }

    return messages.build();
  }

  private String getCollectionsYaml(Map<String, String> nameMap) {
    if (nameMap.isEmpty()) {
      return " []";
    }

    StringBuilder collectionsBuilder = new StringBuilder();
    for (Map.Entry<String, String> entry : nameMap.entrySet()) {
      collectionsBuilder
          .append(System.lineSeparator())
          .append("    - name_pattern: ")
          .append(entry.getKey())
          .append(System.lineSeparator())
          .append("      entity_name: ")
          .append(entry.getValue());
    }
    return collectionsBuilder.toString();
  }

  private String getMethodsYaml(Interface apiInterface, Map<String, String> collectionNameMap) {
    StringBuilder methodsBuilder = new StringBuilder();
    for (MethodView method : methodTransformer.generateMethods(apiInterface, collectionNameMap)) {
      methodsBuilder.append(methodSnippetGenerator.generateMethodYaml(method));
    }
    return methodsBuilder.toString();
  }
}
