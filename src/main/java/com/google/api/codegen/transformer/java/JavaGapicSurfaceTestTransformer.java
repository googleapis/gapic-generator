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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.BundlingConfig;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.ResourceNameMessageConfigs;
import com.google.api.codegen.config.SmokeTestConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.StandardImportTypeTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.util.testing.JavaValueProducer;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.viewmodel.ApiMethodType;
import com.google.api.codegen.viewmodel.FileHeaderView;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestAssertView;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestCaseView;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestClassView;
import com.google.api.codegen.viewmodel.testing.MockGrpcMethodView;
import com.google.api.codegen.viewmodel.testing.MockGrpcResponseView;
import com.google.api.codegen.viewmodel.testing.MockServiceImplView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.api.codegen.viewmodel.testing.MockServiceView;
import com.google.api.codegen.viewmodel.testing.PageStreamingResponseView;
import com.google.api.codegen.viewmodel.testing.SmokeTestClassView;
import com.google.api.codegen.viewmodel.testing.TestMethodView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A subclass of ModelToViewTransformer which translates model into API tests in Java. */
public class JavaGapicSurfaceTestTransformer implements ModelToViewTransformer {
  // Template files
  private static String UNIT_TEST_TEMPLATE_FILE = "java/test.snip";
  private static String SMOKE_TEST_TEMPLATE_FILE = "java/smoke_test.snip";
  private static String MOCK_SERVICE_FILE = "java/mock_service.snip";
  private static String MOCK_SERVICE_IMPL_FILE = "java/mock_service_impl.snip";

