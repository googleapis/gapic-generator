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
package com.google.api.codegen.configgen.mergers;

import com.google.api.codegen.configgen.CollectionPattern;
import com.google.api.codegen.configgen.ListTransformer;
import com.google.api.codegen.configgen.NodeFinder;
import com.google.api.codegen.configgen.StringPairTransformer;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.metadata.DefaultComment;
import com.google.api.codegen.configgen.nodes.metadata.FixmeComment;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Merges the methods property from a Model into a ConfigNode. */
public class MethodMerger {
  private static final ImmutableSet<String> IGNORED_FIELDS =
      ImmutableSet.of("page_token", "page_size");

  // Do not apply flattening if the parameter count exceeds the threshold.
  // TODO(garrettjones): Investigate a more intelligent way to handle this.
  private static final int FLATTENING_THRESHOLD = 4;

  private static final int REQUEST_OBJECT_METHOD_THRESHOLD = 1;

  private static final String METHODS_COMMENT =
      "A list of method configurations.\n"
          + "Common properties:\n\n"
          + "  name - The simple name of the method.\n\n"
          + "  flattening - Specifies the configuration for parameter flattening.\n"
          + "    Describes the parameter groups for which a generator should produce method "
          + "overloads which allow a client to directly pass request message fields as method "
          + "parameters. This information may or may not be used, depending on the target "
          + "language.\n"
          + "    Consists of groups, which each represent a list of parameters to be "
          + "flattened. Each parameter listed must be a field of the request message.\n\n"
          + "  required_fields - Fields that are always required for a request to be valid.\n\n"
          + "  request_object_method - Turns on or off the generation of a method whose sole "
          + "parameter is a request object. Not all languages will generate this method.\n\n"
          + "  page_streaming - Specifies the configuration for paging.\n"
          + "    Describes information for generating a method which transforms a paging list RPC "
          + "into a stream of resources.\n"
          + "    Consists of a request and a response.\n"
          + "    The request specifies request information of the list method. It defines which "
          + "fields match the paging pattern in the request. The request consists of a "
          + "page_size_field and a token_field. The page_size_field is the name of the optional "
          + "field specifying the maximum number of elements to be returned in the response. The "
          + "token_field is the name of the field in the request containing the page token.\n"
          + "    The response specifies response information of the list method. It defines which "
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
          + "    Specifies the string pattern that the field must follow.\n\n"
          + "  timeout_millis - Specifies the default timeout for a non-retrying call. If the call "
          + "is retrying, refer to retry_params_name instead.";

  private final RetryMerger retryMerger = new RetryMerger();
  private final PageStreamingMerger pageStreamingMerger = new PageStreamingMerger();

  public void generateMethodsNode(
      ConfigNode parentNode, Interface apiInterface, Map<String, String> collectionNameMap) {
    FieldConfigNode methodsNode =
        new FieldConfigNode("methods").setComment(new DefaultComment(METHODS_COMMENT));
    NodeFinder.getLastChild(parentNode).insertNext(methodsNode);
    generateMethodsValueNode(methodsNode, apiInterface, collectionNameMap);
  }

  private ConfigNode generateMethodsValueNode(
      ConfigNode parentNode, Interface apiInterface, final Map<String, String> collectionNameMap) {
    return ListTransformer.generateList(
        apiInterface.getReachableMethods(),
        parentNode,
        new ListTransformer.ElementTransformer<Method>() {
          @Override
          public ConfigNode generateElement(Method method) {
            return generateMethodNode(method, collectionNameMap);
          }
        });
  }

  private ListItemConfigNode generateMethodNode(
      Method method, Map<String, String> collectionNameMap) {
    ListItemConfigNode methodNode = new ListItemConfigNode();
    ConfigNode nameNode = StringPairTransformer.generateStringPair("name", method.getSimpleName());
    methodNode.setChild(nameNode);
    ConfigNode prevNode = generateField(nameNode, method);
    prevNode = pageStreamingMerger.generatePageStreamingNode(prevNode, method);
    prevNode = retryMerger.generateRetryNamesNode(prevNode, method);
    prevNode = generateFieldNamePatterns(prevNode, method, collectionNameMap);
    ConfigNode timeoutMillisNode =
        StringPairTransformer.generateStringPair("timeout_millis", "60000")
            .setComment(new FixmeComment("Configure the default timeout for a non-retrying call."));
    prevNode.insertNext(timeoutMillisNode);
    return methodNode;
  }

  private ConfigNode generateField(ConfigNode prevNode, Method method) {
    List<String> parameterList = new ArrayList<>();
    MessageType message = method.getInputMessage();
    for (Field field : message.getReachableFields()) {
      String fieldName = field.getSimpleName();
      if (field.getOneof() == null && !IGNORED_FIELDS.contains(fieldName)) {
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
    int fieldCount = Iterables.size(message.getReachableFields());
    boolean requestObjectMethod =
        (fieldCount > REQUEST_OBJECT_METHOD_THRESHOLD || fieldCount != parameterList.size())
            && !method.getRequestStreaming();
    ConfigNode requestObjectMethodNode =
        StringPairTransformer.generateStringPair(
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
      ConfigNode prevNode, Method method, final Map<String, String> nameMap) {
    ConfigNode fieldNamePatternsNode = new FieldConfigNode("field_name_patterns");
    ConfigNode fieldNamePatternsValueNode =
        ListTransformer.generateList(
            CollectionPattern.getCollectionPatternsFromMethod(method),
            fieldNamePatternsNode,
            new ListTransformer.ElementTransformer<CollectionPattern>() {
              @Override
              public ConfigNode generateElement(CollectionPattern collectionPattern) {
                return StringPairTransformer.generateStringPair(
                    collectionPattern.getFieldPath(),
                    nameMap.get(collectionPattern.getTemplatizedResourcePath()));
              }
            });
    if (!fieldNamePatternsValueNode.isPresent()) {
      return prevNode;
    }

    prevNode.insertNext(fieldNamePatternsNode);
    return fieldNamePatternsNode;
  }
}
