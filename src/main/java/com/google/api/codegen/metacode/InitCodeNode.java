/* Copyright 2016 Google LLC
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
package com.google.api.codegen.metacode;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.OneofConfig;
import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.config.SampleParameterConfig;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.Scanner;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Represents a node in an tree of objects to be initialized.
 */
public class InitCodeNode {
  private static final TypeModel INT_TYPE = ProtoTypeRef.create(TypeRef.of(Type.TYPE_UINT64));
  private static final String ROOT_KEY = "root";
  private String key;
  private InitCodeLineType lineType;
  private InitValueConfig initValueConfig;
  private Map<String, InitCodeNode> children;
  private TypeModel typeRef;
  private FieldConfig nodeFieldConfig;
  private Name identifier;
  private OneofConfig oneofConfig;
  private String varName;
  private SampleParameterConfig sampleParamConfig;

  /*
   * Get the key associated with the node. For InitCodeNode objects that are not a root object, they
   * will be stored in their parent node using this key. For elements of a structure, this is the
   * field name. For map elements, it is the map key. For list elements, it is the list index.
   */
  public String getKey() {
    return key;
  }

  /*
   * Get the variable name, which may be configured differently from the key.
   */
  public String getVarName() {
    return varName == null ? key : varName;
  }

  /*
   * Get the InitCodeLineType.
   */
  public InitCodeLineType getLineType() {
    return lineType;
  }

  public void setLineType(InitCodeLineType lineType) {
    this.lineType = lineType;
  }

  /*
   * Get the InitValueConfig. For InitCodeNode objects with 1 or more children, the InitValueConfig
   * object will not contain an initial value or formatting config.
   */
  public InitValueConfig getInitValueConfig() {
    return initValueConfig;
  }

  /*
   * Updates the InitValueConfig.
   */
  public void updateInitValueConfig(InitValueConfig initValueConfig) {
    this.initValueConfig = initValueConfig;
  }

  /*
   * Get a map of this nodes children. Each child is stored with key equal to child.getKey().
   */
  public Map<String, InitCodeNode> getChildren() {
    return children;
  }

  /*
   * Get the TypeModel of the node.
   */
  public TypeModel getType() {
    return typeRef;
  }

  /*
   * Get the FieldConfig of the node. Nodes that are children of Map or List nodes will share the
   * same field config as their parent.
   */
  public FieldConfig getFieldConfig() {
    return nodeFieldConfig;
  }

  /*
   * Get the unique Name that will be used as a variable name in the initialization code.
   */
  public Name getIdentifier() {
    return identifier;
  }

  public OneofConfig getOneofConfig() {
    return oneofConfig;
  }

  public static InitCodeNode create(String key) {
    return new InitCodeNode(key, InitCodeLineType.Unknown, InitValueConfig.create());
  }

