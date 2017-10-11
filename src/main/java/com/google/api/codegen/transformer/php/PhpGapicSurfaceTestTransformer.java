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
package com.google.api.codegen.transformer.php;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.php.PhpGapicCodePathMapper;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MockServiceTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.api.codegen.util.testing.StandardValueProducer;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.FileHeaderView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.ClientTestClassView;
import com.google.api.codegen.viewmodel.testing.ClientTestFileView;
import com.google.api.codegen.viewmodel.testing.MockGrpcMethodView;
import com.google.api.codegen.viewmodel.testing.MockServiceImplFileView;
import com.google.api.codegen.viewmodel.testing.MockServiceImplView;
import com.google.api.codegen.viewmodel.testing.SmokeTestClassView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/** Responsible for producing testing related views for PHP. */
public class PhpGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static final String MOCK_SERVICE_IMPL_TEMPLATE_FILE = "php/mock_service.snip";
  private static final String SMOKE_TEST_TEMPLATE_FILE = "php/smoke_test.snip";
  private static final String UNIT_TEST_TEMPLATE_FILE = "php/test.snip";

  private final PhpImportSectionTransformer importSectionTransformer =
      new PhpImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);

  private final ValueProducer valueProducer = new StandardValueProducer();
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);
  private final TestValueGenerator valueGenerator = new TestValueGenerator(valueProducer);

  private final PhpFeatureConfig featureConfig = new PhpFeatureConfig();
  private final MockServiceTransformer mockServiceTransformer = new MockServiceTransformer();

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(
        MOCK_SERVICE_IMPL_TEMPLATE_FILE, SMOKE_TEST_TEMPLATE_FILE, UNIT_TEST_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    List<ViewModel> views = new ArrayList<>();
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      GapicInterfaceContext context =
          createContext(apiInterface, productConfig, PhpSurfaceNamer.TestKind.UNIT);
      views.add(createUnitTestFileView(context));
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        context = createContext(apiInterface, productConfig, PhpSurfaceNamer.TestKind.SYSTEM);
        views.add(createSmokeTestClassView(context));
      }
    }
    for (Interface apiInterface :
        mockServiceTransformer.getGrpcInterfacesToMock(model, productConfig)) {
      GapicInterfaceContext context =
          createContext(apiInterface, productConfig, PhpSurfaceNamer.TestKind.UNIT);
      views.add(createMockServiceImplView(context));
    }
    return views;
  }

  private GapicInterfaceContext createContext(
      Interface apiInterface, GapicProductConfig productConfig, PhpSurfaceNamer.TestKind testKind) {
    PhpSurfaceNamer surfacePackageNamer = new PhpSurfaceNamer(productConfig.getPackageName());
    String testPackageName = surfacePackageNamer.getTestPackageName(testKind);
    ModelTypeTable typeTable =
        new ModelTypeTable(
            new PhpTypeTable(testPackageName), new PhpModelTypeNameConverter(testPackageName));
    return GapicInterfaceContext.create(
        apiInterface, productConfig, typeTable, surfacePackageNamer, featureConfig);
  }

  private ClientTestFileView createUnitTestFileView(GapicInterfaceContext context) {
    addUnitTestImports(context.getModelTypeTable());

    String outputPath =
        PhpGapicCodePathMapper.newBuilder()
            .setPrefix("tests/unit")
            .build()
            .getOutputPath(context.getInterface(), context.getProductConfig());
    SurfaceNamer namer = context.getNamer();
    String name = namer.getUnitTestClassName(context.getInterfaceConfig());

    ClientTestClassView.Builder testClass = ClientTestClassView.newBuilder();
    testClass.apiSettingsClassName(
        namer.getNotImplementedString(
            "PhpGapicSurfaceTestTransformer.generateTestView - apiSettingsClassName"));
    testClass.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    testClass.name(name);
    testClass.apiName(
        PhpPackageMetadataNamer.getApiNameFromPackageName(namer.getPackageName())
            .toLowerUnderscore());
    testClass.testCases(createTestCaseViews(context));
    testClass.apiHasLongRunningMethods(context.getInterfaceConfig().hasLongRunningOperations());
    // Add gRPC client imports.
    for (Interface grpcInterface :
        mockServiceTransformer
            .getGrpcInterfacesForService(
                context.getModel(), context.getProductConfig(), context.getInterface())
            .values()) {
      context.getModelTypeTable().getAndSaveNicknameFor(namer.getGrpcClientTypeName(grpcInterface));
    }
    testClass.mockServices(
        mockServiceTransformer.createMockServices(
            context.getNamer(), context.getModel(), context.getProductConfig()));
    testClass.missingDefaultServiceAddress(
        !context.getInterfaceConfig().hasDefaultServiceAddress());
    testClass.missingDefaultServiceScopes(!context.getInterfaceConfig().hasDefaultServiceScopes());

    ClientTestFileView.Builder testFile = ClientTestFileView.newBuilder();
    testFile.testClass(testClass.build());
    testFile.outputPath(namer.getSourceFilePath(outputPath, name));
    testFile.templateFileName(UNIT_TEST_TEMPLATE_FILE);

    ImportSectionView importSection =
        importSectionTransformer.generateImportSection(context.getModelTypeTable().getImports());
    PhpSurfaceNamer testPackageNamer =
        new PhpSurfaceNamer(namer.getTestPackageName(PhpSurfaceNamer.TestKind.UNIT));
    FileHeaderView fileHeader =
        fileHeaderTransformer.generateFileHeader(
            context.getProductConfig(), importSection, testPackageNamer);
    testFile.fileHeader(fileHeader);

    return testFile.build();
  }

  private List<TestCaseView> createTestCaseViews(GapicInterfaceContext context) {
    ArrayList<TestCaseView> testCaseViews = new ArrayList<>();
    SymbolTable testNameTable = new SymbolTable();
    for (Method method : context.getSupportedMethods()) {
      GapicMethodContext methodContext = context.asRequestMethodContext(method);

      if (methodContext.getMethodConfig().getGrpcStreamingType()
          == GrpcStreamingType.ClientStreaming) {
        // TODO: Add unit test generation for ClientStreaming methods
        // Issue: https://github.com/googleapis/toolkit/issues/946
        continue;
      }

      InitCodeOutputType initCodeOutputType = InitCodeOutputType.FieldList;
      if (methodContext.getMethodConfig().getGrpcStreamingType()
          == GrpcStreamingType.BidiStreaming) {
        initCodeOutputType = InitCodeOutputType.SingleObject;
      }

      ClientMethodType clientMethodType = ClientMethodType.OptionalArrayMethod;
      if (methodContext.getMethodConfig().isLongRunningOperation()) {
        clientMethodType = ClientMethodType.OperationOptionalArrayMethod;
      } else if (methodContext.getMethodConfig().isPageStreaming()) {
        clientMethodType = ClientMethodType.PagedOptionalArrayMethod;
      }

      Iterable<FieldConfig> fieldConfigs =
          methodContext.getMethodConfig().getRequiredFieldConfigs();
      InitCodeContext initCodeContext =
          InitCodeContext.newBuilder()
              .initObjectType(methodContext.getMethod().getInputType())
              .suggestedName(Name.from("request"))
              .initFieldConfigStrings(methodContext.getMethodConfig().getSampleCodeInitFields())
              .initValueConfigMap(InitCodeTransformer.createCollectionMap(methodContext))
              .initFields(FieldConfig.toFieldIterable(fieldConfigs))
              .outputType(initCodeOutputType)
              .fieldConfigMap(FieldConfig.toFieldConfigMap(fieldConfigs))
              .valueGenerator(valueGenerator)
              .build();

      testCaseViews.add(
          testCaseTransformer.createTestCaseView(
              methodContext, testNameTable, initCodeContext, clientMethodType));
    }
    return testCaseViews;
  }

  private MockServiceImplFileView createMockServiceImplView(GapicInterfaceContext context) {
    addUnitTestImports(context.getModelTypeTable());

    Interface apiInterface = context.getInterface();
    SurfaceNamer namer = context.getNamer();
    String outputPath =
        PhpGapicCodePathMapper.newBuilder()
            .setPrefix("tests/unit")
            .build()
            .getOutputPath(context.getInterface(), context.getProductConfig());
    String name = namer.getMockGrpcServiceImplName(context.getInterface());
    String grpcClassName =
        context
            .getModelTypeTable()
            .getAndSaveNicknameFor(namer.getGrpcClientTypeName(apiInterface));

    MockServiceImplFileView.Builder mockServiceImplFile = MockServiceImplFileView.newBuilder();

    mockServiceImplFile.serviceImpl(
        MockServiceImplView.newBuilder()
            .name(name)
            .grpcClassName(grpcClassName)
            .grpcMethods(new ArrayList<MockGrpcMethodView>())
            .build());

    mockServiceImplFile.outputPath(namer.getSourceFilePath(outputPath, name));
    mockServiceImplFile.templateFileName(MOCK_SERVICE_IMPL_TEMPLATE_FILE);

    ImportSectionView importSection =
        importSectionTransformer.generateImportSection(context.getModelTypeTable().getImports());
    PhpSurfaceNamer testPackageNamer =
        new PhpSurfaceNamer(namer.getTestPackageName(SurfaceNamer.TestKind.UNIT));
    FileHeaderView fileHeader =
        fileHeaderTransformer.generateFileHeader(
            context.getProductConfig(), importSection, testPackageNamer);
    mockServiceImplFile.fileHeader(fileHeader);

    return mockServiceImplFile.build();
  }

  private SmokeTestClassView createSmokeTestClassView(GapicInterfaceContext context) {
    String outputPath =
        PhpGapicCodePathMapper.newBuilder()
            .setPrefix("tests/system")
            .build()
            .getOutputPath(context.getInterface(), context.getProductConfig());
    SurfaceNamer namer = context.getNamer();
    String name = namer.getSmokeTestClassName(context.getInterfaceConfig());

    SmokeTestClassView.Builder testClass = createSmokeTestClassViewBuilder(context);
    testClass.name(name);
    testClass.outputPath(namer.getSourceFilePath(outputPath, name));
    return testClass.build();
  }

  private SmokeTestClassView.Builder createSmokeTestClassViewBuilder(
      GapicInterfaceContext context) {
    addSmokeTestImports(context.getModelTypeTable());

    Method method = context.getInterfaceConfig().getSmokeTestConfig().getMethod();
    FlatteningConfig flatteningGroup =
        testCaseTransformer.getSmokeTestFlatteningGroup(
            context.getMethodConfig(method), context.getInterfaceConfig().getSmokeTestConfig());
    GapicMethodContext flattenedMethodContext =
        context.asFlattenedMethodContext(method, flatteningGroup);

    SmokeTestClassView.Builder testClass = SmokeTestClassView.newBuilder();
    // TODO: we need to remove testCase after we switch to use apiMethod for smoke test
    TestCaseView testCase = testCaseTransformer.createSmokeTestCaseView(flattenedMethodContext);
    // apiMethod is cloned with an empty type table so types used only by method documentation (ex: an optional
    // FieldMask type) aren't included in the TypeTable by default.
    OptionalArrayMethodView apiMethod =
        createSmokeTestCaseApiMethodView(flattenedMethodContext.cloneWithEmptyTypeTable());

    testClass.apiSettingsClassName(
        context.getNamer().getApiSettingsClassName(context.getInterfaceConfig()));
    testClass.apiClassName(context.getNamer().getApiWrapperClassName(context.getInterfaceConfig()));
    testClass.apiName(
        PhpPackageMetadataNamer.getApiNameFromPackageName(context.getNamer().getPackageName())
            .toLowerUnderscore());
    testClass.templateFileName(SMOKE_TEST_TEMPLATE_FILE);
    testClass.apiMethod(apiMethod);
    testClass.method(testCase);
    testClass.requireProjectId(
        testCaseTransformer.requireProjectIdInSmokeTest(testCase.initCode(), context.getNamer()));

    ImportSectionView importSection =
        importSectionTransformer.generateImportSection(context.getModelTypeTable().getImports());
    PhpSurfaceNamer testPackageNamer =
        new PhpSurfaceNamer(context.getNamer().getTestPackageName(SurfaceNamer.TestKind.SYSTEM));
    FileHeaderView fileHeader =
        fileHeaderTransformer.generateFileHeader(
            context.getProductConfig(), importSection, testPackageNamer);
    testClass.fileHeader(fileHeader);

    return testClass;
  }

  private OptionalArrayMethodView createSmokeTestCaseApiMethodView(GapicMethodContext context) {
    OptionalArrayMethodView initialApiMethodView =
        new DynamicLangApiMethodTransformer(new PhpApiMethodParamTransformer())
            .generateMethod(context);

    OptionalArrayMethodView.Builder apiMethodView = initialApiMethodView.toBuilder();

    InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(
            context, testCaseTransformer.createSmokeTestInitContext(context));
    apiMethodView.initCode(initCodeView);

    return apiMethodView.build();
  }

  private void addUnitTestImports(ModelTypeTable typeTable) {
    typeTable.saveNicknameFor("\\Google\\GAX\\ApiException");
    typeTable.saveNicknameFor("\\Google\\GAX\\BidiStream");
    typeTable.saveNicknameFor("\\Google\\GAX\\ServerStream");
    typeTable.saveNicknameFor("\\Google\\GAX\\GrpcCredentialsHelper");
    typeTable.saveNicknameFor("\\Google\\GAX\\LongRunning\\OperationsClient");
    typeTable.saveNicknameFor("\\Google\\GAX\\Testing\\MockStubTrait");
    typeTable.saveNicknameFor("\\Google\\GAX\\Testing\\LongRunning\\MockOperationsImpl");
    typeTable.saveNicknameFor("\\Google\\GAX\\Testing\\GeneratedTest");
    typeTable.saveNicknameFor("\\PHPUnit_Framework_TestCase");
    typeTable.saveNicknameFor("\\Google\\Protobuf\\Any");
    typeTable.saveNicknameFor("\\Google\\Protobuf\\GPBEmpty");
    typeTable.saveNicknameFor("\\Google\\Longrunning\\GetOperationRequest");
    typeTable.saveNicknameFor("\\Grpc");
    typeTable.saveNicknameFor("\\stdClass");
  }

  private void addSmokeTestImports(ModelTypeTable typeTable) {
    typeTable.saveNicknameFor("\\Google\\GAX\\Testing\\GeneratedTest");
  }
}
