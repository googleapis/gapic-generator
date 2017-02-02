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
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.php.PhpGapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.MockServiceTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.StandardImportTypeTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.api.codegen.util.testing.PhpValueProducer;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.ClientTestClassView;
import com.google.api.codegen.viewmodel.testing.ClientTestFileView;
import com.google.api.codegen.viewmodel.testing.MockGrpcMethodView;
import com.google.api.codegen.viewmodel.testing.MockServiceImplFileView;
import com.google.api.codegen.viewmodel.testing.MockServiceImplView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Responsible for producing testing related views for PHP */
public class PhpGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static final String TEST_TEMPLATE_FILE = "php/test.snip";
  private static final String MOCK_SERVICE_TEMPLATE_FILE = "php/mock_service.snip";

  private final PhpValueProducer valueProducer = new PhpValueProducer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportTypeTransformer());
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
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> models = new ArrayList<ViewModel>();
    PhpSurfaceNamer surfacePackageNamer = new PhpSurfaceNamer(apiConfig.getPackageName());
    PhpSurfaceNamer testPackageNamer =
        new PhpSurfaceNamer(surfacePackageNamer.getTestPackageName());
    models.addAll(generateTestViews(model, apiConfig, surfacePackageNamer, testPackageNamer));
    models.addAll(
        generateMockServiceViews(model, apiConfig, surfacePackageNamer, testPackageNamer));
    return models;
  }

  private static ModelTypeTable createTypeTable(String packageName) {
    return new ModelTypeTable(
        new PhpTypeTable(packageName), new PhpModelTypeNameConverter(packageName));
  }

  private List<MockServiceImplFileView> generateMockServiceViews(
      Model model,
      ApiConfig apiConfig,
      SurfaceNamer surfacePackageNamer,
      SurfaceNamer testPackageNamer) {
    List<MockServiceImplFileView> mockFiles = new ArrayList<>();

    for (Interface grpcInterface :
        mockServiceTransformer.getGrpcInterfacesToMock(model, apiConfig)) {
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
      String outputPath = pathMapper.getOutputPath(grpcInterface, apiConfig);

      addUnitTestImports(typeTable);

      mockFiles.add(
          MockServiceImplFileView.newBuilder()
              .outputPath(outputPath + File.separator + name + ".php")
              .templateFileName(MOCK_SERVICE_TEMPLATE_FILE)
              .fileHeader(
                  fileHeaderTransformer.generateFileHeader(
                      apiConfig, typeTable.getImports(), testPackageNamer))
              .serviceImpl(mockImpl)
              .build());
    }
    return mockFiles;
  }

  private List<ClientTestFileView> generateTestViews(
      Model model,
      ApiConfig apiConfig,
      SurfaceNamer surfacePackageNamer,
      SurfaceNamer testPackageNamer) {
    List<ClientTestFileView> testViews = new ArrayList<>();

    for (Interface service : new InterfaceView().getElementIterable(model)) {
      ModelTypeTable typeTable = createTypeTable(surfacePackageNamer.getTestPackageName());
      List<MockServiceImplView> impls = new ArrayList<>();
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service, apiConfig, typeTable, surfacePackageNamer, featureConfig);
      List<MockServiceUsageView> mockServiceList = new ArrayList<>();

      for (Interface grpcInterface :
          mockServiceTransformer.getGrpcInterfacesForService(model, apiConfig, service).values()) {
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

      String testClassName = surfacePackageNamer.getUnitTestClassName(service);
      ClientTestClassView testClassView =
          ClientTestClassView.newBuilder()
              .apiSettingsClassName(
                  surfacePackageNamer.getNotImplementedString(
                      "PhpGapicSurfaceTestTransformer.generateTestView - apiSettingsClassName"))
              .apiClassName(surfacePackageNamer.getApiWrapperClassName(service))
              .name(testClassName)
              .apiName(
                  PhpPackageMetadataNamer.getApiNameFromPackageName(
                          surfacePackageNamer.getPackageName())
                      .toLowerUnderscore())
              .testCases(createTestCaseViews(context))
              .apiHasLongRunningMethods(context.getInterfaceConfig().hasLongRunningOperations())
              .mockServices(mockServiceList)
              .build();

      addUnitTestImports(typeTable);

      String outputPath = pathMapper.getOutputPath(context.getInterface(), apiConfig);
      testViews.add(
          ClientTestFileView.newBuilder()
              .outputPath(outputPath + File.separator + testClassName + ".php")
              .testClass(testClassView)
              .templateFileName(TEST_TEMPLATE_FILE)
              .fileHeader(
                  fileHeaderTransformer.generateFileHeader(
                      apiConfig, typeTable.getImports(), testPackageNamer))
              .build());
    }

    return testViews;
  }

  private List<TestCaseView> createTestCaseViews(SurfaceTransformerContext context) {
    ArrayList<TestCaseView> testCaseViews = new ArrayList<>();
    SymbolTable testNameTable = new SymbolTable();
    for (Method method : context.getSupportedMethods()) {
      MethodTransformerContext methodContext = context.asRequestMethodContext(method);

      if (methodContext.getMethodConfig().isGrpcStreaming()) {
        // TODO(shinfan): Remove this check once grpc streaming is supported by test
        continue;
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
              .outputType(InitCodeOutputType.FieldList)
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
    typeTable.saveNicknameFor("\\Google\\GAX\\GrpcCredentialsHelper");
    typeTable.saveNicknameFor("\\Google\\GAX\\LongRunning\\OperationsClient");
    typeTable.saveNicknameFor("\\Google\\GAX\\Testing\\MockStubTrait");
    typeTable.saveNicknameFor("\\Google\\GAX\\Testing\\LongRunning\\MockOperationsImpl");
    typeTable.saveNicknameFor("\\PHPUnit_Framework_TestCase");
    typeTable.saveNicknameFor("\\google\\protobuf\\Any");
    typeTable.saveNicknameFor("\\google\\protobuf\\EmptyC");
    typeTable.saveNicknameFor("\\google\\longrunning\\GetOperationRequest");
  }
}
