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

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.php.PhpGapicCodePathMapper;
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
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.ClientTestClassView;
import com.google.api.codegen.viewmodel.testing.ClientTestFileView;
import com.google.api.codegen.viewmodel.testing.MockGrpcMethodView;
import com.google.api.codegen.viewmodel.testing.MockServiceImplFileView;
import com.google.api.codegen.viewmodel.testing.MockServiceImplView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Responsible for producing testing related views for PHP */
public class PhpGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static final String TEST_TEMPLATE_FILE = "php/test.snip";
  private static final String MOCK_SERVICE_TEMPLATE_FILE = "php/mock_service.snip";

  private final ValueProducer valueProducer = new StandardValueProducer();
  private final PhpImportSectionTransformer importSectionTransformer =
      new PhpImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final TestValueGenerator valueGenerator = new TestValueGenerator(valueProducer);
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);
  private final MockServiceTransformer mockServiceTransformer = new MockServiceTransformer();
  private final PhpFeatureConfig featureConfig = new PhpFeatureConfig();
  private final PhpGapicCodePathMapper pathMapper =
      PhpGapicCodePathMapper.newBuilder().setPrefix("tests/unit").build();

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.<String>of(TEST_TEMPLATE_FILE, MOCK_SERVICE_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    ProtoApiModel apiModel = new ProtoApiModel(model);
    List<ViewModel> models = new ArrayList<ViewModel>();
    PhpSurfaceNamer surfacePackageNamer = new PhpSurfaceNamer(productConfig.getPackageName());
    PhpSurfaceNamer testPackageNamer =
        new PhpSurfaceNamer(surfacePackageNamer.getTestPackageName());
    models.addAll(
        generateTestViews(apiModel, productConfig, surfacePackageNamer, testPackageNamer));
    models.addAll(
        generateMockServiceViews(apiModel, productConfig, surfacePackageNamer, testPackageNamer));
    return models;
  }

  private static ModelTypeTable createTypeTable(String packageName) {
    return new ModelTypeTable(
        new PhpTypeTable(packageName), new PhpModelTypeNameConverter(packageName));
  }

  private List<MockServiceImplFileView> generateMockServiceViews(
      ApiModel model,
      GapicProductConfig productConfig,
      SurfaceNamer surfacePackageNamer,
      SurfaceNamer testPackageNamer) {
    List<MockServiceImplFileView> mockFiles = new ArrayList<>();

    for (InterfaceModel grpcInterface :
        mockServiceTransformer.getGrpcInterfacesToMock(model, productConfig)) {
      ModelTypeTable typeTable = createTypeTable(surfacePackageNamer.getTestPackageName());
      String name = surfacePackageNamer.getMockGrpcServiceImplName(grpcInterface);
      String grpcClassName =
          typeTable.getAndSaveNicknameFor(surfacePackageNamer.getGrpcClientTypeName(grpcInterface));
      MockServiceImplView mockImpl =
          MockServiceImplView.newBuilder()
              .name(name)
              .grpcClassName(grpcClassName)
              .grpcMethods(new ArrayList<MockGrpcMethodView>())
              .build();
      String outputPath = pathMapper.getOutputPath(grpcInterface.getFullName(), productConfig);

      addUnitTestImports(typeTable);

      ImportSectionView importSection =
          importSectionTransformer.generateImportSection(typeTable.getImports());
      mockFiles.add(
          MockServiceImplFileView.newBuilder()
              .outputPath(outputPath + File.separator + name + ".php")
              .templateFileName(MOCK_SERVICE_TEMPLATE_FILE)
              .fileHeader(
                  fileHeaderTransformer.generateFileHeader(
                      productConfig, importSection, testPackageNamer))
              .serviceImpl(mockImpl)
              .build());
    }
    return mockFiles;
  }

  private List<ClientTestFileView> generateTestViews(
      ApiModel model,
      GapicProductConfig productConfig,
      SurfaceNamer surfacePackageNamer,
      SurfaceNamer testPackageNamer) {
    List<ClientTestFileView> testViews = new ArrayList<>();

    for (InterfaceModel apiInterface : model.getInterfaces(productConfig)) {
      ModelTypeTable typeTable = createTypeTable(surfacePackageNamer.getTestPackageName());
      List<MockServiceImplView> impls = new ArrayList<>();
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, typeTable, surfacePackageNamer, featureConfig);
      List<MockServiceUsageView> mockServiceList = new ArrayList<>();

      for (InterfaceModel grpcInterface :
          mockServiceTransformer
              .getGrpcInterfacesForService(model, productConfig, apiInterface)
              .values()) {
        String name = surfacePackageNamer.getMockGrpcServiceImplName(grpcInterface);
        String varName = surfacePackageNamer.getMockServiceVarName(grpcInterface);
        String grpcClassName =
            typeTable.getAndSaveNicknameFor(
                surfacePackageNamer.getGrpcClientTypeName(grpcInterface));
        mockServiceList.add(
            MockServiceUsageView.newBuilder()
                .className(name)
                .varName(varName)
                .implName(name)
                .build());
        impls.add(
            MockServiceImplView.newBuilder()
                .name(name)
                .grpcClassName(grpcClassName)
                .grpcMethods(new ArrayList<MockGrpcMethodView>())
                .build());
      }

      String testClassName = surfacePackageNamer.getUnitTestClassName(context.getInterfaceConfig());
      ClientTestClassView testClassView =
          ClientTestClassView.newBuilder()
              .apiSettingsClassName(
                  surfacePackageNamer.getNotImplementedString(
                      "PhpGapicSurfaceTestTransformer.generateTestView - apiSettingsClassName"))
              .apiClassName(
                  surfacePackageNamer.getApiWrapperClassName(context.getInterfaceConfig()))
              .name(testClassName)
              .apiName(
                  PhpPackageMetadataNamer.getApiNameFromPackageName(
                          surfacePackageNamer.getPackageName())
                      .toLowerUnderscore())
              .testCases(createTestCaseViews(context))
              .apiHasLongRunningMethods(context.getInterfaceConfig().hasLongRunningOperations())
              .missingDefaultServiceAddress(
                  !context.getInterfaceConfig().hasDefaultServiceAddress())
              .missingDefaultServiceScopes(!context.getInterfaceConfig().hasDefaultServiceScopes())
              .mockServices(mockServiceList)
              .build();

      addUnitTestImports(typeTable);

      String outputPath =
          pathMapper.getOutputPath(context.getInterfaceModel().getFullName(), productConfig);
      ImportSectionView importSection =
          importSectionTransformer.generateImportSection(typeTable.getImports());
      testViews.add(
          ClientTestFileView.newBuilder()
              .outputPath(outputPath + File.separator + testClassName + ".php")
              .testClass(testClassView)
              .templateFileName(TEST_TEMPLATE_FILE)
              .fileHeader(
                  fileHeaderTransformer.generateFileHeader(
                      productConfig, importSection, testPackageNamer))
              .build());
    }

    return testViews;
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
