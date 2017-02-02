/* Copyright 2016 Google Inc
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
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.ruby.RubyGapicContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import java.util.ArrayList;
import java.util.List;

/** A subclass of ModelToViewTransformer which translates model into API tests in Ruby. */
public class RubyTestsTransformer implements ModelToViewTransformer {
  // Template files
  private static String UNIT_TEST_TEMPLATE_FILE = "ruby/test.snip";

  @Override
  public List<String> getTemplateFileNames() {
    List<String> fileNames = new ArrayList<>();
    fileNames.add(UNIT_TEST_TEMPLATE_FILE);
    return fileNames;
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {

    SurfaceNamer namer = new RubySurfaceNamer(apiConfig.getPackageName());
    RubyGapicContext util = new RubyGapicContext(model, apiConfig);

    List<ViewModel> views = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      views.add(createTestViewModel(service, namer, apiConfig, util));
    }
    return views;
  }

  private ViewModel createTestViewModel(
      final Interface service,
      final SurfaceNamer namer,
      final ApiConfig apiConfig,
      final RubyGapicContext util) {
    final ModelTypeTable typeTable =
        new ModelTypeTable(
            new RubyTypeTable(apiConfig.getPackageName()),
            new RubyModelTypeNameConverter(apiConfig.getPackageName()));

    return new ViewModel() {

      @Override
      public String resourceRoot() {
        return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
      }

      @Override
      public String templateFileName() {
        return UNIT_TEST_TEMPLATE_FILE;
      }

      @Override
      public String outputPath() {
        // We want something like language_service_api_test.rb (i.e. append _test to the name of
        // the generated VKit client).
        return "tests/" + namer.getServiceFileName(service) + "_test.rb";
      }

      public GeneratedLibraryViewModel libraryUnderTest() {
        return new GeneratedLibraryViewModel(service, namer, typeTable);
      }

      public List<UnitTestCaseViewModel> testCases() {
        List<UnitTestCaseViewModel> cases = new ArrayList<>();
        for (final Method method : util.getSupportedMethodsV2(service)) {
          cases.add(new UnitTestCaseViewModel(service, method, typeTable, util));
        }
        return cases;
      }
    };
  }
}
