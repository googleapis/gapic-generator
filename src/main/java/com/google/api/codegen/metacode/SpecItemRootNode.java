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

import com.google.api.tools.framework.model.Field;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpecItemRootNode extends SpecItemNode {

  public SpecItemRootNode(String key, InitCodeLineType nodeType, InitValueConfig initValueConfig) {
    super(key, nodeType, initValueConfig);
  }

  public static SpecItemRootNode createWithChildren(
      String key, InitCodeLineType type, Iterable<SpecItemNode> children) {
    SpecItemRootNode item = new SpecItemRootNode(key, type, InitValueConfig.create());
    for (SpecItemNode childItem : children) {
      item.addChild(childItem);
    }
    return item;
  }

  @Override
  public void createInitCodeLines() {
    super.createInitCodeLines();
  }

  public List<SpecItemNode> listInInitializationOrder() {
    List<SpecItemNode> initCodeLines = new ArrayList<>();
    constructInitializationOrderList(initCodeLines);
    return initCodeLines;
  }

  public static SpecItemRootNode createSpecItemTree(SpecItemParserContext parser) {
    List<SpecItemNode> subTrees = buildSubTrees(parser);
    SpecItemRootNode rootTree =
        createWithChildren("root", InitCodeLineType.StructureInitLine, subTrees);
    rootTree.updateTree(
        parser.table(), parser.valueGenerator(), parser.rootObjectType(), parser.suggestedName());
    return rootTree;
  }

  private static List<SpecItemNode> buildSubTrees(SpecItemParserContext context) {
    List<SpecItemNode> subTrees = new ArrayList<>();
    if (context.sampleCodeInitFields() != null) {
      for (String sampleCodeInitField : context.sampleCodeInitFields()) {
        subTrees.add(FieldStructureParser.parse(sampleCodeInitField, context.initValueConfigMap()));
      }
    }
    if (context.initFieldSet() != null) {
      // Filter subTrees using fieldSet
      Set<String> fieldSet = new HashSet<>();
      for (Field field : context.initFieldSet()) {
        fieldSet.add(field.getSimpleName());
      }
      List<SpecItemNode> filteredSubTrees = new ArrayList<>();
      for (SpecItemNode subTree : subTrees) {
        if (fieldSet.contains(subTree.getKey())) {
          filteredSubTrees.add(subTree);
        }
      }
      subTrees = filteredSubTrees;

      // Add items in fieldSet to subTrees in case they were not included in sampleCodeInitFields
      for (Field field : context.initFieldSet()) {
        String nameString = field.getSimpleName();
        InitValueConfig initValueConfig = context.initValueConfigMap().get(nameString);
        if (initValueConfig == null) {
          subTrees.add(SpecItemNode.create(nameString));
        } else {
          subTrees.add(SpecItemNode.createWithValue(nameString, initValueConfig));
        }
      }
    }
    if (context.subTrees() != null) {
      subTrees.addAll(context.subTrees());
    }
    return subTrees;
  }
}
