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
package com.google.api.codegen.configgen;

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.ProtoInterfaceModel;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** InterfaceTransformer implementation for proto Interfaces. */
public class ProtoInterfaceTransformer implements InterfaceTransformer {
  @Override
  public Map<String, String> getResourceToEntityNameMap(InterfaceModel apiInterface) {
    Interface protoInterface = ((ProtoInterfaceModel) apiInterface).getInterface();
    // Using a map with the string representation of the resource path to avoid duplication
    // of equivalent paths.
    // Using a TreeMap in particular so that the ordering is deterministic
    // (useful for testability).
    Map<String, CollectionPattern> specs = new TreeMap<>();
    for (Method method : protoInterface.getReachableMethods()) {
      for (CollectionPattern collectionPattern :
          CollectionPattern.getCollectionPatternsFromMethod(method)) {
        String resourcePath = collectionPattern.getTemplatizedResourcePath();
        // If there are multiple field segments with the same resource path, the last
        // one will be used, making the output deterministic. Also, the first field path
        // encountered tends to be simply "name" because it is the corresponding create
        // API method for the type.
        specs.put(resourcePath, collectionPattern);
      }
    }

    Set<String> usedNameSet = new HashSet<>();
    ImmutableMap.Builder<String, String> nameMapBuilder = ImmutableMap.builder();
    for (CollectionPattern collectionPattern : specs.values()) {
      String resourceNameString = collectionPattern.getTemplatizedResourcePath();
      String entityNameString = collectionPattern.getUniqueName(usedNameSet);
      usedNameSet.add(entityNameString);
      nameMapBuilder.put(resourceNameString, entityNameString);
    }
    return nameMapBuilder.build();
  }

  @Override
  public void generateResourceNameGenerations(ConfigNode parentNode, ApiModel model) {
    // Do nothing
  }
}
