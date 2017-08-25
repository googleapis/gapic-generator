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

import com.google.api.codegen.CollectionConfigProto;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;

public class CollectionRule implements AdviserRule {
  @Override
  public String getName() {
    return "collection";
  }

  @Override
  public List<String> collectAdvice(Model model, ConfigProto configProto) {
    ImmutableList.Builder<String> messages = ImmutableList.builder();
    for (InterfaceConfigProto interfaceProto : configProto.getInterfacesList()) {
      Interface apiInterface = model.getSymbolTable().lookupInterface(interfaceProto.getName());
      if (apiInterface == null) {
        continue;
      }

      for (MethodConfigProto methodProto : interfaceProto.getMethodsList()) {
        Method method = apiInterface.lookupMethod(methodProto.getName());
        if (method == null) {
          continue;
        }

        for (Map.Entry<String, String> entry : methodProto.getFieldNamePatternsMap().entrySet()) {
          String pathTemplate = entry.getKey();
          String entityName = entry.getValue();

          if (!isFieldName(method, pathTemplate)) {
            messages.add(
                String.format(
                    "Path template %s not found in field names for method %s.%n%n",
                    pathTemplate, method.getFullName()));
          }

          if (!isEntityName(interfaceProto.getCollectionsList(), entityName)
              && !isEntityName(configProto.getCollectionsList(), entityName)) {
            messages.add(
                String.format(
                    "Entity name %s for method %s not found in collections for API interface %s.%n%n",
                    entityName, method.getSimpleName(), apiInterface.getFullName()));
          }
        }
      }
    }
    return messages.build();
  }

  private boolean isFieldName(Method method, String pathTemplate) {
    for (Field field : method.getInputMessage().getReachableFields()) {
      if (pathTemplate.equals(field.getSimpleName())) {
        return true;
      }
    }

    return false;
  }

  private boolean isEntityName(List<CollectionConfigProto> collections, String entityName) {
    for (CollectionConfigProto collectionProto : collections) {
      if (entityName.equals(collectionProto.getEntityName())) {
        return true;
      }
    }

    return false;
  }
}
