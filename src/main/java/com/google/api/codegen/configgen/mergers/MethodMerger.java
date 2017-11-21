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
package com.google.api.codegen.configgen.mergers;

import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.configgen.ListTransformer;
import com.google.api.codegen.configgen.NodeFinder;
import com.google.api.codegen.configgen.PagingParameters;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.metadata.DefaultComment;
import com.google.api.codegen.configgen.nodes.metadata.FixmeComment;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Merges the methods property from an API interface into a ConfigNode. */
public class MethodMerger {
  // Do not apply flattening if the parameter count exceeds the threshold.
  // TODO(garrettjones): Investigate a more intelligent way to handle this.
  private static final int FLATTENING_THRESHOLD = 4;

  private static final int REQUEST_OBJECT_METHOD_THRESHOLD = 1;

  private static final String METHODS_COMMENT =
      "A list of method configurations.\n"
          + "Common properties:\n\n"
          + "  name - The simple name of the method.\n\n"
          + "  flattening - Specifies the configuration for parameter flattening.\n"
          + "  Describes the parameter groups for which a generator should produce method "
          + "overloads which allow a client to directly pass request message fields as method "
          + "parameters. This information may or may not be used, depending on the target "
          + "language.\n"
          + "  Consists of groups, which each represent a list of parameters to be "
          + "flattened. Each parameter listed must be a field of the request message.\n\n"
          + "  required_fields - Fields that are always required for a request to be valid.\n\n"
          + "  request_object_method - Turns on or off the generation of a method whose sole "
          + "parameter is a request object. Not all languages will generate this method.\n\n"
          + "  resource_name_treatment - An enum that specifies how to treat the resource name "
          + "formats defined in the field_name_patterns and response_field_name_patterns fields.\n"
          + "  UNSET: default value\n"
          + "  NONE: the collection configs will not be used by the generated code.\n"
          + "  VALIDATE: string fields will be validated by the client against the specified "
          + "resource name formats.\n"
          + "  STATIC_TYPES: the client will use generated types for resource names.\n\n"
          + "  page_streaming - Specifies the configuration for paging.\n"
          + "  Describes information for generating a method which transforms a paging list RPC "
          + "into a stream of resources.\n"
          + "  Consists of a request and a response.\n"
          + "  The request specifies request information of the list method. It defines which "
          + "fields match the paging pattern in the request. The request consists of a "
          + "page_size_field and a token_field. The page_size_field is the name of the optional "
          + "field specifying the maximum number of elements to be returned in the response. The "
          + "token_field is the name of the field in the request containing the page token.\n"
          + "  The response specifies response information of the list method. It defines which "
          + "fields match the paging pattern in the response. The response consists of a "
          + "token_field and a resources_field. The token_field is the name of the field in the "
          + "response containing the next page token. The resources_field is the name of the field "
          + "in the response containing the list of resources belonging to the page.\n\n"
          + "  retry_codes_name - Specifies the configuration for retryable codes. The name must "
          + "be defined in interfaces.retry_codes_def.\n\n"
          + "  retry_params_name - Specifies the configuration for retry/backoff parameters. The "
          + "name must be defined in interfaces.retry_params_def.\n\n"
          + "  field_name_patterns - Maps the field name of the request type to entity_name of "
          + "interfaces.collections.\n"
          + "  Specifies the string pattern that the field must follow.\n\n"
          + "  timeout_millis - Specifies the default timeout for a non-retrying call. If the call "
          + "is retrying, refer to retry_params_name instead.";

  private final RetryMerger retryMerger;
  private final PageStreamingMerger pageStreamingMerger;
  private final PagingParameters pagingParameters;

  public MethodMerger(
      RetryMerger retryMerger,
      PageStreamingMerger pageStreamingMerger,
      PagingParameters pagingParameters) {
    this.retryMerger = retryMerger;
    this.pageStreamingMerger = pageStreamingMerger;
    this.pagingParameters = pagingParameters;
  }

  public void generateMethodsNode(
      ConfigNode parentNode, InterfaceModel apiInterface, Map<String, String> collectionNameMap) {
    FieldConfigNode methodsNode =
        new FieldConfigNode("methods").setComment(new DefaultComment(METHODS_COMMENT));
    NodeFinder.getLastChild(parentNode).insertNext(methodsNode);
    generateMethodsValueNode(methodsNode, apiInterface, collectionNameMap);
  }

