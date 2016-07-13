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
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.metacode.FieldSetting;
import com.google.api.codegen.metacode.FieldStructureParser;
import com.google.api.codegen.metacode.InitCode;
import com.google.api.codegen.metacode.InitCodeGenerator;
import com.google.api.codegen.metacode.InitCodeLine;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.metacode.ListInitCodeLine;
import com.google.api.codegen.metacode.MapInitCodeLine;
import com.google.api.codegen.metacode.SimpleInitCodeLine;
import com.google.api.codegen.metacode.StructureInitCodeLine;
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
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InitCodeTransformer {

  public InitCodeView generateInitCode(
      TransformerContext context,
      Method method,
      MethodConfig methodConfig,
      Iterable<Field> fields) {
    Map<String, Object> initFieldStructure = createInitFieldStructure(context, methodConfig);
    InitCodeGenerator generator = new InitCodeGenerator();
    InitCode initCode = generator.generateRequestFieldInitCode(method, initFieldStructure, fields);

    InitCodeView surfaceInitCode = new InitCodeView();
    surfaceInitCode.lines = generateSurfaceInitCodeLines(context, initCode);
    surfaceInitCode.fieldSettings = getFieldSettings(context, initCode.getArgFields());
    return surfaceInitCode;
  }

  public InitCodeView generateRequestObjectInitCode(
      TransformerContext context, Method method, MethodConfig methodConfig) {
    Map<String, Object> initFieldStructure = createInitFieldStructure(context, methodConfig);
    InitCodeGenerator generator = new InitCodeGenerator();
    InitCode initCode = generator.generateRequestObjectInitCode(method, initFieldStructure);

    InitCodeView surfaceInitCode = new InitCodeView();
    surfaceInitCode.lines = generateSurfaceInitCodeLines(context, initCode);
    surfaceInitCode.fieldSettings = getFieldSettings(context, initCode.getArgFields());
    return surfaceInitCode;
  }

  public Map<String, Object> createInitFieldStructure(
      TransformerContext context, MethodConfig methodConfig) {
    Map<String, String> fieldNamePatterns = methodConfig.getFieldNamePatterns();

    ImmutableMap.Builder<String, InitValueConfig> initValueConfigMap = ImmutableMap.builder();
    for (Map.Entry<String, String> fieldNamePattern : fieldNamePatterns.entrySet()) {
      CollectionConfig collectionConfig = context.getCollectionConfig(fieldNamePattern.getValue());
      String apiWrapperClassName =
          context.getNamer().getApiWrapperClassName(context.getInterface());
      InitValueConfig initValueConfig =
          InitValueConfig.create(apiWrapperClassName, collectionConfig);
      initValueConfigMap.put(fieldNamePattern.getKey(), initValueConfig);
    }
    Map<String, Object> initFieldStructure =
        FieldStructureParser.parseFields(
            methodConfig.getSampleCodeInitFields(), initValueConfigMap.build());
    return initFieldStructure;
  }

  public List<InitCodeLineView> generateSurfaceInitCodeLines(
      TransformerContext context, InitCode initCode) {
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

  public StructureInitCodeLineView generateStructureInitCodeLine(
      TransformerContext context, StructureInitCodeLine line) {
    StructureInitCodeLineView surfaceLine = new StructureInitCodeLineView();

    SurfaceNamer namer = context.getNamer();
    surfaceLine.lineType = line.getLineType();
    surfaceLine.typeName = context.getTypeTable().getAndSaveNicknameFor(line.getType());
    surfaceLine.identifier = namer.getVariableName(line.getIdentifier(), line.getInitValueConfig());
    surfaceLine.fieldSettings = getFieldSettings(context, line.getFieldSettings());
    surfaceLine.initValue = getInitValue(context, line.getType(), line.getInitValueConfig());

    return surfaceLine;
  }

  public ListInitCodeLineView generateListInitCodeLine(
      TransformerContext context, ListInitCodeLine line) {
    ListInitCodeLineView surfaceLine = new ListInitCodeLineView();

    SurfaceNamer namer = context.getNamer();
    surfaceLine.lineType = line.getLineType();
    surfaceLine.elementTypeName =
        context.getTypeTable().getAndSaveNicknameForElementType(line.getElementType());
    surfaceLine.identifier = namer.getVariableName(line.getIdentifier(), line.getInitValueConfig());
    List<String> elementIdentifiers = new ArrayList<>();
    for (String identifier : line.getElementIdentifiers()) {
      elementIdentifiers.add(namer.getVariableName(identifier, null));
    }
    surfaceLine.elementIdentifiers = elementIdentifiers;

    return surfaceLine;
  }

  public SimpleInitCodeLineView generateSimpleInitCodeLine(
      TransformerContext context, SimpleInitCodeLine line) {
    SimpleInitCodeLineView surfaceLine = new SimpleInitCodeLineView();

    SurfaceNamer namer = context.getNamer();
    surfaceLine.lineType = line.getLineType();
    surfaceLine.typeName = context.getTypeTable().getAndSaveNicknameFor(line.getType());
    surfaceLine.identifier = namer.getVariableName(line.getIdentifier(), line.getInitValueConfig());
    surfaceLine.initValue = getInitValue(context, line.getType(), line.getInitValueConfig());

    return surfaceLine;
  }

  public InitCodeLineView generateMapInitCodeLine(
      TransformerContext context, MapInitCodeLine line) {
    MapInitCodeLineView surfaceLine = new MapInitCodeLineView();

    ModelTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType = line.getLineType();
    surfaceLine.keyTypeName = typeTable.getAndSaveNicknameFor(line.getKeyType());
    surfaceLine.valueTypeName = typeTable.getAndSaveNicknameFor(line.getValueType());
    surfaceLine.identifier =
        context.getNamer().getVariableName(line.getIdentifier(), line.getInitValueConfig());
    List<MapEntryView> entries = new ArrayList<>();
    for (Map.Entry<String, String> entry : line.getElementIdentifierMap().entrySet()) {
      MapEntryView mapEntry = new MapEntryView();
      mapEntry.key = typeTable.renderPrimitiveValue(line.getKeyType(), entry.getKey());
      mapEntry.value =
          context.getNamer().getVariableName(line.getElementIdentifierValue(entry.getKey()), null);
      entries.add(mapEntry);
    }
    surfaceLine.initEntries = entries;

    return surfaceLine;
  }

  public InitValueView getInitValue(
      TransformerContext context, TypeRef type, InitValueConfig initValueConfig) {
    if (initValueConfig.hasFormattingConfig()) {
      FormattedInitValueView initValue = new FormattedInitValueView();

      initValue.apiWrapperName = context.getNamer().getApiWrapperClassName(context.getInterface());
      initValue.formatFunctionName =
          context.getNamer().getFormatFunctionName(initValueConfig.getCollectionConfig());
      List<String> formatFunctionArgs = new ArrayList<>();
      for (String var : initValueConfig.getCollectionConfig().getNameTemplate().vars()) {
        formatFunctionArgs.add("\"[" + LanguageUtil.lowerUnderscoreToUpperUnderscore(var) + "]\"");
      }
      initValue.formatArgs = formatFunctionArgs;

      return initValue;
    } else {
      SimpleInitValueView initValue = new SimpleInitValueView();

      if (initValueConfig.hasInitialValue()) {
        initValue.initialValue =
            context.getTypeTable().renderPrimitiveValue(type, initValueConfig.getInitialValue());
      } else {
        initValue.initialValue = context.getTypeTable().getZeroValueAndSaveNicknameFor(type);
      }

      return initValue;
    }
  }

  public List<FieldSettingView> getFieldSettings(
      TransformerContext context, Iterable<FieldSetting> fieldSettings) {
    SurfaceNamer namer = context.getNamer();
    List<FieldSettingView> allSettings = new ArrayList<>();
    for (FieldSetting setting : fieldSettings) {
      FieldSettingView fieldSetting = new FieldSettingView();
      fieldSetting.setFunctionCallName =
          namer.getSetFunctionCallName(setting.getType(), setting.getFieldName());
      fieldSetting.identifier =
          namer.getVariableName(setting.getIdentifier(), setting.getInitValueConfig());
      allSettings.add(fieldSetting);
    }
    return allSettings;
  }
}
