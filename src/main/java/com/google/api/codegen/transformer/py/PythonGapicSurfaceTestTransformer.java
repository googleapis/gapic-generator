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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.transformer.DefaultFeatureConfig;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FeatureConfig;
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
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.codegen.util.testing.PythonValueProducer;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.ClientTestClassView;
import com.google.api.codegen.viewmodel.testing.ClientTestFileView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.api.codegen.viewmodel.testing.SmokeTestClassView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.List;

/**
 * Transforms the model into API tests for Python. Responsible for producing a list of
 * ClientTestFileViews for unit tests and a SmokeTestClassViews for smoke tests.
 */
public class PythonGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static final String SMOKE_TEST_TEMPLATE_FILE = "py/smoke_test.snip";
  private static final String TEST_TEMPLATE_FILE = "py/test.snip";

  private final GapicCodePathMapper pathMapper;
  private final PackageMetadataConfig packageConfig;
  private final ValueProducer valueProducer = new PythonValueProducer();
  private final PythonImportSectionTransformer importSectionTransformer =
      new PythonImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final TestValueGenerator valueGenerator = new TestValueGenerator(valueProducer);
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);
  private final MockServiceTransformer mockServiceTransformer = new MockServiceTransformer();
  private final FeatureConfig featureConfig = new DefaultFeatureConfig();

  public PythonGapicSurfaceTestTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageConfig) {
    this.pathMapper = pathMapper;
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.<String>of(SMOKE_TEST_TEMPLATE_FILE, TEST_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> models = ImmutableList.builder();
    models.addAll(createUnitTestViews(model, productConfig));
    models.addAll(createSmokeTestViews(model, productConfig));
    return models.build();
  }

  private List<ViewModel> createUnitTestViews(Model model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> models = ImmutableList.builder();
    SurfaceNamer surfacePackageNamer = new PythonSurfaceNamer(productConfig.getPackageName());
    SurfaceNamer testPackageNamer =
        new PythonSurfaceNamer(surfacePackageNamer.getTestPackageName());
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      ModelTypeTable typeTable = createTypeTable(surfacePackageNamer.getTestPackageName());
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, typeTable, surfacePackageNamer, featureConfig);
      String testClassName = surfacePackageNamer.getUnitTestClassName(context.getInterfaceConfig());
      ClientTestClassView testClassView =
          ClientTestClassView.newBuilder()
              .apiSettingsClassName(
                  surfacePackageNamer.getNotImplementedString(
                      "PythonGapicSurfaceTestTransformer.createUnitTestViews - apiSettingsClassName"))
              .apiClassName(
                  surfacePackageNamer.getApiWrapperClassName(context.getInterfaceConfig()))
              .apiVariableName(
                  surfacePackageNamer.getApiWrapperVariableName(context.getInterfaceConfig()))
              .name(testClassName)
              .apiName(
                  surfacePackageNamer.publicClassName(
                      Name.upperCamelKeepUpperAcronyms(apiInterface.getSimpleName())))
              .testCases(createTestCaseViews(context))
              .apiHasLongRunningMethods(context.getInterfaceConfig().hasLongRunningOperations())
              .mockServices(ImmutableList.<MockServiceUsageView>of())
              .build();

      String version = packageConfig.apiVersion();
      String outputDir = pathMapper.getOutputPath(context.getInterface(), productConfig);
      String outputPath =
          outputDir
              + File.separator
              + surfacePackageNamer.classFileNameBase(Name.upperCamel(testClassName).join(version))
              + ".py";
      ImportSectionView importSection = importSectionTransformer.generateTestImportSection(context);
      models.add(
          ClientTestFileView.newBuilder()
              .templateFileName(TEST_TEMPLATE_FILE)
              .outputPath(outputPath)
              .testClass(testClassView)
              .fileHeader(
                  fileHeaderTransformer.generateFileHeader(
                      productConfig, importSection, testPackageNamer))
              .build());
    }
    return models.build();
  }

  private List<TestCaseView> createTestCaseViews(GapicInterfaceContext context) {
    ImmutableList.Builder<TestCaseView> testCaseViews = ImmutableList.builder();
    SymbolTable testNameTable = new SymbolTable();
    for (Method method : context.getSupportedMethods()) {
      GapicMethodContext methodContext = context.asRequestMethodContext(method);
      ClientMethodType clientMethodType = ClientMethodType.OptionalArrayMethod;
      if (methodContext.getMethodConfig().isLongRunningOperation()) {
        clientMethodType = ClientMethodType.OperationOptionalArrayMethod;
      } else if (methodContext.getMethodConfig().isPageStreaming()) {
        clientMethodType = ClientMethodType.PagedOptionalArrayMethod;
      }

      Iterable<FieldConfig> fieldConfigs =
          methodContext.getMethodConfig().getRequiredFieldConfigs();
      InitCodeOutputType initCodeOutputType =
          method.getRequestStreaming()
              ? InitCodeOutputType.SingleObject
              : InitCodeOutputType.FieldList;
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
    return testCaseViews.build();
  }

  private List<ViewModel> createSmokeTestViews(Model model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> models = ImmutableList.builder();
    SurfaceNamer surfacePackageNamer = new PythonSurfaceNamer(productConfig.getPackageName());
    SurfaceNamer testPackageNamer =
        new PythonSurfaceNamer(surfacePackageNamer.getTestPackageName());
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      ModelTypeTable typeTable = createTypeTable(surfacePackageNamer.getTestPackageName());
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, typeTable, surfacePackageNamer, featureConfig);
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        models.add(createSmokeTestClassView(context, testPackageNamer));
      }
    }
    return models.build();
  }

  private SmokeTestClassView createSmokeTestClassView(
      GapicInterfaceContext context, SurfaceNamer testPackageNamer) {
    String outputPath =
        pathMapper.getOutputPath(context.getInterface(), context.getProductConfig());
    SurfaceNamer namer = context.getNamer();
    String name = namer.getSmokeTestClassName(context.getInterfaceConfig());

    Method method = context.getInterfaceConfig().getSmokeTestConfig().getMethod();
    FlatteningConfig flatteningGroup =
        testCaseTransformer.getSmokeTestFlatteningGroup(
            context.getMethodConfig(method), context.getInterfaceConfig().getSmokeTestConfig());
    GapicMethodContext flattenedMethodContext =
        context.asFlattenedMethodContext(method, flatteningGroup);

    // TODO: we need to remove testCaseView after we switch to use apiMethodView for smoke test
    // testCaseView not in use by Python for smoke test.
    TestCaseView testCaseView = testCaseTransformer.createSmokeTestCaseView(flattenedMethodContext);
    OptionalArrayMethodView apiMethodView =
        createSmokeTestCaseApiMethodView(flattenedMethodContext);

    boolean requireProjectId =
        testCaseTransformer.requireProjectIdInSmokeTest(
            apiMethodView.initCode(), context.getNamer());
    ImportSectionView importSection =
        importSectionTransformer.generateSmokeTestImportSection(context, requireProjectId);

    return SmokeTestClassView.newBuilder()
        .apiSettingsClassName(namer.getApiSettingsClassName(context.getInterfaceConfig()))
        .apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()))
        .apiVariableName(namer.getApiWrapperVariableName(context.getInterfaceConfig()))
        .name(name)
        .outputPath(namer.getSourceFilePath(outputPath, name))
        .templateFileName(SMOKE_TEST_TEMPLATE_FILE)
        .apiMethod(apiMethodView)
        .method(testCaseView)
        .requireProjectId(requireProjectId)
        .fileHeader(
            fileHeaderTransformer.generateFileHeader(
                context.getProductConfig(), importSection, testPackageNamer))
        .build();
  }

  private OptionalArrayMethodView createSmokeTestCaseApiMethodView(GapicMethodContext context) {
    OptionalArrayMethodView initialApiMethodView =
        new DynamicLangApiMethodTransformer(new PythonApiMethodParamTransformer())
            .generateMethod(context);

    OptionalArrayMethodView.Builder apiMethodView = initialApiMethodView.toBuilder();

    InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(
            context, testCaseTransformer.createSmokeTestInitContext(context));
    apiMethodView.initCode(initCodeView);

    return apiMethodView.build();
  }

  private static ModelTypeTable createTypeTable(String packageName) {
    return new ModelTypeTable(
        new PythonTypeTable(packageName), new PythonModelTypeNameConverter(packageName));
  }
}
