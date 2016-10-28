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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.CollectionConfig;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.metacode.InitCodeLineType;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.FieldSettingView;
import com.google.api.codegen.viewmodel.FormattedInitValueView;
import com.google.api.codegen.viewmodel.InitCodeLineView;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.InitValueView;
import com.google.api.codegen.viewmodel.ListInitCodeLineView;
import com.google.api.codegen.viewmodel.MapEntryView;
import com.google.api.codegen.viewmodel.MapInitCodeLineView;
import com.google.api.codegen.viewmodel.ResourceNameInitValueView;
import com.google.api.codegen.viewmodel.SimpleInitCodeLineView;
import com.google.api.codegen.viewmodel.SimpleInitValueView;
import com.google.api.codegen.viewmodel.StructureInitCodeLineView;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestAssertView;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * InitCodeTransformer generates initialization code for a given method and then transforms it to a
 * view object which can be rendered by a template engine.
 */
public class InitCodeTransformer {
  /**
   * Generates initialization code from the given MethodTransformerContext and InitCodeContext
   * objects.
   */
  public InitCodeView generateInitCode(
      MethodTransformerContext methodContext, InitCodeContext initCodeContext) {
    InitCodeNode rootNode = InitCodeNode.createTree(initCodeContext);
    if (initCodeContext.outputType() == InitCodeOutputType.FieldList) {
      return buildInitCodeViewFlattened(methodContext, rootNode);
    } else {
      return buildInitCodeViewRequestObject(methodContext, rootNode);
    }
  }

  /** Generates assert views for the test of the tested method and its fields. */
  public List<GapicSurfaceTestAssertView> generateRequestAssertViews(
      MethodTransformerContext context, Iterable<FieldConfig> fieldConfigs) {

    ImmutableMap<String, FieldConfig> fieldConfigMap = FieldConfig.toFieldConfigMap(fieldConfigs);

    InitCodeNode rootNode =
        InitCodeNode.createTree(
            InitCodeContext.newBuilder()
                .initObjectType(context.getMethod().getInputType())
                .initFields(FieldConfig.toFieldIterable(fieldConfigs))
                .initValueConfigMap(createCollectionMap(context))
                .suggestedName(Name.from("request"))
                .fieldConfigMap(fieldConfigMap)
                .build());

    List<GapicSurfaceTestAssertView> assertViews = new ArrayList<>();
    SurfaceNamer namer = context.getNamer();
    // Add request fields checking
    for (InitCodeNode fieldItemTree : rootNode.getChildren().values()) {

      String getterMethod;
      if (context.getFeatureConfig().useResourceNameFormatOption(fieldItemTree.getFieldConfig())) {
        getterMethod =
            namer.getResourceNameFieldGetFunctionName(
                fieldItemTree.getType(), Name.from(fieldItemTree.getKey()));
      } else {
        getterMethod =
            namer.getFieldGetFunctionName(
                fieldItemTree.getType(), Name.from(fieldItemTree.getKey()));
      }

      String expectedValueIdentifier = getVariableName(context, fieldItemTree);

      assertViews.add(createAssertView(expectedValueIdentifier, getterMethod));
    }
    return assertViews;
  }

  /**
   * A utility method which creates the InitValueConfig map that contains the collection config
   * data.
   */
  public static ImmutableMap<String, InitValueConfig> createCollectionMap(
      MethodTransformerContext context) {
    ImmutableMap.Builder<String, InitValueConfig> mapBuilder = ImmutableMap.builder();
    Map<String, String> fieldNamePatterns = context.getMethodConfig().getFieldNamePatterns();
    for (Map.Entry<String, String> fieldNamePattern : fieldNamePatterns.entrySet()) {
      CollectionConfig collectionConfig = context.getCollectionConfig(fieldNamePattern.getValue());
      String apiWrapperClassName =
          context.getNamer().getApiWrapperClassName(context.getInterface());
      InitValueConfig initValueConfig =
          InitValueConfig.create(apiWrapperClassName, collectionConfig);
      mapBuilder.put(fieldNamePattern.getKey(), initValueConfig);
    }
    return mapBuilder.build();
  }

  private GapicSurfaceTestAssertView createAssertView(String expected, String actual) {
    return GapicSurfaceTestAssertView.newBuilder()
        .expectedValueIdentifier(expected)
        .actualValueGetter(actual)
        .build();
  }

  private InitCodeView buildInitCodeViewFlattened(
      MethodTransformerContext context, InitCodeNode root) {
    List<InitCodeNode> orderedItems = root.listInInitializationOrder();
    List<InitCodeNode> argItems = new ArrayList<>(root.getChildren().values());
    //Remove the request object for flattened method
    orderedItems.remove(orderedItems.size() - 1);
    return buildInitCodeView(context, orderedItems, argItems);
  }

  private InitCodeView buildInitCodeViewRequestObject(
      MethodTransformerContext context, InitCodeNode root) {
    List<InitCodeNode> orderedItems = root.listInInitializationOrder();
    List<InitCodeNode> argItems = Lists.newArrayList(root);
    return buildInitCodeView(context, orderedItems, argItems);
  }

