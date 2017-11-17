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
package com.google.api.codegen.transformer.go;

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.transformer.DefaultFeatureConfig;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.MockServiceTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.StandardValueProducer;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.FileHeaderView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.ClientTestClassView;
import com.google.api.codegen.viewmodel.testing.MockCombinedView;
import com.google.api.codegen.viewmodel.testing.MockServiceImplView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.api.codegen.viewmodel.testing.SmokeTestClassView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.api.tools.framework.model.Model;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GoGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static final String MOCK_SERVICE_TEMPLATE_FILE = "go/mock.snip";
  private static final String SMOKE_TEST_TEMPLATE_FILE = "go/smoke.snip";

  private final ValueProducer valueProducer = new StandardValueProducer();
  private final GoImportSectionTransformer importSectionTransformer =
      new GoImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final MockServiceTransformer mockServiceTransformer = new MockServiceTransformer();
  private final FeatureConfig featureConfig = new DefaultFeatureConfig();
  private final TestValueGenerator valueGenerator = new TestValueGenerator(valueProducer);
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(MOCK_SERVICE_TEMPLATE_FILE, SMOKE_TEST_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    GoSurfaceNamer namer = new GoSurfaceNamer(productConfig.getPackageName());
    List<ViewModel> models = new ArrayList<ViewModel>();
    ProtoApiModel apiModel = new ProtoApiModel(model);
    models.add(generateMockServiceView(apiModel, productConfig, namer));

    for (InterfaceModel apiInterface : apiModel.getInterfaces(productConfig)) {
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface,
              productConfig,
              GoGapicSurfaceTransformer.createTypeTable(),
              namer,
              featureConfig);
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        models.add(createSmokeTestClassView(context));
      }
    }
    return models;
  }

  private MockCombinedView generateMockServiceView(
      ApiModel model, GapicProductConfig productConfig, SurfaceNamer namer) {
    ModelTypeTable typeTable = GoGapicSurfaceTransformer.createTypeTable();
    List<MockServiceImplView> impls = new ArrayList<>();
    List<ClientTestClassView> testClasses = new ArrayList<>();

    for (InterfaceModel apiInterface :
        mockServiceTransformer.getGrpcInterfacesToMock(model, productConfig)) {
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, typeTable, namer, featureConfig);
      impls.add(
          MockServiceImplView.newBuilder()
              .grpcClassName(namer.getGrpcServerTypeName(context.getInterfaceModel()))
              .name(namer.getMockGrpcServiceImplName(apiInterface))
              .grpcMethods(mockServiceTransformer.createMockGrpcMethodViews(context))
              .build());
    }
    for (InterfaceModel apiInterface : model.getInterfaces(productConfig)) {
      // We don't need any import here.
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, typeTable, namer, featureConfig);
      testClasses.add(
          ClientTestClassView.newBuilder()
              .apiSettingsClassName(
                  namer.getNotImplementedString(
                      "GoGapicSurfaceTestTransformer.generateMockServiceView - apiSettingsClassName"))
              .apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()))
              .name(
                  namer.getNotImplementedString(
                      "GoGapicSurfaceTestTransformer.generateMockServiceView - name"))
              .testCases(createTestCaseViews(context))
              .apiHasLongRunningMethods(context.getInterfaceConfig().hasLongRunningOperations())
              .missingDefaultServiceAddress(
                  !context.getInterfaceConfig().hasDefaultServiceAddress())
              .missingDefaultServiceScopes(!context.getInterfaceConfig().hasDefaultServiceScopes())
              .mockServices(Collections.<MockServiceUsageView>emptyList())
              .build());
    }

    ImportSectionView importSection =
        importSectionTransformer.generateImportSection(typeTable.getImports());
    return MockCombinedView.newBuilder()
        .outputPath(productConfig.getPackageName() + File.separator + "mock_test.go")
        .serviceImpls(impls)
        .testClasses(testClasses)
        .templateFileName(MOCK_SERVICE_TEMPLATE_FILE)
        .fileHeader(fileHeaderTransformer.generateFileHeader(productConfig, importSection, namer))
        .mockServices(mockServiceTransformer.createMockServices(namer, model, productConfig))
        .build();
  }

  private List<TestCaseView> createTestCaseViews(GapicInterfaceContext context) {
    ArrayList<TestCaseView> testCaseViews = new ArrayList<>();
    SymbolTable testNameTable = new SymbolTable();
    for (MethodModel method : context.getSupportedMethods()) {
      GapicMethodContext methodContext = context.asRequestMethodContext(method);
      ClientMethodType clientMethodType = ClientMethodType.RequestObjectMethod;
      if (methodContext.getMethodConfig().isPageStreaming()) {
        clientMethodType = ClientMethodType.PagedRequestObjectMethod;
      } else if (methodContext.getMethodConfig().isLongRunningOperation()) {
        clientMethodType = ClientMethodType.OperationRequestObjectMethod;
      }
      InitCodeContext initCodeContext =
          initCodeTransformer.createRequestInitCodeContext(
              methodContext,
              new SymbolTable(),
              methodContext.getMethodConfig().getRequiredFieldConfigs(),
              InitCodeOutputType.SingleObject,
              valueGenerator);
      testCaseViews.add(
          testCaseTransformer.createTestCaseView(
              methodContext, testNameTable, initCodeContext, clientMethodType));
    }
    return testCaseViews;
  }

  private SmokeTestClassView createSmokeTestClassView(InterfaceContext context) {
    SurfaceNamer namer = context.getNamer();

    MethodModel method = context.getInterfaceConfig().getSmokeTestConfig().getMethod();
    MethodContext methodContext = context.asRequestMethodContext(method);

    SmokeTestClassView.Builder testClass = SmokeTestClassView.newBuilder();
    TestCaseView testCaseView = testCaseTransformer.createSmokeTestCaseView(methodContext);

    testClass.apiSettingsClassName(namer.getApiSettingsClassName(context.getInterfaceConfig()));
    testClass.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    testClass.name(namer.getSmokeTestClassName(context.getInterfaceConfig()));
    testClass.outputPath(
        context.getProductConfig().getPackageName()
            + File.separator
            + method.getSimpleName()
            + "_smoke_test.go");
    testClass.templateFileName(SMOKE_TEST_TEMPLATE_FILE);
    testClass.apiMethod(
        new StaticLangApiMethodTransformer().generateRequestObjectMethod(methodContext));
    testClass.method(testCaseView);
    testClass.requireProjectId(
        testCaseTransformer.requireProjectIdInSmokeTest(
            testCaseView.initCode(), context.getNamer()));

    // The shared code above add imports both for input and output.
    // Since we use short decls, we don't need to import anything for output.
    context.getImportTypeTable().getImports().clear();
    method.getAndSaveRequestTypeName(methodContext.getTypeTable(), methodContext.getNamer());

    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    testClass.fileHeader(fileHeader);

    return testClass.build();
  }
}
