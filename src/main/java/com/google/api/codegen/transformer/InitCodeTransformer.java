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

import com.google.api.codegen.BundlingConfig;
import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.PageStreamingConfig;
import com.google.api.codegen.SmokeTestConfig;
import com.google.api.codegen.metacode.InitCodeLineType;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.metacode.InitTreeParserContext;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.ResourceNameUtil;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.TestValueGenerator;
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
import com.google.api.tools.framework.model.Field;
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

  public InitCodeView generateInitCode(MethodTransformerContext context, Iterable<Field> fields) {
    InitCodeNode rootNode =
        InitCodeNode.createTree(
            InitTreeParserContext.newBuilder()
                .table(new SymbolTable())
                .rootObjectType(context.getMethod().getInputType())
                .initValueConfigMap(createCollectionMap(context))
                .initFieldConfigStrings(context.getMethodConfig().getSampleCodeInitFields())
                .initFields(fields)
                .suggestedName(Name.from("request"))
                .build());
    return buildInitCodeViewFlattened(context, rootNode);
  }

  public InitCodeView generateRequestObjectInitCode(MethodTransformerContext context) {
    InitCodeNode rootNode =
        InitCodeNode.createTree(
            InitTreeParserContext.newBuilder()
                .table(new SymbolTable())
                .rootObjectType(context.getMethod().getInputType())
                .initValueConfigMap(createCollectionMap(context))
                .initFieldConfigStrings(context.getMethodConfig().getSampleCodeInitFields())
                .suggestedName(Name.from("request"))
                .build());
    return buildInitCodeViewRequestObject(context, rootNode);
  }

  public InitCodeView generateTestMethodInitCode(
      MethodTransformerContext context,
      Iterable<Field> fields,
      SymbolTable table,
      TestValueGenerator valueGenerator) {
    InitCodeNode rootNode =
        InitCodeNode.createTree(
            InitTreeParserContext.newBuilder()
                .table(table)
                .valueGenerator(valueGenerator)
                .rootObjectType(context.getMethod().getInputType())
                .initValueConfigMap(createCollectionMap(context))
                .initFieldConfigStrings(context.getMethodConfig().getSampleCodeInitFields())
                .initFields(fields)
                .suggestedName(Name.from("request"))
                .build());
    return buildInitCodeViewFlattened(context, rootNode);
  }

  public InitCodeView generateMockResponseObjectInitCode(
      MethodTransformerContext context, SymbolTable table, TestValueGenerator valueGenerator) {
    ArrayList<Field> primitiveFields = new ArrayList<>();
    for (Field field : context.getMethod().getOutputMessage().getFields()) {
      if (field.getType().isPrimitive() && !field.getType().isRepeated()) {
        primitiveFields.add(field);
      }
    }
    InitCodeNode rootNode =
        InitCodeNode.createTree(
            InitTreeParserContext.newBuilder()
                .table(table)
                .valueGenerator(valueGenerator)
                .rootObjectType(context.getMethod().getOutputType())
                .initValueConfigMap(createCollectionMap(context))
                .additionalSubTrees(createMockResponseAdditionalSubTrees(context))
                .initFields(primitiveFields)
                .suggestedName(Name.from("expected_response"))
                .build());
    return buildInitCodeViewRequestObject(context, rootNode);
  }

  public InitCodeView generateSmokeTestInitCode(
      MethodTransformerContext context, SymbolTable table) {
    SmokeTestConfig testConfig = context.getInterfaceConfig().getSmokeTestConfig();
    InitCodeNode rootNode =
        InitCodeNode.createTree(
            InitTreeParserContext.newBuilder()
                .table(table)
                .rootObjectType(testConfig.getMethod().getInputType())
                .initValueConfigMap(createCollectionMap(context))
                .initFieldConfigStrings(testConfig.getInitFieldConfigStrings())
                .suggestedName(Name.from("request"))
                .build());
    return buildInitCodeViewFlattened(context, rootNode);
  }

  public List<GapicSurfaceTestAssertView> generateRequestAssertViews(
      MethodTransformerContext context, Iterable<Field> fields) {

    InitCodeNode rootNode =
        InitCodeNode.createTree(
            InitTreeParserContext.newBuilder()
                .table(new SymbolTable())
                .rootObjectType(context.getMethod().getInputType())
                .initValueConfigMap(createCollectionMap(context))
                .initFields(fields)
                .suggestedName(Name.from("request"))
                .build());

    List<GapicSurfaceTestAssertView> assertViews = new ArrayList<>();
    SurfaceNamer namer = context.getNamer();
    // Add request fields checking
    for (InitCodeNode fieldItemTree : rootNode.getChildren().values()) {

      String getterMethod;
      if (context.getFeatureConfig().useResourceNameFormatOption(fieldItemTree.getField())) {
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
    ImportTypeTransformer importTypeTransformer = new ImportTypeTransformer();
    ModelTypeTable typeTable = context.getTypeTable();
    SurfaceNamer namer = context.getNamer();

    // Initialize the type table with the apiClassName since each sample will be using the
    // apiClass.
    typeTable.getAndSaveNicknameFor(
        namer.getFullyQualifiedApiWrapperClassName(
            context.getInterface(), context.getApiConfig().getPackageName()));

    return InitCodeView.newBuilder()
        .lines(generateSurfaceInitCodeLines(context, orderedItems))
        .fieldSettings(getFieldSettings(context, argItems))
        .imports(importTypeTransformer.generateImports(typeTable.getImports()))
        .apiFileName(
            namer.getServiceFileName(
                context.getInterface(), context.getApiConfig().getPackageName()))
        .build();
  }

  private List<InitCodeNode> createMockResponseAdditionalSubTrees(
      MethodTransformerContext context) {
    List<InitCodeNode> additionalSubTrees = new ArrayList<>();
    if (context.getMethodConfig().isPageStreaming()) {
      // Initialize one resource element if it is page-streaming.
      PageStreamingConfig config = context.getMethodConfig().getPageStreaming();
      String resourceFieldName = config.getResourcesField().getSimpleName();
      additionalSubTrees.add(InitCodeNode.createSingletonList(resourceFieldName));

      // Set the initial value of the page token to empty, in order to indicate that no more pages
      // are available
      String responseTokenName = config.getResponseTokenField().getSimpleName();
      additionalSubTrees.add(
          InitCodeNode.createWithValue(responseTokenName, InitValueConfig.createWithValue("")));
    }
    if (context.getMethodConfig().isBundling()) {
      // Initialize one bundling element if it is bundling.
      BundlingConfig config = context.getMethodConfig().getBundling();
      String subResponseFieldName = config.getSubresponseField().getSimpleName();
      additionalSubTrees.add(InitCodeNode.createSingletonList(subResponseFieldName));
    }
    return additionalSubTrees;
  }

  private ImmutableMap<String, InitValueConfig> createCollectionMap(
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

  private List<InitCodeLineView> generateSurfaceInitCodeLines(
      MethodTransformerContext context, Iterable<InitCodeNode> SpecItemNodes) {
    List<InitCodeLineView> surfaceLines = new ArrayList<>();
    for (InitCodeNode item : SpecItemNodes) {
      switch (item.getLineType()) {
        case StructureInitLine:
          surfaceLines.add(generateStructureInitCodeLine(context, item));
          continue;
        case ListInitLine:
          surfaceLines.add(generateListInitCodeLine(context, item));
          continue;
        case SimpleInitLine:
          surfaceLines.add(generateSimpleInitCodeLine(context, item));
          continue;
        case MapInitLine:
          surfaceLines.add(generateMapInitCodeLine(context, item));
          continue;
        default:
          throw new RuntimeException("unhandled line type: " + item.getLineType());
      }
    }
    return surfaceLines;
  }

  private InitCodeLineView generateSimpleInitCodeLine(
      MethodTransformerContext context, InitCodeNode item) {
    SimpleInitCodeLineView.Builder surfaceLine = SimpleInitCodeLineView.newBuilder();

    ModelTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType(InitCodeLineType.SimpleInitLine);

    if (context.getFeatureConfig().useResourceNameFormatOption(item.getField())) {
      Field field = item.getField();
      String resourceName = ResourceNameUtil.getResourceName(field);
      surfaceLine.typeName(typeTable.getAndSaveNicknameForTypedResourceName(field, resourceName));
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
    surfaceLine.identifier(namer.varName(item.getIdentifier()));
    surfaceLine.typeName(typeTable.getAndSaveNicknameFor(item.getType()));

    surfaceLine.fieldSettings(getFieldSettings(context, item.getChildren().values()));

    return surfaceLine.build();
  }

  private InitCodeLineView generateListInitCodeLine(
      MethodTransformerContext context, InitCodeNode item) {
    ListInitCodeLineView.Builder surfaceLine = ListInitCodeLineView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType(InitCodeLineType.ListInitLine);
    surfaceLine.identifier(namer.varName(item.getIdentifier()));

    if (context.getFeatureConfig().useResourceNameFormatOption(item.getField())) {
      Field field = item.getField();
      String resourceName = ResourceNameUtil.getResourceName(field);
      surfaceLine.elementTypeName(
          typeTable.getAndSaveNicknameForTypedResourceName(field, resourceName));
    } else {
      surfaceLine.elementTypeName(
          typeTable.getAndSaveNicknameForElementType(item.getType().makeOptional()));
    }

    List<String> entries = new ArrayList<>();
    for (InitCodeNode child : item.getChildren().values()) {
      entries.add(namer.varName(child.getIdentifier()));
    }
    surfaceLine.elementIdentifiers(entries);

    return surfaceLine.build();
  }

  private InitCodeLineView generateMapInitCodeLine(
      MethodTransformerContext context, InitCodeNode item) {
    MapInitCodeLineView.Builder surfaceLine = MapInitCodeLineView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType(InitCodeLineType.MapInitLine);
    surfaceLine.identifier(namer.varName(item.getIdentifier()));

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
      mapEntry.value(context.getNamer().varName(entry.getValue().getIdentifier()));
      entries.add(mapEntry.build());
    }
    surfaceLine.initEntries(entries);

    return surfaceLine.build();
  }

  private InitValueView getInitValue(MethodTransformerContext context, InitCodeNode item) {

    InitValueConfig initValueConfig = item.getInitValueConfig();
    Field field = item.getField();

    if (context.getFeatureConfig().useResourceNameFormatOption(item.getField())) {
      ResourceNameInitValueView.Builder initValue = ResourceNameInitValueView.newBuilder();

      initValue.resourceTypeName(ResourceNameUtil.getResourceName(field));

      List<String> varList =
          Lists.newArrayList(ResourceNameUtil.getResourceNamePathTemplate(field).vars());
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
      String entityValue =
          "\"[" + LanguageUtil.lowerUnderscoreToUpperUnderscore(entityName) + "]\"";
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

      if (context.getFeatureConfig().useResourceNameFormatOption(item.getField())) {
        fieldSetting.fieldSetFunction(
            namer.getResourceNameFieldSetFunctionName(item.getType(), Name.from(item.getKey())));
      } else {
        fieldSetting.fieldSetFunction(
            namer.getFieldSetFunctionName(item.getType(), Name.from(item.getKey())));
      }

      fieldSetting.identifier(getVariableName(context, item));

      allSettings.add(fieldSetting.build());
    }
    return allSettings;
  }

  private static String getVariableName(MethodTransformerContext context, InitCodeNode item) {
    if (!context.getFeatureConfig().useResourceNameFormatOption(item.getField())
        && item.getInitValueConfig().hasFormattingConfig()) {
      return context.getNamer().getFormattedVariableName(item.getIdentifier());
    }
    return context.getNamer().varName(item.getIdentifier());
  }
}
