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
package com.google.api.codegen.configgen;

import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.NullConfigNode;
import com.google.api.codegen.configgen.nodes.ScalarConfigNode;
import com.google.common.base.Joiner;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.util.ArrayList;
import java.util.List;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/** Recursively reads a snakeyaml MappingNode and all its children into a ConfigNode. */
public class ConfigYamlNodeReader {
  private static final String TYPE_KEY = "type";

  private final List<String> lines;
  private final ConfigHelper helper;

  public ConfigYamlNodeReader(List<String> lines, ConfigHelper helper) {
    this.lines = lines;
    this.helper = helper;
  }

  /**
   * @param node A YAML node representing a protobuf message.
   * @param messageType A descriptor for the message the YAML node represents.
   * @return A ConfigNode containing the data read from the given node.
   */
  public ConfigNode readMessageNode(MappingNode node, Descriptor messageType) {
    return readMessageNode(0, node, messageType);
  }

  private ConfigNode readMessageNode(int prevLine, MappingNode node, Descriptor messageType) {
    ConfigNode configNode = new NullConfigNode();
    ConfigNode prev = new NullConfigNode();
    for (NodeTuple entry : node.getValue()) {
      ConfigNode entryNode = readMessageNodeEntry(entry, messageType);
      ConfigNode commentNode = readCommentNode(prevLine, entry.getKeyNode(), entryNode);
      prevLine = entry.getValueNode().getEndMark().getLine() + 1;

      if (entryNode == null) {
        continue;
      }

      prev.insertNext(commentNode);
      prev = entryNode;

      if (!configNode.isPresent()) {
        configNode = commentNode;
      }
    }
    return configNode;
  }

  private ConfigNode readMessageNode(int prevLine, Node node, FieldDescriptor field) {
    if (isEmpty(node)) {
      return new NullConfigNode();
    }

    if (node instanceof MappingNode) {
      return readMessageNode(prevLine, (MappingNode) node, field.getMessageType());
    } else if (node instanceof ScalarNode) {
      return readScalarNode(node, field);
    }

    helper.error(
        node,
        "Expected a map to merge with '%s', found '%s'.",
        field.getFullName(),
        node.getNodeId());
    return null;
  }

  private ConfigNode readMessageNodeEntry(NodeTuple entry, Descriptor messageType) {
    Node keyNode = entry.getKeyNode();
    if (!(keyNode instanceof ScalarNode)) {
      helper.error(
          keyNode,
          "Expected a scalar value for key in '%s', found '%s'.",
          messageType.getFullName(),
          keyNode.getNodeId());
      return null;
    }

    String key = ((ScalarNode) keyNode).getValue();
    FieldDescriptor field = messageType.findFieldByName(key);

    ConfigNode valueConfigNode = null;
    if (field != null) {
      valueConfigNode = readField(keyNode.getEndMark().getLine() + 1, entry.getValueNode(), field);
    } else if (key.equals(TYPE_KEY)) {
      valueConfigNode = readScalarNode(entry.getValueNode(), field);
    } else {
      helper.error(
          keyNode, "Found field '%s' which is unknown in '%s'.", key, messageType.getFullName());
    }

    if (valueConfigNode == null) {
      return null;
    }

    return new FieldConfigNode(helper.getStartLine(keyNode), key).setChild(valueConfigNode);
  }

  private ConfigNode readMapNode(int prevLine, Node node, FieldDescriptor field) {
    if (isEmpty(node)) {
      return new NullConfigNode();
    }

    if (!(node instanceof MappingNode)) {
      helper.error(
          node,
          "Expected a map to merge with '%s', found '%s'.",
          field.getFullName(),
          node.getNodeId());
      return null;
    }

    Descriptor messageType = field.getMessageType();
    ConfigNode configNode = new NullConfigNode();
    ConfigNode prev = new NullConfigNode();
    for (NodeTuple entry : ((MappingNode) node).getValue()) {
      ConfigNode entryNode = readMapNodeEntry(entry, messageType);
      ConfigNode commentNode = readCommentNode(prevLine, entry.getKeyNode(), entryNode);
      prevLine = entry.getValueNode().getEndMark().getLine() + 1;

      if (entryNode == null) {
        continue;
      }

      prev.insertNext(commentNode);
      prev = entryNode;

      if (!configNode.isPresent()) {
        configNode = commentNode;
      }
    }
    return configNode;
  }

