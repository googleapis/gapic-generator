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
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.util.testing.StandardValueProducer;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.FileHeaderView;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.SmokeTestClassView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/** A subclass of ModelToViewTransformer which translates model into API smoke tests in Ruby. */
public class RubyGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static String SMOKE_TEST_TEMPLATE_FILE = "ruby/smoke_test.snip";

  private final GapicCodePathMapper pathMapper;
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new RubyImportSectionTransformer());
  private final ValueProducer valueProducer = new StandardValueProducer();
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);

  public RubyGapicSurfaceTestTransformer(GapicCodePathMapper rubyPathMapper) {
    this.pathMapper = rubyPathMapper;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(SMOKE_TEST_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> views = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context = createContext(service, apiConfig);
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        views.add(createSmokeTestClassView(context));
      }
    }

    return views;
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
