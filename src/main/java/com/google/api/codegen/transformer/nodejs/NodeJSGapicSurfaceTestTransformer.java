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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.nodejs.NodeJSUtils;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MockServiceTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.js.JSTypeTable;
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
import com.google.api.codegen.viewmodel.testing.MockCombinedView;
import com.google.api.codegen.viewmodel.testing.MockServiceImplView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.api.codegen.viewmodel.testing.SmokeTestClassView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Responsible for producing testing related views for NodeJS */
public class NodeJSGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static final String TEST_TEMPLATE_FILE = "nodejs/test.snip";
  private static final String SMOKE_TEST_TEMPLATE_FILE = "nodejs/smoke_test.snip";
  private static final String SMOKE_TEST_OUTPUT_BASE_PATH = "smoke-test";

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
    return ImmutableList.of(TEST_TEMPLATE_FILE, SMOKE_TEST_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    List<ViewModel> models = new ArrayList<ViewModel>();
    ApiModel apiModel = new ProtoApiModel(model);
    NodeJSSurfaceNamer namer =
        new NodeJSSurfaceNamer(productConfig.getPackageName(), NodeJSUtils.isGcloud(productConfig));
    models.add(generateTestView(apiModel, productConfig, namer));
    models.addAll(createSmokeTestViews(apiModel, productConfig));
    return models;
  }

  private static ModelTypeTable createTypeTable(GapicProductConfig productConfig) {
    String packageName = productConfig.getPackageName();
    return new ModelTypeTable(
        new JSTypeTable(packageName), new NodeJSModelTypeNameConverter(packageName));
  }

  private MockCombinedView generateTestView(
      ApiModel model, GapicProductConfig productConfig, SurfaceNamer namer) {
    ModelTypeTable typeTable = createTypeTable(productConfig);
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
    InterfaceView interfaceView = new InterfaceView();
    for (InterfaceModel apiInterface : model.getInterfaces(productConfig)) {
      // We don't need any imports here.
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, createTypeTable(productConfig), namer, featureConfig);
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
              .packageServiceName(namer.getPackageServiceName(context.getInterfaceModel()))
              .missingDefaultServiceAddress(
                  !context.getInterfaceConfig().hasDefaultServiceAddress())
              .missingDefaultServiceScopes(!context.getInterfaceConfig().hasDefaultServiceScopes())
              .mockServices(Collections.<MockServiceUsageView>emptyList())
              .build());
    }

    ImportSectionView importSection =
        importSectionTransformer.generateImportSection(typeTable.getImports());
    return MockCombinedView.newBuilder()
        .outputPath(testCaseOutputFile(namer))
        .serviceImpls(impls)
        .mockServices(new ArrayList<MockServiceUsageView>())
        .testClasses(testClasses)
        .localPackageName(namer.getLocalPackageName())
        .templateFileName(TEST_TEMPLATE_FILE)
        .packageHasMultipleServices(model.hasMultipleServices(productConfig))
        .fileHeader(fileHeaderTransformer.generateFileHeader(productConfig, importSection, namer))
        .build();
  }

  private String testCaseOutputFile(SurfaceNamer namer) {
    String outputPath = "test";
    String fileName =
        Strings.isNullOrEmpty(namer.getApiWrapperModuleVersion())
            ? "gapic.js"
            : "gapic-" + namer.getApiWrapperModuleVersion() + ".js";
    return outputPath + File.separator + fileName;
  }

  private List<TestCaseView> createTestCaseViews(GapicInterfaceContext context) {
    ArrayList<TestCaseView> testCaseViews = new ArrayList<>();
    SymbolTable testNameTable = new SymbolTable();
    for (MethodModel method : context.getSupportedMethods()) {
      GapicMethodContext methodContext = context.asRequestMethodContext(method);
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
              .initObjectType(methodContext.getMethodModel().getInputType())
              .suggestedName(Name.from("request"))
              .initFieldConfigStrings(methodContext.getMethodConfig().getSampleCodeInitFields())
              .initValueConfigMap(InitCodeTransformer.createCollectionMap(methodContext))
              .initFields(FieldConfig.toFieldTypeIterable(fieldConfigs))
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

  private List<ViewModel> createSmokeTestViews(ApiModel model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    for (InterfaceModel apiInterface : model.getInterfaces(productConfig)) {
      GapicInterfaceContext context = createContext(apiInterface, productConfig);
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        views.add(createSmokeTestClassView(context, model.hasMultipleServices(productConfig)));
      }
    }
    return views.build();
  }

  private SmokeTestClassView createSmokeTestClassView(
      GapicInterfaceContext context, boolean packageHasMultipleServices) {
    SurfaceNamer namer = context.getNamer();
    String name = namer.getSmokeTestClassName(context.getInterfaceConfig());

    MethodModel method = context.getInterfaceConfig().getSmokeTestConfig().getMethod();
    FlatteningConfig flatteningGroup =
        testCaseTransformer.getSmokeTestFlatteningGroup(
            context.getMethodConfig(method), context.getInterfaceConfig().getSmokeTestConfig());
    GapicMethodContext flattenedMethodContext =
        context.asFlattenedMethodContext(method, flatteningGroup);

    SmokeTestClassView.Builder testClass = SmokeTestClassView.newBuilder();
    TestCaseView testCaseView = testCaseTransformer.createSmokeTestCaseView(flattenedMethodContext);
    OptionalArrayMethodView apiMethodView =
        createSmokeTestCaseApiMethodView(flattenedMethodContext, packageHasMultipleServices);

    testClass.apiSettingsClassName(namer.getApiSettingsClassName(context.getInterfaceConfig()));
    testClass.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    testClass.name(name);
    testClass.outputPath(namer.getSourceFilePath(SMOKE_TEST_OUTPUT_BASE_PATH, name));
    testClass.templateFileName(SMOKE_TEST_TEMPLATE_FILE);
    testClass.apiMethod(apiMethodView);
    testClass.method(testCaseView);
    testClass.requireProjectId(
        testCaseTransformer.requireProjectIdInSmokeTest(
            apiMethodView.initCode(), context.getNamer()));

    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    testClass.fileHeader(fileHeader);

    return testClass.build();
  }

  private OptionalArrayMethodView createSmokeTestCaseApiMethodView(
      GapicMethodContext context, boolean packageHasMultipleServices) {
    OptionalArrayMethodView initialApiMethodView =
        new DynamicLangApiMethodTransformer(new NodeJSApiMethodParamTransformer())
            .generateMethod(context, packageHasMultipleServices);

    OptionalArrayMethodView.Builder apiMethodView = initialApiMethodView.toBuilder();

    InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(
            context, testCaseTransformer.createSmokeTestInitContext(context));
    apiMethodView.initCode(initCodeView);
    apiMethodView.packageName("../src");
    return apiMethodView.build();
  }

  private GapicInterfaceContext createContext(
      InterfaceModel apiInterface, GapicProductConfig productConfig) {
    return GapicInterfaceContext.create(
        apiInterface,
        productConfig,
        new ModelTypeTable(
            new JSTypeTable(productConfig.getPackageName()),
            new NodeJSModelTypeNameConverter(productConfig.getPackageName())),
        new NodeJSSurfaceNamer(productConfig.getPackageName(), NodeJSUtils.isGcloud(productConfig)),
        new NodeJSFeatureConfig());
  }
}
