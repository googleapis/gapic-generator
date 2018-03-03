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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicMockServiceTransformer;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.viewmodel.FileHeaderView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.SmokeTestClassView;
import com.google.common.collect.Lists;
import java.util.List;

/** Responsible for producing sample application related views for Java GAPIC clients */
public class JavaGapicSampleAppTransformer implements ModelToViewTransformer {

  // Reuse the smoke test template since the sample application is almost the same as the smoke test class.
  // (with a few exceptions such as class name)
  private static String SAMPLE_TEMPLATE_FILE = "java/smoke_test.snip";

  private final GapicCodePathMapper pathMapper;
  private final JavaSurfaceTestTransformer testTransformer;
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());

  public JavaGapicSampleAppTransformer(GapicCodePathMapper javaPathMapper) {
    this.pathMapper = javaPathMapper;
    this.testTransformer =
        new JavaSurfaceTestTransformer(
            javaPathMapper,
            new JavaGapicSurfaceTransformer(
                javaPathMapper, PackageMetadataConfig.createDummyPackageMetadataConfig()),
            new GapicMockServiceTransformer(),
            "java/test.snip");
  }

  @Override
  public List<ViewModel> transform(ApiModel model, GapicProductConfig productConfig) {
    InterfaceContext context = getSampleContext(model, productConfig);
    return Lists.newArrayList(createSampleClassView(context));
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Lists.newArrayList(SAMPLE_TEMPLATE_FILE);
  }

  InterfaceContext getSampleContext(ApiModel model, GapicProductConfig productConfig) {
    for (InterfaceModel apiInterface : model.getInterfaces()) {
      InterfaceContext context = testTransformer.createContext(apiInterface, productConfig);
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        // We use the first encountered smoke test config as the sample application
        return testTransformer.createContext(context.getInterfaceModel(), productConfig);
      }
    }

    // Use the first interface as the default one if no smoke test config is found
    InterfaceModel defaultInterface = model.getInterfaces().iterator().next();
    return testTransformer.createContext(defaultInterface, productConfig);
  }

  private ViewModel createSampleClassView(InterfaceContext context) {
    if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
      String outputPath =
          pathMapper.getOutputPath(
              context.getInterfaceModel().getFullName(), context.getProductConfig());
      SurfaceNamer namer = context.getNamer();
      String name = namer.getSampleAppClassName();

      // Most of the fields in the sample view are the same as smoke tests
      SmokeTestClassView.Builder testClass =
          testTransformer.createSmokeTestClassViewBuilder(context);
      testClass.name(name);
      testClass.outputPath(namer.getSourceFilePath(outputPath, name));
      return testClass.build();
    } else {
      // Create a sample application without a method call (only creating the client) if no smoke test config is found.
      return createSmokeTestClassViewNoMethod(context);
    }
  }

  private SmokeTestClassView createSmokeTestClassViewNoMethod(InterfaceContext context) {
    testTransformer.addSmokeTestImports(context);

    SurfaceNamer namer = context.getNamer();
    String outputPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    String name = namer.getSampleAppClassName();
    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);

    return SmokeTestClassView.newBuilder()
        .apiSettingsClassName(namer.getApiSettingsClassName(context.getInterfaceConfig()))
        .apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()))
        .templateFileName(SAMPLE_TEMPLATE_FILE)
        .name(name)
        .outputPath(namer.getSourceFilePath(outputPath, name))
        .fileHeader(fileHeader)
        .build();
  }
}
