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

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.ResourceNameOneofConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.metacode.InitCodeLineType;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.metacode.InitValue;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.util.Name;
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
import com.google.api.codegen.viewmodel.OneofConfigView;
import com.google.api.codegen.viewmodel.ResourceNameInitValueView;
import com.google.api.codegen.viewmodel.ResourceNameOneofInitValueView;
import com.google.api.codegen.viewmodel.SimpleInitCodeLineView;
import com.google.api.codegen.viewmodel.SimpleInitValueView;
import com.google.api.codegen.viewmodel.StructureInitCodeLineView;
import com.google.api.codegen.viewmodel.testing.ClientTestAssertView;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
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

  public InitCodeContext createRequestInitCodeContext(
      MethodTransformerContext context,
      SymbolTable symbolTable,
      Iterable<FieldConfig> fieldConfigs,
      InitCodeOutputType outputType,
      TestValueGenerator valueGenerator) {
    return InitCodeContext.newBuilder()
        .initObjectType(context.getMethod().getInputType())
        .symbolTable(symbolTable)
        .suggestedName(Name.from("request"))
        .initFieldConfigStrings(context.getMethodConfig().getSampleCodeInitFields())
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(context))
        .initFields(FieldConfig.toFieldIterable(fieldConfigs))
        .fieldConfigMap(FieldConfig.toFieldConfigMap(fieldConfigs))
        .outputType(outputType)
        .valueGenerator(valueGenerator)
        .build();
  }

  /** Generates assert views for the test of the tested method and its fields. */
  public List<ClientTestAssertView> generateRequestAssertViews(
      MethodTransformerContext methodContext, InitCodeContext initContext) {
    InitCodeNode rootNode =
        InitCodeNode.createTree(
            InitCodeContext.newBuilder()
                .initObjectType(methodContext.getMethod().getInputType())
                .initFields(initContext.initFields())
                .initValueConfigMap(createCollectionMap(methodContext))
                .suggestedName(Name.from("request"))
                .fieldConfigMap(initContext.fieldConfigMap())
                .build());

    List<ClientTestAssertView> assertViews = new ArrayList<>();
    SurfaceNamer namer = methodContext.getNamer();
    // Add request fields checking
    for (InitCodeNode fieldItemTree : rootNode.getChildren().values()) {
      FieldConfig fieldConfig = fieldItemTree.getFieldConfig();

      String getterMethod =
          namer.getFieldGetFunctionName(methodContext.getFeatureConfig(), fieldConfig);

      String expectedValueIdentifier = getVariableName(methodContext, fieldItemTree);
      String expectedTransformFunction = null;
      if (methodContext.getFeatureConfig().useResourceNameFormatOption(fieldConfig)
          && fieldConfig.hasDifferentMessageResourceNameConfig()) {
        expectedTransformFunction =
            namer.getResourceOneofCreateMethod(methodContext.getTypeTable(), fieldConfig);
      }

      assertViews.add(
          createAssertView(expectedValueIdentifier, expectedTransformFunction, getterMethod));
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
      SingleResourceNameConfig resourceNameConfig =
          context.getSingleResourceNameConfig(fieldNamePattern.getValue());
      String apiWrapperClassName =
          context.getNamer().getApiWrapperClassName(context.getInterface());
      InitValueConfig initValueConfig =
          InitValueConfig.create(apiWrapperClassName, resourceNameConfig);
      mapBuilder.put(fieldNamePattern.getKey(), initValueConfig);
    }
    return mapBuilder.build();
  }

  private ClientTestAssertView createAssertView(
      String expected, String expectedTransformFunction, String actual) {
    return ClientTestAssertView.newBuilder()
        .expectedValueIdentifier(expected)
        .expectedValueTransformFunction(expectedTransformFunction)
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
    StandardImportSectionTransformer importSectionTransformer =
        new StandardImportSectionTransformer();
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
        .importSection(importSectionTransformer.generateImportSection(typeTable.getImports()))
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
    FieldConfig fieldConfig = item.getFieldConfig();

    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType(InitCodeLineType.SimpleInitLine);

    if (context.getFeatureConfig().useResourceNameFormatOption(fieldConfig)) {
      if (!context.isFlattenedMethodContext()) {
        // In a non-flattened context, we always use the resource name type set on the message
        // instead of set on the flattened method
        fieldConfig = fieldConfig.getMessageFieldConfig();
      }
      if (item.getType().isRepeated()) {
        surfaceLine.typeName(namer.getAndSaveResourceTypeName(typeTable, fieldConfig));
      } else {
        surfaceLine.typeName(namer.getAndSaveElementResourceTypeName(typeTable, fieldConfig));
      }
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

    String typeName = typeTable.getAndSaveNicknameFor(item.getType());
    surfaceLine.typeName(typeName);
    surfaceLine.typeConstructor(namer.getTypeConstructor(typeName));

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
      surfaceLine.elementTypeName(namer.getAndSaveElementResourceTypeName(typeTable, fieldConfig));
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
    ModelTypeTable typeTable = context.getTypeTable();
    InitValueConfig initValueConfig = item.getInitValueConfig();
    FieldConfig fieldConfig = item.getFieldConfig();

    if (context.getFeatureConfig().useResourceNameFormatOption(fieldConfig)
        && !item.getType().isRepeated()) {
      // For a repeated type, we want to use a SimpleInitValueView
      if (!context.isFlattenedMethodContext()) {
        // In a non-flattened context, we always use the resource name type set on the message
        // instead of set on the flattened method
        fieldConfig = fieldConfig.getMessageFieldConfig();
      }
      SingleResourceNameConfig singleResourceNameConfig;
      switch (fieldConfig.getResourceNameType()) {
        case ANY:
          // TODO(michaelbausor): handle case where there are no other resource names at all...
          singleResourceNameConfig =
              Iterables.get(context.getApiConfig().getSingleResourceNameConfigs(), 0);
          FieldConfig anyResourceNameFieldConfig =
              fieldConfig.withResourceNameConfig(singleResourceNameConfig);
          return createResourceNameInitValueView(context, anyResourceNameFieldConfig, item).build();
        case FIXED:
          throw new UnsupportedOperationException("entity name invalid");
        case ONEOF:
          ResourceNameOneofConfig oneofConfig =
              (ResourceNameOneofConfig) fieldConfig.getResourceNameConfig();
          singleResourceNameConfig = Iterables.get(oneofConfig.getSingleResourceNameConfigs(), 0);
          FieldConfig singleResourceNameFieldConfig =
              fieldConfig.withResourceNameConfig(singleResourceNameConfig);
          ResourceNameInitValueView initView =
              createResourceNameInitValueView(context, singleResourceNameFieldConfig, item).build();
          return ResourceNameOneofInitValueView.newBuilder()
              .resourceOneofTypeName(
                  namer.getAndSaveElementResourceTypeName(typeTable, fieldConfig))
              .specificResourceNameView(initView)
              .build();
        case SINGLE:
          return createResourceNameInitValueView(context, fieldConfig, item).build();
        case NONE:
        default:
          throw new UnsupportedOperationException("unexpected entity name type");
      }
    } else if (initValueConfig.hasFormattingConfig() && !item.getType().isRepeated()) {
      if (context.getFeatureConfig().enableStringFormatFunctions()) {
        FormattedInitValueView.Builder initValue = FormattedInitValueView.newBuilder();

        initValue.apiWrapperName(context.getNamer().getApiWrapperClassName(context.getInterface()));
        initValue.formatFunctionName(
            context
                .getNamer()
                .getFormatFunctionName(
                    context.getInterface(), initValueConfig.getSingleResourceNameConfig()));

        List<String> varList =
            Lists.newArrayList(
                initValueConfig.getSingleResourceNameConfig().getNameTemplate().vars());
        initValue.formatArgs(getFormatFunctionArgs(context, varList, initValueConfig));

        return initValue.build();
      } else {
        return createResourceNameInitValueView(context, fieldConfig, item)
            .convertToString(true)
            .build();
      }
    } else {
      SimpleInitValueView.Builder initValue = SimpleInitValueView.newBuilder();

      if (initValueConfig.hasSimpleInitialValue()) {
        String value = initValueConfig.getInitialValue().getValue();
        switch (initValueConfig.getInitialValue().getType()) {
          case Literal:
            if (item.getType().isEnum()) {
              value = context.getTypeTable().getEnumValue(item.getType(), value);
            } else {
              value = context.getTypeTable().renderPrimitiveValue(item.getType(), value);
            }
            break;
          case Random:
            value = context.getNamer().injectRandomStringGeneratorCode(value);
            break;
          case Variable:
            value = context.getNamer().localVarName(Name.from(value));
            break;
          default:
            throw new IllegalArgumentException("Unhandled init value type");
        }
        initValue.initialValue(value);
      } else {
        initValue.initialValue(
            context.getTypeTable().getZeroValueAndSaveNicknameFor(item.getType()));
        initValue.isRepeated(item.getType().isRepeated());
      }

      return initValue.build();
    }
  }

  private ResourceNameInitValueView.Builder createResourceNameInitValueView(
      MethodTransformerContext context, FieldConfig fieldConfig, InitCodeNode item) {
    String resourceName =
        context.getNamer().getAndSaveElementResourceTypeName(context.getTypeTable(), fieldConfig);
    SingleResourceNameConfig singleResourceNameConfig =
        (SingleResourceNameConfig) fieldConfig.getResourceNameConfig();
    List<String> varList = Lists.newArrayList(singleResourceNameConfig.getNameTemplate().vars());

    return ResourceNameInitValueView.newBuilder()
        .resourceTypeName(resourceName)
        .formatArgs(getFormatFunctionArgs(context, varList, item.getInitValueConfig()));
  }

  private static List<String> getFormatFunctionArgs(
      MethodTransformerContext context, List<String> varList, InitValueConfig initValueConfig) {
    List<String> formatFunctionArgs = new ArrayList<>();
    for (String entityName : varList) {
      String entityValue = "\"[" + Name.from(entityName).toUpperUnderscore() + "]\"";
      if (initValueConfig.hasFormattingConfigInitialValues()
          && initValueConfig.getResourceNameBindingValues().containsKey(entityName)) {
        InitValue initValue = initValueConfig.getResourceNameBindingValues().get(entityName);
        switch (initValue.getType()) {
          case Variable:
            entityValue = context.getNamer().localVarName(Name.from(initValue.getValue()));
            break;
          case Random:
            entityValue = context.getNamer().injectRandomStringGeneratorCode(initValue.getValue());
            break;
          case Literal:
            entityValue = initValue.getValue();
            break;
          default:
            throw new IllegalArgumentException("Unhandled init value type");
        }
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
        fieldSetting.fieldSetFunction(namer.getResourceNameFieldSetFunctionName(fieldConfig));
      } else {
        fieldSetting.fieldSetFunction(
            namer.getFieldSetFunctionName(item.getType(), Name.from(item.getKey())));
      }
      fieldSetting.fieldAddFunction(
          namer.getFieldAddFunctionName(item.getType(), Name.from(item.getKey())));

      fieldSetting.identifier(getVariableName(context, item));
      fieldSetting.initCodeLine(generateSurfaceInitCodeLine(context, item));
      fieldSetting.fieldName(context.getNamer().publicFieldName(Name.from(item.getKey())));

      fieldSetting.isMap(item.getType().isMap());
      fieldSetting.isArray(!item.getType().isMap() && item.getType().isRepeated());
      fieldSetting.elementTypeName(context.getTypeTable().getFullNameFor(item.getType()));
      if (item.getOneofConfig() != null) {
        fieldSetting.oneofConfig(
            OneofConfigView.newBuilder()
                .groupName(namer.publicFieldName(item.getOneofConfig().groupName()))
                .variantType(namer.getOneofVariantTypeName(item.getOneofConfig()))
                .build());
      }

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
