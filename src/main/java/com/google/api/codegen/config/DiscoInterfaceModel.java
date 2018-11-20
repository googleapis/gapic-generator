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
package com.google.api.codegen.config;

import com.google.api.codegen.discogapic.transformer.DiscoGapicParser;
import com.google.api.codegen.discovery.Method;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** Discovery-based InterfaceModel. */
public class DiscoInterfaceModel implements InterfaceModel {
  private final String interfaceName;
  private final DiscoApiModel apiModel;

  public DiscoInterfaceModel(String interfaceName, DiscoApiModel apiModel) {
    this.interfaceName = interfaceName;
    this.apiModel = apiModel;
  }

  @Override
  public String getSimpleName() {
    return DiscoGapicParser.getSimpleInterfaceName(interfaceName);
  }

  @Override
  public String getFullName() {
    return interfaceName;
  }

  @Override
  public String getFileSimpleName() {
    return interfaceName;
  }

  @Override
  public String getParentFullName() {
    return interfaceName;
  }

  @Override
  public boolean isReachable() {
    return true;
  }

  @Override
  public DiscoApiModel getApiModel() {
    return apiModel;
  }

  /** Returns a list of language-agnostic methods. */
  @Override
  public List<MethodModel> getMethods() {
    ImmutableList.Builder<MethodModel> methods = ImmutableList.builder();
    if (!apiModel.getDocument().resources().containsKey(interfaceName)) {
      return ImmutableList.of();
    }
    for (Method method : apiModel.getDocument().resources().get(interfaceName)) {
      methods.add(new DiscoveryMethodModel(method, apiModel));
    }
    return methods.build();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof DiscoInterfaceModel
        && ((DiscoInterfaceModel) o).interfaceName.equals(interfaceName)
        && ((DiscoInterfaceModel) o).apiModel.equals(apiModel);
  }
}
