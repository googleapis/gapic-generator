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
import com.google.api.codegen.java.JavaDocUtil;
import com.google.api.codegen.java.surface.JavaApiCallable;
import com.google.api.codegen.java.surface.JavaApiMethod;
import com.google.api.codegen.java.surface.JavaApiMethodDoc;
import com.google.api.codegen.java.surface.JavaBundlingApiCallable;
import com.google.api.codegen.java.surface.JavaCallableMethod;
import com.google.api.codegen.java.surface.JavaFlattenedMethod;
import com.google.api.codegen.java.surface.JavaFormatResourceFunction;
import com.google.api.codegen.java.surface.JavaPagedApiCallable;
import com.google.api.codegen.java.surface.JavaPagedCallableMethod;
import com.google.api.codegen.java.surface.JavaPagedFlattenedMethod;
import com.google.api.codegen.java.surface.JavaPagedRequestObjectMethod;
import com.google.api.codegen.java.surface.JavaParseResourceFunction;
import com.google.api.codegen.java.surface.JavaPathTemplate;
import com.google.api.codegen.java.surface.JavaPathTemplateCheck;
import com.google.api.codegen.java.surface.JavaRequestObjectMethod;
import com.google.api.codegen.java.surface.JavaRequestObjectParam;
import com.google.api.codegen.java.surface.JavaResourceIdParam;
import com.google.api.codegen.java.surface.JavaSimpleApiCallable;
import com.google.api.codegen.java.surface.JavaSurface;
import com.google.api.codegen.java.surface.JavaUnpagedListCallableMethod;
import com.google.api.codegen.java.surface.JavaXApi;
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

public class ModelToJavaSurfaceTransformer {
  private Interface service;
  private ApiConfig apiConfig;
  private ModelToJavaTypeTable typeTable;
  private IdentifierNamer namer;

  public static JavaSurface defaultTransform(Interface service, ApiConfig apiConfig) {
    return new ModelToJavaSurfaceTransformer(service, apiConfig).transform();
  }

  public ModelToJavaSurfaceTransformer(Interface service, ApiConfig apiConfig) {
    this.service = service;
    this.apiConfig = apiConfig;
    this.namer = new JavaIdentifierNamer();
  }

  public JavaSurface transform() {
    JavaSurface surface = new JavaSurface();

    typeTable = new ModelToJavaTypeTable();
    addAlwaysImports();

    JavaXApi xapiClass = new JavaXApi();
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

    surface.xapiClass = xapiClass;

    typeTable = new ModelToJavaTypeTable();

    return surface;
  }

  private void addAlwaysImports() {
    typeTable.addImport("com.google.api.gax.grpc.ApiCallable");
    typeTable.addImport("com.google.api.gax.protobuf.PathTemplate");
    typeTable.addImport("io.grpc.ManagedChannel");
    typeTable.addImport("java.io.Closeable");
    typeTable.addImport("java.io.IOException");
    typeTable.addImport("java.util.ArrayList");
    typeTable.addImport("java.util.List");
    typeTable.addImport("java.util.concurrent.ScheduledExecutorService");
  }

  private List<JavaApiCallable> generateApiCallables() {
    List<JavaApiCallable> callableMembers = new ArrayList<>();

    for (Method method : service.getMethods()) {
      MethodConfig methodConfig = apiConfig.getInterfaceConfig(service).getMethodConfig(method);
      callableMembers.addAll(generateApiCallables(method, methodConfig));
    }

    return callableMembers;
  }

