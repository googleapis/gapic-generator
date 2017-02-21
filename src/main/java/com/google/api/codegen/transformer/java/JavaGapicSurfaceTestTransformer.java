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
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.MockServiceTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.util.testing.JavaValueProducer;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.FileHeaderView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.ClientTestClassView;
import com.google.api.codegen.viewmodel.testing.ClientTestFileView;
import com.google.api.codegen.viewmodel.testing.MockServiceImplFileView;
import com.google.api.codegen.viewmodel.testing.MockServiceImplView;
import com.google.api.codegen.viewmodel.testing.MockServiceView;
import com.google.api.codegen.viewmodel.testing.SmokeTestClassView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import java.util.ArrayList;
import java.util.List;

/** A subclass of ModelToViewTransformer which translates model into API tests in Java. */
public class JavaGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static String UNIT_TEST_TEMPLATE_FILE = "java/test.snip";
  private static String SMOKE_TEST_TEMPLATE_FILE = "java/smoke_test.snip";
  private static String MOCK_SERVICE_FILE = "java/mock_service.snip";
  private static String MOCK_SERVICE_IMPL_FILE = "java/mock_service_impl.snip";

  private final GapicCodePathMapper pathMapper;
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());
  private final JavaValueProducer valueProducer = new JavaValueProducer();
  private final TestValueGenerator valueGenerator = new TestValueGenerator(valueProducer);
  private final MockServiceTransformer mockServiceTransformer = new MockServiceTransformer();
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);

  public JavaGapicSurfaceTestTransformer(GapicCodePathMapper javaPathMapper) {
    this.pathMapper = javaPathMapper;
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
      views.add(createUnitTestFileView(context));
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        context = createContext(service, apiConfig);
        views.add(createSmokeTestClassView(context));
      }
    }
    for (Interface service : mockServiceTransformer.getGrpcInterfacesToMock(model, apiConfig)) {
      SurfaceTransformerContext context = createContext(service, apiConfig);
      views.add(createMockServiceImplFileView(context));

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
        testCaseTransformer.getSmokeTestFlatteningGroup(
            context.getMethodConfig(method), context.getInterfaceConfig().getSmokeTestConfig());
    MethodTransformerContext methodContext =
        context.asFlattenedMethodContext(method, flatteningGroup);

    SmokeTestClassView.Builder testClass = SmokeTestClassView.newBuilder();
    // TODO: we need to remove testCaseView after we switch to use apiMethodView for smoke test
    TestCaseView testCaseView = testCaseTransformer.createSmokeTestCaseView(methodContext);

    testClass.apiSettingsClassName(namer.getApiSettingsClassName(service));
    testClass.apiClassName(namer.getApiWrapperClassName(service));
    testClass.name(name);
    testClass.outputPath(namer.getSourceFilePath(outputPath, name));
    testClass.templateFileName(SMOKE_TEST_TEMPLATE_FILE);
    // TODO: Java needs to be refactored to use ApiMethodView instead
    testClass.apiMethod(
        new StaticLangApiMethodTransformer().generateFlattenedMethod(methodContext));
    testClass.method(testCaseView);
    testClass.requireProjectId(
        testCaseTransformer.requireProjectIdInSmokeTest(
            testCaseView.initCode(), context.getNamer()));

    // Imports must be done as the last step to catch all imports.
    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    testClass.fileHeader(fileHeader);

    return testClass.build();
  }

  ///////////////////////////////////// Unit Test /////////////////////////////////////////

  private ClientTestFileView createUnitTestFileView(SurfaceTransformerContext context) {
    addUnitTestImports(context);

    Interface service = context.getInterface();
    String outputPath = pathMapper.getOutputPath(service, context.getApiConfig());
    SurfaceNamer namer = context.getNamer();
    String name = namer.getUnitTestClassName(service);

    ClientTestClassView.Builder testClass = ClientTestClassView.newBuilder();
    testClass.apiSettingsClassName(namer.getApiSettingsClassName(service));
    testClass.apiClassName(namer.getApiWrapperClassName(service));
    testClass.name(name);
    testClass.testCases(createTestCaseViews(context));
    testClass.apiHasLongRunningMethods(context.getInterfaceConfig().hasLongRunningOperations());
    testClass.mockServices(
        mockServiceTransformer.createMockServices(
            context.getNamer(), context.getModel(), context.getApiConfig()));

    ClientTestFileView.Builder testFile = ClientTestFileView.newBuilder();
    testFile.testClass(testClass.build());
    testFile.outputPath(namer.getSourceFilePath(outputPath, name));
    testFile.templateFileName(UNIT_TEST_TEMPLATE_FILE);

    // Imports must be done as the last step to catch all imports.
    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    testFile.fileHeader(fileHeader);

    return testFile.build();
  }

  private List<TestCaseView> createTestCaseViews(SurfaceTransformerContext context) {
    ArrayList<TestCaseView> testCaseViews = new ArrayList<>();
    SymbolTable testNameTable = new SymbolTable();
    for (Method method : context.getSupportedMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      if (methodConfig.isGrpcStreaming()) {
        if (methodConfig.getGrpcStreamingType() == GrpcStreamingType.ClientStreaming) {
          //TODO: Add unit test generation for ClientStreaming methods
          // Issue: https://github.com/googleapis/toolkit/issues/946
          continue;
        }
        addGrpcStreamingTestImport(context);
        MethodTransformerContext methodContext = context.asRequestMethodContext(method);
        InitCodeContext initCodeContext =
            initCodeTransformer.createRequestInitCodeContext(
                methodContext,
                new SymbolTable(),
                methodConfig.getRequiredFieldConfigs(),
                InitCodeOutputType.SingleObject,
                valueGenerator);
        testCaseViews.add(
            testCaseTransformer.createTestCaseView(
                methodContext, testNameTable, initCodeContext, ClientMethodType.CallableMethod));
      } else if (methodConfig.isFlattening()) {
        ClientMethodType clientMethodType;
        if (methodConfig.isPageStreaming()) {
          clientMethodType = ClientMethodType.PagedFlattenedMethod;
        } else if (methodConfig.isLongRunningOperation()) {
          clientMethodType = ClientMethodType.AsyncOperationFlattenedMethod;
        } else {
          clientMethodType = ClientMethodType.FlattenedMethod;
        }
        for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
          MethodTransformerContext methodContext =
              context.asFlattenedMethodContext(method, flatteningGroup);
          InitCodeContext initCodeContext =
              initCodeTransformer.createRequestInitCodeContext(
                  methodContext,
                  new SymbolTable(),
                  flatteningGroup.getFlattenedFieldConfigs().values(),
                  InitCodeOutputType.FieldList,
                  valueGenerator);
          testCaseViews.add(
              testCaseTransformer.createTestCaseView(
                  methodContext, testNameTable, initCodeContext, clientMethodType));
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

  ///////////////////////////////////// Mock Service /////////////////////////////////////////

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

  private MockServiceImplFileView createMockServiceImplFileView(SurfaceTransformerContext context) {
    addMockServiceImplImports(context);

    Interface service = context.getInterface();
    SurfaceNamer namer = context.getNamer();
    String outputPath = pathMapper.getOutputPath(service, context.getApiConfig());
    String name = namer.getMockGrpcServiceImplName(context.getInterface());
    String grpcClassName =
        context.getTypeTable().getAndSaveNicknameFor(namer.getGrpcServiceClassName(service));

    MockServiceImplFileView.Builder mockServiceImplFile = MockServiceImplFileView.newBuilder();

    mockServiceImplFile.serviceImpl(
        MockServiceImplView.newBuilder()
            .name(name)
            .grpcClassName(grpcClassName)
            .grpcMethods(mockServiceTransformer.createMockGrpcMethodViews(context))
            .build());

    mockServiceImplFile.outputPath(namer.getSourceFilePath(outputPath, name));
    mockServiceImplFile.templateFileName(MOCK_SERVICE_IMPL_FILE);

    // Imports must be done as the last step to catch all imports.
    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    mockServiceImplFile.fileHeader(fileHeader);

    return mockServiceImplFile.build();
  }

  /////////////////////////////////// General Helpers //////////////////////////////////////

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
        JavaFeatureConfig.newBuilder()
            .enableStringFormatFunctions(apiConfig.getResourceNameMessageConfigs().isEmpty())
            .build());
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
    typeTable.saveNicknameFor("com.google.api.gax.grpc.ApiException");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("com.google.protobuf.GeneratedMessageV3");
    typeTable.saveNicknameFor("io.grpc.Status");
    typeTable.saveNicknameFor("io.grpc.StatusRuntimeException");
    if (context.getInterfaceConfig().hasPageStreamingMethods()) {
      typeTable.saveNicknameFor("com.google.api.gax.core.PagedListResponse");
    }
    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      typeTable.saveNicknameFor("com.google.protobuf.Any");
    }
  }

  private void addSmokeTestImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("java.util.logging.Level");
    typeTable.saveNicknameFor("java.util.logging.Logger");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.Arrays");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("com.google.api.gax.core.PagedListResponse");
    typeTable.saveNicknameFor("org.apache.commons.lang.builder.ReflectionToStringBuilder");
    typeTable.saveNicknameFor("org.apache.commons.lang.builder.ToStringStyle");
    typeTable.saveNicknameFor("org.apache.commons.cli.CommandLine");
    typeTable.saveNicknameFor("org.apache.commons.cli.DefaultParser");
    typeTable.saveNicknameFor("org.apache.commons.cli.HelpFormatter");
    typeTable.saveNicknameFor("org.apache.commons.cli.Option");
    typeTable.saveNicknameFor("org.apache.commons.cli.Options");
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
    typeTable.saveNicknameFor("com.google.api.gax.core.RpcStreamObserver");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.StreamingCallable");
    typeTable.saveNicknameFor("com.google.api.gax.testing.MockStreamObserver");
    typeTable.saveNicknameFor("java.util.concurrent.ExecutionException");
  }
}