  private InitCodeView buildInitCodeView(
      MethodTransformerContext context,
      Iterable<InitCodeNode> orderedItems,
      Iterable<InitCodeNode> argItems) {
    StandardImportTypeTransformer importTypeTransformer = new StandardImportTypeTransformer();
    ModelTypeTable typeTable = context.getTypeTable();
    SurfaceNamer namer = context.getNamer();

    // Initialize the type table with the apiClassName since each sample will be using the
    // apiClass.
    typeTable.getAndSaveNicknameFor(
        namer.getFullyQualifiedApiWrapperClassName(context.getInterface()));

    return InitCodeView.newBuilder()
        .lines(generateSurfaceInitCodeLines(context, orderedItems))
        .topLevelLines(generateSurfaceInitCodeLines(context, argItems))
        .fieldSettings(getFieldSettings(context, argItems))
        .imports(importTypeTransformer.generateImports(typeTable.getImports()))
        .apiFileName(namer.getServiceFileName(context.getInterface()))
        .build();
  }

  private List<InitCodeLineView> generateSurfaceInitCodeLines(
      MethodTransformerContext context, Iterable<InitCodeNode> specItemNode) {
    List<InitCodeLineView> surfaceLines = new ArrayList<>();
    for (InitCodeNode item : specItemNode) {
      surfaceLines.add(generateSurfaceInitCodeLine(context, item));
    }
    return surfaceLines;
  }

  private InitCodeLineView generateSurfaceInitCodeLine(
      MethodTransformerContext context, InitCodeNode specItemNode) {
    switch (specItemNode.getLineType()) {
      case StructureInitLine:
        return generateStructureInitCodeLine(context, specItemNode);
      case ListInitLine:
        return generateListInitCodeLine(context, specItemNode);
      case SimpleInitLine:
        return generateSimpleInitCodeLine(context, specItemNode);
      case MapInitLine:
        return generateMapInitCodeLine(context, specItemNode);
      default:
        throw new RuntimeException("unhandled line type: " + specItemNode.getLineType());
    }
  }

  private InitCodeLineView generateSimpleInitCodeLine(
      MethodTransformerContext context, InitCodeNode item) {
    SimpleInitCodeLineView.Builder surfaceLine = SimpleInitCodeLineView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType(InitCodeLineType.SimpleInitLine);

    if (context.getFeatureConfig().useResourceNameFormatOption(item.getFieldConfig())) {
      surfaceLine.typeName(
          namer.getAndSaveResourceTypeName(
              typeTable,
              item.getFieldConfig().getField(),
              item.getType(),
              item.getFieldConfig().getEntityName()));
    } else {
      surfaceLine.typeName(typeTable.getAndSaveNicknameFor(item.getType()));
    }

    surfaceLine.identifier(getVariableName(context, item));
    surfaceLine.initValue(getInitValue(context, item));

    return surfaceLine.build();
  }

  private InitCodeLineView generateStructureInitCodeLine(
      MethodTransformerContext context, InitCodeNode item) {
    StructureInitCodeLineView.Builder surfaceLine = StructureInitCodeLineView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType(InitCodeLineType.StructureInitLine);
    surfaceLine.identifier(namer.localVarName(item.getIdentifier()));
    surfaceLine.typeName(typeTable.getAndSaveNicknameFor(item.getType()));

    surfaceLine.fieldSettings(getFieldSettings(context, item.getChildren().values()));

    return surfaceLine.build();
  }

  private InitCodeLineView generateListInitCodeLine(
      MethodTransformerContext context, InitCodeNode item) {
    ListInitCodeLineView.Builder surfaceLine = ListInitCodeLineView.newBuilder();
    FieldConfig fieldConfig = item.getFieldConfig();

    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType(InitCodeLineType.ListInitLine);
    surfaceLine.identifier(namer.localVarName(item.getIdentifier()));

    if (context.getFeatureConfig().useResourceNameFormatOption(fieldConfig)) {
      surfaceLine.elementTypeName(
          namer.getAndSaveResourceTypeName(
              typeTable,
              item.getFieldConfig().getField(),
              item.getType().makeOptional(),
              item.getFieldConfig().getEntityName()));
    } else {
      surfaceLine.elementTypeName(
          typeTable.getAndSaveNicknameForElementType(item.getType().makeOptional()));
    }

    List<String> entries = new ArrayList<>();
    List<InitCodeLineView> elements = new ArrayList<>();
    for (InitCodeNode child : item.getChildren().values()) {
      entries.add(namer.localVarName(child.getIdentifier()));
      elements.add(generateSurfaceInitCodeLine(context, child));
    }
    surfaceLine.elementIdentifiers(entries);
    surfaceLine.elements(elements);

    return surfaceLine.build();
  }

