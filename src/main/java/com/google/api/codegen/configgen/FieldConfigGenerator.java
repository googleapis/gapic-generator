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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** Config generator for method parameter flattening, required fields, and request method object. */
public class FieldConfigGenerator implements MethodConfigGenerator {

  private static final String CONFIG_KEY_GROUPS = "groups";
  private static final String CONFIG_KEY_PARAMETERS = "parameters";
  private static final String CONFIG_KEY_FLATTENING = "flattening";
  private static final String CONFIG_KEY_REQUIRED_FIELDS = "required_fields";
  private static final String CONFIG_KEY_REQUEST_OBJECT_METHOD = "request_object_method";

  private static final String PARAMETER_PAGE_TOKEN = "page_token";
  private static final String PARAMETER_PAGE_SIZE = "page_size";

  // Do not apply flattening if the parameter count exceeds the threshold.
  // TODO(shinfan): Investigate a more intelligent way to handle this.
  private static final int FLATTENING_THRESHOLD = 4;

  private static final int REQUEST_OBJECT_METHOD_THRESHOLD = 1;

  @Override
  public Map<String, Object> generate(Method method) {
    List<String> ignoredFields = Arrays.asList(PARAMETER_PAGE_TOKEN, PARAMETER_PAGE_SIZE);

    List<String> parameterList = new LinkedList<String>();
    MessageType message = method.getInputMessage();
    for (Field field : message.getFields()) {
      String fieldName = field.getSimpleName();
      if (!ignoredFields.contains(fieldName)) {
        parameterList.add(field.getSimpleName());
      }
    }

    Map<String, Object> result = new LinkedHashMap<String, Object>();
    if (parameterList.size() > 0) {
      if (parameterList.size() <= FLATTENING_THRESHOLD) {
        result.put(CONFIG_KEY_FLATTENING, createFlatteningConfig(parameterList));
      }
      result.put(CONFIG_KEY_REQUIRED_FIELDS, new LinkedList<String>(parameterList));
    }
    // use all fields for the following check; if there are ignored fields for flattening
    // purposes, the caller still needs a way to set them (by using the request object method).
    if (message.getFields().size() > REQUEST_OBJECT_METHOD_THRESHOLD
        || message.getFields().size() != parameterList.size()) {
      result.put(CONFIG_KEY_REQUEST_OBJECT_METHOD, true);
    } else {
      result.put(CONFIG_KEY_REQUEST_OBJECT_METHOD, false);
    }
    return result;
  }

  private Map<String, Object> createFlatteningConfig(List<String> parameterList) {
    Map<String, Object> parameters = new LinkedHashMap<String, Object>();
    parameters.put(CONFIG_KEY_PARAMETERS, parameterList);

    List<Object> groups = new LinkedList<Object>();
    groups.add(parameters);

    Map<String, Object> flattening = new LinkedHashMap<String, Object>();
    flattening.put(CONFIG_KEY_GROUPS, groups);

    return flattening;
  }
}