  private final GapicCodePathMapper pathMapper;
  private final InitCodeTransformer initCodeTransformer;
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportTypeTransformer());
  private final TestValueGenerator valueGenerator = new TestValueGenerator(new JavaValueProducer());

  public JavaGapicSurfaceTestTransformer(GapicCodePathMapper javaPathMapper) {
    this.pathMapper = javaPathMapper;
    this.initCodeTransformer = new InitCodeTransformer();
  }

  @Override
  public List<String> getTemplateFileNames() {
    List<String> fileNames = new ArrayList<>();
    fileNames.add(UNIT_TEST_TEMPLATE_FILE);
    fileNames.add(SMOKE_TEST_TEMPLATE_FILE);
    fileNames.add(MOCK_SERVICE_IMPL_FILE);
    fileNames.add(MOCK_SERVICE_FILE);
    return fileNames;
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> views = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context = createContext(service, apiConfig);
      views.add(createUnitTestClassView(context));
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        context = createContext(service, apiConfig);
        views.add(createSmokeTestClassView(context));
      }
    }
    for (Interface service : getGrpcInterfacesToMock(model, apiConfig)) {
      SurfaceTransformerContext context = createContext(service, apiConfig);
      views.add(createMockServiceImplView(context));

      context = createContext(service, apiConfig);
      views.add(createMockServiceView(context));
    }
    return views;
  }

  ///////////////////////////////////// Smoke Test ///////////////////////////////////////

  private SmokeTestClassView createSmokeTestClassView(SurfaceTransformerContext context) {
    addSmokeTestImports(context);

    Interface service = context.getInterface();
    String outputPath = pathMapper.getOutputPath(service, context.getApiConfig());
    SurfaceNamer namer = context.getNamer();
    String name = namer.getSmokeTestClassName(service);

    Method method = context.getInterfaceConfig().getSmokeTestConfig().getMethod();
    FlatteningConfig flatteningGroup =
        getFlatteningGroup(
            context.getMethodConfig(method), context.getInterfaceConfig().getSmokeTestConfig());
    MethodTransformerContext methodContext =
        context.asFlattenedMethodContext(method, flatteningGroup);

    SmokeTestClassView.Builder testClass = SmokeTestClassView.newBuilder();
    testClass.apiSettingsClassName(namer.getApiSettingsClassName(service));
    testClass.apiClassName(namer.getApiWrapperClassName(service));
    testClass.name(name);
    testClass.outputPath(namer.getSourceFilePath(outputPath, name));
    testClass.templateFileName(SMOKE_TEST_TEMPLATE_FILE);
    testClass.method(createSmokeTestMethodView(methodContext));

    // Imports must be done as the last step to catch all imports.
    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    testClass.fileHeader(fileHeader);

    return testClass.build();
  }

  private TestMethodView createSmokeTestMethodView(MethodTransformerContext context) {
    Method method = context.getInterfaceConfig().getSmokeTestConfig().getMethod();
    SurfaceNamer namer = context.getNamer();

    ApiMethodType methodType = ApiMethodType.FlattenedMethod;
    if (context.getMethodConfig().isPageStreaming()) {
      methodType = ApiMethodType.PagedFlattenedMethod;
    }

    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(context, createSmokeTestInitContext(context));

    return TestMethodView.newBuilder()
        .name(namer.getApiMethodName(method, context.getMethodConfig().getVisibility()))
        .responseTypeName(context.getTypeTable().getAndSaveNicknameFor(method.getOutputType()))
        .type(methodType)
        .initCode(initCodeView)
        .hasReturnValue(!ServiceMessages.s_isEmptyType(method.getOutputType()))
        .build();
  }

  private InitCodeContext createSmokeTestInitContext(MethodTransformerContext context) {
    SmokeTestConfig testConfig = context.getInterfaceConfig().getSmokeTestConfig();
    InitCodeOutputType outputType;
    ImmutableMap<String, FieldConfig> fieldConfigMap;
    if (context.getMethodConfig().isFlattening()) {
      outputType = InitCodeOutputType.FieldList;
      fieldConfigMap =
          FieldConfig.toFieldConfigMap(
              context.getFlatteningConfig().getFlattenedFieldConfigs().values());
    } else {
      outputType = InitCodeOutputType.SingleObject;
      fieldConfigMap = null;
    }
    return InitCodeContext.newBuilder()
        .initObjectType(testConfig.getMethod().getInputType())
        .suggestedName(Name.from("request"))
        .outputType(outputType)
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(context))
        .initFieldConfigStrings(testConfig.getInitFieldConfigStrings())
        .fieldConfigMap(fieldConfigMap)
        .build();
  }

  private FlatteningConfig getFlatteningGroup(
      MethodConfig methodConfig, SmokeTestConfig smokeTestConfig) {
    for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
      if (flatteningGroup.getFlatteningName().equals(smokeTestConfig.getFlatteningName())) {
        return flatteningGroup;
      }
    }
    throw new IllegalArgumentException(
        "Flattening name in smoke test config did not correspond to any flattened method.");
  }

  ///////////////////////////////////// Unit Test /////////////////////////////////////////

  private GapicSurfaceTestClassView createUnitTestClassView(SurfaceTransformerContext context) {
    addUnitTestImports(context);

    Interface service = context.getInterface();
    String outputPath = pathMapper.getOutputPath(service, context.getApiConfig());
    SurfaceNamer namer = context.getNamer();
    String name = namer.getUnitTestClassName(service);

    GapicSurfaceTestClassView.Builder testClass = GapicSurfaceTestClassView.newBuilder();
    testClass.apiSettingsClassName(namer.getApiSettingsClassName(service));
    testClass.apiClassName(namer.getApiWrapperClassName(service));
    testClass.name(name);
    testClass.testCases(createTestCaseViews(context));
    testClass.mockServices(createMockServices(context));
    testClass.outputPath(namer.getSourceFilePath(outputPath, name));
    testClass.templateFileName(UNIT_TEST_TEMPLATE_FILE);

    // Imports must be done as the last step to catch all imports.
    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    testClass.fileHeader(fileHeader);

    return testClass.build();
  }

  private List<GapicSurfaceTestCaseView> createTestCaseViews(SurfaceTransformerContext context) {
    ArrayList<GapicSurfaceTestCaseView> testCaseViews = new ArrayList<>();
    SymbolTable testNameTable = new SymbolTable();
    for (Method method : context.getSupportedMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      if (MethodConfig.isGrpcStreamingMethod(method)) {
        MethodTransformerContext methodContext = context.asRequestMethodContext(method);
        testCaseViews.add(
            createTestCaseView(
                methodContext, testNameTable, methodConfig.getRequiredFieldConfigs()));
      } else if (methodConfig.isFlattening()) {
        for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
          MethodTransformerContext methodContext =
              context.asFlattenedMethodContext(method, flatteningGroup);
          testCaseViews.add(
              createTestCaseView(
                  methodContext,
                  testNameTable,
                  flatteningGroup.getFlattenedFieldConfigs().values()));
        }
      } else {
        // TODO: Add support of non-flattening method
        // Github issue: https://github.com/googleapis/toolkit/issues/393
        System.err.println(
            "Non-flattening method test is not supported yet for " + method.getSimpleName());
      }
    }
    return testCaseViews;
  }

  // TODO: Convert to use TestMethodView.
  private GapicSurfaceTestCaseView createTestCaseView(
      MethodTransformerContext methodContext,
      SymbolTable testNameTable,
      Iterable<FieldConfig> paramFieldConfigs) {
    MethodConfig methodConfig = methodContext.getMethodConfig();
    SurfaceNamer namer = methodContext.getNamer();
    Method method = methodContext.getMethod();

    // This symbol table is used to produce unique variable names used in the initialization code.
    // Shared by both request and response views.
    SymbolTable initSymbolTable = new SymbolTable();
    InitCodeView initCodeView;
    if (methodConfig.isGrpcStreaming()) {
      initCodeView =
          initCodeTransformer.generateInitCode(
              methodContext,
              createRequestInitCodeContext(
                  methodContext,
                  initSymbolTable,
                  paramFieldConfigs,
                  InitCodeOutputType.SingleObject));
    } else {
      initCodeView =
          initCodeTransformer.generateInitCode(
              methodContext,
              createRequestInitCodeContext(
                  methodContext, initSymbolTable, paramFieldConfigs, InitCodeOutputType.FieldList));
    }

    String requestTypeName =
        methodContext.getTypeTable().getAndSaveNicknameFor(method.getInputType());
    String responseTypeName =
        methodContext.getTypeTable().getAndSaveNicknameFor(method.getOutputType());
    String surfaceMethodName = namer.getApiMethodName(method, methodConfig.getVisibility());

    ApiMethodType type = ApiMethodType.FlattenedMethod;
    if (methodConfig.isPageStreaming()) {
      Field resourcesField = methodConfig.getPageStreaming().getResourcesField();
      responseTypeName =
          namer.getAndSavePagedResponseTypeName(
              method, methodContext.getTypeTable(), resourcesField);
      type = ApiMethodType.PagedFlattenedMethod;
    } else if (methodConfig.isGrpcStreaming()) {
      type = ApiMethodType.CallableMethod;
      surfaceMethodName = namer.getCallableMethodName(method);
      addGrpcStreamingTestImport(methodContext.getSurfaceTransformerContext());
    }

    List<GapicSurfaceTestAssertView> requestAssertViews =
        initCodeTransformer.generateRequestAssertViews(methodContext, paramFieldConfigs);

    return GapicSurfaceTestCaseView.newBuilder()
        .name(namer.getTestCaseName(testNameTable, method))
        .surfaceMethodName(surfaceMethodName)
        .hasReturnValue(!ServiceMessages.s_isEmptyType(method.getOutputType()))
        .requestTypeName(requestTypeName)
        .responseTypeName(responseTypeName)
        .initCode(initCodeView)
        .methodType(type)
        .pageStreamingResponseViews(createPageStreamingResponseViews(methodContext))
        .asserts(requestAssertViews)
        .mockResponse(createMockResponseView(methodContext, initSymbolTable))
        .mockServiceVarName(namer.getMockServiceVarName(methodContext.getTargetInterface()))
        .grpcStreamingType(methodConfig.getGrpcStreamingType())
        .build();
  }

  private List<PageStreamingResponseView> createPageStreamingResponseViews(
      MethodTransformerContext methodContext) {
    MethodConfig methodConfig = methodContext.getMethodConfig();
    SurfaceNamer namer = methodContext.getNamer();

    List<PageStreamingResponseView> pageStreamingResponseViews =
        new ArrayList<PageStreamingResponseView>();

    if (!methodConfig.isPageStreaming()) {
      return pageStreamingResponseViews;
    }

    FieldConfig resourcesFieldConfig = methodConfig.getPageStreaming().getResourcesFieldConfig();
    Field resourcesField = resourcesFieldConfig.getField();
    String resourceTypeName =
        methodContext.getTypeTable().getAndSaveNicknameForElementType(resourcesField.getType());
    String resourcesFieldGetterName =
        namer.getFieldGetFunctionName(
            resourcesField.getType(), Name.from(resourcesField.getSimpleName()));

    pageStreamingResponseViews.add(
        PageStreamingResponseView.newBuilder()
            .resourceTypeName(resourceTypeName)
            .resourcesFieldGetterName(resourcesFieldGetterName)
            .resourcesIterateMethod(namer.getPagedResponseIterateMethod())
            .resourcesVarName(namer.localVarName(Name.from("resources")))
            .build());

    if (methodContext.getFeatureConfig().useResourceNameFormatOption(resourcesFieldConfig)) {
      Name resourceName = namer.getResourceTypeName(resourcesFieldConfig.getEntityName());
      resourceTypeName =
          methodContext
              .getNamer()
              .getAndSaveResourceTypeName(
                  methodContext.getTypeTable(),
                  resourcesField,
                  resourcesField.getType().makeOptional(),
                  resourcesFieldConfig.getEntityName());

      resourcesFieldGetterName =
          namer.getResourceNameFieldGetFunctionName(
              resourcesField.getType(), Name.from(resourcesField.getSimpleName()));
      pageStreamingResponseViews.add(
          PageStreamingResponseView.newBuilder()
              .resourceTypeName(resourceTypeName)
              .resourcesFieldGetterName(resourcesFieldGetterName)
              .resourcesIterateMethod(
                  namer.getPagedResponseIterateMethod(
                      methodContext.getFeatureConfig(), resourcesFieldConfig))
              .resourcesVarName(namer.localVarName(Name.from("resources_as").join(resourceName)))
              .build());
    }

    return pageStreamingResponseViews;
  }

  private MockGrpcResponseView createMockResponseView(
      MethodTransformerContext methodContext, SymbolTable symbolTable) {
    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(
            methodContext, createResponseInitCodeContext(methodContext, symbolTable));

    String typeName =
        methodContext
            .getTypeTable()
            .getAndSaveNicknameFor(methodContext.getMethod().getOutputType());
    return MockGrpcResponseView.newBuilder().typeName(typeName).initCode(initCodeView).build();
  }

  private InitCodeContext createRequestInitCodeContext(
      MethodTransformerContext context,
      SymbolTable symbolTable,
      Iterable<FieldConfig> fieldConfigs,
      InitCodeOutputType outputType) {
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

  private InitCodeContext createResponseInitCodeContext(
      MethodTransformerContext context, SymbolTable symbolTable) {
    ArrayList<Field> primitiveFields = new ArrayList<>();
    for (Field field : context.getMethod().getOutputMessage().getFields()) {
      if (field.getType().isPrimitive() && !field.getType().isRepeated()) {
        primitiveFields.add(field);
      }
    }
    return InitCodeContext.newBuilder()
        .initObjectType(context.getMethod().getOutputType())
        .symbolTable(symbolTable)
        .suggestedName(Name.from("expected_response"))
        .initFieldConfigStrings(context.getMethodConfig().getSampleCodeInitFields())
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(context))
        .initFields(primitiveFields)
        .fieldConfigMap(createResponseFieldConfigMap(context))
        .valueGenerator(valueGenerator)
        .additionalInitCodeNodes(createMockResponseAdditionalSubTrees(context))
        .build();
  }

  private ImmutableMap<String, FieldConfig> createResponseFieldConfigMap(
      MethodTransformerContext context) {
    ApiConfig apiConfig = context.getApiConfig();
    ResourceNameMessageConfigs messageConfig = apiConfig.getResourceNameMessageConfigs();
    ResourceNameTreatment treatment = context.getMethodConfig().getDefaultResourceNameTreatment();

    if (messageConfig == null || treatment == ResourceNameTreatment.NONE) {
      return ImmutableMap.of();
    }
    ImmutableMap.Builder<String, FieldConfig> builder = ImmutableMap.builder();
    for (Field field : context.getMethod().getOutputMessage().getFields()) {
      if (messageConfig.fieldHasResourceName(field)) {
        builder.put(
            field.getFullName(),
            FieldConfig.createFieldConfig(
                field, treatment, messageConfig.getFieldResourceName(field)));
      }
    }
    return builder.build();
  }

  private Iterable<InitCodeNode> createMockResponseAdditionalSubTrees(
      MethodTransformerContext context) {
    List<InitCodeNode> additionalSubTrees = new ArrayList<>();
    if (context.getMethodConfig().isPageStreaming()) {
      // Initialize one resource element if it is page-streaming.
      PageStreamingConfig config = context.getMethodConfig().getPageStreaming();
      String resourceFieldName = config.getResourcesFieldName();
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

  ///////////////////////////////////// Mock Service /////////////////////////////////////////

  private List<MockServiceUsageView> createMockServices(SurfaceTransformerContext context) {
    List<MockServiceUsageView> mockServices = new ArrayList<>();

    SurfaceNamer namer = context.getNamer();
    for (Interface service : getGrpcInterfacesToMock(context.getModel(), context.getApiConfig())) {
      MockServiceUsageView mockService =
          MockServiceUsageView.newBuilder()
              .className(namer.getMockServiceClassName(service))
              .varName(namer.getMockServiceVarName(service))
              .build();
      mockServices.add(mockService);
    }

    return mockServices;
  }

  private MockServiceView createMockServiceView(SurfaceTransformerContext context) {
    addMockServiceImports(context);

    Interface service = context.getInterface();
    SurfaceNamer namer = context.getNamer();
    String outputPath = pathMapper.getOutputPath(service, context.getApiConfig());
    String name = namer.getMockServiceClassName(context.getInterface());

    MockServiceView.Builder mockService = MockServiceView.newBuilder();

    mockService.name(name);
    mockService.serviceImplClassName(namer.getMockGrpcServiceImplName(context.getInterface()));
    mockService.outputPath(namer.getSourceFilePath(outputPath, name));
    mockService.templateFileName(MOCK_SERVICE_FILE);

    // Imports must be done as the last step to catch all imports.
    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    mockService.fileHeader(fileHeader);

    return mockService.build();
  }

  private MockServiceImplView createMockServiceImplView(SurfaceTransformerContext context) {
    addMockServiceImplImports(context);

    Interface service = context.getInterface();
    SurfaceNamer namer = context.getNamer();
    String outputPath = pathMapper.getOutputPath(service, context.getApiConfig());
    String name = namer.getMockGrpcServiceImplName(context.getInterface());
    String grpcClassName =
        context.getTypeTable().getAndSaveNicknameFor(namer.getGrpcServiceClassName(service));

    MockServiceImplView.Builder mockServiceImpl = MockServiceImplView.newBuilder();

    mockServiceImpl.name(name);
    mockServiceImpl.grpcMethods(createGrpcMethodViews(context));
    mockServiceImpl.grpcClassName(grpcClassName);
    mockServiceImpl.outputPath(namer.getSourceFilePath(outputPath, name));
    mockServiceImpl.templateFileName(MOCK_SERVICE_IMPL_FILE);

    // Imports must be done as the last step to catch all imports.
    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    mockServiceImpl.fileHeader(fileHeader);

    return mockServiceImpl.build();
  }

  private List<MockGrpcMethodView> createGrpcMethodViews(SurfaceTransformerContext context) {
    ArrayList<MockGrpcMethodView> testCaseViews = new ArrayList<>();
    for (Method method : context.getInterface().getMethods()) {
      MethodTransformerContext methodContext = context.asRequestMethodContext(method);
      testCaseViews.add(createGrpcMethodView(methodContext));
    }
    return testCaseViews;
  }

  private MockGrpcMethodView createGrpcMethodView(MethodTransformerContext methodContext) {
    Method method = methodContext.getMethod();
    String requestTypeName =
        methodContext.getTypeTable().getAndSaveNicknameFor(method.getInputType());
    String responseTypeName =
        methodContext.getTypeTable().getAndSaveNicknameFor(method.getOutputType());

    return MockGrpcMethodView.newBuilder()
        .name(
            methodContext
                .getNamer()
                .getApiMethodName(method, methodContext.getMethodConfig().getVisibility()))
        .requestTypeName(requestTypeName)
        .responseTypeName(responseTypeName)
        .grpcStreamingType(methodContext.getMethodConfig().getGrpcStreamingType())
        .build();
  }

  /////////////////////////////////// General Helpers //////////////////////////////////////

  private Iterable<Interface> getGrpcInterfacesToMock(Model model, ApiConfig apiConfig) {
    Map<String, Interface> interfaces = new LinkedHashMap<>();

    for (Interface service : new InterfaceView().getElementIterable(model)) {
      if (!service.isReachable()) {
        continue;
      }
      interfaces.put(service.getFullName(), service);
      InterfaceConfig interfaceConfig = apiConfig.getInterfaceConfig(service);
      for (MethodConfig methodConfig : interfaceConfig.getMethodConfigs()) {
        String reroute = methodConfig.getRerouteToGrpcInterface();
        if (!Strings.isNullOrEmpty(reroute)) {
          Interface targetInterface = model.getSymbolTable().lookupInterface(reroute);
          interfaces.put(reroute, targetInterface);
        }
      }
    }

    return interfaces.values();
  }

  private SurfaceTransformerContext createContext(Interface service, ApiConfig apiConfig) {
    ModelTypeTable typeTable =
        new ModelTypeTable(
            new JavaTypeTable(apiConfig.getPackageName()),
            new JavaModelTypeNameConverter(apiConfig.getPackageName()));
    return SurfaceTransformerContext.create(
        service,
        apiConfig,
        typeTable,
        new JavaSurfaceNamer(apiConfig.getPackageName()),
        new JavaFeatureConfig());
  }

  /////////////////////////////////// Imports //////////////////////////////////////

  private void addUnitTestImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("org.junit.After");
    typeTable.saveNicknameFor("org.junit.AfterClass");
    typeTable.saveNicknameFor("org.junit.Assert");
    typeTable.saveNicknameFor("org.junit.Before");
    typeTable.saveNicknameFor("org.junit.BeforeClass");
    typeTable.saveNicknameFor("org.junit.Test");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.util.ArrayList");
    typeTable.saveNicknameFor("java.util.Arrays");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("com.google.api.gax.testing.MockServiceHelper");
    typeTable.saveNicknameFor("com.google.api.gax.testing.MockGrpcService");
    typeTable.saveNicknameFor("com.google.api.gax.core.PagedListResponse");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("com.google.protobuf.GeneratedMessageV3");
  }

  private void addSmokeTestImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("java.util.logging.Level");
    typeTable.saveNicknameFor("java.util.logging.Logger");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("com.google.api.gax.core.PagedListResponse");
    typeTable.saveNicknameFor("org.apache.commons.lang.builder.ReflectionToStringBuilder");
    typeTable.saveNicknameFor("org.apache.commons.lang.builder.ToStringStyle");
  }

  private void addMockServiceImplImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("java.util.ArrayList");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.LinkedList");
    typeTable.saveNicknameFor("java.util.Queue");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("com.google.protobuf.GeneratedMessageV3");
    typeTable.saveNicknameFor("io.grpc.stub.StreamObserver");
  }

  private void addMockServiceImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("com.google.api.gax.testing.MockGrpcService");
    typeTable.saveNicknameFor("com.google.protobuf.GeneratedMessageV3");
    typeTable.saveNicknameFor("io.grpc.ServerServiceDefinition");
  }

  private void addGrpcStreamingTestImport(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("com.google.api.gax.grpc.StreamingCallable");
    typeTable.saveNicknameFor("com.google.api.gax.testing.MockStreamObserver");
    typeTable.saveNicknameFor("io.grpc.stub.StreamObserver");
  }
}
