/* Copyright 2016 Google Inc
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

import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** Config generator for method page streaming. */
public class PageStreamingConfigGenerator implements MethodConfigGenerator {

  private static final String PARAMETER_PAGE_SIZE = "page_size";
  private static final String PARAMETER_PAGE_TOKEN = "page_token";
  private static final String PARAMETER_NEXT_PAGE_TOKEN = "next_page_token";

  private static final String CONFIG_KEY_REQUEST = "request";
  private static final String CONFIG_KEY_RESPONSE = "response";
  private static final String CONFIG_KEY_PAGE_STREAMING = "page_streaming";
  private static final String CONFIG_KEY_PAGE_SIZE_FIELD = "page_size_field";
  private static final String CONFIG_KEY_TOKEN_FIELD = "token_field";
  private static final String CONFIG_KEY_RESOURCE_FIELD = "resources_field";

  @Override
  public Map<String, Object> generate(Method method) {
    Map<String, Object> requestConfig = generateRequestConfig(method);
    if (requestConfig == null) {
      // Not a page streaming API
      return null;
    }
    Map<String, Object> responseConfig = generateResponseConfig(method);
    if (responseConfig == null) {
      // Did not meet the requirements of a page streaming API
      return null;
    }
    return createPageStreamingConfig(requestConfig, responseConfig);
  }

  private Map<String, Object> generateRequestConfig(Method method) {
    MessageType inputMessage = method.getInputMessage();
    Map<String, Object> requestConfig = new LinkedHashMap<String, Object>();
    for (Field field : inputMessage.getFields()) {
      String fieldName = field.getSimpleName();
      if (fieldName.equals(PARAMETER_PAGE_TOKEN)) {
        requestConfig.put(CONFIG_KEY_TOKEN_FIELD, fieldName);
      } else if (fieldName.equals(PARAMETER_PAGE_SIZE)) {
        requestConfig.put(CONFIG_KEY_PAGE_SIZE_FIELD, fieldName);
      }
    }
    if (requestConfig.size() > 0) {
      return requestConfig;
    }
    return null;
  }

  private Map<String, Object> generateResponseConfig(Method method) {
    if (!hasTokenField(method)) {
      return null;
    }
    String resourceField = findResourceField(method);
    if (resourceField == null) {
      return null;
    }
    Map<String, Object> responseConfig = new LinkedHashMap<String, Object>();
    responseConfig.put(CONFIG_KEY_TOKEN_FIELD, PARAMETER_NEXT_PAGE_TOKEN);
    responseConfig.put(CONFIG_KEY_RESOURCE_FIELD, resourceField);
    return responseConfig;
  }

  private boolean hasTokenField(Method method) {
    for (Field field : method.getOutputMessage().getFields()) {
      String fieldName = field.getSimpleName();
      if (fieldName.equals(PARAMETER_NEXT_PAGE_TOKEN)) {
        return true;
      }
    }
    return false;
  }

  private Map<String, Object> createPageStreamingConfig(
      Map<String, Object> requestConfig, Map<String, Object> responseConfig) {
    Map<String, Object> pageStreaming = new LinkedHashMap<String, Object>();
    pageStreaming.put(CONFIG_KEY_REQUEST, requestConfig);
    pageStreaming.put(CONFIG_KEY_RESPONSE, responseConfig);

    Map<String, Object> output = new LinkedHashMap<String, Object>();
    output.put(CONFIG_KEY_PAGE_STREAMING, pageStreaming);
    return output;
  }

  private String findResourceField(Method method) {
    List<String> repeatedFieldNames = new LinkedList<String>();
    for (Field field : method.getOutputMessage().getFields()) {
      if (field.getType().isRepeated()) {
        repeatedFieldNames.add(field.getSimpleName());
      }
    }
    if (repeatedFieldNames.size() == 1) {
      return repeatedFieldNames.get(0);
    } else {
      if (repeatedFieldNames.size() > 1) {
        // TODO(shinfan): Add a warning system that is used when heuristic decision cannot be made.
        System.err.printf(
            "Warning: Page Streaming resource field could not be heuristically"
                + " determined for method %s\n",
            method.getSimpleName());
      }
      return null;
    }
  }
}
