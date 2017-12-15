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

import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Method;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** Discovery-based InterfaceModel. */
public class DiscoInterfaceModel implements InterfaceModel {
  private final String interfaceName;
  private final DiscoApiModel apiModel;

  public DiscoInterfaceModel(String interfaceName, Document document) {
    this.interfaceName = interfaceName;
    this.apiModel = new DiscoApiModel(document);
  }

  @Override
  public ApiSource getApiSource() {
    return ApiSource.DISCOVERY;
  }

  @Override
  public String getSimpleName() {
    return DiscoGapicNamer.getSimpleInterfaceName(interfaceName);
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
  public String getFileFullName() {
    return interfaceName;
  }

  @Override
  public String getParentFullName() {
    return interfaceName;
  }

  @Override
  public boolean isReachable() {
    // TODO(andrealin): Implement.
    return true;
  }

  @Override
  public DiscoApiModel getApiModel() {
    return apiModel;
  }

  /** Returns a list of language-agnostic methods. Some member functions may fail on the methods. */
  @Override
  public List<MethodModel> getMethods() {
    ImmutableList.Builder<MethodModel> methods = ImmutableList.builder();
    if (!apiModel.getDocument().resources().containsKey(interfaceName)) {
      return ImmutableList.of();
    }
    for (Method method : apiModel.getDocument().resources().get(interfaceName)) {
      methods.add(new DiscoveryMethodModel(method, null));
    }
    return methods.build();
  }

  @Override
  public boolean equals(Object o) {
    return o != null
        && o instanceof DiscoInterfaceModel
        && ((DiscoInterfaceModel) o).interfaceName.equals(interfaceName)
        && ((DiscoInterfaceModel) o).apiModel.equals(apiModel);
  }
}