  private List<JavaApiCallable> generateApiCallables(Method method, MethodConfig methodConfig) {
    String methodNameLowCml = LanguageUtil.upperCamelToLowerCamel(method.getSimpleName());

    List<JavaApiCallable> apiCallables = new ArrayList<>();

    if (methodConfig.isBundling()) {
      JavaBundlingApiCallable apiCallable = new JavaBundlingApiCallable();

      apiCallable.inTypeName = typeTable.importAndGetShortestName(method.getInputType());
      apiCallable.outTypeName = typeTable.importAndGetShortestName(method.getOutputType());
      apiCallable.name = methodNameLowCml + "Callable";
      apiCallable.settingsFunctionName = methodNameLowCml + "Settings";

      apiCallables.add(apiCallable);

    } else {
      JavaSimpleApiCallable apiCallable = new JavaSimpleApiCallable();

      apiCallable.inTypeName = typeTable.importAndGetShortestName(method.getInputType());
      apiCallable.outTypeName = typeTable.importAndGetShortestName(method.getOutputType());
      apiCallable.name = methodNameLowCml + "Callable";
      apiCallable.settingsFunctionName = methodNameLowCml + "Settings";

      apiCallables.add(apiCallable);

      if (methodConfig.isPageStreaming()) {
        PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

        JavaPagedApiCallable pagedApiCallable = new JavaPagedApiCallable();

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

  private List<JavaPathTemplate> generatePathTemplates() {
    List<JavaPathTemplate> pathTemplates = new ArrayList<>();

    for (CollectionConfig collectionConfig :
        apiConfig.getInterfaceConfig(service).getCollectionConfigs()) {
      JavaPathTemplate pathTemplate = new JavaPathTemplate();
      pathTemplate.name = getPathTemplateName(collectionConfig);
      pathTemplate.pattern = collectionConfig.getNamePattern();
      pathTemplates.add(pathTemplate);
    }

    return pathTemplates;
  }

  private List<JavaFormatResourceFunction> generateFormatResourceFunctions() {
    List<JavaFormatResourceFunction> functions = new ArrayList<>();

    for (CollectionConfig collectionConfig :
        apiConfig.getInterfaceConfig(service).getCollectionConfigs()) {
      JavaFormatResourceFunction function = new JavaFormatResourceFunction();
      function.entityName = collectionConfig.getEntityName();
      function.name = getFormatFunctionName(collectionConfig);
      function.pathTemplateName = getPathTemplateName(collectionConfig);
      List<JavaResourceIdParam> resourceIdParams = new ArrayList<>();
      for (String var : collectionConfig.getNameTemplate().vars()) {
        JavaResourceIdParam param = new JavaResourceIdParam();
        param.name = LanguageUtil.lowerUnderscoreToLowerCamel(var);
        param.templateKey = var;
        resourceIdParams.add(param);
      }
      function.resourceIdParams = resourceIdParams;

      functions.add(function);
    }

    return functions;
  }

  private List<JavaParseResourceFunction> generateParseResourceFunctions() {
    List<JavaParseResourceFunction> functions = new ArrayList<>();

    for (CollectionConfig collectionConfig :
        apiConfig.getInterfaceConfig(service).getCollectionConfigs()) {
      for (String var : collectionConfig.getNameTemplate().vars()) {
        JavaParseResourceFunction function = new JavaParseResourceFunction();
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

  private List<JavaApiMethod> generateApiMethods() {
    List<JavaApiMethod> apiMethods = new ArrayList<>();

    for (Method method : service.getMethods()) {
      MethodConfig methodConfig = apiConfig.getInterfaceConfig(service).getMethodConfig(method);

      if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            apiMethods.add(generatePagedFlattenedMethod(method, methodConfig, fields));
          }
        }
        apiMethods.add(generatePagedRequestObjectMethod(method, methodConfig));
        apiMethods.add(new JavaPagedCallableMethod());
        apiMethods.add(new JavaUnpagedListCallableMethod());
      } else {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            JavaFlattenedMethod apiMethod = new JavaFlattenedMethod();
            apiMethod.fields = fields;
            apiMethods.add(apiMethod);
          }
        }
        apiMethods.add(new JavaRequestObjectMethod());
        apiMethods.add(new JavaCallableMethod());
      }
    }

    return apiMethods;
  }

  private JavaPagedFlattenedMethod generatePagedFlattenedMethod(
      Method method, MethodConfig methodConfig, ImmutableList<Field> fields) {
    JavaPagedFlattenedMethod apiMethod = new JavaPagedFlattenedMethod();

    apiMethod.initCode = generateInitCode(method, methodConfig, fields);

    JavaApiMethodDoc doc = new JavaApiMethodDoc();

    doc.mainDocLines = getJavaDocLines(method);
    doc.paramDocLines = getMethodParamDocLines(fields);
    doc.throwsDocLines = getThrowsDocLines();

    apiMethod.doc = doc;

    PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();
    apiMethod.resourceTypeName =
        typeTable.importAndGetShortestNameForElementType(
            pageStreaming.getResourcesField().getType());
    apiMethod.name = getApiMethodName(method);

    List<JavaRequestObjectParam> requestObjectParams = new ArrayList<>();
    for (Field field : fields) {
      requestObjectParams.add(generateRequestObjectParam(field));
    }
    apiMethod.requestObjectParams = requestObjectParams;

    List<JavaPathTemplateCheck> pathTemplateChecks = new ArrayList<>();
    for (Field field : fields) {
      ImmutableMap<String, String> fieldNamePatterns = methodConfig.getFieldNamePatterns();
      String entityName = fieldNamePatterns.get(field.getSimpleName());
      if (entityName != null) {
        CollectionConfig collectionConfig =
            apiConfig.getInterfaceConfig(service).getCollectionConfig(entityName);
        JavaPathTemplateCheck check = new JavaPathTemplateCheck();
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

  private JavaPagedRequestObjectMethod generatePagedRequestObjectMethod(
      Method method, MethodConfig methodConfig) {
    JavaPagedRequestObjectMethod apiMethod = new JavaPagedRequestObjectMethod();

    JavaApiMethodDoc doc = new JavaApiMethodDoc();

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

  private JavaRequestObjectParam generateRequestObjectParam(Field field) {
    JavaRequestObjectParam param = new JavaRequestObjectParam();
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
