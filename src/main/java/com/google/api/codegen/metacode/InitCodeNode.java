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
package com.google.api.codegen.metacode;

import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Represents a node in an tree of objects to be initialized.
 */
public class InitCodeNode {

  private String key;
  private InitCodeLineType lineType;
  private InitValueConfig initValueConfig;
  private Map<String, InitCodeNode> children;
  private TypeRef typeRef;
  private Name identifier;
  private InitCodeLine initCodeLine;

  public String getKey() {
    return key;
  }

  public InitCodeLineType getLineType() {
    return lineType;
  }

  public InitValueConfig getInitValueConfig() {
    return initValueConfig;
  }

  public Map<String, InitCodeNode> getChildren() {
    return children;
  }

  public TypeRef getType() {
    return typeRef;
  }

  public Name getIdentifier() {
    return identifier;
  }

  public InitCodeLine getInitCodeLine() {
    return initCodeLine;
  }

  public static InitCodeNode create(String key) {
    return new InitCodeNode(key, InitCodeLineType.Unknown, InitValueConfig.create());
  }

  public static InitCodeNode createWithValue(String key, InitValueConfig initValueConfig) {
    return new InitCodeNode(key, InitCodeLineType.SimpleInitLine, initValueConfig);
  }

  public static InitCodeNode createWithChildren(
      String key, InitCodeLineType type, InitCodeNode... children) {
    return createWithChildren(key, type, Lists.newArrayList(children));
  }

  public static InitCodeNode createWithChildren(
      String key, InitCodeLineType type, Iterable<InitCodeNode> children) {
    InitCodeNode item = new InitCodeNode(key, type, InitValueConfig.create());
    for (InitCodeNode child : children) {
      item.mergeChild(child);
    }
    return item;
  }

  public static InitCodeNode createSingletonList(String key) {
    InitCodeNode child = InitCodeNode.create("0");
    return InitCodeNode.createWithChildren(key, InitCodeLineType.ListInitLine, child);
  }

  /*
   * Constructs a tree of objects to be initialized using the provided context, and returns the root
   */
  public static InitCodeNode createSpecItemTree(SpecItemParserContext context) {
    List<InitCodeNode> subTrees = buildSubTrees(context);
    InitCodeNode root = createWithChildren("root", InitCodeLineType.StructureInitLine, subTrees);
    root.updateTree(
        context.table(),
        context.valueGenerator(),
        context.rootObjectType(),
        context.suggestedName());
    root.createInitCodeLines();
    return root;
  }

  /*
   * Generates a flattened list from a tree in post-order.
   */
  public List<InitCodeNode> listInInitializationOrder() {
    List<InitCodeNode> initCodeLines = new ArrayList<>();
    for (InitCodeNode childItem : children.values()) {
      initCodeLines.addAll(childItem.listInInitializationOrder());
    }
    initCodeLines.add(this);
    return initCodeLines;
  }

  private InitCodeNode(String key, InitCodeLineType nodeType, InitValueConfig initValueConfig) {
    this.key = key;
    this.lineType = nodeType;
    this.initValueConfig = initValueConfig;
    this.children = new LinkedHashMap<>();
  }

  private void mergeChild(InitCodeNode newChild) {
    InitCodeNode oldChild = children.get(newChild.key);
    if (oldChild != null && oldChild.lineType != InitCodeLineType.Unknown) {
      for (InitCodeNode newSubChild : newChild.children.values()) {
        oldChild.mergeChild(newSubChild);
      }
    } else {
      children.put(newChild.key, newChild);
    }
  }

  private static List<InitCodeNode> buildSubTrees(SpecItemParserContext context) {
    List<InitCodeNode> subTrees = new ArrayList<>();
    if (context.sampleCodeInitFields() != null) {
      for (String sampleCodeInitField : context.sampleCodeInitFields()) {
        subTrees.add(FieldStructureParser.parse(sampleCodeInitField, context.initValueConfigMap()));
      }
    }
    if (context.initFieldSet() != null) {
      // Add items in fieldSet to newSubTrees in case they were not included in
      // sampleCodeInitFields, and to ensure the order is determined by initFieldSet
      List<InitCodeNode> newSubTrees = new ArrayList<>();
      for (Field field : context.initFieldSet()) {
        String nameString = field.getSimpleName();
        InitValueConfig initValueConfig = context.initValueConfigMap().get(nameString);
        if (initValueConfig == null) {
          newSubTrees.add(InitCodeNode.create(nameString));
        } else {
          newSubTrees.add(InitCodeNode.createWithValue(nameString, initValueConfig));
        }
      }
      // Filter subTrees using fieldSet
      Set<String> fieldSet = new HashSet<>();
      for (Field field : context.initFieldSet()) {
        fieldSet.add(field.getSimpleName());
      }
      for (InitCodeNode subTree : subTrees) {
        if (fieldSet.contains(subTree.getKey())) {
          newSubTrees.add(subTree);
        }
      }
      subTrees = newSubTrees;
    }
    if (context.subTrees() != null) {
      subTrees.addAll(context.subTrees());
    }
    return subTrees;
  }