  private InitCodeLineView generateMapInitCodeLine(
      MethodTransformerContext context, InitCodeNode item) {
    MapInitCodeLineView.Builder surfaceLine = MapInitCodeLineView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType(InitCodeLineType.MapInitLine);
    surfaceLine.identifier(namer.localVarName(item.getIdentifier()));

    surfaceLine.keyTypeName(
        typeTable.getAndSaveNicknameFor(item.getType().getMapKeyField().getType()));
    surfaceLine.valueTypeName(
        typeTable.getAndSaveNicknameFor(item.getType().getMapValueField().getType()));

    List<MapEntryView> entries = new ArrayList<>();
    for (Map.Entry<String, InitCodeNode> entry : item.getChildren().entrySet()) {
      MapEntryView.Builder mapEntry = MapEntryView.newBuilder();
      mapEntry.key(
          typeTable.renderPrimitiveValue(
              item.getType().getMapKeyField().getType(), entry.getKey()));
      mapEntry.valueString(context.getNamer().localVarName(entry.getValue().getIdentifier()));
      mapEntry.value(generateSurfaceInitCodeLine(context, entry.getValue()));
      entries.add(mapEntry.build());
    }
    surfaceLine.initEntries(entries);

    return surfaceLine.build();
  }

  private InitValueView getInitValue(MethodTransformerContext context, InitCodeNode item) {

    SurfaceNamer namer = context.getNamer();
    InitValueConfig initValueConfig = item.getInitValueConfig();
    FieldConfig fieldConfig = item.getFieldConfig();

    if (context.getFeatureConfig().useResourceNameFormatOption(fieldConfig)
        && !item.getType().isRepeated()) {
      // For a repeated type, we want to use a SimpleInitValueView

      ResourceNameInitValueView.Builder initValue = ResourceNameInitValueView.newBuilder();

      String entityName = fieldConfig.getEntityName();
      Name resourceName = namer.getResourceTypeName(entityName);
      initValue.resourceTypeName(namer.className(resourceName));

      List<String> varList =
          Lists.newArrayList(context.getCollectionConfig(entityName).getNameTemplate().vars());
      initValue.formatArgs(getFormatFunctionArgs(varList, initValueConfig));

      return initValue.build();
    } else if (initValueConfig.hasFormattingConfig()) {
      FormattedInitValueView.Builder initValue = FormattedInitValueView.newBuilder();

      initValue.apiWrapperName(context.getNamer().getApiWrapperClassName(context.getInterface()));
      initValue.formatFunctionName(
          context.getNamer().getFormatFunctionName(initValueConfig.getCollectionConfig()));

      List<String> varList =
          Lists.newArrayList(initValueConfig.getCollectionConfig().getNameTemplate().vars());
      initValue.formatArgs(getFormatFunctionArgs(varList, initValueConfig));

      return initValue.build();
    } else {
      SimpleInitValueView.Builder initValue = SimpleInitValueView.newBuilder();

      if (initValueConfig.hasSimpleInitialValue()) {
        initValue.initialValue(
            context
                .getTypeTable()
                .renderPrimitiveValue(item.getType(), initValueConfig.getInitialValue()));
      } else {
        initValue.initialValue(
            context.getTypeTable().getZeroValueAndSaveNicknameFor(item.getType()));
      }

      return initValue.build();
    }
  }

  private static List<String> getFormatFunctionArgs(
      List<String> varList, InitValueConfig initValueConfig) {
    List<String> formatFunctionArgs = new ArrayList<>();
    for (String entityName : varList) {
      String entityValue = "\"[" + Name.from(entityName).toUpperUnderscore() + "]\"";
      if (initValueConfig.hasFormattingConfigInitialValues()
          && initValueConfig.getCollectionValues().containsKey(entityName)) {
        entityValue = initValueConfig.getCollectionValues().get(entityName);
      }
      formatFunctionArgs.add(entityValue);
    }
    return formatFunctionArgs;
  }

  private List<FieldSettingView> getFieldSettings(
      MethodTransformerContext context, Iterable<InitCodeNode> childItems) {
    SurfaceNamer namer = context.getNamer();
    List<FieldSettingView> allSettings = new ArrayList<>();
    for (InitCodeNode item : childItems) {
      FieldSettingView.Builder fieldSetting = FieldSettingView.newBuilder();
      FieldConfig fieldConfig = item.getFieldConfig();

      if (context.getFeatureConfig().useResourceNameFormatOption(fieldConfig)) {
        fieldSetting.fieldSetFunction(
            namer.getResourceNameFieldSetFunctionName(item.getType(), Name.from(item.getKey())));
      } else {
        fieldSetting.fieldSetFunction(
            namer.getFieldSetFunctionName(item.getType(), Name.from(item.getKey())));
      }

      fieldSetting.identifier(getVariableName(context, item));
      fieldSetting.initCodeLine(generateSurfaceInitCodeLine(context, item));

      fieldSetting.requestFieldName(context.getNamer().localVarName(item.getIdentifier()));

      allSettings.add(fieldSetting.build());
    }
    return allSettings;
  }

  private static String getVariableName(MethodTransformerContext context, InitCodeNode item) {
    if (!context.getFeatureConfig().useResourceNameFormatOption(item.getFieldConfig())
        && item.getInitValueConfig().hasFormattingConfig()) {
      return context.getNamer().getFormattedVariableName(item.getIdentifier());
    }
    return context.getNamer().localVarName(item.getIdentifier());
  }
}
