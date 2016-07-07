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

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.PageStreamingConfig;
import com.google.api.codegen.ServiceConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.java.JavaDocUtil;
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
import com.google.api.codegen.surface.SurfaceApiCallable;
import com.google.api.codegen.surface.SurfaceApiMethod;
import com.google.api.codegen.surface.SurfaceApiMethodDoc;
import com.google.api.codegen.surface.SurfaceBundlingApiCallable;
import com.google.api.codegen.surface.SurfaceCallableMethod;
import com.google.api.codegen.surface.SurfaceDoc;
import com.google.api.codegen.surface.SurfaceFieldSetting;
import com.google.api.codegen.surface.SurfaceFlattenedMethod;
import com.google.api.codegen.surface.SurfaceFormatResourceFunction;
import com.google.api.codegen.surface.SurfaceFormattedInitValue;
import com.google.api.codegen.surface.SurfaceInitCode;
import com.google.api.codegen.surface.SurfaceInitCodeLine;
import com.google.api.codegen.surface.SurfaceInitValue;
import com.google.api.codegen.surface.SurfaceListInitCodeLine;
import com.google.api.codegen.surface.SurfaceMapEntry;
import com.google.api.codegen.surface.SurfaceMapInitCodeLine;
import com.google.api.codegen.surface.SurfacePagedApiCallable;
import com.google.api.codegen.surface.SurfacePagedCallableMethod;
import com.google.api.codegen.surface.SurfacePagedFlattenedMethod;
import com.google.api.codegen.surface.SurfacePagedRequestObjectMethod;
import com.google.api.codegen.surface.SurfaceParseResourceFunction;
import com.google.api.codegen.surface.SurfacePathTemplateCheck;
import com.google.api.codegen.surface.SurfaceRequestObjectMethod;
import com.google.api.codegen.surface.SurfaceRequestObjectParam;
import com.google.api.codegen.surface.SurfaceResourceIdParam;
import com.google.api.codegen.surface.SurfaceSimpleApiCallable;
import com.google.api.codegen.surface.SurfaceSimpleInitCodeLine;
import com.google.api.codegen.surface.SurfaceSimpleInitValue;
import com.google.api.codegen.surface.SurfaceStaticXApi;
import com.google.api.codegen.surface.SurfaceStaticXSettings;
import com.google.api.codegen.surface.SurfaceStructureInitCodeLine;
import com.google.api.codegen.surface.SurfaceUnpagedListCallableMethod;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ModelToJavaSurfaceTransformer implements ModelToSurfaceTransformer {
  private ApiConfig cachedApiConfig;
  private GapicCodePathMapper pathMapper;
  private CommonTransformer commonTransformer;

  public ModelToJavaSurfaceTransformer(ApiConfig apiConfig, GapicCodePathMapper pathMapper) {
    this.cachedApiConfig = apiConfig;
    this.pathMapper = pathMapper;
    this.commonTransformer = new CommonTransformer();
  }

  @Override
  public List<String> getTemplateFileNames() {
    List<String> fileNames = new ArrayList<>();

    fileNames.add(new SurfaceStaticXApi().getTemplateFileName());
    fileNames.add(new SurfaceStaticXSettings().getTemplateFileName());

    return fileNames;
  }

  @Override
  public List<SurfaceDoc> transform(Interface service) {
    ModelToSurfaceContext context =
        ModelToSurfaceContext.create(
            service, cachedApiConfig, new ModelToJavaTypeTable(), new JavaIdentifierNamer());

    String outputPath = pathMapper.getOutputPath(service, context.getApiConfig());

    List<SurfaceDoc> surfaceData = new ArrayList<>();

    addXApiImports(context);

    SurfaceStaticXApi xapiClass = new SurfaceStaticXApi();
    xapiClass.packageName = context.getApiConfig().getPackageName();
    xapiClass.name = getApiWrapperClassName(context);
    xapiClass.settingsClassName = getSettingsClassName(context);
    xapiClass.apiCallableMembers = generateApiCallables(context);
    xapiClass.pathTemplates = commonTransformer.generatePathTemplates(context);
    xapiClass.formatResourceFunctions = generateFormatResourceFunctions(context);
    xapiClass.parseResourceFunctions = generateParseResourceFunctions(context);
    xapiClass.apiMethods = generateApiMethods(context);

    // must be done as the last step to catch all imports
    xapiClass.imports = context.getTypeTable().getImports();

    xapiClass.outputPath = outputPath + "/" + xapiClass.name + ".java";

    surfaceData.add(xapiClass);

    context = context.withNewTypeTable();
    addXSettingsImports(context);

    SurfaceStaticXSettings xsettingsClass = new SurfaceStaticXSettings();
    xsettingsClass.packageName = context.getApiConfig().getPackageName();
    xsettingsClass.name = getSettingsClassName(context);
    ServiceConfig serviceConfig = new ServiceConfig();
    xsettingsClass.serviceAddress = serviceConfig.getServiceAddress(service);
    xsettingsClass.servicePort = serviceConfig.getServicePort();
    xsettingsClass.authScopes = serviceConfig.getAuthScopes(service);

    // must be done as the last step to catch all imports
    xsettingsClass.imports = context.getTypeTable().getImports();

    xsettingsClass.outputPath = outputPath + "/" + xsettingsClass.name + ".java";

    surfaceData.add(xsettingsClass);

    return surfaceData;
  }

  private void addXApiImports(ModelToSurfaceContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.addImport("com.google.api.gax.grpc.ApiCallable");
    typeTable.addImport("com.google.api.gax.protobuf.PathTemplate");
    typeTable.addImport("io.grpc.ManagedChannel");
    typeTable.addImport("java.io.Closeable");
    typeTable.addImport("java.io.IOException");
    typeTable.addImport("java.util.ArrayList");
    typeTable.addImport("java.util.List");
    typeTable.addImport("java.util.concurrent.ScheduledExecutorService");
  }

  private void addXSettingsImports(ModelToSurfaceContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.addImport("com.google.api.gax.core.ConnectionSettings");
    typeTable.addImport("com.google.api.gax.core.RetrySettings");
    typeTable.addImport("com.google.api.gax.grpc.ApiCallSettings");
    typeTable.addImport("com.google.api.gax.grpc.SimpleCallSettings");
    typeTable.addImport("com.google.api.gax.grpc.ServiceApiSettings");
    typeTable.addImport("com.google.auth.Credentials");
    typeTable.addImport("com.google.common.collect.ImmutableList");
    typeTable.addImport("com.google.common.collect.ImmutableMap");
    typeTable.addImport("com.google.common.collect.ImmutableSet");
    typeTable.addImport("com.google.common.collect.Lists");
    typeTable.addImport("com.google.common.collect.Sets");
    typeTable.addImport("io.grpc.ManagedChannel");
    typeTable.addImport("io.grpc.Status");
    typeTable.addImport("org.joda.time.Duration");
    typeTable.addImport("java.io.IOException");
    typeTable.addImport("java.util.List");
    typeTable.addImport("java.util.concurrent.ScheduledExecutorService");
  }

  private List<SurfaceApiCallable> generateApiCallables(ModelToSurfaceContext context) {
    List<SurfaceApiCallable> callableMembers = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      callableMembers.addAll(generateApiCallables(context, method, methodConfig));
    }

    return callableMembers;
  }

  private List<SurfaceApiCallable> generateApiCallables(
      ModelToSurfaceContext context, Method method, MethodConfig methodConfig) {
    String methodNameLowCml = LanguageUtil.upperCamelToLowerCamel(method.getSimpleName());
    ModelTypeTable typeTable = context.getTypeTable();

    List<SurfaceApiCallable> apiCallables = new ArrayList<>();

    if (methodConfig.isBundling()) {
      SurfaceBundlingApiCallable apiCallable = new SurfaceBundlingApiCallable();

      apiCallable.inTypeName = typeTable.importAndGetShortestName(method.getInputType());
      apiCallable.outTypeName = typeTable.importAndGetShortestName(method.getOutputType());
      apiCallable.name = methodNameLowCml + "Callable";
      apiCallable.settingsFunctionName = methodNameLowCml + "Settings";

      apiCallables.add(apiCallable);

    } else {
      SurfaceSimpleApiCallable apiCallable = new SurfaceSimpleApiCallable();

      apiCallable.inTypeName = typeTable.importAndGetShortestName(method.getInputType());
      apiCallable.outTypeName = typeTable.importAndGetShortestName(method.getOutputType());
      apiCallable.name = methodNameLowCml + "Callable";
      apiCallable.settingsFunctionName = methodNameLowCml + "Settings";

      apiCallables.add(apiCallable);

      if (methodConfig.isPageStreaming()) {
        PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

        SurfacePagedApiCallable pagedApiCallable = new SurfacePagedApiCallable();

        pagedApiCallable.inTypeName = apiCallable.inTypeName;
        pagedApiCallable.pageAccessorTypeName =
            typeTable.importAndGetShortestName("com.google.api.gax.core.PageAccessor");
        pagedApiCallable.resourceTypeName =
            typeTable.importAndGetShortestNameForElementType(
                pageStreaming.getResourcesField().getType());
        pagedApiCallable.name = methodNameLowCml + "PagedCallable";
        pagedApiCallable.settingsFunctionName = methodNameLowCml + "Settings";
        apiCallables.add(pagedApiCallable);
      }
    }

    return apiCallables;
  }

  private List<SurfaceFormatResourceFunction> generateFormatResourceFunctions(
      ModelToSurfaceContext context) {
    List<SurfaceFormatResourceFunction> functions = new ArrayList<>();

    for (CollectionConfig collectionConfig : context.getCollectionConfigs()) {
      SurfaceFormatResourceFunction function = new SurfaceFormatResourceFunction();
      function.entityName = collectionConfig.getEntityName();
      function.name = getFormatFunctionName(collectionConfig);
      function.pathTemplateName = context.getNamer().getPathTemplateName(collectionConfig);
      List<SurfaceResourceIdParam> resourceIdParams = new ArrayList<>();
      for (String var : collectionConfig.getNameTemplate().vars()) {
        SurfaceResourceIdParam param = new SurfaceResourceIdParam();
        param.name = LanguageUtil.lowerUnderscoreToLowerCamel(var);
        param.templateKey = var;
        resourceIdParams.add(param);
      }
      function.resourceIdParams = resourceIdParams;

      functions.add(function);
    }

    return functions;
  }

  private List<SurfaceParseResourceFunction> generateParseResourceFunctions(
      ModelToSurfaceContext context) {
    List<SurfaceParseResourceFunction> functions = new ArrayList<>();

    for (CollectionConfig collectionConfig : context.getCollectionConfigs()) {
      for (String var : collectionConfig.getNameTemplate().vars()) {
        SurfaceParseResourceFunction function = new SurfaceParseResourceFunction();
        function.entityName =
            LanguageUtil.lowerUnderscoreToLowerCamel(collectionConfig.getEntityName());
        function.name =
            "parse"
                + LanguageUtil.lowerUnderscoreToUpperCamel(var)
                + "From"
                + LanguageUtil.lowerUnderscoreToUpperCamel(collectionConfig.getEntityName())
                + "Name";
        function.pathTemplateName = context.getNamer().getPathTemplateName(collectionConfig);
        function.entityNameParamName = function.entityName + "Name";
        function.outputResourceId = var;

        functions.add(function);
      }
    }

    return functions;
  }

  private List<SurfaceApiMethod> generateApiMethods(ModelToSurfaceContext context) {
    List<SurfaceApiMethod> apiMethods = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);

      if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            apiMethods.add(generatePagedFlattenedMethod(context, method, methodConfig, fields));
          }
        }
        apiMethods.add(generatePagedRequestObjectMethod(context, method, methodConfig));
        apiMethods.add(new SurfacePagedCallableMethod());
        apiMethods.add(new SurfaceUnpagedListCallableMethod());
      } else {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            SurfaceFlattenedMethod apiMethod = new SurfaceFlattenedMethod();
            apiMethod.fields = fields;
            apiMethods.add(apiMethod);
          }
        }
        apiMethods.add(new SurfaceRequestObjectMethod());
        apiMethods.add(new SurfaceCallableMethod());
      }
    }

    return apiMethods;
  }

  private SurfacePagedFlattenedMethod generatePagedFlattenedMethod(
      ModelToSurfaceContext context,
      Method method,
      MethodConfig methodConfig,
      ImmutableList<Field> fields) {
    SurfacePagedFlattenedMethod apiMethod = new SurfacePagedFlattenedMethod();

    apiMethod.initCode = generateInitCode(context, method, methodConfig, fields);

    SurfaceApiMethodDoc doc = new SurfaceApiMethodDoc();

    doc.mainDocLines = getJavaDocLines(method);
    doc.paramDocLines = getMethodParamDocLines(fields);
    doc.throwsDocLines = getThrowsDocLines();

    apiMethod.doc = doc;

    PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();
    apiMethod.resourceTypeName =
        context
            .getTypeTable()
            .importAndGetShortestNameForElementType(pageStreaming.getResourcesField().getType());
    apiMethod.name = getApiMethodName(method);

    List<SurfaceRequestObjectParam> requestObjectParams = new ArrayList<>();
    for (Field field : fields) {
      requestObjectParams.add(generateRequestObjectParam(context, field));
    }
    apiMethod.requestObjectParams = requestObjectParams;

    List<SurfacePathTemplateCheck> pathTemplateChecks = new ArrayList<>();
    for (Field field : fields) {
      ImmutableMap<String, String> fieldNamePatterns = methodConfig.getFieldNamePatterns();
      String entityName = fieldNamePatterns.get(field.getSimpleName());
      if (entityName != null) {
        CollectionConfig collectionConfig = context.getCollectionConfig(entityName);
        SurfacePathTemplateCheck check = new SurfacePathTemplateCheck();
        check.pathTemplateName = context.getNamer().getPathTemplateName(collectionConfig);
        check.paramName = getVariableNameForField(field);

        pathTemplateChecks.add(check);
      }
    }
    apiMethod.pathTemplateChecks = pathTemplateChecks;

    apiMethod.requestTypeName =
        context.getTypeTable().importAndGetShortestName(method.getInputType());

    apiMethod.apiClassName = getApiWrapperClassName(context);
    apiMethod.apiVariableName = getApiWrapperVariableName(context);

    return apiMethod;
  }

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

  private Map<String, Object> createInitFieldStructure(
      ModelToSurfaceContext context, MethodConfig methodConfig) {
    Map<String, String> fieldNamePatterns = methodConfig.getFieldNamePatterns();

    ImmutableMap.Builder<String, InitValueConfig> initValueConfigMap = ImmutableMap.builder();
    for (Map.Entry<String, String> fieldNamePattern : fieldNamePatterns.entrySet()) {
      CollectionConfig collectionConfig = context.getCollectionConfig(fieldNamePattern.getValue());
      InitValueConfig initValueConfig =
          InitValueConfig.create(getApiWrapperClassName(context), collectionConfig);
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

  private SurfaceStructureInitCodeLine generateStructureInitCodeLine(
      ModelToSurfaceContext context, StructureInitCodeLine line) {
    SurfaceStructureInitCodeLine surfaceLine = new SurfaceStructureInitCodeLine();

    IdentifierNamer namer = context.getNamer();
    surfaceLine.lineType = line.getLineType();
    surfaceLine.typeName = context.getTypeTable().importAndGetShortestName(line.getType());
    surfaceLine.identifier = namer.getVariableName(line.getIdentifier(), line.getInitValueConfig());
    surfaceLine.fieldSettings = getFieldSettings(context, line.getFieldSettings());
    surfaceLine.initValue = getInitValue(context, line.getType(), line.getInitValueConfig());

    return surfaceLine;
  }

  private SurfaceListInitCodeLine generateListInitCodeLine(
      ModelToSurfaceContext context, ListInitCodeLine line) {
    SurfaceListInitCodeLine surfaceLine = new SurfaceListInitCodeLine();

    IdentifierNamer namer = context.getNamer();
    surfaceLine.lineType = line.getLineType();
    surfaceLine.elementTypeName =
        context.getTypeTable().importAndGetShortestNameForElementType(line.getElementType());
    surfaceLine.identifier = namer.getVariableName(line.getIdentifier(), line.getInitValueConfig());
    List<String> elementIdentifiers = new ArrayList<>();
    for (String identifier : line.getElementIdentifiers()) {
      elementIdentifiers.add(namer.getVariableName(identifier, null));
    }
    surfaceLine.elementIdentifiers = elementIdentifiers;

    return surfaceLine;
  }

  private SurfaceSimpleInitCodeLine generateSimpleInitCodeLine(
      ModelToSurfaceContext context, SimpleInitCodeLine line) {
    SurfaceSimpleInitCodeLine surfaceLine = new SurfaceSimpleInitCodeLine();

    IdentifierNamer namer = context.getNamer();
    surfaceLine.lineType = line.getLineType();
    surfaceLine.typeName = context.getTypeTable().importAndGetShortestName(line.getType());
    surfaceLine.identifier = namer.getVariableName(line.getIdentifier(), line.getInitValueConfig());
    surfaceLine.initValue = getInitValue(context, line.getType(), line.getInitValueConfig());

    return surfaceLine;
  }

  private SurfaceInitCodeLine generateMapInitCodeLine(
      ModelToSurfaceContext context, MapInitCodeLine line) {
    SurfaceMapInitCodeLine surfaceLine = new SurfaceMapInitCodeLine();

    ModelTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType = line.getLineType();
    surfaceLine.keyTypeName = typeTable.importAndGetShortestName(line.getKeyType());
    surfaceLine.valueTypeName = typeTable.importAndGetShortestName(line.getValueType());
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

  private SurfaceInitValue getInitValue(
      ModelToSurfaceContext context, TypeRef type, InitValueConfig initValueConfig) {
    if (initValueConfig.hasFormattingConfig()) {
      SurfaceFormattedInitValue initValue = new SurfaceFormattedInitValue();

      initValue.apiWrapperName = getApiWrapperClassName(context);
      initValue.formatFunctionName = getFormatFunctionName(initValueConfig.getCollectionConfig());
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
        initValue.initialValue = context.getTypeTable().importAndGetZeroValue(type);
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

  private SurfacePagedRequestObjectMethod generatePagedRequestObjectMethod(
      ModelToSurfaceContext context, Method method, MethodConfig methodConfig) {
    SurfacePagedRequestObjectMethod apiMethod = new SurfacePagedRequestObjectMethod();

    SurfaceApiMethodDoc doc = new SurfaceApiMethodDoc();

    doc.mainDocLines = getJavaDocLines(method);
    doc.paramDocLines = getRequestObjectDocLines();
    doc.throwsDocLines = getThrowsDocLines();

    apiMethod.doc = doc;

    if (methodConfig.hasRequestObjectMethod()) {
      apiMethod.accessModifier = "public";
    } else {
      apiMethod.accessModifier = "private";
    }

    PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();
    apiMethod.resourceTypeName =
        context
            .getTypeTable()
            .importAndGetShortestNameForElementType(pageStreaming.getResourcesField().getType());
    apiMethod.name = getApiMethodName(method);
    apiMethod.requestTypeName =
        context.getTypeTable().importAndGetShortestName(method.getInputType());

    return apiMethod;
  }

  private SurfaceRequestObjectParam generateRequestObjectParam(
      ModelToSurfaceContext context, Field field) {
    SurfaceRequestObjectParam param = new SurfaceRequestObjectParam();
    param.name = getVariableNameForField(field);
    param.typeName = context.getTypeTable().importAndGetShortestName(field.getType());
    param.setCallName = getSetFunctionCallName(field.getType(), field.getSimpleName());
    return param;
  }

  public static String getSetFunctionCallName(TypeRef type, String identifier) {
    String fieldSuffix = LanguageUtil.lowerUnderscoreToUpperCamel(identifier);
    if (type.isMap()) {
      return "putAll" + fieldSuffix;
    } else if (type.isRepeated()) {
      return "addAll" + fieldSuffix;
    } else {
      return "set" + fieldSuffix;
    }
  }

  private List<String> getJavaDocLines(ProtoElement element) {
    return JavaDocUtil.getJavaDocLines(DocumentationUtil.getDescription(element));
  }

  private List<String> getMethodParamDocLines(Iterable<Field> fields) {
    List<String> allDocLines = new ArrayList<>();
    for (Field field : fields) {
      List<String> docLines = getJavaDocLines(field);

      String firstLine = "@param " + getVariableNameForField(field) + " " + docLines.get(0);
      docLines.set(0, firstLine);

      allDocLines.addAll(docLines);
    }
    return allDocLines;
  }

  private List<String> getThrowsDocLines() {
    return Arrays.asList("@throws com.google.api.gax.grpc.ApiException if the remote call fails");
  }

  private List<String> getRequestObjectDocLines() {
    return Arrays.asList(
        "@param request The request object containing all of the parameters for the API call.");
  }

  private String getVariableNameForField(Field field) {
    return LanguageUtil.lowerUnderscoreToLowerCamel(field.getSimpleName());
  }

  private String getApiMethodName(Method method) {
    return LanguageUtil.upperCamelToLowerCamel(method.getSimpleName());
  }

  private String getApiWrapperClassName(ModelToSurfaceContext context) {
    return context.getInterface().getSimpleName() + "Api";
  }

  private String getApiWrapperVariableName(ModelToSurfaceContext context) {
    return LanguageUtil.upperCamelToLowerCamel(getApiWrapperClassName(context));
  }

  private String getSettingsClassName(ModelToSurfaceContext context) {
    return context.getInterface().getSimpleName() + "Settings";
  }

  private String getFormatFunctionName(CollectionConfig collectionConfig) {
    return "format"
        + LanguageUtil.lowerUnderscoreToUpperCamel(collectionConfig.getEntityName())
        + "Name";
  }
}