  private void updateTree(
      SymbolTable table, TestValueGenerator valueGenerator, TypeRef type, Name suggestedName) {

    // Check and update key values against type, then remove and re-add children with updated keys
    // Call updateTree() recursively
    for (InitCodeNode child : new ArrayList<>(children.values())) {
      children.remove(child.key);
      child.key = getValidatedKeyValue(type, child.key);
      child.updateTree(
          table,
          valueGenerator,
          getChildType(type, child.key),
          getChildSuggestedName(suggestedName, lineType, child));
      mergeChild(child);
    }

    // Update the nodeType, typeRef, identifier and initValueConfig
    typeRef = type;
    getValidateType();
    identifier = table.getNewSymbol(suggestedName);
    if (children.size() == 0) {
      if (initValueConfig.hasInitialValue()) {
        // Update initValueConfig with checked value
        String newValue = getValidatedValue(type, initValueConfig.getInitialValue());
        initValueConfig = InitValueConfig.createWithValue(newValue);
      } else if (initValueConfig.isEmpty()
          && type.isPrimitive()
          && !type.isRepeated()
          && valueGenerator != null) {
        String newValue = valueGenerator.getAndStoreValue(type, identifier);
        initValueConfig = InitValueConfig.createWithValue(newValue);
      }
    }
  }

  /**
   * TODO(michaelbausor): delete this method once DocConfig is no longer used (when python converts
   * to MVVM)
   */
  private void createInitCodeLines() {
    if (children.size() == 0) {
      initCodeLine = SimpleInitCodeLine.create(typeRef, identifier, initValueConfig);
      return;
    }
    for (InitCodeNode childItem : children.values()) {
      childItem.createInitCodeLines();
    }
    switch (lineType) {
      case StructureInitLine:
        List<FieldSetting> fieldSettings = new ArrayList<>();
        for (InitCodeNode childItem : children.values()) {
          FieldSetting fieldSetting =
              FieldSetting.create(
                  childItem.typeRef,
                  childItem.identifier,
                  childItem.initCodeLine.getIdentifier(),
                  childItem.initCodeLine.getInitValueConfig());
          fieldSettings.add(fieldSetting);
        }
        initCodeLine = StructureInitCodeLine.create(typeRef, identifier, fieldSettings);
        break;
      case ListInitLine:
        List<Name> elementIdentifiers = new ArrayList<>();
        for (InitCodeNode childItem : children.values()) {
          elementIdentifiers.add(childItem.initCodeLine.getIdentifier());
        }
        initCodeLine = ListInitCodeLine.create(typeRef, identifier, elementIdentifiers);
        break;
      case MapInitLine:
        Map<String, Name> elementIdentifierMap = new LinkedHashMap<>();
        for (InitCodeNode childItem : children.values()) {
          elementIdentifierMap.put(childItem.key, childItem.initCodeLine.getIdentifier());
        }
        initCodeLine =
            MapInitCodeLine.create(
                typeRef.getMapKeyField().getType(),
                typeRef.getMapValueField().getType(),
                typeRef,
                identifier,
                elementIdentifierMap);
        break;
      default:
        throw new IllegalArgumentException("Unexpected ParsedNodeType: " + lineType);
    }
  }