  private ConfigNode readMapNodeEntry(NodeTuple entry, Descriptor messageType) {
    Node keyNode = entry.getKeyNode();
    if (!(keyNode instanceof ScalarNode)) {
      helper.error(
          keyNode,
          "Expected a scalar value for key in '%s', found '%s'.",
          messageType.getFullName(),
          keyNode.getNodeId());
      return null;
    }

    String key = ((ScalarNode) keyNode).getValue();
    FieldDescriptor valueField = messageType.findFieldByName("value");
    ConfigNode valueConfigNode =
        readField(keyNode.getEndMark().getLine() + 1, entry.getValueNode(), valueField);
    if (valueConfigNode == null) {
      return null;
    }

    return new FieldConfigNode(helper.getStartLine(keyNode), key).setChild(valueConfigNode);
  }

  private ConfigNode readListNode(int prevLine, Node node, final FieldDescriptor field) {
    if (isEmpty(node)) {
      return new NullConfigNode();
    }

    if (!(node instanceof SequenceNode)) {
      return new ListItemConfigNode(helper.getStartLine(node))
          .setChild(readSingularField(prevLine, node, field));
    }

    ConfigNode configNode = new NullConfigNode();
    ConfigNode prev = new NullConfigNode();
    for (Node elem : ((SequenceNode) node).getValue()) {
      ConfigNode elemNode =
          new ListItemConfigNode(helper.getStartLine(elem))
              .setChild(readSingularField(prevLine, elem, field));
      ConfigNode commentNode = readCommentNode(prevLine, elem, elemNode);
      prevLine = elem.getEndMark().getLine() + 1;

      if (elemNode == null) {
        continue;
      }

      prev.insertNext(commentNode);
      prev = elemNode;

      if (!configNode.isPresent()) {
        configNode = commentNode;
      }
    }
    return configNode;
  }

  private ConfigNode readScalarNode(Node node, FieldDescriptor field) {
    if (!(node instanceof ScalarNode)) {
      helper.error(
          node,
          "Expected a scalar value for field '%s', found '%s'.",
          field.getFullName(),
          node.getNodeId());
      return null;
    }

    return new ScalarConfigNode(helper.getStartLine(node), ((ScalarNode) node).getValue());
  }

  private ConfigNode readField(int prevLine, Node node, FieldDescriptor field) {
    if (field.isMapField()) {
      return readMapNode(prevLine, node, field);
    }

    if (field.isRepeated()) {
      return readListNode(prevLine, node, field);
    }

    return readSingularField(prevLine, node, field);
  }

  private ConfigNode readSingularField(int prevLine, Node node, FieldDescriptor field) {
    if (field.getType() == FieldDescriptor.Type.MESSAGE) {
      return readMessageNode(prevLine, node, field);
    }

    return readScalarNode(node, field);
  }

  private ConfigNode readCommentNode(int prevLine, Node node, ConfigNode configNode) {
    int startLine = node.getStartMark().getLine();
    int endLine = node.getEndMark().getLine();
    List<String> commentLines =
        startLine <= prevLine
            ? new ArrayList<>()
            : new ArrayList<>(lines.subList(prevLine, startLine));
    for (String line : lines.subList(startLine, endLine + 1)) {
      int commentIndex = line.indexOf("#");
      if (commentIndex >= 0) {
        commentLines.add(line.substring(commentIndex));
      }
    }

    if (commentLines.isEmpty()) {
      return configNode;
    }

    String comment = Joiner.on(System.lineSeparator()).join(commentLines);
    return new ScalarConfigNode(startLine + 1, comment).insertNext(configNode);
  }

  private static boolean isEmpty(Node node) {
    return node instanceof ScalarNode && ((ScalarNode) node).getValue().trim().isEmpty();
  }
}
