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
import com.google.api.codegen.PageStreamingConfigProto;
import com.google.api.codegen.PageStreamingRequestProto;
import com.google.api.codegen.PageStreamingResponseProto;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class PageStreamingRule implements AdviserRule {
  @Override
  public String getName() {
    return "page-streaming";
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

        PageStreamingConfigProto pageStreaming = methodProto.getPageStreaming();
        if (pageStreaming.equals(PageStreamingConfigProto.getDefaultInstance())) {
          continue;
        }

        PageStreamingRequestProto request = pageStreaming.getRequest();
        if (request.equals(PageStreamingRequestProto.getDefaultInstance())) {
          messages.add(
              String.format(
                  "Missing page streaming request in method %s.%n%n"
                      + "page_streaming:%n"
                      + "  # ...%n"
                      + "  request:%n"
                      + "    page_size_field: page_size%n"
                      + "    token_field: page_token%n%n",
                  method.getFullName()));
        } else if (request.getTokenField().isEmpty()) {
          messages.add(
              String.format(
                  "Missing token_field in page streaming request of method %s.%n%n"
                      + "request:%n"
                      + "  # ...%n"
                      + "  token_field: page_token%n%n",
                  method.getFullName()));
        }

        PageStreamingResponseProto response = pageStreaming.getResponse();
        if (response.equals(PageStreamingResponseProto.getDefaultInstance())) {
          messages.add(
              String.format(
                  "Missing page streaming response in method %s.%n%n%s",
                  method.getFullName(), getResponseSnippet(method)));
        } else {
          if (response.getTokenField().isEmpty()) {
            messages.add(
                String.format(
                    "Missing token_field in page streaming response of method %s.%n%n"
                        + "response:%n"
                        + "  # ...%n"
                        + "  token_field: next_page_token%n%n",
                    method.getFullName()));
          }

          if (response.getResourcesField().isEmpty()) {
            messages.add(
                String.format(
                    "Missing resources_field in page streaming response of method %s.%n%n%s",
                    method.getFullName(), getResourcesFieldSnippet(method)));
          }
        }
      }
    }
    return messages.build();
  }

  private String getResponseSnippet(Method method) {
    String resourcesField = getResourcesField(method);
    if (resourcesField == null) {
      return "";
    }

    return String.format(
        "page_streaming:%n"
            + "  # ...%n"
            + "  response:%n"
            + "    token_field: next_page_token%n"
            + "    resources_field: %s%n%n",
        resourcesField);
  }

  private String getResourcesFieldSnippet(Method method) {
    String resourcesField = getResourcesField(method);
    if (resourcesField == null) {
      return "";
    }

    return String.format("response:%n  # ...%n  resources_field: %s%n%n", resourcesField);
  }

  private String getResourcesField(Method method) {
    for (Field field : method.getOutputMessage().getFields()) {
      if (field.getType().isRepeated()) {
        return field.getSimpleName();
      }
    }
    return null;
  }
}
