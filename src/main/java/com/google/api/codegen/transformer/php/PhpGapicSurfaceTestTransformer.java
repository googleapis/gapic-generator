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
import com.google.api.codegen.viewmodel.testing.MockCombinedView;
import com.google.api.codegen.viewmodel.testing.MockGrpcMethodView;
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

/** Responsible for producing testing related views for PHP */
public class PhpGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static final String TEST_TEMPLATE_FILE = "php/test.snip";

  private final PhpValueProducer valueProducer = new PhpValueProducer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportTypeTransformer());
  private final TestValueGenerator valueGenerator = new TestValueGenerator(valueProducer);
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);
  private final MockServiceTransformer mockServiceTransformer = new MockServiceTransformer();
  private final PhpFeatureConfig featureConfig = new PhpFeatureConfig();

  @Override
  public List<String> getTemplateFileNames() {
    return Collections.singletonList(TEST_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> models = new ArrayList<ViewModel>();
    PhpSurfaceNamer namer = new PhpSurfaceNamer(apiConfig.getPackageName());
    models.add(generateTestView(model, apiConfig, namer));
    return models;
  }

  private static ModelTypeTable createTypeTable(ApiConfig apiConfig) {
    String packageName = apiConfig.getPackageName();
    return new ModelTypeTable(
        new PhpTypeTable(packageName), new PhpModelTypeNameConverter(packageName));
  }

  private MockCombinedView generateTestView(Model model, ApiConfig apiConfig, SurfaceNamer namer) {
    ModelTypeTable typeTable = createTypeTable(apiConfig);
    List<MockServiceImplView> impls = new ArrayList<>();
    List<ClientTestClassView> testClasses = new ArrayList<>();

    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(service, apiConfig, typeTable, namer, featureConfig);
      List<MockServiceUsageView> mockServiceList = new ArrayList<>();

      for (Interface grpcInterface :
          mockServiceTransformer.getGrpcInterfacesForService(model, apiConfig, service).values()) {
        String name = namer.getMockGrpcServiceImplName(grpcInterface);
        String varName = namer.getMockServiceVarName(grpcInterface);
        mockServiceList.add(
            MockServiceUsageView.newBuilder()
                .className(name)
                .varName(varName)
                .implName(name)
                .build());
      }

      testClasses.add(
          ClientTestClassView.newBuilder()
              .apiSettingsClassName(
                  namer.getNotImplementedString(
                      "PhpGapicSurfaceTestTransformer.generateTestView - apiSettingsClassName"))
              .apiClassName(namer.getApiWrapperClassName(service))
              .name(namer.getUnitTestClassName(service))
              .testCases(createTestCaseViews(context))
              .mockServices(mockServiceList)
              .build());
    }
    for (Interface grpcInterface :
        mockServiceTransformer.getGrpcInterfacesToMock(model, apiConfig)) {
      String name = namer.getMockGrpcServiceImplName(grpcInterface);
      String grpcClassName =
          typeTable.getAndSaveNicknameFor(namer.getGrpcClientTypeName(grpcInterface));
      impls.add(
          MockServiceImplView.newBuilder()
              .name(name)
              .grpcClassName(grpcClassName)
              .grpcMethods(new ArrayList<MockGrpcMethodView>())
              .build());
    }

    addUnitTestImports(typeTable);

    return MockCombinedView.newBuilder()
        .outputPath("test" + File.separator + "Test.php")
        .serviceImpls(impls)
        .mockServices(new ArrayList<MockServiceUsageView>())
        .testClasses(testClasses)
        .templateFileName(TEST_TEMPLATE_FILE)
        .fileHeader(
            fileHeaderTransformer.generateFileHeader(apiConfig, typeTable.getImports(), namer))
        .build();
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

      if (methodContext.getMethodConfig().isLongRunningOperation()) {
        // TODO(michaelbausor): remove once LRO methods are supported
        continue;
      }

      ClientMethodType clientMethodType = ClientMethodType.OptionalArrayMethod;
      if (methodContext.getMethodConfig().isPageStreaming()) {
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
    typeTable.saveNicknameFor("\\Google\\GAX\\Testing\\MockStubTrait");
    typeTable.saveNicknameFor("\\PHPUnit_Framework_TestCase");
  }
}
