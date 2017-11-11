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
package com.google.api.codegen.configgen;

import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.ScalarConfigNode;
import com.google.api.tools.framework.util.VisitsBefore;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.List;

/** Generates a Message from a ConfigNode representation. */
public class MessageGenerator extends NodeVisitor {
  private static final String TYPE_KEY = "type";

  private Message.Builder messageBuilder;
  private final FieldDescriptor field;
  private List<Object> values;

  public MessageGenerator(Message.Builder messageBuilder) {
    this(messageBuilder, null);
  }

  private MessageGenerator(Message.Builder messageBuilder, FieldDescriptor field) {
    this.messageBuilder = messageBuilder;
    this.field = field;
  }

  @VisitsBefore
  boolean generate(FieldConfigNode node) {
    String name = node.getText();
    Descriptor messageType = messageBuilder.getDescriptorForType();
    if (field != null && field.isMapField()) {
      FieldDescriptor keyField = messageType.findFieldByName("key");
      if (keyField == null) {
        values = null;
        return false;
      }

      messageBuilder.setField(keyField, name);
      FieldDescriptor valueField = messageType.findFieldByName("value");
      if (valueField == null) {
        values = null;
        return false;
      }

      Message.Builder childBuilder =
          valueField.getType() == FieldDescriptor.Type.MESSAGE
              ? messageBuilder.newBuilderForField(valueField)
              : null;
      Object value = generateSingularValue(node.getChild(), valueField, childBuilder);
      if (value == null) {
        values = null;
        return false;
      }

      messageBuilder.setField(valueField, value);
      values.add(messageBuilder.build());
      messageBuilder.clear();
    } else {
      if (name.equals(TYPE_KEY) && node.getChild().getText().equals(messageType.getFullName())) {
        return true;
      }

      FieldDescriptor childField = messageType.findFieldByName(name);
      if (childField == null) {
        messageBuilder = null;
        return false;
      }

      Message.Builder childBuilder =
          childField.getType() == FieldDescriptor.Type.MESSAGE
              ? messageBuilder.newBuilderForField(childField)
              : null;
      MessageGenerator messageGenerator = new MessageGenerator(childBuilder, childField);
      if (childField.isRepeated() || childField.getType() != FieldDescriptor.Type.MESSAGE) {
        messageGenerator.values = new ArrayList<>();
      }

      messageGenerator.visit(node.getChild());
      Object value = messageGenerator.getValue();
      if (value == null) {
        messageBuilder = null;
        return false;
      }

      if (childField.isRepeated() || childField.getType() == FieldDescriptor.Type.MESSAGE) {
        messageBuilder.setField(childField, value);
      } else {
        List<?> childValues = (List<?>) value;
        if (childValues.size() != 1) {
          messageBuilder = null;
          return false;
        }

        messageBuilder.setField(childField, childValues.get(0));
      }
    }

    return true;
  }

  @VisitsBefore
  boolean generate(ListItemConfigNode node) {
    Object value = generateSingularValue(node.getChild(), field, messageBuilder);
    if (value == null) {
      values = null;
      return false;
    }

    values.add(value);
    if (messageBuilder != null) {
      messageBuilder.clear();
    }

    return true;
  }

  @VisitsBefore
  boolean generate(ScalarConfigNode node) {
    String text = node.getText().trim();
    if (text.isEmpty() || text.startsWith("#")) {
      return true;
    }

    Object value = parseValue(field, text);
    if (value == null) {
      values = null;
      return false;
    }

    values.add(value);
    return true;
  }

  public Object getValue() {
    if (values != null) {
      return values;
    }

    if (messageBuilder != null) {
      return messageBuilder.build();
    }

    return null;
  }

  private Object generateSingularValue(
      ConfigNode configNode, FieldDescriptor field, Message.Builder messageBuilder) {
    MessageGenerator messageGenerator = new MessageGenerator(messageBuilder, field);
    if (field.getType() != FieldDescriptor.Type.MESSAGE) {
      messageGenerator.values = new ArrayList<>();
    }

    messageGenerator.visit(configNode);
    Object value = messageGenerator.getValue();
    if (value instanceof List) {
      List<?> list = (List<?>) value;
      return list.size() == 1 ? list.get(0) : null;
    }

    return value;
  }

  private Object parseValue(FieldDescriptor descriptor, String text) {
    switch (descriptor.getType()) {
      case STRING:
        return text;
      case INT32:
      case SINT32:
      case SFIXED32:
        return parseInt32(text);
      case UINT32:
      case FIXED32:
        return parseUint32(text);
      case INT64:
      case SINT64:
      case SFIXED64:
        return parseInt64(text);
      case UINT64:
      case FIXED64:
        return parseUint64(text);
      case FLOAT:
        return parseFloat32(text);
      case DOUBLE:
        return parseFloat64(text);
      case BOOL:
        return Boolean.parseBoolean(text);
      case ENUM:
        return descriptor.getEnumType().findValueByName(text);
      default:
        return null;
    }
  }

  private Integer parseInt32(String text) {
    try {
      return Integer.parseInt(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Integer parseUint32(String text) {
    Integer value = Integer.parseInt(text);
    return value < 0 ? null : value;
  }

  private Long parseInt64(String text) {
    try {
      return Long.parseLong(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Long parseUint64(String text) {
    Long value = Long.parseLong(text);
    return value < 0 ? null : value;
  }

  private Float parseFloat32(String text) {
    try {
      return Float.parseFloat(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Double parseFloat64(String text) {
    try {
      return Double.parseDouble(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
