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

import com.google.api.codegen.configgen.viewmodel.FieldNamePatternView;
import com.google.api.codegen.configgen.viewmodel.FlatteningGroupView;
import com.google.api.codegen.configgen.viewmodel.MethodView;
import com.google.api.codegen.configgen.viewmodel.PageStreamingRequestView;
import com.google.api.codegen.configgen.viewmodel.PageStreamingResponseView;
import com.google.common.base.Strings;

public class MethodSnippetGenerator {
  private String prefix;

  public MethodSnippetGenerator(int indentSize) {
    prefix = System.lineSeparator() + Strings.repeat(" ", indentSize);
  }

  public String generateMethodYaml(MethodView method) {
    StringBuilder methodBuilder = new StringBuilder();
    methodBuilder.append(prefix).append("- name: ").append(method.name());
    if (method.hasFlattening()) {
      methodBuilder.append(prefix).append("  flattening:").append(prefix).append("    groups:");
      for (FlatteningGroupView group : method.flattening().groups()) {
        methodBuilder.append(prefix).append("    - parameters:");
        for (String parameter : group.parameters()) {
          methodBuilder.append(prefix).append("      - ").append(parameter);
        }
      }
    }
    if (!method.requiredFields().isEmpty()) {
      methodBuilder.append(prefix).append("  required_fields:");
      for (String requiredField : method.requiredFields()) {
        methodBuilder.append(prefix).append("  - ").append(requiredField);
      }
    }
    methodBuilder
        .append(prefix)
        .append("  request_object_method: ")
        .append(method.requestObjectMethod());
    if (method.hasPageStreaming()) {
      PageStreamingRequestView request = method.pageStreaming().request();
      PageStreamingResponseView response = method.pageStreaming().response();
      methodBuilder.append(prefix).append("  page_streaming:");
      methodBuilder.append(prefix).append("    request:");
      if (request.hasPageSizeField()) {
        methodBuilder
            .append(prefix)
            .append("      page_size_field: ")
            .append(request.pageSizeField());
      }
      if (request.hasTokenField()) {
        methodBuilder.append(prefix).append("      token_field: ").append(request.tokenField());
      }
      methodBuilder.append(prefix).append("    response:");
      methodBuilder.append(prefix).append("      token_field: ").append(response.tokenField());
      methodBuilder
          .append(prefix)
          .append("      resources_field: ")
          .append(response.resourcesField());
    }
    methodBuilder.append(prefix).append("  retry_codes_name: ").append(method.retryCodesName());
    methodBuilder.append(prefix).append("  retry_params_name: ").append(method.retryParamsName());
    if (!method.fieldNamePatterns().isEmpty()) {
      methodBuilder.append(prefix).append("  field_name_patterns:");
      for (FieldNamePatternView pattern : method.fieldNamePatterns()) {
        methodBuilder
            .append(prefix)
            .append("    ")
            .append(pattern.pathTemplate())
            .append(": ")
            .append(pattern.entityName());
      }
    }
    methodBuilder.append(prefix).append("  timeout_millis: ").append(method.timeoutMillis());
    return methodBuilder.toString();
  }
}
