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
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
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
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.*;
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

  private final GapicCodePathMapper pathMapper;

  public PhpGapicSurfaceTestTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.<String>of(
        MOCK_SERVICE_IMPL_TEMPLATE_FILE, SMOKE_TEST_TEMPLATE_FILE, UNIT_TEST_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    List<ViewModel> views = new ArrayList<ViewModel>();
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      GapicInterfaceContext context = createContext(apiInterface, productConfig);
      views.add(createUnitTestFileView(context));
      //if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
      //  context = createContext(apiInterface, productConfig);
      //  views.add(createSmokeTestClassView(context));
      //}
    }
    for (Interface apiInterface :
        mockServiceTransformer.getGrpcInterfacesToMock(model, productConfig)) {
      GapicInterfaceContext context = createContext(apiInterface, productConfig);
      views.add(createMockServiceImplView(context));
    }
    return views;
  }

  private GapicInterfaceContext createContext(
      Interface apiInterface, GapicProductConfig productConfig) {
    PhpSurfaceNamer surfacePackageNamer = new PhpSurfaceNamer(productConfig.getPackageName());
    ModelTypeTable typeTable =
        new ModelTypeTable(
            new PhpTypeTable(surfacePackageNamer.getTestPackageName()),
            new PhpModelTypeNameConverter(surfacePackageNamer.getTestPackageName()));
    return GapicInterfaceContext.create(
        apiInterface, productConfig, typeTable, surfacePackageNamer, featureConfig);
  }

  private ClientTestFileView createUnitTestFileView(GapicInterfaceContext context) {
    addUnitTestImports(context.getModelTypeTable());

    String outputPath =
        pathMapper.getOutputPath(context.getInterface(), context.getProductConfig());
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

    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
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
    //addMockServiceImplImports(context);

    Interface apiInterface = context.getInterface();
    SurfaceNamer namer = context.getNamer();
    String outputPath = pathMapper.getOutputPath(apiInterface, context.getProductConfig());
    String name = namer.getMockGrpcServiceImplName(context.getInterface());
    String grpcClassName =
        context
            .getModelTypeTable()
            .getAndSaveNicknameFor(namer.getGrpcServiceClassName(apiInterface));

    MockServiceImplFileView.Builder mockServiceImplFile = MockServiceImplFileView.newBuilder();

    mockServiceImplFile.serviceImpl(
        MockServiceImplView.newBuilder()
            .name(name)
            .grpcClassName(grpcClassName)
            .grpcMethods(new ArrayList<MockGrpcMethodView>())
            .build());

    mockServiceImplFile.outputPath(namer.getSourceFilePath(outputPath, name));
    mockServiceImplFile.templateFileName(MOCK_SERVICE_IMPL_TEMPLATE_FILE);

    // Imports must be done as the last step to catch all imports.
    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    mockServiceImplFile.fileHeader(fileHeader);

    return mockServiceImplFile.build();
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
}
