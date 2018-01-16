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
package com.google.api.codegen.transformer.php;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoApiModel;
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
import com.google.api.codegen.viewmodel.testing.SmokeTestClassView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/** Responsible for producing testing related views for PHP. */
public class PhpGapicSurfaceTestTransformer implements ModelToViewTransformer {
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
    return ImmutableList.of(SMOKE_TEST_TEMPLATE_FILE, UNIT_TEST_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    ProtoApiModel apiModel = new ProtoApiModel(model);
    List<ViewModel> views = new ArrayList<>();
    for (InterfaceModel apiInterface : apiModel.getInterfaces()) {
      GapicInterfaceContext context =
          createContext(apiInterface, productConfig, PhpSurfaceNamer.TestKind.UNIT);
      views.add(createUnitTestFileView(context));
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        context = createContext(apiInterface, productConfig, PhpSurfaceNamer.TestKind.SYSTEM);
        views.add(createSmokeTestClassView(context));
      }
    }

    return views;
  }

  private GapicInterfaceContext createContext(
      InterfaceModel apiInterface,
      GapicProductConfig productConfig,
      PhpSurfaceNamer.TestKind testKind) {
    PhpSurfaceNamer surfacePackageNamer = new PhpSurfaceNamer(productConfig.getPackageName());
    String testPackageName = surfacePackageNamer.getTestPackageName(testKind);
    ModelTypeTable typeTable =
        new ModelTypeTable(
            new PhpTypeTable(testPackageName), new PhpModelTypeNameConverter(testPackageName));
    return GapicInterfaceContext.create(
        apiInterface, productConfig, typeTable, surfacePackageNamer, featureConfig);
  }

  private ClientTestFileView createUnitTestFileView(GapicInterfaceContext context) {
    addUnitTestImports(context.getImportTypeTable());

    String outputPath =
        PhpGapicCodePathMapper.newBuilder()
            .setPrefix("tests/unit")
            .build()
            .getOutputPath(context.getInterfaceModel().getFullName(), context.getProductConfig());
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
    for (InterfaceModel grpcInterface :
        mockServiceTransformer
            .getGrpcInterfacesForService(
                context.getApiModel(), context.getProductConfig(), context.getInterfaceModel())
            .values()) {
      context
          .getImportTypeTable()
          .getAndSaveNicknameFor(namer.getGrpcClientTypeName(grpcInterface));
    }
    testClass.mockServices(
        mockServiceTransformer.createMockServices(
            context.getNamer(), context.getApiModel(), context.getProductConfig()));
    testClass.missingDefaultServiceAddress(
        !context.getInterfaceConfig().hasDefaultServiceAddress());
    testClass.missingDefaultServiceScopes(!context.getInterfaceConfig().hasDefaultServiceScopes());

    ClientTestFileView.Builder testFile = ClientTestFileView.newBuilder();
    testFile.testClass(testClass.build());
    testFile.outputPath(namer.getSourceFilePath(outputPath, name));
    testFile.templateFileName(UNIT_TEST_TEMPLATE_FILE);

    ImportSectionView importSection =
        importSectionTransformer.generateImportSection(context.getImportTypeTable().getImports());
    SurfaceNamer testPackageNamer =
        namer.cloneWithPackageName(namer.getTestPackageName(SurfaceNamer.TestKind.UNIT));
    FileHeaderView fileHeader =
        fileHeaderTransformer.generateFileHeader(
            context.getProductConfig(), importSection, testPackageNamer);
    testFile.fileHeader(fileHeader);

    return testFile.build();
  }

  private List<TestCaseView> createTestCaseViews(GapicInterfaceContext context) {
    ArrayList<TestCaseView> testCaseViews = new ArrayList<>();
    SymbolTable testNameTable = new SymbolTable();
    for (MethodModel method : context.getSupportedMethods()) {
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
              .initObjectType(methodContext.getMethodModel().getInputType())
              .suggestedName(Name.from("request"))
              .initFieldConfigStrings(methodContext.getMethodConfig().getSampleCodeInitFields())
              .initValueConfigMap(InitCodeTransformer.createCollectionMap(methodContext))
              .initFields(FieldConfig.toFieldTypeIterable(fieldConfigs))
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

  private SmokeTestClassView createSmokeTestClassView(GapicInterfaceContext context) {
    String outputPath =
        PhpGapicCodePathMapper.newBuilder()
            .setPrefix("tests/system")
            .build()
            .getOutputPath(context.getInterfaceModel().getFullName(), context.getProductConfig());
    SurfaceNamer namer = context.getNamer();
    String name = namer.getSmokeTestClassName(context.getInterfaceConfig());

    SmokeTestClassView.Builder testClass = createSmokeTestClassViewBuilder(context);
    testClass.name(name);
    testClass.outputPath(namer.getSourceFilePath(outputPath, name));
    return testClass.build();
  }

  private SmokeTestClassView.Builder createSmokeTestClassViewBuilder(
      GapicInterfaceContext context) {
    addSmokeTestImports(context.getImportTypeTable());

    SurfaceNamer namer = context.getNamer();
    MethodModel method = context.getInterfaceConfig().getSmokeTestConfig().getMethod();
    FlatteningConfig flatteningGroup =
        testCaseTransformer.getSmokeTestFlatteningGroup(
            context.getMethodConfig(method), context.getInterfaceConfig().getSmokeTestConfig());
    GapicMethodContext flattenedMethodContext =
        context.asFlattenedMethodContext(method, flatteningGroup);

    SmokeTestClassView.Builder testClass = SmokeTestClassView.newBuilder();
    OptionalArrayMethodView apiMethod = createSmokeTestCaseApiMethodView(flattenedMethodContext);

    testClass.apiSettingsClassName(
        context.getNamer().getApiSettingsClassName(context.getInterfaceConfig()));
    testClass.apiClassName(context.getNamer().getApiWrapperClassName(context.getInterfaceConfig()));
    testClass.apiName(
        PhpPackageMetadataNamer.getApiNameFromPackageName(context.getNamer().getPackageName())
            .toLowerUnderscore());
    testClass.templateFileName(SMOKE_TEST_TEMPLATE_FILE);
    testClass.apiMethod(apiMethod);
    testClass.requireProjectId(
        testCaseTransformer.requireProjectIdInSmokeTest(apiMethod.initCode(), context.getNamer()));
    testClass.methodName(namer.getTestCaseName(new SymbolTable(), method));

    ImportSectionView importSection =
        importSectionTransformer.generateImportSection(context.getImportTypeTable().getImports());
    SurfaceNamer testPackageNamer =
        namer.cloneWithPackageName(namer.getTestPackageName(SurfaceNamer.TestKind.SYSTEM));
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
    typeTable.saveNicknameFor("\\Google\\ApiCore\\ApiException");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\BidiStream");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\ServerStream");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\LongRunning\\OperationsClient");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\Testing\\GeneratedTest");
    typeTable.saveNicknameFor("\\Google\\ApiCore\\Testing\\MockTransport");
    typeTable.saveNicknameFor("\\PHPUnit\\Framework\\TestCase");
    typeTable.saveNicknameFor("\\Google\\Protobuf\\Any");
    typeTable.saveNicknameFor("\\Google\\Protobuf\\GPBEmpty");
    typeTable.saveNicknameFor("\\Google\\LongRunning\\GetOperationRequest");
    typeTable.saveNicknameFor("\\Grpc");
    typeTable.saveNicknameFor("\\stdClass");
  }

  private void addSmokeTestImports(ModelTypeTable typeTable) {
    typeTable.saveNicknameFor("\\Google\\ApiCore\\Testing\\GeneratedTest");
  }
}
