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

import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.PageStreamingConfig;
import com.google.api.codegen.SmokeTestConfig;
import com.google.api.codegen.metacode.FieldSetting;
import com.google.api.codegen.metacode.FieldStructureParser;
import com.google.api.codegen.metacode.InitCode;
import com.google.api.codegen.metacode.InitCodeGenerator;
import com.google.api.codegen.metacode.InitCodeGeneratorContext;
import com.google.api.codegen.metacode.InitCodeLine;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.metacode.ListInitCodeLine;
import com.google.api.codegen.metacode.MapInitCodeLine;
import com.google.api.codegen.metacode.SimpleInitCodeLine;
import com.google.api.codegen.metacode.StructureInitCodeLine;
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
import com.google.api.codegen.viewmodel.SimpleInitCodeLineView;
import com.google.api.codegen.viewmodel.SimpleInitValueView;
import com.google.api.codegen.viewmodel.StructureInitCodeLineView;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestAssertView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
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

  public InitCodeView generateInitCode(
      MethodTransformerContext context,
      Iterable<Field> fields,
      SymbolTable table,
      TestValueGenerator valueGenerator) {
    return buildInitCodeView(
        context,
        generateInitCodeHelper(
            Lists.newArrayList(fields),
            table,
            valueGenerator,
            createSampleInitFieldStructure(context),
            Name.from("request"),
            context.getMethod().getInputType()));
  }

  public InitCodeView generateRequestObjectInitCode(
      MethodTransformerContext context, SymbolTable table, TestValueGenerator valueGenerator) {
    return buildInitCodeView(
        context,
        generateInitCodeHelper(
            null,
            table,
            valueGenerator,
            createSampleInitFieldStructure(context),
            Name.from("request"),
            context.getMethod().getInputType()));
  }

  public InitCodeView generateTestMethodInitCode(
      MethodTransformerContext context,
      Iterable<Field> fields,
      SymbolTable table,
      TestValueGenerator valueGenerator) {
    return buildInitCodeView(
        context,
        generateInitCodeHelper(
            Lists.newArrayList(fields),
            table,
            valueGenerator,
            createTestRequestInitFieldStructure(context, fields),
            Name.from("request"),
            context.getMethod().getInputType()));
  }

  public InitCodeView generateMockResponseObjectInitCode(
      MethodTransformerContext context, SymbolTable table, TestValueGenerator valueGenerator) {
    return buildInitCodeView(
        context,
        generateInitCodeHelper(
            null,
            table,
            valueGenerator,
            createMockResponseInitFieldStructure(context),
            Name.from("expected_response"),
            context.getMethod().getOutputType()));
  }

  public InitCodeView generateSmokeTestInitCode(
      MethodTransformerContext methodContext, SymbolTable table) {
    SmokeTestConfig testConfig = methodContext.getInterfaceConfig().getSmokeTestConfig();
    Method method = testConfig.getMethod();

    ArrayList<Field> fields = new ArrayList<>();
    for (Field field : method.getInputMessage().getFields()) {
      if (testConfig.getInitFields().contains(field.getSimpleName())) {
        fields.add(field);
      }
    }

    return buildInitCodeView(
        methodContext,
        generateInitCodeHelper(
            fields,
            table,
            null,
            createSmokeTestInitMap(testConfig.getInitFields(), methodContext),
            Name.from("request"),
            method.getInputType()));
  }

  private Map<String, Object> createSmokeTestInitMap(
      List<String> smokeTestInitFields, MethodTransformerContext context) {
    Map<String, Object> initFieldStructure =
        FieldStructureParser.parseFields(smokeTestInitFields, createInitValueMap(context));
    return initFieldStructure;
  }

  private InitCode generateInitCodeHelper(
      ArrayList<Field> fields,
      SymbolTable table,
      TestValueGenerator valueGenerator,
      Map<String, Object> initFieldStructure,
      Name initObjectName,
      TypeRef initObjectType) {
    InitCodeGeneratorContext initCodeContextBuilder =
        InitCodeGeneratorContext.newBuilder()
            .symbolTable(table)
            .valueGenerator(valueGenerator)
            .initStructure(initFieldStructure)
            .initObjectName(initObjectName)
            .initObjectType(initObjectType)
            .flattenedFields(fields)
            .build();
    InitCodeGenerator generator = new InitCodeGenerator();
    return generator.generate(initCodeContextBuilder);
  }

  private InitCodeView buildInitCodeView(MethodTransformerContext context, InitCode initCode) {
    ImportTypeTransformer importTypeTransformer = new ImportTypeTransformer();
    ModelTypeTable typeTable = context.getTypeTable();
    SurfaceNamer namer = context.getNamer();

    // Initialize the type table with the apiClassName since each sample will be using the
    // apiClass.
    typeTable.getAndSaveNicknameFor(
        namer.getFullyQualifiedApiWrapperClassName(
            context.getInterface(), context.getApiConfig().getPackageName()));

    return InitCodeView.newBuilder()
        .lines(generateSurfaceInitCodeLines(context, initCode))
        .fieldSettings(getFieldSettings(context, initCode.getArgFields()))
        .imports(importTypeTransformer.generateImports(typeTable.getImports()))
        .apiFileName(
            namer.getServiceFileName(
                context.getInterface(), context.getApiConfig().getPackageName()))
        .build();
  }

  public List<GapicSurfaceTestAssertView> generateRequestAssertViews(
      MethodTransformerContext context, Iterable<Field> fields) {
    InitCode initCode =
        generateInitCodeHelper(
            Lists.newArrayList(fields),
            new SymbolTable(),
            null,
            createSampleInitFieldStructure(context),
            Name.from("request"),
            context.getMethod().getInputType());

    List<GapicSurfaceTestAssertView> assertViews = new ArrayList<>();
    SurfaceNamer namer = context.getNamer();
    // Add request fields checking
    for (FieldSetting fieldSetting : initCode.getArgFields()) {
      String getterMethod =
          namer.getFieldGetFunctionName(fieldSetting.getType(), fieldSetting.getIdentifier());
      String expectedValueIdentifier =
          namer.getVariableName(fieldSetting.getIdentifier(), fieldSetting.getInitValueConfig());
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

  private Map<String, Object> createSampleInitFieldStructure(MethodTransformerContext context) {
    Map<String, Object> initFieldStructure =
        FieldStructureParser.parseFields(
            context.getMethodConfig().getSampleCodeInitFields(), createInitValueMap(context));
    return initFieldStructure;
  }

  private Map<String, Object> createTestRequestInitFieldStructure(
      MethodTransformerContext context, Iterable<Field> fields) {
    ArrayList<String> fieldStrings = new ArrayList<>();
    createTestRequestInitFieldStructure(context, fieldStrings, fields, "", 1);
    return FieldStructureParser.parseFields(fieldStrings, createInitValueMap(context));
  }

  private void createTestRequestInitFieldStructure(
      MethodTransformerContext context,
      ArrayList<String> fieldStrings,
      Iterable<Field> fields,
      String prefix,
      int depth) {
    for (Field field : fields) {
      String initString = prefix + field.getSimpleName();

      if (field.getType().isMap()) {
        // TODO(michaelbausor) support recursive map initialization
        fieldStrings.add(initString);
      } else {
        if (field.getType().isRepeated()) {
          initString += "[0]";
        }
        fieldStrings.add(initString);
        if (field.getType().isMessage() && depth > 0) {
          createTestRequestInitFieldStructure(
              context,
              fieldStrings,
              field.getType().getMessageType().getFields(),
              initString + ".",
              depth - 1);
        }
      }
    }
  }

  private Map<String, Object> createMockResponseInitFieldStructure(
      MethodTransformerContext context) {
    ArrayList<String> fields = new ArrayList<>();
    for (Field field : context.getMethod().getOutputMessage().getFields()) {
      if (field.getType().isPrimitive()) {
        fields.add(field.getSimpleName());
      }
    }
    if (context.getMethodConfig().isPageStreaming()) {
      // Initialize one resource element if it is page-streaming, and set the initial value of the
      // page token to empty, in order to indicate that no more pages are available.
      PageStreamingConfig config = context.getMethodConfig().getPageStreaming();
      fields.add(config.getResourcesField().getSimpleName() + "[0]");
      fields.add(config.getResponseTokenField().getSimpleName() + "=\"\"");
    }
    if (context.getMethodConfig().isBundling()) {
      // Initialize one bundling element if it is bundling.
      fields.add(
          context.getMethodConfig().getBundling().getSubresponseField().getSimpleName() + "[0]");
    }
    Map<String, Object> initFieldStructure =
        FieldStructureParser.parseFields(fields, createInitValueMap(context));
    return initFieldStructure;
  }

  private ImmutableMap<String, InitValueConfig> createInitValueMap(
      MethodTransformerContext context) {
    Map<String, String> fieldNamePatterns = context.getMethodConfig().getFieldNamePatterns();
    ImmutableMap.Builder<String, InitValueConfig> mapBuilder = ImmutableMap.builder();
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
      MethodTransformerContext context, InitCode initCode) {
    List<InitCodeLineView> surfaceLines = new ArrayList<>();
    for (InitCodeLine line : initCode.getLines()) {
      switch (line.getLineType()) {
        case StructureInitLine:
          surfaceLines.add(generateStructureInitCodeLine(context, (StructureInitCodeLine) line));
          continue;
        case ListInitLine:
          surfaceLines.add(generateListInitCodeLine(context, (ListInitCodeLine) line));
          continue;
        case SimpleInitLine:
          surfaceLines.add(generateSimpleInitCodeLine(context, (SimpleInitCodeLine) line));
          continue;
        case MapInitLine:
          surfaceLines.add(generateMapInitCodeLine(context, (MapInitCodeLine) line));
          continue;
        default:
          throw new RuntimeException("unhandled line type: " + line.getLineType());
      }
    }
    return surfaceLines;
  }

  private StructureInitCodeLineView generateStructureInitCodeLine(
      MethodTransformerContext context, StructureInitCodeLine line) {
    StructureInitCodeLineView.Builder surfaceLine = StructureInitCodeLineView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    surfaceLine.lineType(line.getLineType());
    surfaceLine.typeName(context.getTypeTable().getAndSaveNicknameFor(line.getType()));
    surfaceLine.identifier(namer.getVariableName(line.getIdentifier(), line.getInitValueConfig()));
    surfaceLine.fieldSettings(getFieldSettings(context, line.getFieldSettings()));
    surfaceLine.initValue(getInitValue(context, line.getType(), line.getInitValueConfig()));

    return surfaceLine.build();
  }

  private ListInitCodeLineView generateListInitCodeLine(
      MethodTransformerContext context, ListInitCodeLine line) {
    ListInitCodeLineView.Builder surfaceLine = ListInitCodeLineView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    surfaceLine.lineType(line.getLineType());
    surfaceLine.elementTypeName(
        context.getTypeTable().getAndSaveNicknameForElementType(line.getElementType()));
    surfaceLine.identifier(namer.getVariableName(line.getIdentifier(), line.getInitValueConfig()));
    List<String> elementIdentifiers = new ArrayList<>();
    for (Name identifier : line.getElementIdentifiers()) {
      elementIdentifiers.add(namer.varName(identifier));
    }
    surfaceLine.elementIdentifiers(elementIdentifiers);

    return surfaceLine.build();
  }

  private SimpleInitCodeLineView generateSimpleInitCodeLine(
      MethodTransformerContext context, SimpleInitCodeLine line) {
    SimpleInitCodeLineView.Builder surfaceLine = SimpleInitCodeLineView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    surfaceLine.lineType(line.getLineType());
    surfaceLine.typeName(context.getTypeTable().getAndSaveNicknameFor(line.getType()));
    surfaceLine.identifier(namer.getVariableName(line.getIdentifier(), line.getInitValueConfig()));
    surfaceLine.initValue(getInitValue(context, line.getType(), line.getInitValueConfig()));

    return surfaceLine.build();
  }

  private InitCodeLineView generateMapInitCodeLine(
      MethodTransformerContext context, MapInitCodeLine line) {
    MapInitCodeLineView.Builder surfaceLine = MapInitCodeLineView.newBuilder();

    ModelTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType(line.getLineType());
    surfaceLine.keyTypeName(typeTable.getAndSaveNicknameFor(line.getKeyType()));
    surfaceLine.valueTypeName(typeTable.getAndSaveNicknameFor(line.getValueType()));
    surfaceLine.identifier(
        context.getNamer().getVariableName(line.getIdentifier(), line.getInitValueConfig()));
    List<MapEntryView> entries = new ArrayList<>();
    for (Map.Entry<String, Name> entry : line.getElementIdentifierMap().entrySet()) {
      MapEntryView.Builder mapEntry = MapEntryView.newBuilder();
      mapEntry.key(typeTable.renderPrimitiveValue(line.getKeyType(), entry.getKey()));
      mapEntry.value(
          context.getNamer().getVariableName(line.getElementIdentifierValue(entry.getKey()), null));
      entries.add(mapEntry.build());
    }
    surfaceLine.initEntries(entries);

    return surfaceLine.build();
  }

  private InitValueView getInitValue(
      MethodTransformerContext context, TypeRef type, InitValueConfig initValueConfig) {
    if (initValueConfig.hasFormattingConfig()) {
      FormattedInitValueView.Builder initValue = FormattedInitValueView.newBuilder();

      initValue.apiWrapperName(context.getNamer().getApiWrapperClassName(context.getInterface()));
      initValue.formatFunctionName(
          context.getNamer().getFormatFunctionName(initValueConfig.getCollectionConfig()));
      List<String> formatFunctionArgs = new ArrayList<>();
      for (String var : initValueConfig.getCollectionConfig().getNameTemplate().vars()) {
        formatFunctionArgs.add("\"[" + LanguageUtil.lowerUnderscoreToUpperUnderscore(var) + "]\"");
      }
      initValue.formatArgs(formatFunctionArgs);

      return initValue.build();
    } else {
      SimpleInitValueView.Builder initValue = SimpleInitValueView.newBuilder();

      if (initValueConfig.hasInitialValue()) {
        initValue.initialValue(
            context.getTypeTable().renderPrimitiveValue(type, initValueConfig.getInitialValue()));
      } else {
        initValue.initialValue(context.getTypeTable().getZeroValueAndSaveNicknameFor(type));
      }

      return initValue.build();
    }
  }

  private List<FieldSettingView> getFieldSettings(
      MethodTransformerContext context, Iterable<FieldSetting> fieldSettings) {
    SurfaceNamer namer = context.getNamer();
    List<FieldSettingView> allSettings = new ArrayList<>();
    for (FieldSetting setting : fieldSettings) {
      FieldSettingView.Builder fieldSetting = FieldSettingView.newBuilder();
      fieldSetting.fieldSetFunction(
          namer.getFieldSetFunctionName(setting.getType(), setting.getFieldName()));
      fieldSetting.identifier(
          namer.getVariableName(setting.getIdentifier(), setting.getInitValueConfig()));
      allSettings.add(fieldSetting.build());
    }
    return allSettings;
  }
}
