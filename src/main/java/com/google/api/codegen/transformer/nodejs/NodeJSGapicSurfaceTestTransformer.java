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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.nodejs.NodeJSUtils;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.MockServiceTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.js.JSTypeTable;
import com.google.api.codegen.util.testing.StandardValueProducer;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.ImportSectionView;
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

/** Responsible for producing testing related views for NodeJS */
public class NodeJSGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static final String TEST_TEMPLATE_FILE = "nodejs/test.snip";

  private final ValueProducer valueProducer = new StandardValueProducer();
  private final StandardImportSectionTransformer importSectionTransformer =
      new StandardImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final MockServiceTransformer mockServiceTransformer = new MockServiceTransformer();
  private final TestValueGenerator valueGenerator = new TestValueGenerator(valueProducer);
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);
  private final NodeJSFeatureConfig featureConfig = new NodeJSFeatureConfig();

  @Override
  public List<String> getTemplateFileNames() {
    return Collections.singletonList(TEST_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> models = new ArrayList<ViewModel>();
    NodeJSSurfaceNamer namer =
        new NodeJSSurfaceNamer(apiConfig.getPackageName(), NodeJSUtils.isGcloud(apiConfig));
    models.add(generateTestView(model, apiConfig, namer));
    return models;
  }

  private static ModelTypeTable createTypeTable(ApiConfig apiConfig) {
    String packageName = apiConfig.getPackageName();
    return new ModelTypeTable(
        new JSTypeTable(packageName), new NodeJSModelTypeNameConverter(packageName));
  }

  private MockCombinedView generateTestView(Model model, ApiConfig apiConfig, SurfaceNamer namer) {
    ModelTypeTable typeTable = createTypeTable(apiConfig);
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
      // We don't need any imports here.
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service, apiConfig, createTypeTable(apiConfig), namer, featureConfig);
      testClasses.add(
          ClientTestClassView.newBuilder()
              .apiSettingsClassName(
                  namer.getNotImplementedString(
                      "NodeJSGapicSurfaceTestTransformer.generateTestView - apiSettingsClassName"))
              .apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()))
              .name(
                  namer.getNotImplementedString(
                      "NodeJSGapicSurfaceTestTransformer.generateTestView - name"))
              .testCases(createTestCaseViews(context))
              .apiHasLongRunningMethods(context.getInterfaceConfig().hasLongRunningOperations())
              .mockServices(Collections.<MockServiceUsageView>emptyList())
              .build());
    }

    ImportSectionView importSection =
        importSectionTransformer.generateImportSection(typeTable.getImports());
    return MockCombinedView.newBuilder()
        .outputPath("test" + File.separator + "test.js")
        .serviceImpls(impls)
        .mockServices(new ArrayList<MockServiceUsageView>())
        .testClasses(testClasses)
        .apiWrapperModuleName(namer.getApiWrapperModuleName())
        .templateFileName(TEST_TEMPLATE_FILE)
        .fileHeader(fileHeaderTransformer.generateFileHeader(apiConfig, importSection, namer))
        .build();
  }

  private List<TestCaseView> createTestCaseViews(SurfaceTransformerContext context) {
    ArrayList<TestCaseView> testCaseViews = new ArrayList<>();
    SymbolTable testNameTable = new SymbolTable();
    for (Method method : context.getSupportedMethods()) {
      MethodTransformerContext methodContext = context.asRequestMethodContext(method);
      if (methodContext.getMethodConfig().getGrpcStreamingType()
          == GrpcStreamingType.ClientStreaming) {
        //TODO: Add unit test generation for ClientStreaming methods
        // Issue: https://github.com/googleapis/toolkit/issues/946
        continue;
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
              .outputType(InitCodeOutputType.SingleObject)
              .fieldConfigMap(FieldConfig.toFieldConfigMap(fieldConfigs))
              .valueGenerator(valueGenerator)
              .build();

      testCaseViews.add(
          testCaseTransformer.createTestCaseView(
              methodContext,
              testNameTable,
              initCodeContext,
              getMethodType(methodContext.getMethodConfig())));
    }
    return testCaseViews;
  }

  private ClientMethodType getMethodType(MethodConfig config) {
    ClientMethodType clientMethodType = ClientMethodType.RequestObjectMethod;
    if (config.isPageStreaming()) {
      clientMethodType = ClientMethodType.PagedRequestObjectMethod;
    } else if (config.isGrpcStreaming()) {
      clientMethodType = ClientMethodType.AsyncRequestObjectMethod;
    } else if (config.isLongRunningOperation()) {
      clientMethodType = ClientMethodType.OperationCallableMethod;
    }
    return clientMethodType;
  }
}
