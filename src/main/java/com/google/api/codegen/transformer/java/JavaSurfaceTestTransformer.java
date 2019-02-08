/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.TransportProtocol;
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
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.testing.StandardValueProducer;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.FileHeaderView;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
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

/** A subclass of ModelToViewTransformer which translates an ApiModel into API tests in Java. */
public class JavaSurfaceTestTransformer<ApiModelT extends ApiModel>
    implements ModelToViewTransformer<ApiModelT> {

  private static String SMOKE_TEST_TEMPLATE_FILE = "java/smoke_test.snip";
  private static String MOCK_SERVICE_FILE = "java/mock_service.snip";
  private static String MOCK_SERVICE_IMPL_FILE = "java/mock_service_impl.snip";

  private final String unitTestTemplateFile;
  private final SurfaceTransformer surfaceTransformer;
  private final GapicCodePathMapper pathMapper;
  private final MockServiceTransformer mockServiceTransformer = new MockServiceTransformer();
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());
  private final ValueProducer valueProducer = new StandardValueProducer();
  private final TestValueGenerator valueGenerator = new TestValueGenerator(valueProducer);
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);
  private final StaticLangApiMethodTransformer apiMethodTransformer =
      new StaticLangApiMethodTransformer();

  public JavaSurfaceTestTransformer(
      GapicCodePathMapper javaPathMapper,
      SurfaceTransformer surfaceTransformer,
      String unitTestTemplateFile) {
    this.surfaceTransformer = surfaceTransformer;
    this.pathMapper = javaPathMapper;
    this.unitTestTemplateFile = unitTestTemplateFile;
  }

  @Override
  public List<String> getTemplateFileNames() {
    List<String> fileNames = new ArrayList<>();
    fileNames.add(unitTestTemplateFile);
    fileNames.add(SMOKE_TEST_TEMPLATE_FILE);
    fileNames.add(MOCK_SERVICE_IMPL_FILE);
    fileNames.add(MOCK_SERVICE_FILE);
    return fileNames;
  }

  @Override
  public List<ViewModel> transform(ApiModelT model, GapicProductConfig productConfig) {
    SurfaceNamer namer = surfaceTransformer.createSurfaceNamer(productConfig);

    List<ViewModel> views = new ArrayList<>();
    for (InterfaceModel apiInterface : model.getInterfaces(productConfig)) {
      if (!productConfig.hasInterfaceConfig(apiInterface)) {
        continue;
      }

      ImportTypeTable typeTable =
          surfaceTransformer.createTypeTable(productConfig.getPackageName());
      InterfaceContext context =
          surfaceTransformer.createInterfaceContext(apiInterface, productConfig, namer, typeTable);
      views.add(createUnitTestFileView(context));
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        context =
            surfaceTransformer.createInterfaceContext(
                apiInterface, productConfig, namer, typeTable.cloneEmpty());
        views.add(createSmokeTestClassView(context));
      }
    }

    for (InterfaceModel apiInterface :
        mockServiceTransformer.getGrpcInterfacesToMock(model, productConfig)) {
      ImportTypeTable typeTable =
          surfaceTransformer.createTypeTable(productConfig.getPackageName());
      InterfaceContext context =
          surfaceTransformer.createInterfaceContext(apiInterface, productConfig, namer, typeTable);
      views.add(createMockServiceImplFileView(context));

      context =
          surfaceTransformer.createInterfaceContext(
              apiInterface, productConfig, namer, typeTable.cloneEmpty());
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
    if (FlatteningConfig.hasAnyRepeatedResourceNameParameter(flatteningGroup)) {
      methodContext = methodContext.withResourceNamesInSamplesOnly();
    }

    SmokeTestClassView.Builder testClass = SmokeTestClassView.newBuilder();
    StaticLangApiMethodView apiMethodView = createSmokeTestCaseApiMethodView(methodContext);

    testClass.apiSettingsClassName(namer.getApiSettingsClassName(context.getInterfaceConfig()));
    testClass.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    testClass.templateFileName(SMOKE_TEST_TEMPLATE_FILE);
    testClass.apiMethod(apiMethodView);
    testClass.requireProjectId(
        testCaseTransformer.requireProjectIdInSmokeTest(
            apiMethodView.initCode(), context.getNamer()));

    // Imports must be done as the last step to catch all imports.
    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    testClass.fileHeader(fileHeader);

    return testClass;
  }

  private StaticLangApiMethodView createSmokeTestCaseApiMethodView(MethodContext methodContext) {
    MethodConfig methodConfig = methodContext.getMethodConfig();
    StaticLangApiMethodView initialApiMethodView;
    if (methodConfig.isPageStreaming()) {
      if (methodContext.isFlattenedMethodContext()) {
        initialApiMethodView = apiMethodTransformer.generatePagedFlattenedMethod(methodContext);
      } else {
        throw new UnsupportedOperationException(
            "Unsupported smoke test type: page-streaming + request-object");
      }
    } else if (methodConfig.isGrpcStreaming()) {
      throw new UnsupportedOperationException("Unsupported smoke test type: grpc-streaming");
    } else if (methodConfig.isLongRunningOperation()) {
      if (methodContext.isFlattenedMethodContext()) {
        initialApiMethodView =
            apiMethodTransformer.generateAsyncOperationFlattenedMethod(methodContext);
      } else {
        throw new UnsupportedOperationException(
            "Unsupported smoke test type: long-running + request-object");
      }
    } else {
      if (methodContext.isFlattenedMethodContext()) {
        initialApiMethodView = apiMethodTransformer.generateFlattenedMethod(methodContext);
      } else {
        throw new UnsupportedOperationException(
            "Unsupported smoke test type: simple-call + request-object");
      }
    }

    StaticLangApiMethodView.Builder apiMethodView = initialApiMethodView.toBuilder();
    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(
            methodContext, testCaseTransformer.createSmokeTestInitContext(methodContext));
    apiMethodView.initCode(initCodeView);
    return apiMethodView.build();
  }

  ///////////////////////////////////// Unit Test /////////////////////////////////////////

  private ClientTestFileView createUnitTestFileView(InterfaceContext context) {
    addUnitTestImports(context);

    String outputPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    SurfaceNamer namer = context.getNamer();
    String name = namer.getUnitTestClassName(context.getInterfaceConfig());

    ClientTestClassView.Builder testClass = ClientTestClassView.newBuilder();
    testClass.apiSettingsClassName(namer.getApiSettingsClassName(context.getInterfaceConfig()));
    testClass.apiStubSettingsClassName(
        namer.getApiStubSettingsClassName(context.getInterfaceConfig()));
    testClass.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    testClass.name(name);
    testClass.testCases(createTestCaseViews(context));

    testClass.mockServices(
        mockServiceTransformer.createMockServices(
            context.getNamer(), context.getApiModel(), context.getProductConfig()));

    testClass.missingDefaultServiceAddress(
        !context.getInterfaceConfig().hasDefaultServiceAddress());
    testClass.missingDefaultServiceScopes(!context.getInterfaceConfig().hasDefaultServiceScopes());

    ClientTestFileView.Builder testFile = ClientTestFileView.newBuilder();
    testFile.testClass(testClass.build());
    testFile.outputPath(namer.getSourceFilePath(outputPath, name));
    testFile.templateFileName(unitTestTemplateFile);

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
          // TODO: Add unit test generation for ClientStreaming methods
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
          MethodContext methodContext = context.asFlattenedMethodContext(method, flatteningGroup);
          if (FlatteningConfig.hasAnyRepeatedResourceNameParameter(flatteningGroup)) {
            methodContext = methodContext.withResourceNamesInSamplesOnly();
            flatteningGroup = methodContext.getFlatteningConfig();
          }
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
            .mockGrpcClassName(grpcClassName)
            .grpcMethods(mockServiceTransformer.createMockGrpcMethodViews(context))
            .build());

    mockServiceImplFile.outputPath(namer.getSourceFilePath(outputPath, name));
    mockServiceImplFile.templateFileName(MOCK_SERVICE_IMPL_FILE);

    // Imports must be done as the last step to catch all imports.
    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    mockServiceImplFile.fileHeader(fileHeader);

    return mockServiceImplFile.build();
  }

  /////////////////////////////////// Imports //////////////////////////////////////

  private void addUnitTestImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    typeTable.saveNicknameFor("com.google.api.gax.core.NoCredentialsProvider");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.InvalidArgumentException");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.util.Arrays");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("org.junit.After");
    typeTable.saveNicknameFor("org.junit.AfterClass");
    typeTable.saveNicknameFor("org.junit.Assert");
    typeTable.saveNicknameFor("org.junit.BeforeClass");
    typeTable.saveNicknameFor("org.junit.Test");
    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      typeTable.saveNicknameFor("com.google.protobuf.Any");
    }
    switch (context.getProductConfig().getTransportProtocol()) {
      case GRPC:
        typeTable.saveNicknameFor("com.google.api.gax.rpc.ApiClientHeaderProvider");
        typeTable.saveNicknameFor("com.google.api.gax.rpc.StatusCode");
        typeTable.saveNicknameFor("com.google.api.gax.grpc.GaxGrpcProperties");
        typeTable.saveNicknameFor("com.google.api.gax.grpc.GrpcStatusCode");
        typeTable.saveNicknameFor("com.google.api.gax.grpc.testing.LocalChannelProvider");
        typeTable.saveNicknameFor("com.google.api.gax.grpc.testing.MockGrpcService");
        typeTable.saveNicknameFor("com.google.api.gax.grpc.testing.MockServiceHelper");
        typeTable.saveNicknameFor("com.google.protobuf.GeneratedMessageV3");
        typeTable.saveNicknameFor("io.grpc.Status");
        typeTable.saveNicknameFor("io.grpc.StatusRuntimeException");
        typeTable.saveNicknameFor("java.util.ArrayList");
        typeTable.saveNicknameFor("java.util.Objects");
        typeTable.saveNicknameFor("java.util.concurrent.ExecutionException");
        typeTable.saveNicknameFor("org.junit.Before");
        break;
      case HTTP:
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.ApiMethodDescriptor");
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.GaxHttpJsonProperties");
        typeTable.saveNicknameFor("com.google.api.gax.httpjson.testing.MockHttpService");
        typeTable.saveNicknameFor("com.google.api.gax.rpc.ApiClientHeaderProvider");
        typeTable.saveNicknameFor("com.google.api.gax.rpc.ApiException");
        typeTable.saveNicknameFor("com.google.api.gax.rpc.ApiExceptionFactory");
        typeTable.saveNicknameFor("com.google.api.gax.rpc.StatusCode.Code");
        typeTable.saveNicknameFor("com.google.api.gax.rpc.testing.FakeStatusCode");
        typeTable.saveNicknameFor("com.google.common.collect.ImmutableList");
        typeTable.saveNicknameFor("java.util.Map");
        typeTable.saveNicknameFor("java.util.HashMap");

        // Import stub settings class in unit test file.
        SurfaceNamer stubNamer =
            context.getNamer().cloneWithPackageName(context.getNamer().getStubPackageName());
        TypeName rpcStubClassName =
            stubNamer
                .getTypeNameConverter()
                .getTypeNameInImplicitPackage(
                    stubNamer.getApiStubSettingsClassName(context.getInterfaceConfig()));
        rpcStubClassName.getAndSaveNicknameIn(typeTable.getTypeTable());
        break;
    }
  }

  /** package-private */
  void addSmokeTestImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    typeTable.saveNicknameFor("java.util.logging.Level");
    typeTable.saveNicknameFor("java.util.logging.Logger");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.Arrays");
    typeTable.saveNicknameFor("com.google.common.base.Preconditions");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("org.junit.Test");
  }

  private void addMockServiceImplImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    typeTable.saveNicknameFor("java.util.ArrayList");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.LinkedList");
    typeTable.saveNicknameFor("java.util.Queue");
    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    if (context.getProductConfig().getTransportProtocol().equals(TransportProtocol.GRPC)) {
      typeTable.saveNicknameFor("com.google.protobuf.GeneratedMessageV3");
      typeTable.saveNicknameFor("io.grpc.stub.StreamObserver");
    }
  }

  private void addMockServiceImports(InterfaceContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.testing.MockGrpcService");
    if (context.getProductConfig().getTransportProtocol().equals(TransportProtocol.GRPC)) {
      typeTable.saveNicknameFor("com.google.protobuf.GeneratedMessageV3");
      typeTable.saveNicknameFor("io.grpc.ServerServiceDefinition");
    }
  }

  private void addGrpcStreamingTestImports(
      InterfaceContext context, GrpcStreamingType streamingType) {
    ImportTypeTable typeTable = context.getImportTypeTable();
    if (context.getProductConfig().getTransportProtocol().equals(TransportProtocol.GRPC)) {
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
