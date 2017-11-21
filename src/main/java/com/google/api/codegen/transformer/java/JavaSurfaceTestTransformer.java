/* Copyright 2016 Google LLC
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

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.ApiSource;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.MockServiceTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformer;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.StandardValueProducer;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.util.testing.ValueProducer;
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
import java.util.ArrayList;
import java.util.List;

/** A subclass of ModelToViewTransformer which translates model into API tests in Java. */
public class JavaSurfaceTestTransformer implements ModelToViewTransformer {
  private static String UNIT_TEST_TEMPLATE_FILE = "java/test.snip";
  private static String SMOKE_TEST_TEMPLATE_FILE = "java/smoke_test.snip";
  private static String MOCK_SERVICE_FILE = "java/mock_service.snip";
  private static String MOCK_SERVICE_IMPL_FILE = "java/mock_service_impl.snip";

  private final SurfaceTransformer surfaceTransformer;
  private final GapicCodePathMapper pathMapper;
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());
  private final ValueProducer valueProducer = new StandardValueProducer();
  private final TestValueGenerator valueGenerator = new TestValueGenerator(valueProducer);
  private final MockServiceTransformer mockServiceTransformer = new MockServiceTransformer();
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);

  public JavaSurfaceTestTransformer(GapicCodePathMapper javaPathMapper, SurfaceTransformer surfaceTransformer) {
    this.surfaceTransformer = surfaceTransformer;
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
  public List<ViewModel> transform(ApiModel model, GapicProductConfig productConfig) {
    SurfaceNamer namer = surfaceTransformer.createSurfaceNamer(productConfig);
      boolean enableStringFormatFunctions = productConfig.getResourceNameMessageConfigs().isEmpty();

    List<ViewModel> views = new ArrayList<>();
    for (InterfaceModel apiInterface : model.getInterfaces(productConfig)) {
      ImportTypeTable typeTable =
          surfaceTransformer.createTypeTable(productConfig.getPackageName());
      InterfaceContext context = surfaceTransformer.createInterfaceContext(apiInterface, productConfig, namer, typeTable, enableStringFormatFunctions);
      views.add(createUnitTestFileView(context));
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        context = surfaceTransformer.createInterfaceContext(apiInterface, productConfig, namer, typeTable.cloneEmpty(), enableStringFormatFunctions);
        views.add(createSmokeTestClassView(context));
      }
    }

    for (InterfaceModel apiInterface :
        mockServiceTransformer.getGrpcInterfacesToMock(model, productConfig)) {
      ImportTypeTable typeTable =
          surfaceTransformer.createTypeTable(productConfig.getPackageName());
      InterfaceContext context =  surfaceTransformer.createInterfaceContext(apiInterface, productConfig, namer, typeTable, enableStringFormatFunctions);
      views.add(createMockServiceImplFileView(context));

      context =  surfaceTransformer.createInterfaceContext(apiInterface, productConfig, namer, typeTable.cloneEmpty(), enableStringFormatFunctions);
      views.add(createMockServiceView(context));
    }
    return views;
  }

  ///////////////////////////////////// Smoke Test ///////////////////////////////////////

  private SmokeTestClassView createSmokeTestClassView(InterfaceContext context) {
    String outputPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    SurfaceNamer namer = context.getNamer();
    String name = namer.getSmokeTestClassName(context.getInterfaceConfig());

    SmokeTestClassView.Builder testClass = createSmokeTestClassViewBuilder(context);
    testClass.name(name);
    testClass.outputPath(namer.getSourceFilePath(outputPath, name));
    return testClass.build();
  }

  /**
   * Package-private
   *
   * <p>A helper method that creates a partially initialized builder that can be customized and
   * build the smoke test class view later.
   */
  SmokeTestClassView.Builder createSmokeTestClassViewBuilder(InterfaceContext context) {
    addSmokeTestImports(context);

    MethodModel method = context.getInterfaceConfig().getSmokeTestConfig().getMethod();
    SurfaceNamer namer = context.getNamer();

    FlatteningConfig flatteningGroup =
        testCaseTransformer.getSmokeTestFlatteningGroup(
            context.getMethodConfig(method), context.getInterfaceConfig().getSmokeTestConfig());
    MethodContext methodContext = context.asFlattenedMethodContext(method, flatteningGroup);

    SmokeTestClassView.Builder testClass = SmokeTestClassView.newBuilder();
    // TODO: we need to remove testCaseView after we switch to use apiMethodView for smoke test
    TestCaseView testCaseView = testCaseTransformer.createSmokeTestCaseView(methodContext);

    testClass.apiSettingsClassName(namer.getApiSettingsClassName(context.getInterfaceConfig()));
    testClass.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
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

    return testClass;
  }

  ///////////////////////////////////// Unit Test /////////////////////////////////////////

  private ClientTestFileView createUnitTestFileView(InterfaceContext context) {
    addUnitTestImports(context);

    String outputPath =
        pathMapper.getOutputPath(context.getInterfaceModel().getFullName(), context.getProductConfig());
    SurfaceNamer namer = context.getNamer();
    String name = namer.getUnitTestClassName(context.getInterfaceConfig());

    ClientTestClassView.Builder testClass = ClientTestClassView.newBuilder();
    testClass.apiSettingsClassName(namer.getApiSettingsClassName(context.getInterfaceConfig()));
    testClass.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    testClass.name(name);
    testClass.testCases(createTestCaseViews(context));
    testClass.apiHasLongRunningMethods(context.getInterfaceConfig().hasLongRunningOperations());
    testClass.mockServices(
        mockServiceTransformer.createMockServices(
            context.getNamer(), context.getApiModel(), (GapicProductConfig) context.getProductConfig()));

    testClass.missingDefaultServiceAddress(
        !context.getInterfaceConfig().hasDefaultServiceAddress());
    testClass.missingDefaultServiceScopes(!context.getInterfaceConfig().hasDefaultServiceScopes());

    ClientTestFileView.Builder testFile = ClientTestFileView.newBuilder();
    testFile.testClass(testClass.build());
    testFile.outputPath(namer.getSourceFilePath(outputPath, name));
    testFile.templateFileName(UNIT_TEST_TEMPLATE_FILE);

    // Imports must be done as the last step to catch all imports.
    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    testFile.fileHeader(fileHeader);

    return testFile.build();
  }

  private List<TestCaseView> createTestCaseViews(InterfaceContext context) {
    ArrayList<TestCaseView> testCaseViews = new ArrayList<>();
    SymbolTable testNameTable = new SymbolTable();
    for (MethodModel method : context.getSupportedMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      if (methodConfig.isGrpcStreaming()) {
        if (methodConfig.getGrpcStreamingType() == GrpcStreamingType.ClientStreaming) {
          //TODO: Add unit test generation for ClientStreaming methods
          // Issue: https://github.com/googleapis/toolkit/issues/946
          continue;
        }
        addGrpcStreamingTestImports(context, methodConfig.getGrpcStreamingType());
        MethodContext methodContext = context.asRequestMethodContext(method);
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
          MethodContext methodContext =
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

  private MockServiceView createMockServiceView(InterfaceContext context) {
    addMockServiceImports(context);

    SurfaceNamer namer = context.getNamer();
    String outputPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    String name = namer.getMockServiceClassName(context.getInterfaceModel());

    MockServiceView.Builder mockService = MockServiceView.newBuilder();

    mockService.name(name);
    mockService.serviceImplClassName(namer.getMockGrpcServiceImplName(context.getInterfaceModel()));
    mockService.outputPath(namer.getSourceFilePath(outputPath, name));
    mockService.templateFileName(MOCK_SERVICE_FILE);

    // Imports must be done as the last step to catch all imports.
    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    mockService.fileHeader(fileHeader);

    return mockService.build();
  }

  private MockServiceImplFileView createMockServiceImplFileView(InterfaceContext context) {
    addMockServiceImplImports(context);

    SurfaceNamer namer = context.getNamer();
    String outputPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    String name = namer.getMockGrpcServiceImplName(context.getInterfaceModel());
    String grpcClassName =
        context
            .getImportTypeTable()
            .getAndSaveNicknameFor(namer.getGrpcServiceClassName(context.getInterfaceModel()));

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

  /** Package-private */
  InterfaceContext createContext(
      InterfaceModel apiInterface, GapicProductConfig productConfig) {
    return surfaceTransformer.createInterfaceContext(apiInterface, productConfig, surfaceTransformer.createSurfaceNamer(productConfig), surfaceTransformer.createTypeTable(productConfig.getPackageName()), productConfig.getResourceNameMessageConfigs().isEmpty() );
  }


  /////////////////////////////////// Imports //////////////////////////////////////

  private void addUnitTestImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    typeTable.saveNicknameFor("com.google.api.gax.core.NoCredentialsProvider");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.InvalidArgumentException");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.StatusCode");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.GrpcStatusCode");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.testing.MockGrpcService");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.testing.MockServiceHelper");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("com.google.protobuf.GeneratedMessageV3");
    typeTable.saveNicknameFor("io.grpc.Status");
    typeTable.saveNicknameFor("io.grpc.StatusRuntimeException");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.util.ArrayList");
    typeTable.saveNicknameFor("java.util.Arrays");
    typeTable.saveNicknameFor("java.util.concurrent.ExecutionException");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.Objects");
    typeTable.saveNicknameFor("org.junit.After");
    typeTable.saveNicknameFor("org.junit.AfterClass");
    typeTable.saveNicknameFor("org.junit.Assert");
    typeTable.saveNicknameFor("org.junit.Before");
    typeTable.saveNicknameFor("org.junit.BeforeClass");
    typeTable.saveNicknameFor("org.junit.Test");
    if (context.getInterfaceConfig().hasPageStreamingMethods()) {
      typeTable.saveNicknameFor("com.google.api.gax.core.PagedListResponse");
    }
    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      typeTable.saveNicknameFor("com.google.protobuf.Any");
    }
  }

  /** package-private */
  void addSmokeTestImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();
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

  private void addMockServiceImplImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    typeTable.saveNicknameFor("java.util.ArrayList");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.LinkedList");
    typeTable.saveNicknameFor("java.util.Queue");
    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("com.google.protobuf.GeneratedMessageV3");
    typeTable.saveNicknameFor("io.grpc.stub.StreamObserver");
  }

  private void addMockServiceImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.testing.MockGrpcService");
    typeTable.saveNicknameFor("com.google.protobuf.GeneratedMessageV3");
    typeTable.saveNicknameFor("io.grpc.ServerServiceDefinition");
  }

  private void addGrpcStreamingTestImports(
      InterfaceContext context, GrpcStreamingType streamingType) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    if (context.getApiModel().getApiSource().equals(ApiSource.PROTO)) {
      typeTable.saveNicknameFor("com.google.api.gax.grpc.testing.MockStreamObserver");
      typeTable.saveNicknameFor("com.google.api.gax.rpc.ApiStreamObserver");
      switch (streamingType) {
        case BidiStreaming:
          typeTable.saveNicknameFor("com.google.api.gax.rpc.BidiStreamingCallable");
          break;
        case ClientStreaming:
          typeTable.saveNicknameFor("com.google.api.gax.rpc.ClientStreamingCallable");
          break;
        case ServerStreaming:
          typeTable.saveNicknameFor("com.google.api.gax.rpc.ServerStreamingCallable");
          break;
        default:
          throw new IllegalArgumentException("Invalid streaming type: " + streamingType);
      }
    }
  }
}
