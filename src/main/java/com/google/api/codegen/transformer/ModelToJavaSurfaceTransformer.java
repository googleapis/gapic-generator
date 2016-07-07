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
import com.google.api.codegen.surface.SurfacePathTemplate;
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
  private Interface service;
  private ApiConfig apiConfig;
  private ModelToJavaTypeTable typeTable;
  private IdentifierNamer namer;
  private GapicCodePathMapper pathMapper;

  public ModelToJavaSurfaceTransformer(ApiConfig apiConfig, GapicCodePathMapper pathMapper) {
    this.apiConfig = apiConfig;
    this.pathMapper = pathMapper;
    this.namer = new JavaIdentifierNamer();
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
    // FIXME pass around service instead of mutating state
    this.service = service;

    String outputPath = pathMapper.getOutputPath(service, apiConfig);

    List<SurfaceDoc> surfaceData = new ArrayList<>();

    typeTable = new ModelToJavaTypeTable();
    addXApiImports();

    SurfaceStaticXApi xapiClass = new SurfaceStaticXApi();
    xapiClass.packageName = apiConfig.getPackageName();
    xapiClass.name = getApiWrapperClassName();
    xapiClass.settingsClassName = getSettingsClassName();
    xapiClass.apiCallableMembers = generateApiCallables();
    xapiClass.pathTemplates = generatePathTemplates();
    xapiClass.formatResourceFunctions = generateFormatResourceFunctions();
    xapiClass.parseResourceFunctions = generateParseResourceFunctions();
    xapiClass.apiMethods = generateApiMethods();

    // must be done as the last step to catch all imports
    xapiClass.imports = typeTable.getImports();

    xapiClass.outputPath = outputPath + "/" + xapiClass.name + ".java";

    surfaceData.add(xapiClass);

    typeTable = new ModelToJavaTypeTable();
    addXSettingsImports();

    SurfaceStaticXSettings xsettingsClass = new SurfaceStaticXSettings();
    xsettingsClass.packageName = apiConfig.getPackageName();
    xsettingsClass.name = getSettingsClassName();
    ServiceConfig serviceConfig = new ServiceConfig();
    xsettingsClass.serviceAddress = serviceConfig.getServiceAddress(service);
    xsettingsClass.servicePort = serviceConfig.getServicePort();
    xsettingsClass.authScopes = serviceConfig.getAuthScopes(service);

    // must be done as the last step to catch all imports
    xsettingsClass.imports = typeTable.getImports();

    xsettingsClass.outputPath = outputPath + "/" + xsettingsClass.name + ".java";

    surfaceData.add(xsettingsClass);

    return surfaceData;
  }

  private void addXApiImports() {
    typeTable.addImport("com.google.api.gax.grpc.ApiCallable");
    typeTable.addImport("com.google.api.gax.protobuf.PathTemplate");
    typeTable.addImport("io.grpc.ManagedChannel");
    typeTable.addImport("java.io.Closeable");
    typeTable.addImport("java.io.IOException");
    typeTable.addImport("java.util.ArrayList");
    typeTable.addImport("java.util.List");
    typeTable.addImport("java.util.concurrent.ScheduledExecutorService");
  }

  private void addXSettingsImports() {
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

  private List<SurfaceApiCallable> generateApiCallables() {
    List<SurfaceApiCallable> callableMembers = new ArrayList<>();

    for (Method method : service.getMethods()) {
      MethodConfig methodConfig = apiConfig.getInterfaceConfig(service).getMethodConfig(method);
      callableMembers.addAll(generateApiCallables(method, methodConfig));
    }

    return callableMembers;
  }

  private List<SurfaceApiCallable> generateApiCallables(Method method, MethodConfig methodConfig) {
    String methodNameLowCml = LanguageUtil.upperCamelToLowerCamel(method.getSimpleName());

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

  private List<SurfacePathTemplate> generatePathTemplates() {
    List<SurfacePathTemplate> pathTemplates = new ArrayList<>();

    for (CollectionConfig collectionConfig :
        apiConfig.getInterfaceConfig(service).getCollectionConfigs()) {
      SurfacePathTemplate pathTemplate = new SurfacePathTemplate();
      pathTemplate.name = getPathTemplateName(collectionConfig);
      pathTemplate.pattern = collectionConfig.getNamePattern();
      pathTemplates.add(pathTemplate);
    }

    return pathTemplates;
  }

  private List<SurfaceFormatResourceFunction> generateFormatResourceFunctions() {
    List<SurfaceFormatResourceFunction> functions = new ArrayList<>();

    for (CollectionConfig collectionConfig :
        apiConfig.getInterfaceConfig(service).getCollectionConfigs()) {
      SurfaceFormatResourceFunction function = new SurfaceFormatResourceFunction();
      function.entityName = collectionConfig.getEntityName();
      function.name = getFormatFunctionName(collectionConfig);
      function.pathTemplateName = getPathTemplateName(collectionConfig);
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

  private List<SurfaceParseResourceFunction> generateParseResourceFunctions() {
    List<SurfaceParseResourceFunction> functions = new ArrayList<>();

    for (CollectionConfig collectionConfig :
        apiConfig.getInterfaceConfig(service).getCollectionConfigs()) {
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
        function.pathTemplateName = getPathTemplateName(collectionConfig);
        function.entityNameParamName = function.entityName + "Name";
        function.outputResourceId = var;

        functions.add(function);
      }
    }

    return functions;
  }

  private List<SurfaceApiMethod> generateApiMethods() {
    List<SurfaceApiMethod> apiMethods = new ArrayList<>();

    for (Method method : service.getMethods()) {
      MethodConfig methodConfig = apiConfig.getInterfaceConfig(service).getMethodConfig(method);

      if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            apiMethods.add(generatePagedFlattenedMethod(method, methodConfig, fields));
          }
        }
        apiMethods.add(generatePagedRequestObjectMethod(method, methodConfig));
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
      Method method, MethodConfig methodConfig, ImmutableList<Field> fields) {
    SurfacePagedFlattenedMethod apiMethod = new SurfacePagedFlattenedMethod();

    apiMethod.initCode = generateInitCode(method, methodConfig, fields);

    SurfaceApiMethodDoc doc = new SurfaceApiMethodDoc();

    doc.mainDocLines = getJavaDocLines(method);
    doc.paramDocLines = getMethodParamDocLines(fields);
    doc.throwsDocLines = getThrowsDocLines();

    apiMethod.doc = doc;

    PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();
    apiMethod.resourceTypeName =
        typeTable.importAndGetShortestNameForElementType(
            pageStreaming.getResourcesField().getType());
    apiMethod.name = getApiMethodName(method);

    List<SurfaceRequestObjectParam> requestObjectParams = new ArrayList<>();
    for (Field field : fields) {
      requestObjectParams.add(generateRequestObjectParam(field));
    }
    apiMethod.requestObjectParams = requestObjectParams;

    List<SurfacePathTemplateCheck> pathTemplateChecks = new ArrayList<>();
    for (Field field : fields) {
      ImmutableMap<String, String> fieldNamePatterns = methodConfig.getFieldNamePatterns();
      String entityName = fieldNamePatterns.get(field.getSimpleName());
      if (entityName != null) {
        CollectionConfig collectionConfig =
            apiConfig.getInterfaceConfig(service).getCollectionConfig(entityName);
        SurfacePathTemplateCheck check = new SurfacePathTemplateCheck();
        check.pathTemplateName = getPathTemplateName(collectionConfig);
        check.paramName = getVariableNameForField(field);

        pathTemplateChecks.add(check);
      }
    }
    apiMethod.pathTemplateChecks = pathTemplateChecks;

    apiMethod.requestTypeName = typeTable.importAndGetShortestName(method.getInputType());

    apiMethod.apiClassName = getApiWrapperClassName();
    apiMethod.apiVariableName = getApiWrapperVariableName();

    return apiMethod;
  }

  public SurfaceInitCode generateInitCode(
      Method method, MethodConfig methodConfig, Iterable<Field> fields) {
    Map<String, Object> initFieldStructure = createInitFieldStructure(methodConfig);
    InitCodeGenerator generator = new InitCodeGenerator();
    InitCode initCode = generator.generateRequestFieldInitCode(method, initFieldStructure, fields);

    SurfaceInitCode surfaceInitCode = new SurfaceInitCode();
    surfaceInitCode.lines = generateSurfaceInitCodeLines(initCode);
    surfaceInitCode.fieldSettings = getFieldSettings(initCode.getArgFields());
    return surfaceInitCode;
  }

  private Map<String, Object> createInitFieldStructure(MethodConfig methodConfig) {
    Map<String, String> fieldNamePatterns = methodConfig.getFieldNamePatterns();

    ImmutableMap.Builder<String, InitValueConfig> initValueConfigMap = ImmutableMap.builder();
    for (Map.Entry<String, String> fieldNamePattern : fieldNamePatterns.entrySet()) {
      CollectionConfig collectionConfig = getCollectionConfig(fieldNamePattern.getValue());
      InitValueConfig initValueConfig =
          InitValueConfig.create(getApiWrapperClassName(), collectionConfig);
      initValueConfigMap.put(fieldNamePattern.getKey(), initValueConfig);
    }
    Map<String, Object> initFieldStructure =
        FieldStructureParser.parseFields(
            methodConfig.getSampleCodeInitFields(), initValueConfigMap.build());
    return initFieldStructure;
  }

  private CollectionConfig getCollectionConfig(String entityName) {
    return apiConfig.getInterfaceConfig(service).getCollectionConfig(entityName);
  }

  public List<SurfaceInitCodeLine> generateSurfaceInitCodeLines(InitCode initCode) {
    List<SurfaceInitCodeLine> surfaceLines = new ArrayList<>();
    for (InitCodeLine line : initCode.getLines()) {
      switch (line.getLineType()) {
        case StructureInitLine:
          surfaceLines.add(generateStructureInitCodeLine((StructureInitCodeLine) line));
          continue;
        case ListInitLine:
          surfaceLines.add(generateListInitCodeLine((ListInitCodeLine) line));
          continue;
        case SimpleInitLine:
          surfaceLines.add(generateSimpleInitCodeLine((SimpleInitCodeLine) line));
          continue;
        case MapInitLine:
          surfaceLines.add(generateMapInitCodeLine((MapInitCodeLine) line));
          continue;
        default:
          throw new RuntimeException("unhandled line type: " + line.getLineType());
      }
    }
    return surfaceLines;
  }

  private SurfaceStructureInitCodeLine generateStructureInitCodeLine(StructureInitCodeLine line) {
    SurfaceStructureInitCodeLine surfaceLine = new SurfaceStructureInitCodeLine();

    surfaceLine.lineType = line.getLineType();
    surfaceLine.typeName = namer.getTypeName(line.getType());
    surfaceLine.identifier = namer.getVariableName(line.getIdentifier(), line.getInitValueConfig());
    surfaceLine.fieldSettings = getFieldSettings(line.getFieldSettings());
    surfaceLine.initValue = getInitValue(line.getType(), line.getInitValueConfig());

    return surfaceLine;
  }

  private SurfaceListInitCodeLine generateListInitCodeLine(ListInitCodeLine line) {
    SurfaceListInitCodeLine surfaceLine = new SurfaceListInitCodeLine();

    surfaceLine.lineType = line.getLineType();
    surfaceLine.elementTypeName = namer.getElementTypeName(line.getElementType());
    surfaceLine.identifier = namer.getVariableName(line.getIdentifier(), line.getInitValueConfig());
    List<String> elementIdentifiers = new ArrayList<>();
    for (String identifier : line.getElementIdentifiers()) {
      elementIdentifiers.add(namer.getVariableName(identifier, null));
    }
    surfaceLine.elementIdentifiers = elementIdentifiers;

    return surfaceLine;
  }

  private SurfaceSimpleInitCodeLine generateSimpleInitCodeLine(SimpleInitCodeLine line) {
    SurfaceSimpleInitCodeLine surfaceLine = new SurfaceSimpleInitCodeLine();

    surfaceLine.lineType = line.getLineType();
    surfaceLine.typeName = namer.getTypeName(line.getType());
    surfaceLine.identifier = namer.getVariableName(line.getIdentifier(), line.getInitValueConfig());
    surfaceLine.initValue = getInitValue(line.getType(), line.getInitValueConfig());

    return surfaceLine;
  }

  private SurfaceInitCodeLine generateMapInitCodeLine(MapInitCodeLine line) {
    SurfaceMapInitCodeLine surfaceLine = new SurfaceMapInitCodeLine();

    surfaceLine.lineType = line.getLineType();
    surfaceLine.keyTypeName = namer.getTypeName(line.getKeyType());
    surfaceLine.valueTypeName = namer.getTypeName(line.getValueType());
    surfaceLine.identifier = namer.getVariableName(line.getIdentifier(), line.getInitValueConfig());
    List<SurfaceMapEntry> entries = new ArrayList<>();
    for (Map.Entry<String, String> entry : line.getElementIdentifierMap().entrySet()) {
      SurfaceMapEntry mapEntry = new SurfaceMapEntry();
      mapEntry.key = namer.renderPrimitiveValue(line.getKeyType(), entry.getKey());
      mapEntry.value = namer.renderPrimitiveValue(line.getElementType(), entry.getValue());
      entries.add(mapEntry);
    }
    surfaceLine.initEntries = entries;

    return surfaceLine;
  }

  private SurfaceInitValue getInitValue(TypeRef type, InitValueConfig initValueConfig) {
    if (initValueConfig.hasFormattingConfig()) {
      SurfaceFormattedInitValue initValue = new SurfaceFormattedInitValue();

      initValue.apiWrapperName = getApiWrapperClassName();
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
            namer.renderPrimitiveValue(type, initValueConfig.getInitialValue());
      } else {
        initValue.initialValue = namer.zeroValue(type);
      }

      return initValue;
    }
  }

  public List<SurfaceFieldSetting> getFieldSettings(Iterable<FieldSetting> fieldSettings) {
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
      Method method, MethodConfig methodConfig) {
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
        typeTable.importAndGetShortestNameForElementType(
            pageStreaming.getResourcesField().getType());
    apiMethod.name = getApiMethodName(method);
    apiMethod.requestTypeName = typeTable.importAndGetShortestName(method.getInputType());

    return apiMethod;
  }

  private SurfaceRequestObjectParam generateRequestObjectParam(Field field) {
    SurfaceRequestObjectParam param = new SurfaceRequestObjectParam();
    param.name = getVariableNameForField(field);
    param.typeName = typeTable.importAndGetShortestName(field.getType());
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

  private String getPathTemplateName(CollectionConfig collectionConfig) {
    return LanguageUtil.lowerUnderscoreToUpperUnderscore(collectionConfig.getEntityName())
        + "_PATH_TEMPLATE";
  }

  private String getApiWrapperClassName() {
    return service.getSimpleName() + "Api";
  }

  private String getApiWrapperVariableName() {
    return LanguageUtil.upperCamelToLowerCamel(getApiWrapperClassName());
  }

  private String getSettingsClassName() {
    return service.getSimpleName() + "Settings";
  }

  private String getFormatFunctionName(CollectionConfig collectionConfig) {
    return "format"
        + LanguageUtil.lowerUnderscoreToUpperCamel(collectionConfig.getEntityName())
        + "Name";
  }
}