  /*
   * Validate the lineType against the typeRef that has been set, and against child objects. In the
   * case of no child objects being present, update the lineType to SimpleInitLine.
   */
  private void getValidateType() {
    switch (lineType) {
      case StructureInitLine:
        if (!typeRef.isMessage() || typeRef.isRepeated()) {
          throw new IllegalArgumentException(
              "typeRef " + typeRef + " not compatible with " + lineType);
        }
        break;
      case ListInitLine:
        if (typeRef.isMap() || !typeRef.isRepeated()) {
          throw new IllegalArgumentException(
              "typeRef " + typeRef + " not compatible with " + lineType);
        }
        for (int i = 0; i < children.size(); i++) {
          if (!children.containsKey(Integer.toString(i))) {
            throw new IllegalArgumentException(
                "typeRef " + typeRef + " must have ordered indices, got " + children.keySet());
          }
        }
        break;
      case MapInitLine:
        if (!typeRef.isMap()) {
          throw new IllegalArgumentException(
              "typeRef " + typeRef + " not compatible with " + lineType);
        }
        break;
      case SimpleInitLine:
        if (!typeRef.isPrimitive() && !typeRef.isEnum()) {
          throw new IllegalArgumentException(
              "typeRef " + typeRef + " not compatible with " + lineType);
        }
        // Fall through to Unknown to check for no children.
      case Unknown:
        // Any typeRef is acceptable, but we need to check that there are no children.
        if (children.size() != 0) {
          throw new IllegalArgumentException("node with Unknown type cannot have children");
        }
        break;
      default:
        throw new IllegalArgumentException("unexpected InitcodeLineType: " + lineType);
    }
    // Update the type of childless nodes to SimpleInitLine
    if (children.size() == 0) {
      lineType = InitCodeLineType.SimpleInitLine;
    }
  }

  private static Name getChildSuggestedName(
      Name parentName, InitCodeLineType parentType, InitCodeNode child) {
    switch (parentType) {
      case StructureInitLine:
        return Name.from(child.key);
      case MapInitLine:
        return parentName.join("item");
      case ListInitLine:
        return parentName.join("element");
      default:
        throw new IllegalArgumentException("Cannot generate child name for " + parentType);
    }
  }

  private static String getValidatedKeyValue(TypeRef parentType, String key) {
    if (parentType.isMap()) {
      TypeRef keyType = parentType.getMapKeyField().getType();
      return getValidatedValue(keyType, key);
    } else if (parentType.isRepeated()) {
      TypeRef keyType = TypeRef.of(Type.TYPE_UINT64);
      return getValidatedValue(keyType, key);
    } else {
      // Don't validate message types, field will be missing for a bad key
      return key;
    }
  }

  private static TypeRef getChildType(TypeRef parentType, String key) {
    if (parentType.isMap()) {
      return parentType.getMapValueField().getType();
    } else if (parentType.isRepeated()) {
      // Using the Optional cardinality replaces the Repeated cardinality
      return parentType.makeOptional();
    } else if (parentType.isMessage()) {
      for (Field field : parentType.getMessageType().getFields()) {
        if (field.getSimpleName().equals(key)) {
          return field.getType();
        }
      }
      throw new IllegalArgumentException(
          "Message type " + parentType + " does not have field " + key);
    } else {
      throw new IllegalArgumentException(
          "Primitive type " + parentType + " cannot have children. Child key: " + key);
    }
  }

  /**
   * Validates that the provided value matches the provided type, and returns the validated value.
   * For string and byte types, the returned value has quote characters removed. Throws an
   * IllegalArgumentException is the provided type is not supported or doesn't match the value.
   */
  private static String getValidatedValue(TypeRef type, String value) {
    Type descType = type.getKind();
    switch (descType) {
      case TYPE_BOOL:
        String lowerCaseValue = value.toLowerCase();
        if (lowerCaseValue.equals("true") || lowerCaseValue.equals("false")) {
          return lowerCaseValue;
        }
        break;
      case TYPE_DOUBLE:
      case TYPE_FLOAT:
        if (Pattern.matches("[+-]?([0-9]*[.])?[0-9]+", value)) {
          return value;
        }
        break;
      case TYPE_INT64:
      case TYPE_UINT64:
      case TYPE_SINT64:
      case TYPE_FIXED64:
      case TYPE_SFIXED64:
      case TYPE_INT32:
      case TYPE_UINT32:
      case TYPE_SINT32:
      case TYPE_FIXED32:
      case TYPE_SFIXED32:
        if (Pattern.matches("[+-]?[0-9]+", value)) {
          return value;
        }
        break;
      case TYPE_STRING:
      case TYPE_BYTES:
        Matcher matcher = Pattern.compile("\"([^\\\"]*)\"").matcher(value);
        if (matcher.matches()) {
          return matcher.group(1);
        }
        break;
      default:
        // Throw an exception if a value is unsupported for the given type.
        throw new IllegalArgumentException(
            "Tried to assign value for unsupported type " + type + "; value " + value);
    }
    throw new IllegalArgumentException("Could not assign value '" + value + "' to type " + type);
  }
}
