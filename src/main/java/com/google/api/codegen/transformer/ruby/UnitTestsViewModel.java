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

import com.google.api.codegen.GapicContext;
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Method;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the generated unit test suite for a specific library. It represents a generated
 * tests file.
 */
public class UnitTestsViewModel implements ViewModel {
  private final String templateFile; // the language-specific snippet file.

  private final InterfaceConfig interfaceConfig;
  private final SurfaceNamer namer;
  private final ModelTypeTable typeTable;
  private final GapicContext context;
  private final ApiConfig apiConfig;

  public UnitTestsViewModel(
      InterfaceConfig interfaceConfig,
      SurfaceNamer namer,
      ModelTypeTable typeTable,
      GapicContext context,
      String templateFile,
      ApiConfig apiConfig) {
    this.interfaceConfig = Preconditions.checkNotNull(interfaceConfig);
    this.namer = Preconditions.checkNotNull(namer);
    this.typeTable = Preconditions.checkNotNull(typeTable);
    this.context = Preconditions.checkNotNull(context);
    this.templateFile = Preconditions.checkNotNull(templateFile);
    this.apiConfig = Preconditions.checkNotNull(apiConfig);
  }

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public String templateFileName() {
    return templateFile;
  }

  @Override
  public String outputPath() {
    // TODO(jcanizales): This is Ruby-specific. Move it inside the SurfaceNamer.

    // We want something like language_service_api_test.rb (i.e. append _test to the name of
    // the generated VKit client).
    return "tests/" + namer.getServiceFileName(interfaceConfig) + "_test.rb";
  }

  public GeneratedLibraryViewModel libraryUnderTest() {
    return new GeneratedLibraryViewModel(interfaceConfig, namer, typeTable);
  }

  public List<UnitTestCaseViewModel> testCases() {
    List<UnitTestCaseViewModel> cases = new ArrayList<>();
    for (final Method method : context.getSupportedMethodsV2(interfaceConfig.getInterface())) {
      cases.add(new UnitTestCaseViewModel(interfaceConfig, method, typeTable, apiConfig));
    }
    return cases;
  }
}
