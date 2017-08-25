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
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.configgen.transformer.ConfigTransformer;
import com.google.api.codegen.configgen.transformer.MethodTransformer;
import com.google.api.codegen.configgen.viewmodel.MethodView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MethodRule implements AdviserRule {
  private MethodSnippetGenerator snippetGenerator = new MethodSnippetGenerator(0);
  private MethodTransformer methodTransformer = new MethodTransformer();

  @Override
  public String getName() {
    return "method";
  }

  @Override
  public List<String> collectAdvice(Model model, ConfigProto configProto) {
    ImmutableList.Builder<String> messages = ImmutableList.builder();
    for (InterfaceConfigProto interfaceProto : configProto.getInterfacesList()) {
      Interface apiInterface = model.getSymbolTable().lookupInterface(interfaceProto.getName());
      if (apiInterface != null) {
        messages.addAll(collectInterfaceAdvice(apiInterface, interfaceProto));
      }
    }
    return messages.build();
  }

  private List<String> collectInterfaceAdvice(
      Interface apiInterface, InterfaceConfigProto interfaceProto) {
    ImmutableList.Builder<String> messages = ImmutableList.builder();
    Map<String, Integer> foundMethods = new HashMap<>();
    Collection<String> extraMethods = new HashSet<>();
    int missingNameCount = 0;
    for (MethodConfigProto methodProto : interfaceProto.getMethodsList()) {
      String methodName = methodProto.getName();
      if (methodName.isEmpty()) {
        ++missingNameCount;
        continue;
      }

      Method method = apiInterface.lookupMethod(methodName);
      if (method == null) {
        extraMethods.add(methodName);
      } else if (foundMethods.containsKey(method.getSimpleName())) {
        foundMethods.put(method.getSimpleName(), foundMethods.get(method.getSimpleName()) + 1);
      } else {
        foundMethods.put(method.getSimpleName(), 1);
      }
    }

    Collection<String> missingMethods = new HashSet<>();
    for (Method method : apiInterface.getReachableMethods()) {
      if (!foundMethods.containsKey(method.getSimpleName())) {
        missingMethods.add(method.getSimpleName());
      }
    }

    if (missingNameCount > 0) {
      messages.add(
          String.format(
              "Missing name in %d %s of API interface %s.%n%n",
              missingNameCount,
              missingNameCount == 1 ? "method" : "methods",
              apiInterface.getFullName()));
    }

    if (!missingMethods.isEmpty()) {
      Map<String, String> collectionNameMap =
          ConfigTransformer.getResourceToEntityNameMap(apiInterface.getMethods());
      for (MethodView method : methodTransformer.generateMethods(apiInterface, collectionNameMap)) {
        if (missingMethods.contains(method.name())) {
          String methodYaml = snippetGenerator.generateMethodYaml(method);
          messages.add(
              String.format(
                  "Missing method %s in API interface %s.%n%nmethods:%n# ...%s%n%n",
                  method.name(), apiInterface.getFullName(), methodYaml));
        }
      }
    }

    if (!extraMethods.isEmpty()) {
      messages.add(
          String.format(
              "Methods not found in API interface %s:%n%s%n%n",
              apiInterface.getFullName(), Joiner.on(", ").join(extraMethods)));
    }

    for (Map.Entry<String, Integer> entry : foundMethods.entrySet()) {
      if (entry.getValue() > 1) {
        messages.add(
            String.format(
                "Method name %s duplicated %d times in API interface %s.%n%n",
                entry.getKey(), entry.getValue(), apiInterface.getFullName()));
      }
    }

    return messages.build();
  }
}
