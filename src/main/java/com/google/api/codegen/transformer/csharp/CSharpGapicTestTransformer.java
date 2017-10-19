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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoMethodModel;
import com.google.api.codegen.config.SmokeTestConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.testing.StandardValueProducer;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.api.codegen.viewmodel.testing.SmokeTestClassView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSharpGapicTestTransformer implements ModelToViewTransformer {

  private static final String SMOKETEST_SNIPPETS_TEMPLATE_FILENAME = "csharp/gapic_smoketest.snip";
  private static final String SMOKETEST_CSPROJ_TEMPLATE_FILENAME =
      "csharp/gapic_smoketest_csproj.snip";

  private final GapicCodePathMapper pathMapper;
  private final PackageMetadataConfig packageMetadataConfig;
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());
  private final ValueProducer valueProducer = new StandardValueProducer();
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);
  private final CSharpCommonTransformer csharpCommonTransformer = new CSharpCommonTransformer();
  private final PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();

  public CSharpGapicTestTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageMetadataConfig) {
    this.pathMapper = pathMapper;
    this.packageMetadataConfig = packageMetadataConfig;
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new CSharpSurfaceNamer(productConfig.getPackageName());

    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface,
              productConfig,
              csharpCommonTransformer.createTypeTable(namer.getPackageName()),
              namer,
              new CSharpFeatureConfig());
      csharpCommonTransformer.addCommonImports(context);
      SmokeTestClassView smokeTests = generateSmokeTest(context);
      if (smokeTests != null) {
        surfaceDocs.add(smokeTests);
        surfaceDocs.add(generatePackageMetadataView(context));
      }
    }

    return surfaceDocs;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(SMOKETEST_SNIPPETS_TEMPLATE_FILENAME, SMOKETEST_CSPROJ_TEMPLATE_FILENAME);
  }

  private SmokeTestClassView generateSmokeTest(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    String name = namer.getSmokeTestClassName(context.getInterfaceConfig());
    SmokeTestClassView.Builder smokeTestBuilder = SmokeTestClassView.newBuilder();

    SmokeTestConfig smokeTestConfig = context.getInterfaceConfig().getSmokeTestConfig();
    if (smokeTestConfig == null) {
      return null;
    }
    ProtoMethodModel method = new ProtoMethodModel(smokeTestConfig.getMethod());
    FlatteningConfig flatteningGroup =
        testCaseTransformer.getSmokeTestFlatteningGroup(
            context.getMethodConfig(method), context.getInterfaceConfig().getSmokeTestConfig());
    GapicMethodContext methodContext = context.asFlattenedMethodContext(method, flatteningGroup);

    smokeTestBuilder.templateFileName(SMOKETEST_SNIPPETS_TEMPLATE_FILENAME);
    String outputPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), context.getProductConfig());
    smokeTestBuilder.outputPath(outputPath + File.separator + name + ".g.cs");
    smokeTestBuilder.name(name);
    smokeTestBuilder.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    smokeTestBuilder.apiSettingsClassName(
        namer.getApiSettingsClassName(context.getInterfaceConfig()));

    // TODO: we need to remove testCaseView after we switch to use apiMethodView for smoke test
    TestCaseView testCaseView = testCaseTransformer.createSmokeTestCaseView(methodContext);
    smokeTestBuilder.method(testCaseView);
    smokeTestBuilder.requireProjectId(
        testCaseTransformer.requireProjectIdInSmokeTest(testCaseView.initCode(), namer));

    // must be done as the last step to catch all imports
    smokeTestBuilder.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return smokeTestBuilder.build();
  }

  private PackageMetadataView generatePackageMetadataView(GapicInterfaceContext context) {
    GapicProductConfig productConfig = context.getProductConfig();
    String outputPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), productConfig);
    return metadataTransformer
        .generateMetadataView(
            packageMetadataConfig,
            context.getApiModel(),
            SMOKETEST_CSPROJ_TEMPLATE_FILENAME,
            outputPath + File.separator + productConfig.getPackageName() + ".smoketest.csproj",
            TargetLanguage.CSHARP)
        .build();
  }
}
