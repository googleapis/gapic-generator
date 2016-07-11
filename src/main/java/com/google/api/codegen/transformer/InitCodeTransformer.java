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
import com.google.api.codegen.surface.SurfaceFieldSetting;
import com.google.api.codegen.surface.SurfaceFormattedInitValue;
import com.google.api.codegen.surface.SurfaceInitCode;
import com.google.api.codegen.surface.SurfaceInitCodeLine;
import com.google.api.codegen.surface.SurfaceInitValue;
import com.google.api.codegen.surface.SurfaceListInitCodeLine;
import com.google.api.codegen.surface.SurfaceMapEntry;
import com.google.api.codegen.surface.SurfaceMapInitCodeLine;
import com.google.api.codegen.surface.SurfaceSimpleInitCodeLine;
import com.google.api.codegen.surface.SurfaceSimpleInitValue;
import com.google.api.codegen.surface.SurfaceStructureInitCodeLine;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InitCodeTransformer {

  public SurfaceInitCode generateInitCode(
      ModelToSurfaceContext context,
      Method method,
      MethodConfig methodConfig,
      Iterable<Field> fields) {
    Map<String, Object> initFieldStructure = createInitFieldStructure(context, methodConfig);
    InitCodeGenerator generator = new InitCodeGenerator();
    InitCode initCode = generator.generateRequestFieldInitCode(method, initFieldStructure, fields);

    SurfaceInitCode surfaceInitCode = new SurfaceInitCode();
    surfaceInitCode.lines = generateSurfaceInitCodeLines(context, initCode);
    surfaceInitCode.fieldSettings = getFieldSettings(context, initCode.getArgFields());
    return surfaceInitCode;
  }

  public Map<String, Object> createInitFieldStructure(
      ModelToSurfaceContext context, MethodConfig methodConfig) {
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

  public List<SurfaceInitCodeLine> generateSurfaceInitCodeLines(
      ModelToSurfaceContext context, InitCode initCode) {
    List<SurfaceInitCodeLine> surfaceLines = new ArrayList<>();
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

  public SurfaceStructureInitCodeLine generateStructureInitCodeLine(
      ModelToSurfaceContext context, StructureInitCodeLine line) {
    SurfaceStructureInitCodeLine surfaceLine = new SurfaceStructureInitCodeLine();

    IdentifierNamer namer = context.getNamer();
    surfaceLine.lineType = line.getLineType();
    surfaceLine.typeName = context.getTypeTable().getAndSaveNicknameFor(line.getType());
    surfaceLine.identifier = namer.getVariableName(line.getIdentifier(), line.getInitValueConfig());
    surfaceLine.fieldSettings = getFieldSettings(context, line.getFieldSettings());
    surfaceLine.initValue = getInitValue(context, line.getType(), line.getInitValueConfig());

    return surfaceLine;
  }

  public SurfaceListInitCodeLine generateListInitCodeLine(
      ModelToSurfaceContext context, ListInitCodeLine line) {
    SurfaceListInitCodeLine surfaceLine = new SurfaceListInitCodeLine();

    IdentifierNamer namer = context.getNamer();
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

  public SurfaceSimpleInitCodeLine generateSimpleInitCodeLine(
      ModelToSurfaceContext context, SimpleInitCodeLine line) {
    SurfaceSimpleInitCodeLine surfaceLine = new SurfaceSimpleInitCodeLine();

    IdentifierNamer namer = context.getNamer();
    surfaceLine.lineType = line.getLineType();
    surfaceLine.typeName = context.getTypeTable().getAndSaveNicknameFor(line.getType());
    surfaceLine.identifier = namer.getVariableName(line.getIdentifier(), line.getInitValueConfig());
    surfaceLine.initValue = getInitValue(context, line.getType(), line.getInitValueConfig());

    return surfaceLine;
  }

  public SurfaceInitCodeLine generateMapInitCodeLine(
      ModelToSurfaceContext context, MapInitCodeLine line) {
    SurfaceMapInitCodeLine surfaceLine = new SurfaceMapInitCodeLine();

    ModelTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType = line.getLineType();
    surfaceLine.keyTypeName = typeTable.getAndSaveNicknameFor(line.getKeyType());
    surfaceLine.valueTypeName = typeTable.getAndSaveNicknameFor(line.getValueType());
    surfaceLine.identifier =
        context.getNamer().getVariableName(line.getIdentifier(), line.getInitValueConfig());
    List<SurfaceMapEntry> entries = new ArrayList<>();
    for (Map.Entry<String, String> entry : line.getElementIdentifierMap().entrySet()) {
      SurfaceMapEntry mapEntry = new SurfaceMapEntry();
      mapEntry.key = typeTable.renderPrimitiveValue(line.getKeyType(), entry.getKey());
      mapEntry.value = typeTable.renderPrimitiveValue(line.getElementType(), entry.getValue());
      entries.add(mapEntry);
    }
    surfaceLine.initEntries = entries;

    return surfaceLine;
  }

  public SurfaceInitValue getInitValue(
      ModelToSurfaceContext context, TypeRef type, InitValueConfig initValueConfig) {
    if (initValueConfig.hasFormattingConfig()) {
      SurfaceFormattedInitValue initValue = new SurfaceFormattedInitValue();

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
      SurfaceSimpleInitValue initValue = new SurfaceSimpleInitValue();

      if (initValueConfig.hasInitialValue()) {
        initValue.initialValue =
            context.getTypeTable().renderPrimitiveValue(type, initValueConfig.getInitialValue());
      } else {
        initValue.initialValue = context.getTypeTable().getZeroValueAndSaveNicknameFor(type);
      }

      return initValue;
    }
  }

  public List<SurfaceFieldSetting> getFieldSettings(
      ModelToSurfaceContext context, Iterable<FieldSetting> fieldSettings) {
    IdentifierNamer namer = context.getNamer();
    List<SurfaceFieldSetting> allSettings = new ArrayList<>();
    for (FieldSetting setting : fieldSettings) {
      SurfaceFieldSetting fieldSetting = new SurfaceFieldSetting();
      fieldSetting.setFunctionCallName =
          namer.getSetFunctionCallName(setting.getType(), setting.getFieldName());
      fieldSetting.identifier =
          namer.getVariableName(setting.getIdentifier(), setting.getInitValueConfig());
      allSettings.add(fieldSetting);
    }
    return allSettings;
  }
}
