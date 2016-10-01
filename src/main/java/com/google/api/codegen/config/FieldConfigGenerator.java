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
package com.google.api.codegen.config;

import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Config generator for method parameter flattening, required fields, and request method object.
 */
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
    }

    // Required parameters for this method.
    List<String> requiredParameters = new LinkedList<>();
    for (Field field : message.getFields()) {
      // We skip the fields that we deem optional. Ideally, for each of them we'd still produce a
      // placeholder comment in the config, naming it and explaining why we decided it's not
      // required. That'd help both reviewing the generated config, and spotting changes when it's
      // regenerated. Unfortunately there doesn't seem to be an easy way to output comments.

      String fieldName = field.getSimpleName();
      // Ignore pagination parameters.
      if (ignoredFields.contains(fieldName)) {
        continue;
      }
      // Ignore boolean fields. Those need always be optional, for requiring one to have a value
      // other than the default (false) is requiring it to be set to true, which doesn't make sense.
      if (field.getType().getKind() == Type.TYPE_BOOL && !field.getType().isRepeated()) {
        continue;
      }
      // Ignore fields whose documentation indicates are optional.
      if (isDescribedAsOptional(DocumentationUtil.getDescription(field))) {
        continue;
      }
      requiredParameters.add(fieldName);
    }
    result.put(CONFIG_KEY_REQUIRED_FIELDS, requiredParameters);

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

  // Heuristic created from the comments in Cloud Debugger API. To be refined as we review more
  // APIs.
  private static boolean isDescribedAsOptional(String description) {
    return description.contains(", if specified,")
        || description.contains("If specified,")
        || description.contains(", if set,")
        || description.contains("If set,")
        || description.contains(", when set,")
        || description.contains("When set,");
    // Indicators of being required: "must be set" (which usually refers to message subfields),
    // appearing in the URL of a GET method, or being a string field named *_id (which has a big
    // overlap with the former).
  }
}
