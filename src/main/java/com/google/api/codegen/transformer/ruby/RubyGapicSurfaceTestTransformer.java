/* Copyright 2017 Google LLC
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

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.ruby.RubyUtil;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
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
import com.google.api.codegen.viewmodel.testing.ClientInitParamView;
import com.google.api.codegen.viewmodel.testing.ClientTestClassView;
import com.google.api.codegen.viewmodel.testing.ClientTestFileView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.api.codegen.viewmodel.testing.SmokeTestClassView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** A subclass of ModelToViewTransformer which translates model into API smoke tests in Ruby. */
public class RubyGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static String SMOKE_TEST_TEMPLATE_FILE = "ruby/smoke_test.snip";
  private static String UNIT_TEST_TEMPLATE_FILE = "ruby/test.snip";

  private final GapicCodePathMapper unitTestPathMapper;
  private final GapicCodePathMapper smokeTestPathMapper;
  private PackageMetadataConfig packageConfig;
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new RubyImportSectionTransformer());
  private final RubyImportSectionTransformer importSectionTransformer =
      new RubyImportSectionTransformer();
  private final ValueProducer valueProducer = new StandardValueProducer();

  public RubyGapicSurfaceTestTransformer(
      GapicCodePathMapper rubyUnitTestPathMapper,
      GapicCodePathMapper rubySmokeTestPathMapper,
      PackageMetadataConfig packageConfig) {
    this.unitTestPathMapper = rubyUnitTestPathMapper;
    this.smokeTestPathMapper = rubySmokeTestPathMapper;
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(SMOKE_TEST_TEMPLATE_FILE, UNIT_TEST_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    ProtoApiModel apiModel = new ProtoApiModel(model);
    views.addAll(createUnitTestViews(apiModel, productConfig));
    views.addAll(createSmokeTestViews(apiModel, productConfig));
    return views.build();
  }

  ///////////////////////////////////// Unit Test ///////////////////////////////////////

  private List<ClientTestFileView> createUnitTestViews(
      ApiModel model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ClientTestFileView> views = ImmutableList.builder();
    SurfaceNamer namer = new RubySurfaceNamer(productConfig.getPackageName());
    for (InterfaceModel apiInterface : model.getInterfaces()) {
      GapicInterfaceContext context = createContext(apiInterface, productConfig);
      String testClassName = namer.getUnitTestClassName(context.getInterfaceConfig());
      String outputPath =
          unitTestPathMapper.getOutputPath(
              context.getInterfaceModel().getFullName(), productConfig);
      ImportSectionView importSection = importSectionTransformer.generateTestImportSection(context);
      views.add(
          ClientTestFileView.newBuilder()
              .templateFileName(UNIT_TEST_TEMPLATE_FILE)
              .outputPath(namer.getSourceFilePath(outputPath, testClassName))
              .testClass(createUnitTestClassView(context, model.hasMultipleServices()))
              .fileHeader(
                  fileHeaderTransformer.generateFileHeader(productConfig, importSection, namer))
              .apiVersion(packageConfig.apiVersion())
              .build());
    }
    return views.build();
  }

  private ClientTestClassView createUnitTestClassView(
      GapicInterfaceContext context, boolean packageHasMultipleServices) {
    SurfaceNamer namer = context.getNamer();
    String apiSettingsClassName =
        namer.getNotImplementedString(
            "RubyGapicSurfaceTestTransformer.generateUnitTestClassView - apiSettingsClassName");
    String testClassName =
        namer.getNotImplementedString(
            "RubyGapicSurfaceTestTransformer.generateUnitTestClassView - name");

    ImmutableList.Builder<ClientInitParamView> clientInitOptionalParams = ImmutableList.builder();
    if (RubyUtil.hasMajorVersion(context.getProductConfig().getPackageName())) {
      clientInitOptionalParams.add(
          ClientInitParamView.newBuilder()
              .key("version")
              .value(":" + packageConfig.apiVersion())
              .build());
    }
    return ClientTestClassView.newBuilder()
        .apiSettingsClassName(apiSettingsClassName)
        .apiClassName(namer.getFullyQualifiedApiWrapperClassName(context.getInterfaceConfig()))
        .name(testClassName)
        .testCases(createUnitTestCaseViews(context, packageHasMultipleServices))
        .apiHasLongRunningMethods(context.getInterfaceConfig().hasLongRunningOperations())
        .missingDefaultServiceAddress(!context.getInterfaceConfig().hasDefaultServiceAddress())
        .missingDefaultServiceScopes(!context.getInterfaceConfig().hasDefaultServiceScopes())
        .mockCredentialsClassName(namer.getMockCredentialsClassName(context.getInterface()))
        .fullyQualifiedCredentialsClassName(namer.getFullyQualifiedCredentialsClassName())
        .clientInitOptionalParams(clientInitOptionalParams.build())
        .mockServices(ImmutableList.<MockServiceUsageView>of())
        .build();
  }

  private List<TestCaseView> createUnitTestCaseViews(
      GapicInterfaceContext context, boolean packageHasMultipleServices) {
    ImmutableList.Builder<TestCaseView> testCases = ImmutableList.builder();
    for (MethodModel method : context.getSupportedMethods()) {
      GapicMethodContext requestMethodContext =
          context.withNewTypeTable().asRequestMethodContext(method);
      MethodConfig methodConfig = requestMethodContext.getMethodConfig();
      TestCaseTransformer testCaseTransformer =
          new TestCaseTransformer(valueProducer, packageHasMultipleServices);
      TestCaseView testCase =
          testCaseTransformer.createTestCaseView(
              requestMethodContext,
              new SymbolTable(),
              createUnitTestCaseInitCodeContext(context, method),
              getMethodType(methodConfig));
      testCases.add(testCase);
    }
    return testCases.build();
  }

  private InitCodeContext createUnitTestCaseInitCodeContext(
      GapicInterfaceContext context, MethodModel method) {
    MethodContext requestMethodContext = context.asRequestMethodContext(method);
    MethodContext dynamicMethodContext = context.asDynamicMethodContext(method);
    MethodConfig methodConfig = requestMethodContext.getMethodConfig();
    Iterable<FieldConfig> fieldConfigs = methodConfig.getRequiredFieldConfigs();

    InitCodeOutputType outputType =
        method.getRequestStreaming()
            ? InitCodeOutputType.SingleObject
            : InitCodeOutputType.FieldList;

    return InitCodeContext.newBuilder()
        .initObjectType(method.getInputType())
        .suggestedName(Name.from("request"))
        .initFieldConfigStrings(methodConfig.getSampleCodeInitFields())
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(dynamicMethodContext))
        .initFields(FieldConfig.toFieldTypeIterable(fieldConfigs))
        .outputType(outputType)
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

  private List<ViewModel> createSmokeTestViews(ApiModel model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    for (InterfaceModel apiInterface : model.getInterfaces()) {
      GapicInterfaceContext context = createContext(apiInterface, productConfig);
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        views.add(createSmokeTestClassView(context));
      }
    }
    return views.build();
  }

  private SmokeTestClassView createSmokeTestClassView(GapicInterfaceContext context) {
    boolean packageHasMultipleServices = context.getApiModel().hasMultipleServices();
    String outputPath =
        smokeTestPathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    SurfaceNamer namer = context.getNamer();
    String name = namer.getSmokeTestClassName(context.getInterfaceConfig());

    MethodModel method = context.getInterfaceConfig().getSmokeTestConfig().getMethod();
    TestCaseTransformer testCaseTransformer =
        new TestCaseTransformer(valueProducer, packageHasMultipleServices);
    FlatteningConfig flatteningGroup =
        testCaseTransformer.getSmokeTestFlatteningGroup(
            context.getMethodConfig(method), context.getInterfaceConfig().getSmokeTestConfig());
    GapicMethodContext flattenedMethodContext =
        context.asFlattenedMethodContext(method, flatteningGroup);

    SmokeTestClassView.Builder testClass = SmokeTestClassView.newBuilder();
    // TODO: we need to remove testCaseView after we switch to use apiMethodView for smoke test
    // testCaseView not in use by Ruby for smoke test.
    TestCaseView testCaseView = testCaseTransformer.createSmokeTestCaseView(flattenedMethodContext);
    OptionalArrayMethodView apiMethodView =
        createSmokeTestCaseApiMethodView(flattenedMethodContext, packageHasMultipleServices);

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
    testClass.apiVersion(packageConfig.apiVersion());

    FileHeaderView fileHeader =
        fileHeaderTransformer.generateFileHeader(
            context.getProductConfig(),
            importSectionTransformer.generateSmokeTestImportSection(context),
            namer);
    testClass.fileHeader(fileHeader);

    return testClass.build();
  }

  private OptionalArrayMethodView createSmokeTestCaseApiMethodView(
      GapicMethodContext context, boolean packageHasMultipleServices) {
    OptionalArrayMethodView initialApiMethodView =
        new DynamicLangApiMethodTransformer(new RubyApiMethodParamTransformer())
            .generateMethod(context, packageHasMultipleServices);

    OptionalArrayMethodView.Builder apiMethodView = initialApiMethodView.toBuilder();
    TestCaseTransformer testCaseTransformer =
        new TestCaseTransformer(valueProducer, packageHasMultipleServices);

    InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(
            context, testCaseTransformer.createSmokeTestInitContext(context));
    apiMethodView.initCode(initCodeView);

    return apiMethodView.build();
  }

  /////////////////////////////////// General Helpers //////////////////////////////////////

  private GapicInterfaceContext createContext(
      InterfaceModel apiInterface, GapicProductConfig productConfig) {
    return GapicInterfaceContext.create(
        apiInterface,
        productConfig,
        new ModelTypeTable(
            new RubyTypeTable(productConfig.getPackageName()),
            new RubyModelTypeNameConverter(productConfig.getPackageName())),
        new RubySurfaceNamer(productConfig.getPackageName()),
        new RubyFeatureConfig());
  }
}
