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
package com.google.api.codegen.transformer.go;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.MockServiceTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.GoValueProducer;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.ClientTestClassView;
import com.google.api.codegen.viewmodel.testing.MockCombinedView;
import com.google.api.codegen.viewmodel.testing.MockServiceImplView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static final String MOCK_SERVICE_TEMPLATE_FILE = "go/mock.snip";

  private final GoValueProducer valueProducer = new GoValueProducer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new GoImportTransformer());
  private final MockServiceTransformer mockServiceTransformer = new MockServiceTransformer();
  private final FeatureConfig featureConfig = new GoFeatureConfig();
  private final TestValueGenerator valueGenerator = new TestValueGenerator(valueProducer);
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);

  @Override
  public List<String> getTemplateFileNames() {
    return Collections.singletonList(MOCK_SERVICE_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> models = new ArrayList<ViewModel>();
    GoSurfaceNamer namer = new GoSurfaceNamer(apiConfig.getPackageName());
    models.add(generateMockServiceView(model, apiConfig, namer));
    return models;
  }

  private MockCombinedView generateMockServiceView(
      Model model, ApiConfig apiConfig, SurfaceNamer namer) {
    ModelTypeTable typeTable = GoGapicSurfaceTransformer.createTypeTable();
    List<MockServiceImplView> impls = new ArrayList<>();
    List<ClientTestClassView> testClasses = new ArrayList<>();

    for (Interface service : mockServiceTransformer.getGrpcInterfacesToMock(model, apiConfig)) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(service, apiConfig, typeTable, namer, featureConfig);
      impls.add(
          MockServiceImplView.newBuilder()
              .grpcClassName(namer.getGrpcServerTypeName(service))
              .name(namer.getMockGrpcServiceImplName(service))
              .grpcMethods(mockServiceTransformer.createMockGrpcMethodViews(context))
              .build());
    }
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      // We don't need any import here.
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(service, apiConfig, typeTable, namer, featureConfig);
      testClasses.add(
          ClientTestClassView.newBuilder()
              .apiSettingsClassName(
                  namer.getNotImplementedString(
                      "GoGapicSurfaceTestTransformer.generateMockServiceView - apiSettingsClassName"))
              .apiClassName(namer.getApiWrapperClassName(service))
              .name(
                  namer.getNotImplementedString(
                      "GoGapicSurfaceTestTransformer.generateMockServiceView - name"))
              .testCases(createTestCaseViews(context))
              .apiHasLongRunningMethods(context.getInterfaceConfig().hasLongRunningOperations())
              .mockServices(Collections.<MockServiceUsageView>emptyList())
              .build());
    }

    return MockCombinedView.newBuilder()
        .outputPath(apiConfig.getPackageName() + File.separator + "mock_test.go")
        .serviceImpls(impls)
        .testClasses(testClasses)
        .templateFileName(MOCK_SERVICE_TEMPLATE_FILE)
        .fileHeader(
            fileHeaderTransformer.generateFileHeader(apiConfig, typeTable.getImports(), namer))
        .mockServices(mockServiceTransformer.createMockServices(namer, model, apiConfig))
        .build();
  }

  private List<TestCaseView> createTestCaseViews(SurfaceTransformerContext context) {
    ArrayList<TestCaseView> testCaseViews = new ArrayList<>();
    SymbolTable testNameTable = new SymbolTable();
    for (Method method : context.getSupportedMethods()) {
      MethodTransformerContext methodContext = context.asRequestMethodContext(method);
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
}
