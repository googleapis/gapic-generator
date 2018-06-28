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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.SmokeTestConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ParamWithSimpleDoc;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.csharp.CSharpAliasMode;
import com.google.api.codegen.util.testing.StandardValueProducer;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.SmokeTestClassView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* Transforms a ProtoApiModel into the smoke tests of an API for C#. */
public class CSharpGapicSmokeTestTransformer implements ModelToViewTransformer<ProtoApiModel> {

  private static final String SMOKETEST_SNIPPETS_TEMPLATE_FILENAME = "csharp/gapic_smoketest.snip";

  private static final CSharpAliasMode ALIAS_MODE = CSharpAliasMode.MessagesOnly;

  private final GapicCodePathMapper pathMapper;
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());
  private final ValueProducer valueProducer = new StandardValueProducer();
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);
  private final CSharpCommonTransformer csharpCommonTransformer = new CSharpCommonTransformer();
  private final StaticLangApiMethodTransformer apiMethodTransformer =
      new CSharpApiMethodTransformer();

  public CSharpGapicSmokeTestTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new CSharpSurfaceNamer(productConfig.getPackageName(), ALIAS_MODE);

    for (InterfaceModel apiInterface : model.getInterfaces()) {
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface,
              productConfig,
              csharpCommonTransformer.createTypeTable(namer.getPackageName(), ALIAS_MODE),
              namer,
              new CSharpFeatureConfig());
      csharpCommonTransformer.addCommonImports(context);
      SmokeTestClassView smokeTests = generateSmokeTest(context);
      if (smokeTests != null) {
        surfaceDocs.add(smokeTests);
      }
    }

    return surfaceDocs;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(SMOKETEST_SNIPPETS_TEMPLATE_FILENAME);
  }

  private SmokeTestClassView generateSmokeTest(GapicInterfaceContext context) {
    SmokeTestClassView.Builder builder = generateSmokeTestViewBuilder(context);
    if (builder == null) {
      return null;
    }
    builder.templateFileName(SMOKETEST_SNIPPETS_TEMPLATE_FILENAME);
    SurfaceNamer namer = context.getNamer();
    String name = namer.getSmokeTestClassName(context.getInterfaceConfig());
    String outputPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), context.getProductConfig());
    builder.outputPath(outputPath + File.separator + name + ".g.cs");
    return builder.build();
  }

  private SmokeTestClassView.Builder generateSmokeTestViewBuilder(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    String name = namer.getSmokeTestClassName(context.getInterfaceConfig());
    SmokeTestClassView.Builder smokeTestBuilder = SmokeTestClassView.newBuilder();

    SmokeTestConfig smokeTestConfig = context.getInterfaceConfig().getSmokeTestConfig();
    if (smokeTestConfig == null) {
      return null;
    }
    MethodModel method = smokeTestConfig.getMethod();
    FlatteningConfig flatteningGroup =
        testCaseTransformer.getSmokeTestFlatteningGroup(
            context.getMethodConfig(method), context.getInterfaceConfig().getSmokeTestConfig());
    GapicMethodContext methodContext = context.asFlattenedMethodContext(method, flatteningGroup);

    smokeTestBuilder.name(name);
    smokeTestBuilder.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    smokeTestBuilder.apiSettingsClassName(
        namer.getApiSettingsClassName(context.getInterfaceConfig()));

    StaticLangApiMethodView apiMethodView =
        createSmokeTestCaseApiMethodView(context, methodContext);
    smokeTestBuilder.apiMethod(apiMethodView);
    smokeTestBuilder.requireProjectId(
        testCaseTransformer.requireProjectIdInSmokeTest(apiMethodView.initCode(), namer));

    // must be done as the last step to catch all imports
    smokeTestBuilder.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return smokeTestBuilder;
  }

  private StaticLangApiMethodView createSmokeTestCaseApiMethodView(
      GapicInterfaceContext context, MethodContext methodContext) {
    SurfaceNamer namer = context.getNamer();
    MethodConfig methodConfig = methodContext.getMethodConfig();
    StaticLangApiMethodView.Builder apiMethodView;
    if (methodConfig.isPageStreaming()) {
      apiMethodView = apiMethodTransformer.generatePagedFlattenedMethod(methodContext).toBuilder();
      FieldConfig resourceFieldConfig =
          methodContext.getMethodConfig().getPageStreaming().getResourcesFieldConfig();
      String callerResponseTypeName =
          namer.getAndSaveCallerPagedResponseTypeName(methodContext, resourceFieldConfig);
      apiMethodView.responseTypeName(callerResponseTypeName);
    } else if (methodConfig.isLongRunningOperation()) {
      ArrayList<ParamWithSimpleDoc> emptyParams = new ArrayList<ParamWithSimpleDoc>();
      apiMethodView =
          apiMethodTransformer
              .generateOperationFlattenedMethod(methodContext, emptyParams)
              .toBuilder();
    } else {
      apiMethodView = apiMethodTransformer.generateFlattenedMethod(methodContext).toBuilder();
    }
    InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(
            methodContext, testCaseTransformer.createSmokeTestInitContext(methodContext));
    apiMethodView.initCode(initCodeView);
    return apiMethodView.build();
  }
}
