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
import com.google.api.codegen.RetryCodesDefinitionProto;
import com.google.api.codegen.RetryParamsDefinitionProto;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

public class RetryRule implements AdviserRule {
  @Override
  public String getName() {
    return "retry";
  }

  @Override
  public List<String> collectAdvice(Model model, ConfigProto configProto) {
    ImmutableList.Builder<String> messages = ImmutableList.builder();
    for (InterfaceConfigProto interfaceProto : configProto.getInterfacesList()) {
      if (!interfaceProto.getName().isEmpty()) {
        messages.addAll(collectInterfaceAdvice(interfaceProto));
      }
    }
    return messages.build();
  }

  private List<String> collectInterfaceAdvice(InterfaceConfigProto interfaceProto) {
    ImmutableList.Builder<String> messages = ImmutableList.builder();
    String interfaceName = interfaceProto.getName();
    List<String> retryCodesNames = new ArrayList<>();
    List<String> retryParamsNames = new ArrayList<>();
    int missingRetryCodesNameCount = 0;
    int missingRetryParamsNameCount = 0;
    if (interfaceProto.getRetryCodesDefCount() == 0) {
      messages.add(
          String.format(
              "Missing retry_codes_def for API interface %s.%n%n"
                  + "- name: %s%n"
                  + "  # ...%n"
                  + "  retry_codes_def:%n"
                  + "  - name: idempotent%n"
                  + "    retry_codes:%n"
                  + "    - UNAVAILABLE%n"
                  + "    - DEADLINE_EXCEEDED%n"
                  + "  - name: non_idempotent%n"
                  + "    retry_codes: []%n%n",
              interfaceName, interfaceName));
    } else {
      for (RetryCodesDefinitionProto retryCodesDef : interfaceProto.getRetryCodesDefList()) {
        String name = retryCodesDef.getName();
        if (name.isEmpty()) {
          ++missingRetryCodesNameCount;
        } else {
          retryCodesNames.add(name);
        }
      }
    }

    if (interfaceProto.getRetryParamsDefCount() == 0) {
      messages.add(
          String.format(
              "Missing retry_params_def for API interface %s.%n%n"
                  + "- name: %s%n"
                  + "  # ...%n"
                  + "  retry_params_def:%n"
                  + "  - name: default%n"
                  + "    initial_retry_delay_millis: 100%n"
                  + "    retry_delay_multiplier: 1.3%n"
                  + "    max_retry_delay_millis: 60000%n"
                  + "    initial_rpc_timeout_millis: 20000%n"
                  + "    rpc_timeout_multiplier: 1%n"
                  + "    max_rpc_timeout_millis: 20000%n"
                  + "    total_timeout_millis: 600000%n%n",
              interfaceName, interfaceName));
    } else {
      for (RetryParamsDefinitionProto retryParamsDef : interfaceProto.getRetryParamsDefList()) {
        String name = retryParamsDef.getName();
        if (name.isEmpty()) {
          ++missingRetryParamsNameCount;
        } else {
          retryParamsNames.add(name);
        }
      }
    }

    if (missingRetryCodesNameCount > 0) {
      messages.add(
          String.format(
              "Missing name in %d retry codes %s of API interface %s.%n%n",
              missingRetryCodesNameCount,
              missingRetryCodesNameCount == 1 ? "def" : "defs",
              interfaceName));
    }

    if (missingRetryParamsNameCount > 0) {
      messages.add(
          String.format(
              "Missing name in %d retry params %s of API interface %s.%n%n",
              missingRetryParamsNameCount,
              missingRetryParamsNameCount == 1 ? "def" : "defs",
              interfaceName));
    }

    for (MethodConfigProto methodProto : interfaceProto.getMethodsList()) {
      String methodName = methodProto.getName();
      String retryCodesName = methodProto.getRetryCodesName();
      String retryParamsName = methodProto.getRetryParamsName();
      if (retryCodesName.isEmpty()) {
        messages.add(
            String.format(
                "Missing retry_codes_name in method %s.%s.%n%n%s",
                interfaceName,
                methodName,
                getSnippet(methodName, "retry_codes_name", retryCodesNames)));
      } else if (!retryCodesNames.isEmpty() && !retryCodesNames.contains(retryCodesName)) {
        messages.add(
            String.format(
                "Retry codes name %s for method %s not found in retry codes defs of API interface %s.%n"
                    + "Must be one of: %s%n%n",
                retryCodesName, methodName, interfaceName, Joiner.on(", ").join(retryCodesNames)));
      }

      if (retryParamsName.isEmpty()) {
        messages.add(
            String.format(
                "Missing retry_params_name in method %s.%s.%n%n%s",
                interfaceName,
                methodName,
                getSnippet(methodName, "retry_params_name", retryParamsNames)));
      } else if (!retryParamsNames.isEmpty() && !retryParamsNames.contains(retryParamsName)) {
        messages.add(
            String.format(
                "Retry params name %s for method %s not found in retry params defs of API interface %s.%n"
                    + "Must be one of: %s%n%n",
                retryParamsName,
                methodName,
                interfaceName,
                Joiner.on(", ").join(retryParamsNames)));
      }
    }
    return messages.build();
  }

  private String getSnippet(String methodName, String keyName, List<String> names) {
    if (names.isEmpty()) {
      return "";
    }

    return String.format("- name: %s%n  # ...%n  %s: %s%n%n", methodName, keyName, names.get(0));
  }
}
