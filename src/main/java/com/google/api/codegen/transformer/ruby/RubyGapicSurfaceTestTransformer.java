/* Copyright 2017 Google Inc
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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.util.testing.StandardValueProducer;
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
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.Collections;
import java.util.List;

/** A subclass of ModelToViewTransformer which translates model into API smoke tests in Ruby. */
public class RubyGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static String SMOKE_TEST_TEMPLATE_FILE = "ruby/smoke_test.snip";
  private static String UNIT_TEST_TEMPLATE_FILE = "ruby/test.snip";

  private final GapicCodePathMapper pathMapper;
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new RubyImportSectionTransformer());
  private final RubyImportSectionTransformer importSectionTransformer =
      new RubyImportSectionTransformer();
  private final ValueProducer valueProducer = new StandardValueProducer();
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);

  public RubyGapicSurfaceTestTransformer(GapicCodePathMapper rubyPathMapper) {
    this.pathMapper = rubyPathMapper;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(SMOKE_TEST_TEMPLATE_FILE, UNIT_TEST_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    views.add(createUnitTestView(model, apiConfig));
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context = createContext(service, apiConfig);
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        views.add(createSmokeTestClassView(context));
      }
    }

    return views.build();
  }

  ///////////////////////////////////// Unit Test ///////////////////////////////////////

  private MockCombinedView createUnitTestView(Model model, ApiConfig apiConfig) {
    SurfaceNamer namer = new RubySurfaceNamer(apiConfig.getPackageName());
    ImportSectionView importSection =
        importSectionTransformer.generateTestImportSection(model, apiConfig);
    return MockCombinedView.newBuilder()
        .outputPath("test" + File.separator + "test.rb")
        .serviceImpls(ImmutableList.<MockServiceImplView>of())
        .mockServices(ImmutableList.<MockServiceUsageView>of())
        .testClasses(generateTestClasses(model, apiConfig, namer))
        .templateFileName(UNIT_TEST_TEMPLATE_FILE)
        .fileHeader(fileHeaderTransformer.generateFileHeader(apiConfig, importSection, namer))
        .build();
  }

  private List<ClientTestClassView> generateTestClasses(
      Model model, ApiConfig apiConfig, SurfaceNamer namer) {
    ImmutableList.Builder<ClientTestClassView> testClasses = ImmutableList.builder();
    String apiSettingsClassName =
        namer.getNotImplementedString(
            "RubyGapicSurfaceTestTransformer.generateTestView - apiSettingsClassName");
    String testClassName =
        namer.getNotImplementedString("RubyGapicSurfaceTestTransformer.generateTestView - name");

    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context = createContext(service, apiConfig);
      InterfaceConfig serviceConfig = context.getInterfaceConfig();
      ClientTestClassView testClass =
          ClientTestClassView.newBuilder()
              .apiSettingsClassName(apiSettingsClassName)
              .apiClassName(namer.getApiWrapperClassName(serviceConfig))
              .fullyQualifiedApiClassName(namer.getFullyQualifiedApiWrapperClassName(serviceConfig))
              .name(testClassName)
              .testCases(createTestCaseViews(context))
              .apiHasLongRunningMethods(serviceConfig.hasLongRunningOperations())
              .mockServices(Collections.<MockServiceUsageView>emptyList())
              .build();
      testClasses.add(testClass);
    }
    return testClasses.build();
  }

  private List<TestCaseView> createTestCaseViews(SurfaceTransformerContext context) {
    ImmutableList.Builder<TestCaseView> testCases = ImmutableList.builder();
    List<Method> methods = getTestedMethods(context);
    for (Method method : methods) {
      MethodTransformerContext requestMethodContext =
          context.withNewTypeTable().asRequestMethodContext(method);
      MethodConfig methodConfig = requestMethodContext.getMethodConfig();
      TestCaseView testCase =
          testCaseTransformer.createTestCaseView(
              requestMethodContext,
              new SymbolTable(),
              createTestCaseInitCodeContext(context, method),
              getMethodType(methodConfig));
      testCases.add(testCase);
    }
    return testCases.build();
  }

  // TODO(landrito): Remove this function when all test types are supported.
  private List<Method> getTestedMethods(SurfaceTransformerContext context) {
    ImmutableList.Builder<Method> methods = ImmutableList.builder();
    for (Method method : context.getSupportedMethods()) {
      MethodTransformerContext requestMethodContext = context.asRequestMethodContext(method);
      MethodConfig methodConfig = requestMethodContext.getMethodConfig();
      if (methodConfig.isPageStreaming()
          || methodConfig.isGrpcStreaming()
          || methodConfig.isLongRunningOperation()) {
        continue;
      }
      methods.add(method);
    }
    return methods.build();
  }

  private InitCodeContext createTestCaseInitCodeContext(
      SurfaceTransformerContext context, Method method) {
    MethodTransformerContext requestMethodContext = context.asRequestMethodContext(method);
    MethodTransformerContext dynamicMethodContext = context.asDynamicMethodContext(method);
    MethodConfig methodConfig = requestMethodContext.getMethodConfig();
    Iterable<FieldConfig> fieldConfigs = methodConfig.getRequiredFieldConfigs();

    return InitCodeContext.newBuilder()
        .initObjectType(method.getInputType())
        .suggestedName(Name.from("expected_request"))
        .initFieldConfigStrings(methodConfig.getSampleCodeInitFields())
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(dynamicMethodContext))
        .initFields(FieldConfig.toFieldIterable(fieldConfigs))
        // This field will vary when page streaming tests are supported.
        .outputType(InitCodeOutputType.FieldList)
        .fieldConfigMap(FieldConfig.toFieldConfigMap(fieldConfigs))
        .build();
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

  ///////////////////////////////////// Smoke Test ///////////////////////////////////////

  private SmokeTestClassView createSmokeTestClassView(SurfaceTransformerContext context) {
    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    SurfaceNamer namer = context.getNamer();
    String name = namer.getSmokeTestClassName(context.getInterfaceConfig());

    Method method = context.getInterfaceConfig().getSmokeTestConfig().getMethod();
    FlatteningConfig flatteningGroup =
        testCaseTransformer.getSmokeTestFlatteningGroup(
            context.getMethodConfig(method), context.getInterfaceConfig().getSmokeTestConfig());
    MethodTransformerContext flattenedMethodContext =
        context.asFlattenedMethodContext(method, flatteningGroup);

    SmokeTestClassView.Builder testClass = SmokeTestClassView.newBuilder();
    // TODO: we need to remove testCaseView after we switch to use apiMethodView for smoke test
    // testCaseView not in use by Ruby for smoke test.
    TestCaseView testCaseView = testCaseTransformer.createSmokeTestCaseView(flattenedMethodContext);
    OptionalArrayMethodView apiMethodView =
        createSmokeTestCaseApiMethodView(flattenedMethodContext);

    testClass.apiSettingsClassName(namer.getApiSettingsClassName(context.getInterfaceConfig()));
    testClass.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    testClass.name(name);
    testClass.outputPath(namer.getSourceFilePath(outputPath, name));
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
      MethodTransformerContext context) {
    OptionalArrayMethodView initialApiMethodView =
        new DynamicLangApiMethodTransformer(new RubyApiMethodParamTransformer())
            .generateMethod(context);

    OptionalArrayMethodView.Builder apiMethodView = initialApiMethodView.toBuilder();

    InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(
            context, testCaseTransformer.createSmokeTestInitContext(context));
    apiMethodView.initCode(initCodeView);

    return apiMethodView.build();
  }

  /////////////////////////////////// General Helpers //////////////////////////////////////

  private SurfaceTransformerContext createContext(Interface service, ApiConfig apiConfig) {
    return SurfaceTransformerContext.create(
        service,
        apiConfig,
        new ModelTypeTable(
            new RubyTypeTable(apiConfig.getPackageName()),
            new RubyModelTypeNameConverter(apiConfig.getPackageName())),
        new RubySurfaceNamer(apiConfig.getPackageName()),
        new RubyFeatureConfig());
  }
}