  public static InitCodeNode createWithName(String key, String varName) {
    return new InitCodeNode(key, InitCodeLineType.Unknown, InitValueConfig.create(), varName);
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

  /** Creates a node to be used as the root of the initialization tree. */
  public static InitCodeNode newRoot() {
    return new InitCodeNode(ROOT_KEY, InitCodeLineType.StructureInitLine, InitValueConfig.create());
  }

  /**
   * Constructs a tree of objects to be initialized using the provided context, and returns the
   * root.
   */
  public static InitCodeNode createTree(InitCodeContext context) {
    Preconditions.checkArgument(
        context.initFields() != null || context.outputType() != InitCodeOutputType.FieldList,
        "init field array is not set for flattened method");
    InitCodeNode root = newRoot();

    if (context.initFields() != null) {
      for (FieldModel field : context.initFields()) {
        String nameString = field.getNameAsParameter();
        InitValueConfig initValueConfig = context.initValueConfigMap().get(nameString);
        if (initValueConfig == null) {
          root.mergeChild(InitCodeNode.createWithName(nameString, nameString));
        } else {
          root.mergeChild(InitCodeNode.createWithValue(nameString, initValueConfig));
        }
      }
    }

    for (String initFieldConfigString : context.initFieldConfigStrings()) {
      FieldStructureParser.parse(root, initFieldConfigString, context.initValueConfigMap());
    }

    for (InitCodeNode node : context.additionalInitCodeNodes()) {
      root.mergeChild(node);
    }

    root.resolveNamesAndTypes(context, context.initObjectType(), context.suggestedName(), null);
    root.resolveSampleParamConfigs(context, "");
    return root;
  }

  public InitCodeNode subTree(String config) {
    return FieldStructureParser.parsePath(this, new Scanner(config));
  }

  /*
   * Generates a flattened list from a tree in post-order.
   */
  public List<InitCodeNode> listInInitializationOrder() {
    ArrayList<InitCodeNode> initOrder = new ArrayList<>();
    ArrayDeque<InitCodeNode> initStack = new ArrayDeque<>();
    initStack.add(this);

    while (!initStack.isEmpty()) {
      InitCodeNode node = initStack.pollLast();
      initStack.addAll(node.children.values());
      initOrder.add(node);
    }
    return Lists.reverse(initOrder);
  }

  private InitCodeNode(String key, InitCodeLineType nodeType, InitValueConfig initValueConfig) {
    this(key, nodeType, initValueConfig, key);
  }

  private InitCodeNode(
      String key, InitCodeLineType nodeType, InitValueConfig initValueConfig, String varName) {
    this.key = key;
    this.lineType = nodeType;
    this.initValueConfig = initValueConfig;
    this.children = new LinkedHashMap<>();
    this.varName = varName;
  }

  InitCodeNode mergeChild(InitCodeNode newChild) {
    InitCodeNode oldChild = children.get(newChild.key);
    if (oldChild == null || oldChild.lineType == InitCodeLineType.Unknown) {
      children.put(newChild.key, newChild);
      return newChild;
    }
    InitValueConfig mergedValueConfig =
        mergeInitValueConfig(oldChild.getInitValueConfig(), newChild.getInitValueConfig());
    oldChild.updateInitValueConfig(mergedValueConfig);
    for (InitCodeNode newSubChild : newChild.children.values()) {
      oldChild.mergeChild(newSubChild);
    }
    return oldChild;
  }

  static InitValueConfig mergeInitValueConfig(
      InitValueConfig oldConfig, InitValueConfig newConfig) {
    return InitValueConfig.newBuilder()
        .setApiWrapperName(
            merge("api wrapper", oldConfig.getApiWrapperName(), newConfig.getApiWrapperName()))
        .setSingleResourceNameConfig(
            merge(
                "resource name config",
                oldConfig.getSingleResourceNameConfig(),
                newConfig.getSingleResourceNameConfig()))
        .setInitialValue(
            merge("init value", oldConfig.getInitialValue(), newConfig.getInitialValue()))
        .setResourceNameBindingValues(
            ImmutableMap.<String, InitValue>builder()
                .putAll(oldConfig.getResourceNameBindingValues())
                .putAll(newConfig.getResourceNameBindingValues())
                .build())
        .build();
  }

  /**
   * If both {@code a} and {@code b} are non-null, verify that they are equal and return {@code a}.
   * If both are null, return null. Otherwise, return whichever is non-null.
   */
  private static <T> T merge(String desc, T a, T b) {
    if (a != null && b != null) {
      Preconditions.checkArgument(a.equals(b), "inconsistent %s, old: %s, new: %s", desc, a, b);
      return a;
    }
    return a != null ? a : b;
  }

  private void resolveNamesAndTypes(
      InitCodeContext context, TypeModel type, Name suggestedName, FieldConfig fieldConfig) {

    for (InitCodeNode child : children.values()) {
      validateKeyValue(type, child.key);
      child.resolveNamesAndTypes(
          context,
          getChildType(type, child.key),
          getChildSuggestedName(suggestedName, lineType, child),
          getChildFieldConfig(context.fieldConfigMap(), fieldConfig, type, child.key));
      if (type.isMessage()) {
        child.oneofConfig = type.getOneOfConfig(child.getKey());
      }
    }

    SymbolTable table = context.symbolTable();
    TestValueGenerator valueGenerator = context.valueGenerator();

    validateType(lineType, type, children.keySet());
    typeRef = type;
    nodeFieldConfig = fieldConfig;
    identifier = table.getNewSymbol(suggestedName);

    if (children.size() == 0) {
      // Set the lineType of childless nodes to SimpleInitLine
      lineType = InitCodeLineType.SimpleInitLine;

      // Validate initValueConfig, or generate random value
      if (initValueConfig.hasSimpleInitialValue()) {
        validateValue(type, initValueConfig.getInitialValue().getValue());
      } else if (initValueConfig.isEmpty()
          && type.isPrimitive()
          && !type.isRepeated()
          && valueGenerator != null) {
        String newValue = valueGenerator.getAndStoreValue(type, identifier);
        initValueConfig = InitValueConfig.createWithValue(InitValue.createLiteral(newValue));
      }
    }
  }

  /*
   * Validate the lineType against the typeRef that has been set, and against child objects. In the
   * case of no child objects being present, update the lineType to SimpleInitLine.
   */
  private static void validateType(
      InitCodeLineType lineType, TypeModel typeRef, Set<String> childKeys) {
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
        for (int i = 0; i < childKeys.size(); i++) {
          if (!childKeys.contains(Integer.toString(i))) {
            throw new IllegalArgumentException(
                "typeRef " + typeRef + " must have ordered indices, got " + childKeys);
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
        if (childKeys.size() != 0) {
          throw new IllegalArgumentException("node with SimpleInitLine type cannot have children");
        }
        break;
      case Unknown:
        // Any typeRef is acceptable, but we need to check that there are no children.
        if (childKeys.size() != 0) {
          throw new IllegalArgumentException("node with Unknown type cannot have children");
        }
        break;
      default:
        throw new IllegalArgumentException("unexpected InitcodeLineType: " + lineType);
    }
  }

  /**
   * Apply {@code sampleParamConfig} to the nodes in this tree.
   *
   * @param parentFieldPath The full path of the parent object of {@code typeRef}. Set to an empty
   *     string if {@code typeRef} is a top level proto object. We need to keep track of this
   *     because the keys in {@code sampleParamConfigMap} are full paths while {@code key} is a
   *     simple field name.
   */
  private void resolveSampleParamConfigs(InitCodeContext context, String parentFieldPath) {
    ImmutableMap<String, SampleParameterConfig> sampleParamConfigMap =
        context.sampleParamConfigMap();
    String fieldPath;
    if (ROOT_KEY.equals(key)) {
      fieldPath = "";
    } else if (parentFieldPath.isEmpty()) {
      fieldPath = key;
    } else {
      fieldPath = parentFieldPath + "." + key;
    }
    this.sampleParamConfig = sampleParamConfigMap.get(fieldPath);

    if (sampleParamConfig != null) {

      // If read_file is set to true in config, change the line type to ReadFileInitLine.
      if (sampleParamConfig.readFromFile()) {
        setLineType(InitCodeLineType.ReadFileInitLine);
      }

      // If sample_argument_name is specified in config, set identifier to this name if the name has
      // not been used yet and error out otherwise.
      if (sampleParamConfig.isSampleArgument()) {
        Name argName = Name.anyLower(sampleParamConfig.sampleArgumentName());
        if (!argName.equals(identifier)) {
          Preconditions.checkArgument(
              !context.symbolTable().contains(argName),
              "sample_argument_name \"%s\" is already in use.",
              sampleParamConfig.sampleArgumentName());
          identifier =
              context
                  .symbolTable()
                  .getNewSymbol(Name.anyLower(sampleParamConfig.sampleArgumentName()));
        }
      }
    } else {
      children.values().forEach(child -> child.resolveSampleParamConfigs(context, fieldPath));
    }
  }

  private static Name getChildSuggestedName(
      Name parentName, InitCodeLineType parentType, InitCodeNode child) {
    switch (parentType) {
      case StructureInitLine:
        return Name.anyLower(child.key);
      case MapInitLine:
        return parentName.join("item");
      case ListInitLine:
        return parentName.join("element");
      default:
        throw new IllegalArgumentException("Cannot generate child name for " + parentType);
    }
  }

  private static void validateKeyValue(TypeModel parentType, String key) {
    if (parentType.isMap()) {
      TypeModel keyType = parentType.getMapKeyType();
      validateValue(keyType, key);
    } else if (parentType.isRepeated()) {
      validateValue(INT_TYPE, key);
    } else {
      // Don't validate message types, field will be missing for a bad key
    }
  }

  private static TypeModel getChildType(TypeModel parentType, String key) {
    if (parentType.isMap()) {
      return parentType.getMapValueType();
    } else if (parentType.isRepeated()) {
      // Using the Optional cardinality replaces the Repeated cardinality
      return parentType.makeOptional();
    } else if (parentType.isMessage()) {
      FieldModel childField = parentType.getField(key);
      if (childField == null) {
        throw new IllegalArgumentException(
            "Message type " + parentType + " does not have field " + key);
      }
      return childField.getType();
    } else {
      throw new IllegalArgumentException(
          "Primitive type " + parentType + " cannot have children. Child key: " + key);
    }
  }

  private static FieldConfig getChildFieldConfig(
      Map<String, FieldConfig> fieldConfigMap,
      FieldConfig parentFieldConfig,
      TypeModel parentType,
      String key) {
    if (parentType.isMap()) {
      return parentFieldConfig;
    } else if (parentType.isRepeated()) {
      return parentFieldConfig;
    } else if (parentType.isMessage()) {
      FieldModel childField = parentType.getField(key);
      if (childField == null) {
        throw new IllegalArgumentException(
            "Message type " + parentType + " does not have field " + key);
      }
      FieldConfig fieldConfig = fieldConfigMap.get(childField.getFullName());
      if (fieldConfig == null) {
        fieldConfig = FieldConfig.createDefaultFieldConfig(childField);
      }
      return fieldConfig;
    } else {
      throw new IllegalArgumentException(
          "Primitive type " + parentType + " cannot have children. Child key: " + key);
    }
  }

  /**
   * Validates that the provided value matches the provided type. Throws an IllegalArgumentException
   * if the provided type is not supported or doesn't match the value.
   */
  private static void validateValue(TypeModel type, String value) {
    type.validateValue(value);
  }
}
