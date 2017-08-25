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
import com.google.api.codegen.FlatteningGroupProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class FieldRule implements AdviserRule {
  @Override
  public String getName() {
    return "field";
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

        Collection<String> fieldNames = getFieldNames(method);
        for (FlatteningGroupProto group : methodProto.getFlattening().getGroupsList()) {
          for (String parameter : group.getParametersList()) {
            if (!fieldNames.contains(parameter)) {
              messages.add(
                  String.format(
                      "Flattening group parameter %s was not found in the field names of method %s.%n"
                          + "Must be one of: %s%n%n",
                      parameter, method.getFullName(), Joiner.on(", ").join(fieldNames)));
            }
          }
        }

        for (String field : methodProto.getRequiredFieldsList()) {
          if (!fieldNames.contains(field)) {
            messages.add(
                String.format(
                    "Required field %s was not found in the field names of method %s.%n"
                        + "Must be one of: %s%n%n",
                    field, method.getFullName(), Joiner.on(", ").join(fieldNames)));
          }
        }
      }
    }
    return messages.build();
  }

  private Collection<String> getFieldNames(Method method) {
    Collection<String> fieldNames = new HashSet<>();
    for (Field field : method.getInputMessage().getReachableFields()) {
      fieldNames.add(field.getSimpleName());
    }
    return fieldNames;
  }
}