  private ConfigNode generateMethodsValueNode(
      ConfigNode parentNode,
      InterfaceModel apiInterface,
      final Map<String, String> collectionNameMap) {
    return ListTransformer.generateList(
        apiInterface.getMethods(),
        parentNode,
        method -> generateMethodNode(method, collectionNameMap));
  }

  private ListItemConfigNode generateMethodNode(
      MethodModel method, Map<String, String> collectionNameMap) {
    ListItemConfigNode methodNode = new ListItemConfigNode();
    ConfigNode nameNode = FieldConfigNode.createStringPair("name", method.getSimpleName());
    methodNode.setChild(nameNode);
    ConfigNode prevNode = generateField(nameNode, method);
    prevNode = pageStreamingMerger.generatePageStreamingNode(prevNode, method);
    prevNode = retryMerger.generateRetryNamesNode(prevNode, method);
    prevNode = generateFieldNamePatterns(prevNode, method, collectionNameMap);
    ConfigNode timeoutMillisNode =
        FieldConfigNode.createStringPair("timeout_millis", "60000")
            .setComment(new FixmeComment("Configure the default timeout for a non-retrying call."));
    prevNode.insertNext(timeoutMillisNode);
    return methodNode;
  }

  private ConfigNode generateField(ConfigNode prevNode, MethodModel method) {
    List<String> parameterList = new ArrayList<>();
    for (FieldModel field : method.getInputFields()) {
      String fieldName = field.getSimpleName();
      if (field.getOneof() == null
          && !pagingParameters.getIgnoredParameters().contains(fieldName)) {
        parameterList.add(fieldName);
      }
    }

    if (parameterList.size() > 0 && parameterList.size() <= FLATTENING_THRESHOLD) {
      prevNode = generateFlatteningNode(prevNode, parameterList);
    }

    FieldConfigNode requiredFieldsNode = new FieldConfigNode("required_fields");
    requiredFieldsNode.setComment(new FixmeComment("Configure which fields are required."));
    ConfigNode requiredFieldsValueNode =
        ListTransformer.generateStringList(parameterList, requiredFieldsNode);
    if (requiredFieldsValueNode.isPresent()) {
      prevNode.insertNext(requiredFieldsNode);
      prevNode = requiredFieldsNode;
    }

    // use all fields for the following check; if there are ignored fields for flattening
    // purposes, the caller still needs a way to set them (by using the request object method).
    int fieldCount = Iterables.size(method.getInputFields());
    boolean requestObjectMethod =
        (fieldCount > REQUEST_OBJECT_METHOD_THRESHOLD || fieldCount != parameterList.size())
            && !method.getRequestStreaming();
    ConfigNode requestObjectMethodNode =
        FieldConfigNode.createStringPair(
            "request_object_method", String.valueOf(requestObjectMethod));
    prevNode.insertNext(requestObjectMethodNode);
    return requestObjectMethodNode;
  }

  private ConfigNode generateFlatteningNode(ConfigNode prevNode, List<String> parameterList) {
    ConfigNode flatteningNode =
        new FieldConfigNode("flattening")
            .setComment(
                new FixmeComment(
                    "Configure which groups of fields should be flattened into method params."));
    prevNode.insertNext(flatteningNode);
    ConfigNode flatteningGroupsNode = new FieldConfigNode("groups");
    flatteningNode.setChild(flatteningGroupsNode);
    ConfigNode groupNode = new ListItemConfigNode();
    flatteningGroupsNode.setChild(groupNode);
    ConfigNode parametersNode = new FieldConfigNode("parameters");
    groupNode.setChild(parametersNode);
    ListTransformer.generateStringList(parameterList, parametersNode);
    return flatteningNode;
  }

  private ConfigNode generateFieldNamePatterns(
      ConfigNode prevNode, MethodModel method, final Map<String, String> nameMap) {
    ConfigNode fieldNamePatternsNode = new FieldConfigNode("field_name_patterns");
    ConfigNode fieldNamePatternsValueNode =
        ListTransformer.generateList(
            method.getResourcePatternNameMap(nameMap).entrySet(),
            fieldNamePatternsNode,
            entry -> FieldConfigNode.createStringPair(entry.getKey(), entry.getValue()));
    if (!fieldNamePatternsValueNode.isPresent()) {
      return prevNode;
    }

    prevNode.insertNext(fieldNamePatternsNode);
    return fieldNamePatternsNode;
  }
}
