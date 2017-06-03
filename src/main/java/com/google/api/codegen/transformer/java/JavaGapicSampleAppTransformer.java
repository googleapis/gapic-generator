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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.*;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.SmokeTestClassView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.Lists;
import java.util.List;

/** Responsible for producing sample application related views for Java GAPIC clients */
public class JavaGapicSampleAppTransformer implements ModelToViewTransformer {

  // Reuse the smoke test template since the sample application is almost the same as the smoke test class.
  // (with a few exceptions such as class name)
  private static String SMOKE_TEST_TEMPLATE_FILE = "java/smoke_test.snip";

  private final GapicCodePathMapper pathMapper;
  private final JavaGapicSurfaceTestTransformer testTransformer;

  public JavaGapicSampleAppTransformer(GapicCodePathMapper javaPathMapper) {
    this.pathMapper = javaPathMapper;
    this.testTransformer = new JavaGapicSurfaceTestTransformer(javaPathMapper);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    GapicInterfaceContext context = getSampleContext(model, productConfig);
    if (context == null) {
      throw new IllegalArgumentException(
          "At lease one smoke test config is required for the sample application "
              + "generation but zero is found.");
    }
    return Lists.newArrayList(createSampleClassView(context));
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Lists.newArrayList(SMOKE_TEST_TEMPLATE_FILE);
  }

  public GapicInterfaceContext getSampleContext(Model model, GapicProductConfig productConfig) {
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      GapicInterfaceContext context = testTransformer.createContext(apiInterface, productConfig);
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        // We use the first encountered smoke test config as the sample application
        return testTransformer.createContext(apiInterface, productConfig);
      }
    }
    return null;
  }

  private ViewModel createSampleClassView(GapicInterfaceContext context) {
    String outputPath =
        pathMapper.getOutputPath(context.getInterface(), context.getProductConfig());
    SurfaceNamer namer = context.getNamer();
    String name = namer.getSampleAppClassName();

    // Most of the fields in the sample view are the same as smoke tests
    SmokeTestClassView.Builder testClass = testTransformer.createSmokeTestClassViewBuilder(context);

    testClass.name(name);
    testClass.outputPath(namer.getSourceFilePath(outputPath, name));
    return testClass.build();
  }
}
