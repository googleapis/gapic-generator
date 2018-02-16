/* Copyright 2017 Google LLC
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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.GrpcStreamingConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodModel;
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
import com.google.api.codegen.transformer.GapicMockServiceTransformer;
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
import com.google.common.base.Joiner;
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
  private final MockServiceTransformer mockServiceTransformer = new GapicMockServiceTransformer();
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
  public List<ViewModel> transform(ApiModel model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> models = ImmutableList.builder();
    models.addAll(createUnitTestViews(model, productConfig));
    models.addAll(createSmokeTestViews(model, productConfig));
    return models.build();
  }

  private List<ViewModel> createUnitTestViews(ApiModel model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> models = ImmutableList.builder();
    SurfaceNamer surfacePackageNamer = new PythonSurfaceNamer(productConfig.getPackageName());
    SurfaceNamer testPackageNamer =
        new PythonSurfaceNamer(surfacePackageNamer.getTestPackageName());
    for (InterfaceModel apiInterface : model.getInterfaces()) {
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
              .apiVariableName(surfacePackageNamer.getApiWrapperModuleName())
              .name(testClassName)
              .apiName(
                  surfacePackageNamer.publicClassName(
                      Name.upperCamelKeepUpperAcronyms(
                          context.getInterfaceModel().getSimpleName())))
              .testCases(createTestCaseViews(context))
              .apiHasLongRunningMethods(context.getInterfaceConfig().hasLongRunningOperations())
              .apiHasUnaryUnaryMethod(hasUnaryUnary(context.getInterfaceConfig()))
              .apiHasUnaryStreamingMethod(hasUnaryStreaming(context.getInterfaceConfig()))
              .apiHasStreamingUnaryMethod(hasStreamingUnary(context.getInterfaceConfig()))
              .apiHasStreamingStreamingMethod(hasStreamingStreaming(context.getInterfaceConfig()))
              .missingDefaultServiceAddress(
                  !context.getInterfaceConfig().hasDefaultServiceAddress())
              .missingDefaultServiceScopes(!context.getInterfaceConfig().hasDefaultServiceScopes())
              .mockServices(ImmutableList.<MockServiceUsageView>of())
              .build();

      String version = packageConfig.apiVersion();
      String filename =
          surfacePackageNamer.classFileNameBase(Name.upperCamel(testClassName).join(version))
              + ".py";
      String outputPath =
          Joiner.on(File.separator).join("tests", "unit", "gapic", version, filename);
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

  private boolean hasUnaryUnary(GapicInterfaceConfig interfaceConfig) {
    return interfaceConfig
        .getMethodConfigs()
        .stream()
        .anyMatch(method -> !method.isGrpcStreaming());
  }

  private boolean hasUnaryStreaming(GapicInterfaceConfig interfaceConfig) {
    return interfaceConfig
        .getMethodConfigs()
        .stream()
        .anyMatch(
            method ->
                method.isGrpcStreaming()
                    && method.getGrpcStreaming().getType()
                        == GrpcStreamingConfig.GrpcStreamingType.ServerStreaming);
  }

  private boolean hasStreamingUnary(GapicInterfaceConfig interfaceConfig) {
    return interfaceConfig
        .getMethodConfigs()
        .stream()
        .anyMatch(
            method ->
                method.isGrpcStreaming()
                    && method.getGrpcStreaming().getType()
                        == GrpcStreamingConfig.GrpcStreamingType.ClientStreaming);
  }

  private boolean hasStreamingStreaming(GapicInterfaceConfig interfaceConfig) {
    return interfaceConfig
        .getMethodConfigs()
        .stream()
        .anyMatch(
            method ->
                method.isGrpcStreaming()
                    && method.getGrpcStreaming().getType()
                        == GrpcStreamingConfig.GrpcStreamingType.BidiStreaming);
  }

  private List<TestCaseView> createTestCaseViews(GapicInterfaceContext context) {
    ImmutableList.Builder<TestCaseView> testCaseViews = ImmutableList.builder();

    // There are situations where a type name has a collision with another type name found in
    // a later seen test case. When this occurs the first seen type name will become invalid.
    // Run twice in order to fully disambiguate these types.
    // TODO(landrito): figure out a way to guarantee that python typenames are not bound to a
    // certain string until all of the types have been disambiguated.
    for (int i = 0; i < 2; ++i) {
      testCaseViews = ImmutableList.builder();
      SymbolTable testNameTable = new SymbolTable();
      for (MethodModel method : context.getSupportedMethods()) {
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
    }
    return testCaseViews.build();
  }

  private List<ViewModel> createSmokeTestViews(ApiModel model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> models = ImmutableList.builder();
    SurfaceNamer surfacePackageNamer = new PythonSurfaceNamer(productConfig.getPackageName());
    SurfaceNamer testPackageNamer =
        new PythonSurfaceNamer(surfacePackageNamer.getTestPackageName());
    for (InterfaceModel apiInterface : model.getInterfaces()) {
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
    SurfaceNamer namer = context.getNamer();
    String name = namer.getSmokeTestClassName(context.getInterfaceConfig());

    String version = packageConfig.apiVersion();
    String filename = namer.classFileNameBase(Name.upperCamel(name).join(version)) + ".py";
    String outputPath =
        Joiner.on(File.separator).join("tests", "system", "gapic", version, filename);

    MethodModel method = context.getInterfaceConfig().getSmokeTestConfig().getMethod();
    FlatteningConfig flatteningGroup =
        testCaseTransformer.getSmokeTestFlatteningGroup(
            context.getMethodConfig(method), context.getInterfaceConfig().getSmokeTestConfig());
    GapicMethodContext flattenedMethodContext =
        context.asFlattenedMethodContext(method, flatteningGroup);
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
        .apiVariableName(namer.getApiWrapperModuleName())
        .name(name)
        .outputPath(outputPath)
        .templateFileName(SMOKE_TEST_TEMPLATE_FILE)
        .apiMethod(apiMethodView)
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
