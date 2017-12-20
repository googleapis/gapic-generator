/* Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A wrapper over {@link com.fasterxml.jackson.databind.JsonNode} that introduces more convenient
 * methods for accessing JSON values.
 */
public class DiscoveryNode {

  private static JsonNode EMPTY_ARRAY_JSON_NODE = JsonNodeFactory.instance.arrayNode();
  private static JsonNode EMPTY_OBJECT_JSON_NODE = JsonNodeFactory.instance.objectNode();
  private JsonNode jsonNode;

  /**
   * Constructs a DiscoveryNode that wraps jsonNode.
   *
   * <p>If jsonNode is null, then an empty JsonNode is wrapped instead.
   *
   * @param jsonNode the JsonNode to be wrapped.
   */
  public DiscoveryNode(JsonNode jsonNode) {
    if (jsonNode == null) {
      jsonNode = EMPTY_OBJECT_JSON_NODE.deepCopy();
    }
    this.jsonNode = jsonNode;
  }

  /** @return a valid string representation of this node. */
  public String asText() {
    Preconditions.checkArgument(jsonNode.isTextual());
    return jsonNode.asText();
  }

  /**
   * Returns this node's elements. If this node is not an array node, an empty list is returned.
   *
   * @return a list of this node's elements.
   */
  public List<DiscoveryNode> getElements() {
    List<DiscoveryNode> elements = new ArrayList<>();
    for (Iterator<JsonNode> it = jsonNode.elements(); it.hasNext(); ) {
      elements.add(new DiscoveryNode(it.next()));
    }
    return elements;
  }

  /**
   * Returns this node's field names. If this node is not an object node, an empty list is returned.
   *
   * @return a list of this node's field names.
   */
  public List<String> getFieldNames() {
    List<String> fieldNames = new ArrayList<>();
    for (Iterator<String> it = jsonNode.fieldNames(); it.hasNext(); ) {
      fieldNames.add(it.next());
    }
    return fieldNames;
  }

  /**
   * Returns the array node at fieldName. If fieldName is not present, an empty array node is
   * returned.
   *
   * @param fieldName key of the child node.
   * @return an array node.
   */
  public DiscoveryNode getArray(String fieldName) {
    if (!jsonNode.has(fieldName)) {
      return new DiscoveryNode(EMPTY_ARRAY_JSON_NODE);
    }
    Preconditions.checkArgument(jsonNode.get(fieldName).isArray());
    return new DiscoveryNode(jsonNode.get(fieldName));
  }

  /**
   * Returns the boolean node at fieldName. If fieldName is not present, false is returned.
   *
   * <p>Note that string nodes with the values "True" or "False" are also parsed by this function,
   * in addition to boolean nodes.
   *
   * @param fieldName key of the child node.
   * @return a boolean.
   */
  public boolean getBoolean(String fieldName) {
    if (!jsonNode.has(fieldName)) {
      return false;
    }
    if (jsonNode.get(fieldName).isTextual()) {
      String text = jsonNode.get(fieldName).asText();
      if (text.equals("True")) {
        return true;
      } else if (text.equals("False")) {
        return false;
      }
    }
    Preconditions.checkArgument(jsonNode.get(fieldName).isBoolean());
    return jsonNode.get(fieldName).asBoolean();
  }

  /**
   * Returns the object node at fieldName. If fieldName is not present, an empty object node is
   * returned.
   *
   * @param fieldName key of the child node.
   * @return an object node.
   */
  public DiscoveryNode getObject(String fieldName) {
    if (!jsonNode.has(fieldName)) {
      return new DiscoveryNode(EMPTY_OBJECT_JSON_NODE);
    }
    Preconditions.checkArgument(jsonNode.get(fieldName).isObject());
    return new DiscoveryNode(jsonNode.get(fieldName));
  }

  /**
   * Returns the string node at fieldName. If fieldName is not present, an empty string is returned.
   *
   * @param fieldName key of the child node.
   * @return a string.
   */
  public String getString(String fieldName) {
    if (!jsonNode.has(fieldName)) {
      return "";
    }
    Preconditions.checkArgument(jsonNode.get(fieldName).isTextual());
    return jsonNode.get(fieldName).asText();
  }

  /**
   * Returns true if this node is an object node and if it has the key getFieldName.
   *
   * @param fieldName key of the child node.
   * @return whether or not this node has the key getFieldName.
   */
  public boolean has(String fieldName) {
    Preconditions.checkArgument(jsonNode.isObject());
    return jsonNode.has(fieldName);
  }

  /** @return true if this node has no children. */
  public boolean isEmpty() {
    return jsonNode.size() == 0;
  }

  /** @return the number of child nodes this node contains. */
  public int size() {
    return jsonNode.size();
  }
}
